package au.csiro.casda.datadeposit.observation.assembler;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.csiro.util.AstroConversion;
import au.csiro.casda.datadeposit.observation.jaxb.Scans;
import au.csiro.casda.datadeposit.ChildDepositableArtefact;
import au.csiro.casda.datadeposit.observation.ObservationParser.MalformedFileException;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.parser.XmlObservation;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.MeasurementSet;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Project;
import au.csiro.casda.entity.observation.Scan;

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
    

    /**
     * {@inheritDoc}
     * @throws MalformedFileException 
     */
    @Override
    public Observation createObservationFromParsedObservation(XmlObservation dataset, int fileIdMaxSize)
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
        Map<String, Project> projectsByOpalCode = new HashMap<>();

        observation.setObsStart(new DateTime(dataset.getObservation().getObsstart().asTimestamp().getTime(),
                DateTimeZone.UTC));
        observation.setObsEnd(new DateTime(dataset.getObservation().getObsend().asTimestamp().getTime(),
                DateTimeZone.UTC));

        observation.setObsStartMjd(dataset.getObservation().getObsstart().asModifiedJulianDate());
        observation.setObsEndMjd(dataset.getObservation().getObsend().asModifiedJulianDate());

        // TODO: Remove if not needed
        if (StringUtils.isNotBlank(dataset.getIdentity().getObsprogram()))
        {
            observation.setObsProgram(dataset.getIdentity().getObsprogram());
        }
        observation.setSbid(Integer.parseInt(dataset.getIdentity().getSbid()));
        
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

        // assemble the project and related objects
        for (au.csiro.casda.datadeposit.observation.jaxb.Catalogue jaxbCatalogue : dataset.getCatalogues())
        {
            observation.addCatalogue(createCatalogue(observation, jaxbCatalogue, projectsByOpalCode));
        }
        for (au.csiro.casda.datadeposit.observation.jaxb.Image jaxbImage : dataset.getImages())
        {
            observation.addImageCube(createImageCube(jaxbImage, projectsByOpalCode));
        }
        for (au.csiro.casda.datadeposit.observation.jaxb.MeasurementSet jaxbMeasurementSet : dataset
                .getMeasurementSets())
        {
            observation.addMeasurementSet(createMeasurementSet(jaxbMeasurementSet, projectsByOpalCode));

        }
        for (au.csiro.casda.datadeposit.observation.jaxb.Evaluation jaxbFile : dataset.getEvaluationFiles())
        {
            observation.addEvaluationFile(createEvaluationFile(jaxbFile));
        }
        
        for(ChildDepositableArtefact observArtefact : observation.getDepositableArtefacts())
        {
        	if(observArtefact.getFileId().length() > fileIdMaxSize)
        	{
        		throw new MalformedFileException("File ID longer then "	+ fileIdMaxSize + " char: " 
        				+ observArtefact.getFileId().length() + " " + observArtefact.getFileId());
        	}
        }

        logger.debug("Finished assembling JPA classes.");

        return observation;
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
        return imageCube;
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
            scan.setPolarisations(parsedScan.getPolarisations());
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

    private EvaluationFile createEvaluationFile(
            au.csiro.casda.datadeposit.observation.jaxb.Evaluation parsedEvaluationFile)
    {
        EvaluationFile evaluationFile = new EvaluationFile();
        evaluationFile.setFormat(parsedEvaluationFile.getFormat());
        evaluationFile.setFilename(parsedEvaluationFile.getFilename());
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
