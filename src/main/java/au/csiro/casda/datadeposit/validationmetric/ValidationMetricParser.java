package au.csiro.casda.datadeposit.validationmetric;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import au.csiro.casda.datadeposit.DepositStateImpl;
import au.csiro.casda.datadeposit.DepositState.Type;
import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.DatabaseException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.MalformedFileException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.Mode;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.ValidationModeSignal;
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ValidationMetricRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ValidationMetricValueRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableElement;
import au.csiro.casda.datadeposit.votable.parser.VoTableXmlElementObjectFactory;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.observation.Observation;

/*
 * #%L
 * CSIRO ASKAP Science Data Archive
 * %%
 * Copyright (C) 2017 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * Instances of this class are Spring components that orchestrate the parsing of a validation metric file, 
 * in a VOTABLE format, and the persistence of the parsed data to the database.
 * <p>
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ValidationMetricParser 
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationMetricParser.class);
    
    private static final String SCHEMA_FILE_RESOURCE_PATH = "schemas/VOTable-1.3.xsd";

    private EvaluationFileRepository evaluationFileRepository;
    private ObservationRepository observationRepository;
    
    
    private ValidationMetricVoTableVisitor validationMetricVoTableVisitor;
    
    /**
     * Constructor 
     */
    public ValidationMetricParser()
    {
    	
    }
    
    /**
     * Constructor
     * @param observationRepository the observation JPA repository
     * @param validationMetricValueRepository the metric value repository
     * @param validationMetricRepository the metric repository
     * @param evaluationFileRepository the evaluation file repository
     */
    @Autowired
    public ValidationMetricParser(ObservationRepository observationRepository, 
    		ValidationMetricValueRepository validationMetricValueRepository, 
    		EvaluationFileRepository evaluationFileRepository,
    		ValidationMetricRepository validationMetricRepository)
    {
        this.observationRepository = observationRepository;
    	this.validationMetricVoTableVisitor = 
    			new ValidationMetricVoTableVisitor(validationMetricValueRepository, validationMetricRepository);
    	this.evaluationFileRepository = evaluationFileRepository;
    }
    
    /**
     * Helper methods for subclasses that need to load a VOTABLE XML Schema.
     * 
     * @return a Schema
     */
    protected Schema getVoTableXmlSchema()
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream schemaFileInputStream = new ClassPathResource(SCHEMA_FILE_RESOURCE_PATH).getInputStream())
        {
            return schemaFactory.newSchema(new StreamSource(schemaFileInputStream));
        }
        catch (IOException e)
        {
            throw new RuntimeException(String.format("Unexpected IOException accessing the resource '%s'",
                    SCHEMA_FILE_RESOURCE_PATH), e);
        }
        catch (SAXException e)
        {
            throw new RuntimeException(String.format("Unexpected SAXException reading the schema file resource '%s'",
                    SCHEMA_FILE_RESOURCE_PATH), e);
        }
    }
    
    /**
     * Parses the datafile and creates the Vo Table Object for the data in the file
     * @param validationMetricfile the name/address of the data file
     * @param schema the schema to use for parsing
     * @return the VisitableVoTableElement
     * @throws FileNotFoundException thrown if the file does not exist or cannot be accessed
     * @throws CatalogueParser.MalformedFileException en exception thrown 
     * if the data file does not match the expected format
     */
    protected VisitableVoTableElement parseDatafile(String validationMetricfile, Schema schema)
            throws FileNotFoundException, CatalogueParser.MalformedFileException
    {
        FileInputStream validationMetricDatafileInputStream = new FileInputStream(validationMetricfile);
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(VoTableXmlElementObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(new ValidationEventHandler()
            {
                // The default XMLValidationEventHandler is too noisy
                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean handleEvent(ValidationEvent event)
                {
                    return false;
                }
            });
            unmarshaller.setProperty("com.sun.xml.internal.bind.ObjectFactory", new VoTableXmlElementObjectFactory());
            return (VisitableVoTableElement) unmarshaller.unmarshal(validationMetricDatafileInputStream);
        }
        catch (JAXBException e)
        {
            throw new CatalogueParser.MalformedFileException(e);
        }
        finally
        {
            if (validationMetricDatafileInputStream != null)
            {
                try
                {
                	validationMetricDatafileInputStream.close();
                }
                catch (IOException e)
                {
                    LOGGER.debug(
                            "Encountered IOException closing a resource.  Ignoring in favour of any other exceptions",
                            e);
                }
            }
        }
    }
    
    private EvaluationFile getEvaluationFileForFilename(Integer sbid, String EvaluationFileDatafile)
            throws CatalogueParser.DatabaseException
    {
        Observation observation = observationRepository.findBySbid(sbid);
        if (observation == null)
        {
            throw new CatalogueParser.DatabaseException(String.format(
                    "Could not find matching Observation record for sbid '%d'", sbid));
        }
        Collection<EvaluationFile> evaluationFiles = observation.getEvaluationFiles().stream().filter((evaluationFile) -> {
            return evaluationFile.getFilename().equals(EvaluationFileDatafile);
        }).collect(Collectors.toList());
        
        switch (evaluationFiles.size())
        {
        case 0:
            throw new CatalogueParser.DatabaseException(String.format(
                    "Could not find matching Evaluation File record for filename '%s' on Observation '%d'",
                    EvaluationFileDatafile, sbid));
        case 1:
            return (EvaluationFile) evaluationFiles.iterator().next();
        default:
            throw new CatalogueParser.DatabaseException(String.format(
                    "Found multiple matching Evaluation File records for filename '%s' on Observation '%d'",
                    EvaluationFileDatafile, sbid));
        }
    }
    
    private void parseEvaluationFileDatafile(EvaluationFile evaluationFile, String EvaluationDatafile, Mode mode)
            throws FileNotFoundException, MalformedFileException, DatabaseException
    {
    	validationMetricVoTableVisitor.setEvaluationFile(evaluationFile);

        Schema schema = getVoTableXmlSchema();
        VisitableVoTableElement table = parseDatafile(EvaluationDatafile, schema);

        validationMetricVoTableVisitor.setFailFast(mode != Mode.VALIDATE_ONLY);
        try
        {
            table.accept(validationMetricVoTableVisitor);
        }
        catch (AbstractCatalogueVoTableVisitor.MalformedVoTableException ex)
        {
            throw new CatalogueParser.MalformedFileException(ex);
        }
        List<Throwable> exceptions = validationMetricVoTableVisitor.getErrors();
        if (CollectionUtils.isNotEmpty(exceptions))
        {
            throw new CatalogueParser.MalformedFileException(exceptions);
        }
    }

    /**
     * @param parentId the parent id (observation)
     * @param filename the file name
     * @param infile the complete path of the file, including name
     * @param mode the mode (normal or validate)
     * @return the evaluation file containing the parsed validation metric records
     * @throws FileNotFoundException
     *             if the specified file could not be found
     * @throws MalformedFileException
     *             if the specified file does not match the expected file format
     * @throws DatabaseException
     *             if there were any issues persisting the catalogue data to the database
     * @throws ValidationModeSignal
     *             if the mode was VALIDATE_ONLY - NOTE: this exception does NOT signal an error
     */
    @Transactional(rollbackOn = { Exception.class })
	public EvaluationFile parseFile(Integer parentId, String filename, String infile, Mode mode) throws 
	FileNotFoundException, CatalogueParser.MalformedFileException, CatalogueParser.DatabaseException, ValidationModeSignal
	{
        if (parentId == null)
        {
            throw new IllegalArgumentException("expected parentId != null");
        }
        if (StringUtils.isBlank(filename))
        {
            throw new IllegalArgumentException("expected StringUtils.isNotBlank(filename)");
        }
        if (StringUtils.isBlank(infile))
        {
            throw new IllegalArgumentException("expected StringUtils.isNotBlank(infile)");
        }
        else
        {
            File evaluationFile = new File(infile);
            if (!evaluationFile.exists())
            {
                throw new FileNotFoundException(infile + " does not exist");
            }
        }
        
        try
        {
            EvaluationFile evaluationFile = getEvaluationFileForFilename(parentId, filename);
            if (evaluationFile.getValidationMetricValues().size() > 0)
            {
                throw new CatalogueParser.DatabaseException(String.format("Evaluation File record for filename '%s' "
                		+ "on Observation '%d' already has validation metric entries",
                		filename, parentId));
            }
            parseEvaluationFileDatafile(evaluationFile, infile, mode);

            // Advance the deposit status of the catalogue here to avoid concurrent mod errors 
            evaluationFile.setDepositState(new DepositStateImpl(Type.ENCAPSULATING, evaluationFile));

            try
            {
                evaluationFileRepository.save(evaluationFile);
            }
            catch (RuntimeException e)
            {
                throw new CatalogueParser.DatabaseException(e);
            }
            if (mode == Mode.VALIDATE_ONLY)
            {
                // Throw an exception to cause a rollback in case any of the parsing code modified the database
                throw new CatalogueParser.ValidationModeSignal();
            }
            return evaluationFile;
        }
        catch (CatalogueParser.MalformedFileException e)
        {
            if (mode == Mode.VALIDATE_ONLY)
            {
                // cause messages will be empty if there are no errors
                throw new CatalogueParser.ValidationModeSignal(e.getCauseMessages().toArray(new String[0]));
            }
            else
            {
                throw e;
            }
        }
        catch (CatalogueParser.DatabaseException e)
        {
            if (mode == Mode.VALIDATE_ONLY)
            {
                throw new CatalogueParser.ValidationModeSignal(e.getMessage());
            }
            else
            {
                throw e;
            }
        }
	}
}
