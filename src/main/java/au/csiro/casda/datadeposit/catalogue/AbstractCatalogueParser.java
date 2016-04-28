package au.csiro.casda.datadeposit.catalogue;

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
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import au.csiro.casda.datadeposit.observation.jpa.repository.AbstractCatalogueEntryRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableElement;
import au.csiro.casda.datadeposit.votable.parser.VoTableXmlElementObjectFactory;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.Observation;

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
 * Base class for CatalogueParsers dealing with All Catalogues.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 * @param <T> the catalogue type; continuum island, continuum component, spectral line absorption, 
 * spectral line emission, Polarisation or level 7
 */
public abstract class AbstractCatalogueParser<T> implements CatalogueParser
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCatalogueParser.class);
    
    private static final String SCHEMA_FILE_RESOURCE_PATH = "schemas/VOTable-1.3.xsd";
    
    private ObservationRepository observationRepository;

    private CatalogueRepository catalogueRepository;
    
    private AbstractCatalogueVoTableVisitor catalogueVoTableVisitor;
    
    private AbstractCatalogueEntryRepository<T> catalogueEntryRepository;
    
    
    /**
     * Constructor 
     */
    public AbstractCatalogueParser()
    {
    }
    
    /**
     * Constructor
     * @param catalogueRepository the catalogue JPA repository
     * @param observationRepository the observation JPA repository
     * @param voTableVisitor the voTablevisitor object for the specific catalogue type
     * @param entityRepository the JPA repository for the specific catalogue type
     */
    public AbstractCatalogueParser(CatalogueRepository catalogueRepository, 
            ObservationRepository observationRepository, AbstractCatalogueVoTableVisitor voTableVisitor, 
            AbstractCatalogueEntryRepository<T> entityRepository)
    {
        this.catalogueRepository = catalogueRepository;
        this.observationRepository = observationRepository;
        this.catalogueVoTableVisitor = voTableVisitor;
        this.catalogueEntryRepository  = entityRepository;
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
     * @param CatalogueDatafile the name/address of the data file
     * @param schema the schema to use for parsing
     * @return the VisitableVoTableElement
     * @throws FileNotFoundException thrown if the file does not exist or cannot be accessed
     * @throws CatalogueParser.MalformedFileException en exception thrown 
     * if the data file does not match the expected format
     */
    protected VisitableVoTableElement parseDatafile(String CatalogueDatafile, Schema schema)
            throws FileNotFoundException, CatalogueParser.MalformedFileException
    {
        FileInputStream catalogueDatafileInputStream = new FileInputStream(CatalogueDatafile);
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
            return (VisitableVoTableElement) unmarshaller.unmarshal(catalogueDatafileInputStream);
        }
        catch (JAXBException e)
        {
            throw new CatalogueParser.MalformedFileException(e);
        }
        finally
        {
            if (catalogueDatafileInputStream != null)
            {
                try
                {
                    catalogueDatafileInputStream.close();
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
    

    private Catalogue getCatalogueForFilename(Integer sbid, String catalogueDatafile)
            throws CatalogueParser.DatabaseException
    {
        Observation observation = observationRepository.findBySbid(sbid);
        if (observation == null)
        {
            throw new CatalogueParser.DatabaseException(String.format(
                    "Could not find matching Observation record for sbid '%d'", sbid));
        }
        Collection<Catalogue> catalogues = observation.getCatalogues().stream().filter((catalogue) -> {
            return catalogue.getFilename().equals(catalogueDatafile);
        }).collect(Collectors.toList());
        
        switch (catalogues.size())
        {
        case 0:
            throw new CatalogueParser.DatabaseException(String.format(
                    "Could not find matching Catalogue record for filename '%s' on Observation '%d'",
                    catalogueDatafile, sbid));
        case 1:
            return (Catalogue) catalogues.iterator().next();
        default:
            throw new CatalogueParser.DatabaseException(String.format(
                    "Found multiple matching Catalogue records for filename '%s' on Observation '%d'",
                    catalogueDatafile, sbid));
        }
    }

    private void parseCatalogueDatafile(Catalogue catalogue, String catalogueDatafile, Mode mode)
            throws FileNotFoundException, MalformedFileException, DatabaseException
    {
        catalogueVoTableVisitor.setCatalogue(catalogue);

        Schema schema = getVoTableXmlSchema();
        VisitableVoTableElement table = parseDatafile(catalogueDatafile, schema);

        catalogueVoTableVisitor.setFailFast(mode != Mode.VALIDATE_ONLY);
        try
        {
            table.accept(catalogueVoTableVisitor);
        }
        catch (AbstractCatalogueVoTableVisitor.MalformedVoTableException ex)
        {
            throw new CatalogueParser.MalformedFileException(ex);
        }
        List<Throwable> exceptions = catalogueVoTableVisitor.getErrors();
        if (CollectionUtils.isNotEmpty(exceptions))
        {
            throw new CatalogueParser.MalformedFileException(exceptions);
        }
    }
    
    /**
     * Parses the given datafile into a Catalogue and persists the object to the database.
     * 
     * @param sbid
     *            the scheduling block id of the observation for which to import the catalogue data
     * @param catalogueFilename
     *            the name of the catalogue filename
     * @param catalogueDatafile
     *            the file to parse
     * @param mode
     *            the CatalogueParser.MODE to use when parsing
     * @return the Catalogue
     * @throws FileNotFoundException
     *             if the specified file could not be found
     * @throws MalformedFileException
     *             if the specified file does not match the expected file format
     * @throws DatabaseException
     *             if there were any issues persisting the catalogue data to the database
     * @throws ValidationModeSignal
     *             if the mode was VALIDATE_ONLY - NOTE: this exception does NOT signal an error
     */
    @Override
    @Transactional(rollbackOn = { Exception.class })
    public Catalogue parseFile(Integer sbid, String catalogueFilename, String catalogueDatafile, Mode mode)
            throws FileNotFoundException, MalformedFileException, DatabaseException, ValidationModeSignal 
    {
     // Note: For some weird reason (looks like a compiler bug), we can't use the try-with-resources here.
        if (sbid == null)
        {
            throw new IllegalArgumentException("expected sbid != null");
        }
        if (StringUtils.isBlank(catalogueFilename))
        {
            throw new IllegalArgumentException("expected StringUtils.isNotBlank(catalogueFilename)");
        }
        if (StringUtils.isBlank(catalogueDatafile))
        {
            throw new IllegalArgumentException("expected StringUtils.isNotBlank(catalogueDatafile)");
        }
        else
        {
            File continuumCatalogueFile = new File(catalogueDatafile);
            if (!continuumCatalogueFile.exists())
            {
                throw new FileNotFoundException(catalogueDatafile + " does not exist");
            }
        }

        try
        {
            Catalogue catalogue = getCatalogueForFilename(sbid, catalogueFilename);
            if (getCatalogueEntryCount(catalogue) > 0)
            {
                throw new CatalogueParser.DatabaseException(String.format(
                        "Catalogue record for filename '%s' on Observation '%d' already has catalogue entries",
                        catalogueFilename, sbid));
            }
            parseCatalogueDatafile(catalogue, catalogueDatafile, mode);

            try
            {
                this.catalogueRepository.save(catalogue);
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
            return catalogue;
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

    /**
     * @param catalogue the catalogue to count by
     * @return the amount of matching entries
     */
    private long getCatalogueEntryCount(Catalogue catalogue)
    {
        return catalogueEntryRepository.countByCatalogue(catalogue);
    }

}
