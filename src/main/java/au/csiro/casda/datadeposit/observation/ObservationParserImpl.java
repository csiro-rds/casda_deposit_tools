package au.csiro.casda.datadeposit.observation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import au.csiro.casda.datadeposit.ChildDepositableArtefact;
import au.csiro.casda.datadeposit.observation.assembler.ObservationAssembler;
import au.csiro.casda.datadeposit.observation.assembler.ObservationAssembler.ObservationAlreadyExistsException;
import au.csiro.casda.datadeposit.observation.assembler.ObservationAssemblerImpl;
import au.csiro.casda.datadeposit.observation.jaxb.Catalogue;
import au.csiro.casda.datadeposit.observation.jaxb.Image;
import au.csiro.casda.datadeposit.observation.jaxb.MeasurementSet;
import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.parser.XmlObservation;
import au.csiro.casda.datadeposit.observation.parser.XmlObservationParser;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.ObservationMetadataFile;

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
 * <add description here>
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@Component
public class ObservationParserImpl implements ObservationParser
{
    private static final Logger logger = LoggerFactory.getLogger(ObservationParserImpl.class);

    private ObservationRepository observationRepository;

    private ProjectRepository projectRepository;

    private SimpleJdbcRepository simpleJdbcRepository;

    @Autowired
    @Value("${fileIdMaxSize}")
    private int fileIdMaxSize;
    
    @Autowired
    @Value("${thumbnail.max.size.kilobytes}")
    private long thumbnailMaxSize;

    @Autowired
    @Value("${cubelets.enabled:false}")
    private boolean cubeletsEnabled;

    /**
     * Constructs an ObservationParserImpl that uses the given repositories for access to and updating of persistent
     * Projects and Observations.
     * 
     * @param projectRepository
     *            a ProjectRepository
     * @param observationRepository
     *            an ObservationRepository
     * @param simpleJdbcRepository
     *            a simpleJdbcRepository
     */
    @Autowired
    public ObservationParserImpl(ProjectRepository projectRepository, ObservationRepository observationRepository,
            SimpleJdbcRepository simpleJdbcRepository)
    {
        super();
        this.projectRepository = projectRepository;
        this.observationRepository = observationRepository;
        this.simpleJdbcRepository = simpleJdbcRepository;
    }

    /**
     * Constructs an ObservationParserImpl that uses the given repositories for access to and updating of persistent
     * Projects and Observations along with config property fileIdMaxSize
     * 
     * @param projectRepository
     *            a ProjectRepository
     * @param observationRepository
     *            an ObservationRepository
     * @param simpleJdbcRepository
     *            a simpleJdbcRepository
     * @param fileIdMaxSize
     *            Allowed max file Id int
     * @param thumbnailMaxSize
     *            Allowed max size of thumbnail
     * 
     */
    public ObservationParserImpl(ProjectRepository projectRepository, ObservationRepository observationRepository,
            SimpleJdbcRepository simpleJdbcRepository, @Value("${fileIdMaxSize}") int fileIdMaxSize,
            @Value("${thumbnail.max.size.kilobytes}") long thumbnailMaxSize)
    {
        super();
        this.projectRepository = projectRepository;
        this.observationRepository = observationRepository;
        this.simpleJdbcRepository = simpleJdbcRepository;
        this.fileIdMaxSize = fileIdMaxSize;
        this.thumbnailMaxSize = thumbnailMaxSize;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackOn = { Exception.class })
    public Observation parseFile(Integer sbid, String observationMetadataFilename, boolean redeposit)
            throws FileNotFoundException, MalformedFileException, RepositoryException
    {
        String fullPath = FilenameUtils.getFullPath(observationMetadataFilename);
        String obsFilePath = fullPath != null ? FilenameUtils.separatorsToUnix(fullPath.trim()) : "";

        if (sbid == null)
        {
            throw new IllegalArgumentException("expected sbid != null");
        }
        if (StringUtils.isBlank(observationMetadataFilename))
        {
            throw new IllegalArgumentException("expected !StringUtils.isBlank(observationMetadataFilename)");
        }

        XmlObservation dataset = parseObservationMetadataFile(observationMetadataFilename);
        validateDataset(sbid, dataset);
        ObservationAssembler observationAssembler =
                new ObservationAssemblerImpl(this.projectRepository, this.observationRepository);

        Observation observation = null;
        try
        {
            if (redeposit)
            {
                observation = observationAssembler.updateObservationFromParsedObservation(dataset, fileIdMaxSize,
                        new File(obsFilePath), cubeletsEnabled);
            }
            else
            {
                observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                        new File(obsFilePath), cubeletsEnabled);
            }
            List<ChildDepositableArtefact> artefacts = observation.getDepositableArtefacts();
            validateFileExistsAndSetFilesize(artefacts, obsFilePath);
        }
        catch (ObservationAlreadyExistsException e)
        {
            throw new RepositoryException(e);
        }

        return observationRepository.save(observation);
    }

    private void validateDataset(Integer sbid, XmlObservation observation) throws MalformedFileException
    {
        logger.debug("Validating that the sbid argument matches the value in the input file.");

        List<String> validTypes = simpleJdbcRepository.getImageTypes();

        // sbid must be a integer > 0
        int sbidInDataset = -1;
        try
        {
            sbidInDataset = Integer.parseInt(observation.getIdentity().getSbid());
        }
        catch (NumberFormatException e)
        {
            throw new MalformedFileException("Not an integer: " + observation.getIdentity().getSbid(), e);
        }
        if (sbidInDataset <= 0)
        {
            throw new MalformedFileException(
                    String.format("sbid [%s] in dataset is invalid, it must be a positive integer",
                            observation.getIdentity().getSbid()));
        }

        if (sbidInDataset != sbid)
        {
            throw new MalformedFileException(String.format(
                    "Supplied sbid [%s] does not match the value in the /dataset/identity/sbid element [%s].", sbid,
                    observation.getIdentity().getSbid()));
        }

        validateDateProgression(observation.getObservation());

        for (Image image : observation.getImages())
        {
            validateFileName(image.getFilename(), "Image");
            if (StringUtils.isNotBlank(image.getThumbnailLarge()))
            {
                validateThumbnail(image.getThumbnailLarge());
            }
            if (StringUtils.isNotBlank(image.getThumbnailSmall()))
            {
                validateThumbnail(image.getThumbnailSmall());
            }
            validateImageType(image, "Image", validTypes);
        }
        for (Catalogue catalogue : observation.getCatalogues())
        {
            validateFileName(catalogue.getFilename(), "Catalogue");
        }
        for (MeasurementSet measurementSet : observation.getMeasurementSets())
        {
            validateFileName(measurementSet.getFilename(), "Measurement set");
        }
        for (au.csiro.casda.datadeposit.observation.jaxb.Evaluation evaluation : observation.getEvaluationFiles())
        {
        	validateFileName(evaluation.getFilename(), "Evaluation file");
        }
    }

    /**
     * validates the observation dates to ensure the start date is before the end date
     * @param observation the observation
     * @throws MalformedFileException an exception created when the date order is invalid.
     */
    private void validateDateProgression(au.csiro.casda.datadeposit.observation.jaxb.Observation observation)
            throws MalformedFileException
    {
        if (observation.getObsend().asModifiedJulianDate() <= observation.getObsstart().asModifiedJulianDate())
        {
            throw new MalformedFileException(String.format("Observation contains an observation End date/time "
                    + "which is earlier than or equal to the observation start time."));
        }
    }
    
    /**
     * Validates thumbnail format and ensure the formats are restricted to png and jpeg
     * 
     * @param fileName
     *            the file name of the thumbnail
     * @throws MalformedFileException
     *             an exception created when the date order is invalid.
     */
    private void validateThumbnail(String fileName) throws MalformedFileException
    {
        validateFileName(fileName, "Thumbnail");
        fileName = fileName.toLowerCase();
        String format = FilenameUtils.getExtension(fileName);
        if (!StringUtils.equals(format, "png") && !StringUtils.equals(format, "jpg")
                && !StringUtils.equals(format, "jpeg"))
        {
            throw new MalformedFileException(
                    String.format("%s Thumbnail format [%s] is not supported.", fileName, format));
        }
    }

    /**
     * checks the image type against the list of allowed types in the database (excluding 'unknown')
     * 
     * @param fileType
     *            The file type for reporting purposes
     * @param image
     *            the image cube.
     * @param validTypes
     *            The list of acceptable image types to test against that parsed from the observation xml.
     * @throws MalformedFileException
     *             raise an exception for invalid types
     */
    private void validateImageType(Image image, String fileType, List<String> validTypes) throws MalformedFileException
    {
        if (!validTypes.contains(image.getType()))
        {
            throw new MalformedFileException(String.format("%s file name [%s] contains invalid image type [%s].",
                    fileType, image.getFilename(), image.getType()));
        }
    }

    /**
     * Check that a file name does not start with a "/" or contain a "//"
     * 
     * @param fileName
     *            The file name
     * @param fileType
     *            The file type for reporting purposes
     * @throws MetadataStoreException
     *             raise an exception for invalid types
     */
    private void validateFileName(String fileName, String fileType) throws MalformedFileException
    {
        if (fileName.startsWith("/"))
        {
            throw new MalformedFileException(String
                    .format("%s file name [%s] must be a relative path without a leading slash.", fileType, fileName));
        }
        if (fileName.contains("//"))
        {
            throw new MalformedFileException(
                    String.format("%s file name [%s] must not contain a double slash.", fileType, fileName));
        }
    }

    private void validateFileExistsAndSetFilesize(List<ChildDepositableArtefact> artefacts, String parentFilePath)
            throws MalformedFileException
    {
        File artefactFile = null;
        for (ChildDepositableArtefact artefact : artefacts)
        {
            if (!(artefact instanceof ObservationMetadataFile) && !(artefact instanceof EncapsulationFile)
                    && !(artefact.isDeposited()))
            {
                // We only want the non-deposited files as when redpositing we don;t expect the existing files to be
                // recreated
                logger.debug("****** CollectionName: {}; filename: {}; type: {}", artefact.getCollectionName(),
                        artefact.getFilename(), artefact.getDepositableArtefactTypeName());

                artefactFile = new File(parentFilePath, artefact.getFilename().trim());
                if (!artefactFile.exists())
                {
                    throw new MalformedFileException(String.format("%s file name [%s] does not exist.",
                            artefact.getDepositableArtefactTypeName(), artefact.getFilename().trim()));

                }
                // store the file size in kilobytes
                artefact.setFilesize(ObservationParser.calculateFileSizeKb(artefactFile));
                if ("thumbnail".equals(artefact.getDepositableArtefactTypeName())
                        && artefact.getFilesize() > thumbnailMaxSize)
                {
                    throw new MalformedFileException(
                            String.format("%s file name [%s] size [%d KB] is larger than allowed size [%d KB].",
                                    artefact.getDepositableArtefactTypeName(), artefact.getFilename().trim(),
                                    artefact.getFilesize(), thumbnailMaxSize));
                }             
            }
        }
    }

    private XmlObservation parseObservationMetadataFile(String observationMetadataFile)
            throws FileNotFoundException, MalformedFileException
    {
        // Note: For some weird reason (looks like a compiler bug), we can't use the try-with-resources here.
        FileInputStream observationMetadataFileInputStream = new FileInputStream(observationMetadataFile);
        try
        {
            return XmlObservationParser.parseDataSetXML(observationMetadataFileInputStream);
        }
        catch (JAXBException | SAXException | IOException e)
        {
            throw new MalformedFileException(e);
        }
        finally
        {
            if (observationMetadataFileInputStream != null)
            {
                try
                {
                    observationMetadataFileInputStream.close();
                }
                catch (IOException e)
                {
                    logger.debug(
                            "Encountered IOException closing a resource.  Ignoring in favour of any other exceptions",
                            e);
                }
            }
        }
    }

    public void setSimpleJdbcRepository(SimpleJdbcRepository simpleJdbcRepository)
    {
        this.simpleJdbcRepository = simpleJdbcRepository;
    }
}
