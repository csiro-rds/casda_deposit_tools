package au.csiro.casda.datadeposit.observation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

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
     * 
     */
    public ObservationParserImpl(ProjectRepository projectRepository, ObservationRepository observationRepository,
            SimpleJdbcRepository simpleJdbcRepository, @Value("${fileIdMaxSize}") int fileIdMaxSize)
    {
        super();
        this.projectRepository = projectRepository;
        this.observationRepository = observationRepository;
        this.simpleJdbcRepository = simpleJdbcRepository;
        this.fileIdMaxSize = fileIdMaxSize;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackOn = { Exception.class })
    public Observation parseFile(Integer sbid, String observationMetadataFilename)
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
            observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize);
            Set<ChildDepositableArtefact> artefacts = observation.getDepositableArtefacts();
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

        for (Image image : observation.getImages())
        {
            validateFileName(image.getFilename(), "Image");
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
        for (au.csiro.casda.datadeposit.observation.jaxb.Evaluation measurementSet : observation.getEvaluationFiles())
        {
            validateFileName(measurementSet.getFilename(), "Evaluation file");
        }
    }

    /**
     * checks the image type against the list of allowed types in the database (excluding 'unknown')
     * 
     * @param fileType
     *         The file type for reporting purposes
     * @param image 
     *          the image cube.
     * @param validTypes
     *          The list of acceptable image types to test against that parsed from the observation xml.
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

    private void validateFileExistsAndSetFilesize(Set<ChildDepositableArtefact> artefacts, String parentFilePath)
            throws MalformedFileException
    {
        File artefactFile = null;
        for (ChildDepositableArtefact artefact : artefacts)
        {
            if (!(artefact instanceof ObservationMetadataFile))
            {
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
