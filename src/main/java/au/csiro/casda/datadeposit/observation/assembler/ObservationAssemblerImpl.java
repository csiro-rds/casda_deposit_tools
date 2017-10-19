package au.csiro.casda.datadeposit.observation.assembler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.csiro.casda.datadeposit.ChildDepositableArtefact;
import au.csiro.casda.datadeposit.DepositStateImpl;
import au.csiro.casda.datadeposit.DepositState.Type;
import au.csiro.casda.datadeposit.observation.ObservationParser.MalformedFileException;
import au.csiro.casda.datadeposit.observation.jaxb.Evaluation;
import au.csiro.casda.datadeposit.observation.jaxb.Scans;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.parser.XmlObservation;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.observation.FitsObject;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.MeasurementSet;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Project;
import au.csiro.casda.entity.observation.Scan;
import au.csiro.casda.entity.observation.Spectrum;
import au.csiro.casda.entity.observation.Thumbnail;
import au.csiro.util.AstroConversion;

/*
 * #%L
 * CSIRO ASKAP Science Data Archive
 * %%
 * Copyright (C) 2015 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * Implementation of ObservationAssembler.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class ObservationAssemblerImpl implements ObservationAssembler
{
    private static final Pattern FIELD_CENTRE_PATTERN = Pattern
            .compile("\\[\\s*((\\+|\\-)?\\d*(\\.\\d+)?)\\s*,\\s*((\\+|\\-)?\\d*(\\.\\d+)?)\\s*\\]");
    private static final int FIELD_CENTRE_PATTERN_X_GROUP = 1;
    private static final int FIELD_CENTRE_PATTERN_Y_GROUP = 4;

    private Logger logger = LoggerFactory.getLogger(ObservationAssemblerImpl.class);

    private ProjectRepository projectRepository;

    private ObservationRepository observationRepository;

    /**
     * Constructs an ObservationAssemblerImpl that will use the given repositories for access to persistent Projects and
     * Observations.
     * 
     * @param projectRepository
     *            a ProjectRepository
     * @param observationRepository
     *            an ObservationRepository
     */
    public ObservationAssemblerImpl(ProjectRepository projectRepository, ObservationRepository observationRepository)
    {
        this.projectRepository = projectRepository;
        this.observationRepository = observationRepository;       
    }


    @Override
    public Observation updateObservationFromParsedObservation(XmlObservation dataset, int fileIdMaxSize,
            File parentFilePath, boolean cubeletEnabled)
            throws MalformedFileException
    {
        Observation observation = observationRepository.findBySbid(Integer.parseInt(dataset.getIdentity().getSbid()));
        if (observation == null)
        {
            throw new IllegalArgumentException("Asked to redeposit an observation that does not exist");
        }
        if (observation.getDepositStateType() != Type.DEPOSITED)
        {
            throw new IllegalArgumentException("Asked to redeposit an observation that has not finished depositing");
        }
        
        // Reset the observation to undeposited
        observation.setDepositState(new DepositStateImpl(Type.UNDEPOSITED, observation));
        observation.setRedepositStarted(new DateTime(System.currentTimeMillis(), DateTimeZone.UTC));
        // Ensure the updated observation.xml gets archived too, 
        // Set it through failed to bump up the failure count to ensure it is seen as a new job. 
        ChildDepositableArtefact metadataFile = observation.getObservationMetadataFileDepositable();
        metadataFile.setDepositState(new DepositStateImpl(Type.FAILED, metadataFile));
        metadataFile.setDepositState(new DepositStateImpl(Type.UNDEPOSITED, metadataFile));

        return populateObservation(dataset, fileIdMaxSize, parentFilePath, cubeletEnabled, observation);
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation createObservationFromParsedObservation(XmlObservation dataset, int fileIdMaxSize,
            File parentFilePath, boolean cubeletEnabled)
            throws ObservationAlreadyExistsException, MalformedFileException
    {

        logger.debug("Started assembling JPA classes from JAXB data structure.");

        String sbid = dataset.getIdentity().getSbid();
        Observation observation = observationRepository.findBySbid(Integer.parseInt(sbid));
        if (observation != null)
        {
            throw new ObservationAlreadyExistsException(sbid);
        }
        
        observation = new Observation();
        observation.setSbid(Integer.parseInt(dataset.getIdentity().getSbid()));
        
        return populateObservation(dataset, fileIdMaxSize, parentFilePath, cubeletEnabled, observation);
    }
    
    private Observation populateObservation(XmlObservation dataset, int fileIdMaxSize,
            File parentFilePath, boolean cubeletEnabled, Observation observation)
            throws MalformedFileException
    {
        Map<String, Project> projectsByOpalCode = new HashMap<>();

        observation.setObsStart(new DateTime(dataset.getObservation().getObsstart().asTimestamp().getTime(),
                DateTimeZone.UTC));
        observation.setObsEnd(new DateTime(dataset.getObservation().getObsend().asTimestamp().getTime(),
                DateTimeZone.UTC));

        observation.setObsStartMjd(dataset.getObservation().getObsstart().asModifiedJulianDate());
        observation.setObsEndMjd(dataset.getObservation().getObsend().asModifiedJulianDate());

        if (StringUtils.isNotBlank(dataset.getIdentity().getObsprogram()))
        {
            observation.setObsProgram(dataset.getIdentity().getObsprogram());
        }
        
        if (dataset.getIdentity().getSbids()  != null && 
        		CollectionUtils.isNotEmpty(dataset.getIdentity().getSbids().getSbid()))
        {
    		observation.setSbids(dataset.getIdentity().getSbids().getSbid()
    					.stream().distinct().map(s -> Integer.parseInt(s))
    					.collect(Collectors.toList()));
    		observation.getSbids().remove(observation.getSbid());
        }

        if (StringUtils.isNotBlank(dataset.getIdentity().getTelescope()))
        {
            observation.setTelescope(dataset.getIdentity().getTelescope());
        }

        List<EncapsulationFile> existingEncapsulationFiles = new ArrayList<>(observation.getEncapsulationFiles());
        int numPrevEncapsPairs = getNextEncapsPairNum(existingEncapsulationFiles);
        // assemble the project and related objects
        List<Catalogue> existingCatalogues = new ArrayList<>(observation.getCatalogues());
        for (au.csiro.casda.datadeposit.observation.jaxb.Catalogue jaxbCatalogue : dataset.getCatalogues())
        {
            Catalogue catalogue = createCatalogue(observation, jaxbCatalogue, projectsByOpalCode);
            if (!hasCatalogue(existingCatalogues, catalogue))
            {
                observation.addCatalogue(catalogue);
            }
        }
        
        List<ImageCube> existingImages = new ArrayList<>(observation.getImageCubes());
        List<Spectrum> existingSpectra = new ArrayList<>(observation.getSpectra());
        List<MomentMap> existingMomentMaps = new ArrayList<>(observation.getMomentMaps());
        List<Cubelet> existingCubelets = new ArrayList<>(observation.getCubelets());
        for (au.csiro.casda.datadeposit.observation.jaxb.Image jaxbImage : dataset.getImages())
        {
            ImageCube imageCube = createImageCube(jaxbImage, projectsByOpalCode);
            ImageCube existingImageCube = (ImageCube) getExistingFitsObject(existingImages, imageCube);
            if (existingImageCube == null)
            {
                observation.addImageCube(imageCube);
            }
            else
            {
                if (imageCube.getLargeThumbnail() != null && existingImageCube.getLargeThumbnail() == null)
                {
                    existingImageCube.setLargeThumbnail(imageCube.getLargeThumbnail());
                    existingImageCube.getLargeThumbnail().setParent(observation);
                }
                if (imageCube.getSmallThumbnail() != null && existingImageCube.getSmallThumbnail() == null)
                {
                    existingImageCube.setSmallThumbnail(imageCube.getSmallThumbnail());
                    existingImageCube.getSmallThumbnail().setParent(observation);
                }

                // Make sure we use the database object for future database operations
                imageCube = existingImageCube;
            }
            if (jaxbImage.getSpectra() != null)
            {
                for (au.csiro.casda.datadeposit.observation.jaxb.Spectrum jaxbSpectrum : jaxbImage.getSpectra()
                        .getSpectrum())
                {
                    for (Spectrum spectrum : createSpectra(jaxbSpectrum, parentFilePath, imageCube.getProject(),
                            numPrevEncapsPairs, observation))
                    {
                        if (getExistingFitsObject(existingSpectra, spectrum) == null)
                        {
                            imageCube.addSpectra(spectrum);
                            observation.addSpectra(spectrum);
                            addEncapsFiles(spectrum, observation);
                        }
                        else
                        {
                            removeFromEncapsFile(spectrum);
                        }
                    }
                    numPrevEncapsPairs++;
                }
            }
            if (jaxbImage.getMomentMaps() != null)
            {
                for (au.csiro.casda.datadeposit.observation.jaxb.MomentMap jaxbMomentMap : jaxbImage.getMomentMaps()
                        .getMomentMap())
                {
                    for (MomentMap momentMap : createMomentMap(jaxbMomentMap, parentFilePath, imageCube.getProject(),
                            numPrevEncapsPairs, observation))
                    {
                        if (getExistingFitsObject(existingMomentMaps, momentMap) == null)
                        {
                            imageCube.addMomentMap(momentMap);
                            observation.addMomentMap(momentMap);
                            addEncapsFiles(momentMap, observation);
                        }
                        else
                        {
                            removeFromEncapsFile(momentMap);
                        }
                    }
                    numPrevEncapsPairs++;
                }
            }
            if (jaxbImage.getCubelets() != null)
            {
                for (au.csiro.casda.datadeposit.observation.jaxb.Cubelet jaxbCubelet : jaxbImage.getCubelets()
                        .getCubelet())
                {
                    for (Cubelet cubelet : createCubelet(jaxbCubelet, parentFilePath, imageCube.getProject(),
                            numPrevEncapsPairs, observation))
                    {
                        if (getExistingFitsObject(existingCubelets, cubelet) == null)
                        {
                            imageCube.addCubelet(cubelet);
                            observation.addCubelet(cubelet);
                            addEncapsFiles(cubelet, observation);
                        }
                        else
                        {
                            removeFromEncapsFile(cubelet);
                        }
                    }
                    numPrevEncapsPairs++;
                }
            }
        }
        List<MeasurementSet> existingMeasurementSets = new ArrayList<>(observation.getMeasurementSets());
        for (au.csiro.casda.datadeposit.observation.jaxb.MeasurementSet jaxbMeasurementSet : dataset
                .getMeasurementSets())
        {
            MeasurementSet measurementSet = createMeasurementSet(jaxbMeasurementSet, projectsByOpalCode);
            if (!hasMeasurementSet(existingMeasurementSets, measurementSet))
            {
                observation.addMeasurementSet(measurementSet);
            }

        }
        List<EvaluationFile> existingEvaluationFiles = new ArrayList<>(observation.getEvaluationFiles());
        int fileNum = numPrevEncapsPairs + 1;

        Set<String> evalProjects = new HashSet<String>();
        for(Evaluation eval : dataset.getEvaluationFiles())
        {
        	evalProjects.add(eval.getProject());
        }
        
        for(String project : evalProjects)
        {
            EncapsulationFile evalEncapsFile = createEncapsulationObject("", project + "-eval", fileNum);
            for (Evaluation jaxbFile : dataset.getEvaluationFiles())
            {
            	if(jaxbFile.getProject().equals(project))
            	{
                    EvaluationFile evaluationFile = createEvaluationFile(jaxbFile, evalEncapsFile, projectsByOpalCode);
                    if (!hasEvaluationFile(existingEvaluationFiles, evaluationFile))
                    {
                        observation.addEvaluationFile(evaluationFile);
                        addEncapsFiles(evaluationFile, observation);
                    }
                    else
                    {
                        removeFromEncapsFile(evaluationFile);
                    }
            	}
            }
        }

        for(ChildDepositableArtefact observArtefact : observation.getDepositableArtefacts())
        {
        	if(observArtefact.getFileId().length() > fileIdMaxSize)
        	{
        		throw new MalformedFileException("File ID longer then "	+ fileIdMaxSize + " char: " 
        				+ observArtefact.getFileId().length() + " " + observArtefact.getFileId());
        	}
        }

        if (!cubeletEnabled && !observation.getCubelets().isEmpty())
        {
            throw new MalformedFileException(observation.getMomentMaps().size() + " cubelet file(s) were found, "
                    + "however cubelets are not yet supported in this CASDA environment.");
        }
        
        logger.debug("Finished assembling JPA classes.");

        return observation;
    }

    private void removeFromEncapsFile(Spectrum spectrum)
    {
        EncapsulationFile encapsFile = spectrum.getEncapsulationFile();
        if (encapsFile != null)
        {
            encapsFile.removeSpectrum(spectrum);
        }
        Thumbnail thumbnail = spectrum.getThumbnail();
        if (thumbnail != null)
        {
            encapsFile = thumbnail.getEncapsulationFile();
            if (encapsFile != null)
            {
                encapsFile.removeThumbnail(thumbnail);
            }
        }
    }

    private void removeFromEncapsFile(MomentMap momentMap)
    {
        EncapsulationFile encapsFile = momentMap.getEncapsulationFile();
        if (encapsFile != null)
        {
            encapsFile.removeMomentMap(momentMap);
        }
        Thumbnail thumbnail = momentMap.getThumbnail();
        if (thumbnail != null)
        {
            encapsFile = thumbnail.getEncapsulationFile();
            if (encapsFile != null)
            {
                encapsFile.removeThumbnail(thumbnail);
            }
        }
    }
    
    private void removeFromEncapsFile(Cubelet cubelet)
    {
        EncapsulationFile encapsFile = cubelet.getEncapsulationFile();
        if (encapsFile != null)
        {
            encapsFile.removeCubelet(cubelet);
        }
        Thumbnail thumbnail = cubelet.getThumbnail();
        if (thumbnail != null)
        {
            encapsFile = thumbnail.getEncapsulationFile();
            if (encapsFile != null)
            {
                encapsFile.removeThumbnail(thumbnail);
            }
        }
    }

    private void removeFromEncapsFile(EvaluationFile evaluationFile)
    {
        EncapsulationFile encapsFile = evaluationFile.getEncapsulationFile();
        if (encapsFile != null)
        {
            encapsFile.removeEvaluationFile(evaluationFile);
        }
    }

    /**
     * Ensure that any encapsulation file associated with the FitsObject is included in the observation's list of
     * encapsulation files. 
     * 
     * @param fitsObject The potentially encapsulated object.
     * @param observation The observation to be updated.
     */
    private void addEncapsFiles(FitsObject fitsObject, Observation observation)
    {
        EncapsulationFile encapsFile = null;
        Thumbnail thumbnail = null;
        if (fitsObject instanceof Spectrum)
        {
            Spectrum spectrum = (Spectrum) fitsObject;
            encapsFile = spectrum.getEncapsulationFile();
            thumbnail = spectrum.getThumbnail();
        }
        else if (fitsObject instanceof MomentMap)
        {
            MomentMap momentMap = (MomentMap) fitsObject;
            encapsFile = momentMap.getEncapsulationFile();
            thumbnail = momentMap.getThumbnail();
        }
        else if (fitsObject instanceof Cubelet)
        {
        	Cubelet cubelet = (Cubelet) fitsObject;
            encapsFile = cubelet.getEncapsulationFile();
            thumbnail = cubelet.getThumbnail();
        }
        
        if (encapsFile != null && !observation.getEncapsulationFiles().contains(encapsFile))
        {
            observation.addEncapsulationFile(encapsFile);
        }
        
        if (thumbnail != null)
        {
            encapsFile = thumbnail.getEncapsulationFile();
            if (encapsFile != null && !observation.getEncapsulationFiles().contains(encapsFile))
            {
                observation.addEncapsulationFile(encapsFile);
            }
        }
    }

    /**
     * Ensure that any encapsulation file associated with the FitsObject is included in the observation's list of
     * encapsulation files. 
     * 
     * @param fitsObject The potentially encapsulated object.
     * @param observation The observation to be updated.
     */
    private void addEncapsFiles(EvaluationFile evaluationFile, Observation observation)
    {
    	EncapsulationFile encapsFile = null;
    	encapsFile = evaluationFile.getEncapsulationFile();

        if (encapsFile != null && !observation.getEncapsulationFiles().contains(encapsFile))
        {
            observation.addEncapsulationFile(encapsFile);
        }
    }
    
    /**
     * Calculate the pair number (which goes into the counter in the name) where a new set of encapsulations can start
     * from. This will be 0 for a new observation and will be based on the maximum encapsulation file number for
     * observations which have existing encapsulations.
     * 
     * @param existingEncapsulationFiles The list of encapsulation files
     * @return The pair number to start at.
     */
    int getNextEncapsPairNum(List<EncapsulationFile> existingEncapsulationFiles)
    {
        if (existingEncapsulationFiles.isEmpty())
        {
            return 0;
        }
        int maxEncapsId = 0;
        
        Pattern pattern = Pattern.compile("encaps-.*-([0-9]+)\\.tar");
        for (EncapsulationFile encapsulationFile : existingEncapsulationFiles)
        {
            Matcher matcher = pattern.matcher(encapsulationFile.getFilename());
            matcher.matches();
            String group = matcher.group(1);
            int encapsId = Integer.parseInt(group);
            if (encapsId > maxEncapsId)
            {
                maxEncapsId = encapsId;
            }
        }
        return (maxEncapsId+1)/2;
    }


    private boolean hasCatalogue(List<Catalogue> existingCatalogues, Catalogue catalogue)
    {
        for (Catalogue existing : existingCatalogues)
        {
            if (existing.getCatalogueType() == catalogue.getCatalogueType()
                    && existing.getProject().getOpalCode().equals(catalogue.getProject().getOpalCode())
                    && existing.getFilename().equals(catalogue.getFilename()))
            {
                return true;
            }
        }
        return false;
    }

    private FitsObject getExistingFitsObject(List<? extends FitsObject> existingFitsObjects, FitsObject fitsObject)
    {
        for (FitsObject existing : existingFitsObjects)
        {
            if (existing.getFilename().equals(fitsObject.getFilename()))
            {
                return existing;
            }
        }
        return null;
    }

    private boolean hasMeasurementSet(List<MeasurementSet> existingMeasurementSets, MeasurementSet measurementSet)
    {
        for (MeasurementSet existing : existingMeasurementSets)
        {
            if (existing.getFilename().equals(measurementSet.getFilename()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasEvaluationFile(List<EvaluationFile> existingEvaluationFiles, EvaluationFile evaluationFile)
    {
        for (EvaluationFile existing : existingEvaluationFiles)
        {
            if (existing.getFilename().equals(evaluationFile.getFilename()))
            {
                return true;
            }
        }
        return false;
    }

    private Catalogue createCatalogue(Observation observation,
            au.csiro.casda.datadeposit.observation.jaxb.Catalogue parsedCatalogue,
            Map<String, Project> projectsByOpalCode)
    {
        Catalogue catalogue = new Catalogue();
        catalogue.setProject(getProject(parsedCatalogue.getProject(), projectsByOpalCode));
        catalogue.setFormat(parsedCatalogue.getFormat());
        catalogue.setCatalogueType(CatalogueType.valueOf(parsedCatalogue.getType().toString()));
        catalogue.setFilename(parsedCatalogue.getFilename());
        catalogue.setTimeObs(observation.getObsStart());
        catalogue.setTimeObsMjd(observation.getObsStartMjd());
        return catalogue;
    }

    private ImageCube createImageCube(au.csiro.casda.datadeposit.observation.jaxb.Image parserImageCube,
            Map<String, Project> projectsByOpalCode)
    {
        ImageCube imageCube = new ImageCube();
        imageCube.setProject(getProject(parserImageCube.getProject(), projectsByOpalCode));
        imageCube.setFormat(parserImageCube.getFormat());
        imageCube.setFilename(parserImageCube.getFilename());
        imageCube.setType(parserImageCube.getType());
        imageCube.setLargeThumbnail(createThumbnail(parserImageCube.getThumbnailLarge()));
        imageCube.setSmallThumbnail(createThumbnail(parserImageCube.getThumbnailSmall()));
        return imageCube;
    }
    
    private Thumbnail createThumbnail(String fileName)
    {
        if (StringUtils.isBlank(fileName))
        {
            return null;
        }
        Thumbnail thumbnail = new Thumbnail();
        thumbnail.setFilename(fileName);
        thumbnail.setFormat(FilenameUtils.getExtension(fileName));
        return thumbnail;
    }

    private List<Spectrum> createSpectra(au.csiro.casda.datadeposit.observation.jaxb.Spectrum jaxbSpectrum,
            File parentFilePath, Project project, int numPrevEncapsPairs, Observation observation)
            throws MalformedFileException
    {
        List<Spectrum> spectraList = new ArrayList<>();
        
        String pattern = jaxbSpectrum.getFilename();

        //TODO: Cope with directories here
        Collection<File> files = FileUtils.listFiles(parentFilePath, new WildcardFileFilter(pattern), null);
        if (files.size() != jaxbSpectrum.getNumber())
        {
            String message = String.format("Expected %d spectra matching %s but found %d.", jaxbSpectrum.getNumber(),
                    pattern, files.size());
            logger.error(message);
            throw new MalformedFileException(message);
        }

        int fileNum = numPrevEncapsPairs*2+1;
        EncapsulationFile spectraEncapsFile = createEncapsulationObject(pattern, "spectra", fileNum);
        EncapsulationFile thumbEncapsFile = createEncapsulationObject(jaxbSpectrum.getThumbnail(), "thumb", fileNum+1);
        
        for (File file : files)
        {
            Spectrum spectrum = new Spectrum();
            spectrum.setProject(project);
            spectrum.setFilename(file.getName());
            spectrum.setFormat(jaxbSpectrum.getFormat());
            spectrum.setType(jaxbSpectrum.getType());
            String thumbnailName =
                    identifyMatchingThumbnail(pattern, jaxbSpectrum.getThumbnail(), parentFilePath, file.getName());
            if (StringUtils.isNotBlank(thumbnailName))
            {
                Thumbnail thumbnail = createThumbnail(thumbnailName);
                spectrum.setThumbnail(thumbnail);
                thumbEncapsFile.addThumbnail(thumbnail);
            }
            
            spectraEncapsFile.addSpectrum(spectrum);
            spectraList.add(spectrum);
        }

        return spectraList;
    }


    private EncapsulationFile createEncapsulationObject(String pattern, String encapsType, int fileNum)
    {
        EncapsulationFile encapsFile = new EncapsulationFile();
        encapsFile.setFilename("encaps-"+encapsType+"-"+fileNum+".tar");
        encapsFile.setFilePattern(pattern);
        encapsFile.setFormat("tar");
        return encapsFile;
    }

    private List<MomentMap> createMomentMap(au.csiro.casda.datadeposit.observation.jaxb.MomentMap jaxbMomentMap,
            File parentFilePath, Project project, int numPrevEncapsPairs, Observation observation)
            throws MalformedFileException
    {
        List<MomentMap> momentMapList = new ArrayList<>();
        
        String pattern = jaxbMomentMap.getFilename();
        //TODO: Cope with directories here
        Collection<File> files = FileUtils.listFiles(parentFilePath, new WildcardFileFilter(pattern), null);
        if (files.size() != jaxbMomentMap.getNumber())
        {
            String message = String.format("Expected %d moment maps matching %s but found %d.", jaxbMomentMap.getNumber(),
                    pattern, files.size());
            logger.error(message);
            throw new MalformedFileException(message);
        }

        int fileNum = numPrevEncapsPairs * 2 + 1;
        EncapsulationFile momEncapsFile = createEncapsulationObject(pattern, "mom", fileNum);
        EncapsulationFile thumbEncapsFile = createEncapsulationObject(jaxbMomentMap.getThumbnail(), "thumb", fileNum+1);
        
        for (File file : files)
        {
            MomentMap momentMap = new MomentMap();
            momentMap.setProject(project);
            momentMap.setFilename(file.getName());
            momentMap.setFormat(jaxbMomentMap.getFormat());
            momentMap.setType(jaxbMomentMap.getType());
            String thumbnailName =
                    identifyMatchingThumbnail(pattern, jaxbMomentMap.getThumbnail(), parentFilePath, file.getName());
            if (StringUtils.isNotBlank(thumbnailName))
            {
                Thumbnail thumbnail = createThumbnail(thumbnailName);
                momentMap.setThumbnail(thumbnail);
                thumbEncapsFile.addThumbnail(thumbnail);
            }
            
            momEncapsFile.addMomentMap(momentMap);
            momentMapList.add(momentMap);
        }

        return momentMapList;
    }
    
    private List<Cubelet> createCubelet(au.csiro.casda.datadeposit.observation.jaxb.Cubelet jaxbCubelet,
            File parentFilePath, Project project, int numPrevEncapsPairs, Observation observation)
            throws MalformedFileException
    {
        List<Cubelet> cubeletList = new ArrayList<>();
        
        String pattern = jaxbCubelet.getFilename();

        Collection<File> files = FileUtils.listFiles(parentFilePath, new WildcardFileFilter(pattern), null);
        if (files.size() != jaxbCubelet.getNumber())
        {
            String message = String.format("Expected %d cubelets matching %s but found %d.", jaxbCubelet.getNumber(),
                    pattern, files.size());
            logger.error(message);
            throw new MalformedFileException(message);
        }

        int fileNum = numPrevEncapsPairs * 2 + 1;
        EncapsulationFile cubeEncapsFile = createEncapsulationObject(pattern, "cube", fileNum);
        EncapsulationFile thumbEncapsFile = createEncapsulationObject(jaxbCubelet.getThumbnail(), "thumb", fileNum+1);
        
        for (File file : files)
        {
            Cubelet cubelet = new Cubelet();
            cubelet.setProject(project);
            cubelet.setFilename(file.getName());
            cubelet.setFormat(jaxbCubelet.getFormat());
            cubelet.setType(jaxbCubelet.getType());
            String thumbnailName =
                    identifyMatchingThumbnail(pattern, jaxbCubelet.getThumbnail(), parentFilePath, file.getName());
            if (StringUtils.isNotBlank(thumbnailName))
            {
                Thumbnail thumbnail = createThumbnail(thumbnailName);
                cubelet.setThumbnail(thumbnail);
                thumbEncapsFile.addThumbnail(thumbnail);
            }
            
            cubeEncapsFile.addCubelet(cubelet);
            cubeletList.add(cubelet);
        }

        return cubeletList;
    }

    private String identifyMatchingThumbnail(String spectraPattern, String thumbnailPattern, File parentFilePath,
            String spectrumFilename)
    {
        if (StringUtils.isBlank(thumbnailPattern))
        {
            return "";
        }
        
        // Convert spectra pattern to regex with capturing groups
        String regex = spectraPattern.replace("?", "(.?)").replace("*", "(.*?)");
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(spectrumFilename);
        matcher.find();
        
        // Convert thumbnail pattern to a name using wildcards as references to wildcards in the spectrum pattern
        StringBuilder thumbnailBuf = new StringBuilder();
        int patCount = 1;
        for (int i = 0; i < thumbnailPattern.length(); i++)
        {
            char c = thumbnailPattern.charAt(i);
            switch (c)
            {
            case '*':
            case '?':
                if (patCount <= matcher.groupCount())
                {
                    thumbnailBuf.append(matcher.group(patCount++));
                }
                break;

            default:
                thumbnailBuf.append(c);
                break;
            }
        }
        String thumbnailName = thumbnailBuf.toString();

        // Check for existence of matching thumbnail
        File thumbnail = new File(parentFilePath, thumbnailName);
        if (thumbnail.exists())
        {
            return thumbnailName;
        }
        logger.warn("Unable to find thumbnail at " + thumbnail.getAbsolutePath());
        return null;
    }


    private Scan createScan(au.csiro.casda.datadeposit.observation.jaxb.Scan parsedScan)
    {
        Scan scan = new Scan();
        scan.setScanId(parsedScan.getId());

        scan.setCentreFrequency(parsedScan.getCentrefreq().getValue());
        scan.setChannelWidth(parsedScan.getChanwidth().getValue());

        // TODO: ENUM?
        if (StringUtils.isNotBlank(parsedScan.getCoordsystem()))
        {
            scan.setCoordSystem(parsedScan.getCoordsystem());
        }

        setScanFieldCentre(parsedScan, scan);

        if (StringUtils.isNotBlank(parsedScan.getFieldname()))
        {
            scan.setFieldName(parsedScan.getFieldname());
        }

        scan.setNumChannels(parsedScan.getNumchan());

        if (StringUtils.isNotBlank(parsedScan.getPolarisations()))
        {
            scan.setPolarisations(parsedScan.getPolarisations().replaceAll("(, )|[\\[]|([\\]])", "/"));
        }

        scan.setScanStart(new DateTime(parsedScan.getScanstart().asTimestamp(), DateTimeZone.UTC));
        scan.setScanEnd(new DateTime(parsedScan.getScanend().asTimestamp(), DateTimeZone.UTC));

        return scan;
    }

    private void setScanFieldCentre(au.csiro.casda.datadeposit.observation.jaxb.Scan parsedScan, Scan scan)
    {
        Matcher matcher = FIELD_CENTRE_PATTERN.matcher(parsedScan.getFieldcentre().getValue());
        matcher.matches();
        scan.setFieldCentreX(Double.parseDouble(matcher.group(FIELD_CENTRE_PATTERN_X_GROUP)));
        scan.setFieldCentreY(Double.parseDouble(matcher.group(FIELD_CENTRE_PATTERN_Y_GROUP)));
    }

    private MeasurementSet createMeasurementSet(
            au.csiro.casda.datadeposit.observation.jaxb.MeasurementSet parsedMeasurementSet,
            Map<String, Project> projectsByOpalCode)
    {
        MeasurementSet measurementSet = new MeasurementSet();
        measurementSet.setProject(getProject(parsedMeasurementSet.getProject(), projectsByOpalCode));
        measurementSet.setFilename(parsedMeasurementSet.getFilename());
        measurementSet.setFormat(parsedMeasurementSet.getFormat());

        Scans parsedScans = parsedMeasurementSet.getScans();
        if (parsedScans != null && CollectionUtils.isNotEmpty(parsedScans.getScan()))
        {
            Double minMsWavelen = null;
            Double maxMsWavelen = null;
            for (au.csiro.casda.datadeposit.observation.jaxb.Scan parsedScan : parsedScans.getScan())
            {
                Scan scan = createScan(parsedScan);
                measurementSet.addScan(scan);
                double minWaveLen = calcMinWavelength(scan);
                if (minMsWavelen == null || minWaveLen < minMsWavelen)
                {
                    minMsWavelen = minWaveLen;
                }
                double maxWaveLen = calcMaxWavelength(scan);
                if (maxMsWavelen == null || maxWaveLen > maxMsWavelen)
                {
                    maxMsWavelen = maxWaveLen;
                }
            }
            
            measurementSet.setEmMin(minMsWavelen);
            measurementSet.setEmMax(maxMsWavelen);
        }
        return measurementSet;
    }


    private double calcMinWavelength(Scan scan)
    {
        double maxFreq = scan.getCentreFrequency()
                + (scan.getChannelWidth() * scan.getNumChannels() / 2.0);
        return AstroConversion.frequencyToWavelength(maxFreq);
    }

    private double calcMaxWavelength(Scan scan)
    {
        double minFreq = scan.getCentreFrequency()
                - (scan.getChannelWidth() * scan.getNumChannels() / 2.0);
        return AstroConversion.frequencyToWavelength(minFreq);
    }

    private EvaluationFile createEvaluationFile(Evaluation parsedEvaluationFile, 
    		EncapsulationFile evalEncapsFile, Map<String, Project> projectsByOpalCode)
    {
        EvaluationFile evaluationFile = new EvaluationFile();
        evaluationFile.setProject(getProject(parsedEvaluationFile.getProject(), projectsByOpalCode));
        evaluationFile.setFormat(parsedEvaluationFile.getFormat());
        evaluationFile.setFilename(parsedEvaluationFile.getFilename());
        evalEncapsFile.addEvaluationFile(evaluationFile);  
   
        return evaluationFile;
    }

    private Project getProject(String projectName, Map<String, Project> projectsByOpalCode)
    {
        if (!projectsByOpalCode.containsKey(projectName))
        {
            Project project = projectRepository.findByOpalCode(projectName);
            if (project == null)
            {
                project = new Project(projectName);
            }
            projectsByOpalCode.put(projectName, project);
        }
        return projectsByOpalCode.get(projectName);
    }
    
    
}
