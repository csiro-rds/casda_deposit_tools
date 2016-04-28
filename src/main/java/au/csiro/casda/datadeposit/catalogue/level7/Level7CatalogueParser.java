package au.csiro.casda.datadeposit.catalogue.level7;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.xml.validation.Schema;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueParser;
import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableElement;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.Project;

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
 * Instances of this class are Spring components that orchestrate the parsing of a level 7 catalogue datafile, in a
 * VOTABLE format, and the persistence of the parsed data to the database.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Level7CatalogueParser extends AbstractCatalogueParser<Level7Collection>
{
    private Level7VoTableVisitor voTableVisitor;

    private Level7CollectionRepository level7CollectionRepository;

    /**
     * For test cases.
     * 
     * @param voTableVisitor
     *            an AbstractCatalogueVoTableVisitor which will be used to visit a parsed catalogue file
     * @param level7CollectionRepository
     *            the Level7Collection jpa repository
     */
    Level7CatalogueParser(Level7VoTableVisitor voTableVisitor, Level7CollectionRepository level7CollectionRepository)
    {
        super();
        this.voTableVisitor = voTableVisitor;
        this.level7CollectionRepository = level7CollectionRepository;
    }

    /**
     * Constructor
     * 
     * @param repository
     *            Autowired JDBC repository
     * @param level7CollectionRepository
     *            the Level7Collection jpa repository
     * @param descriptionMaxLimit
     *            description max length limit
     */
    @Autowired
    public Level7CatalogueParser(SimpleJdbcRepository repository,
            Level7CollectionRepository level7CollectionRepository,
            @Value("${level7.element.description.max.length}") int descriptionMaxLimit)
    {
        /*
         * The visitor uses an output stream to output the DDL that it generates for the Level 7 Catalogue. We don't
         * want that behaviour anymore because a) we will the apply the DDL directly to the database, and b) we will be
         * using stdout to report validation failures. However, until the DDL is being written to the database we want
         * to retain the ability of the tests to look at the generated DDL.
         */
        this(new Level7VoTableVisitor(repository, descriptionMaxLimit), level7CollectionRepository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackOn = { Exception.class })
    public Catalogue parseFile(Integer collectionId, String catalogueFilename, String catalogueDatafile, Mode mode)
            throws FileNotFoundException, CatalogueParser.MalformedFileException, CatalogueParser.DatabaseException,
            CatalogueParser.ValidationModeSignal
    {
        File catalogueFile = new File(catalogueDatafile);

        if (collectionId == null)
        {
            throw new IllegalArgumentException("expected collectionId != null");
        }
        if (StringUtils.isBlank(catalogueFilename))
        {
            throw new IllegalArgumentException("expected !StringUtils.isBlank(catalogueFilename)");
        }
        if (StringUtils.isBlank(catalogueDatafile))
        {
            throw new IllegalArgumentException("expected !StringUtils.isBlank(catalogueDatafile)");
        }
        else
        {
            if (!catalogueFile.exists())
            {
                throw new FileNotFoundException(catalogueDatafile + " does not exist");
            }
        }

        try
        {
            Level7Collection level7Collection =
                    level7CollectionRepository.findByDapCollectionId(collectionId.longValue());
            if (level7Collection == null)
            {
                if (mode == Mode.NORMAL)
                {
                    throw new CatalogueParser.DatabaseException(String.format(
                            "Level 7 Collection record with collection id %d does not exist", collectionId));
                }
                else
                {
                    // create a dummy level 7 collection and project to test
                    // these will be rolled back by the transaction in validate mode
                    level7Collection = new Level7Collection(collectionId);
                    level7Collection.setProject(new Project("VALIDATE"));
                    Catalogue catalogue = new Catalogue(CatalogueType.LEVEL7);
                    catalogue.setFilename(catalogueFile.getName());
                    catalogue.setFormat("VOTABLE");
                    level7Collection.addCatalogue(catalogue);
                    level7Collection = level7CollectionRepository.save(level7Collection);
                }
            }

            level7Collection.toString();
            level7Collection.getCatalogues().size();

            Optional<Catalogue> catalogue =
                    level7Collection.getCatalogues().stream()
                            .filter(cat -> catalogueFile.getName().equals(cat.getFilename())).findFirst();

            if (mode == Mode.NORMAL && !catalogue.isPresent())
            {
                throw new CatalogueParser.DatabaseException(String.format(
                        "Level 7 catalogue record for collection id %d and file '%s' doesn't exist", collectionId,
                        catalogueDatafile));
            }

            parseCatalogueDatafile(catalogueDatafile, level7Collection, mode);

            if (mode == Mode.VALIDATE_ONLY)
            {
                // Throw an exception to cause a rollback in case any of the parsing code modified the database
                throw new CatalogueParser.ValidationModeSignal();
            }
            return catalogue.get();
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
     * Parse and process the catalogue's datafile using the visitor provided by the subclass on creation. This method is
     * package-visible for testing purposes.
     * 
     * @param catalogueDatafile
     *            a path to the datafile to parse
     * @param level7Collection
     *            the level 7 Collection record
     * @param mode
     *            the Mode to parse in
     * @throws FileNotFoundException
     *             if the datafile could not be found
     * @throws MalformedFileException
     *             if the datafile is malformed
     * @throws DatabaseException
     *             if the datafile's contents could not be persisted
     */
    void parseCatalogueDatafile(String catalogueDatafile, Level7Collection level7Collection, Mode mode)
            throws FileNotFoundException, MalformedFileException, DatabaseException
    {
        Schema schema = getVoTableXmlSchema();
        VisitableVoTableElement table = parseDatafile(catalogueDatafile, schema);

        this.voTableVisitor.setProjectCode(level7Collection.getProject().getOpalCode());
        this.voTableVisitor.setLevel7CollectionId(level7Collection.getDapCollectionId());
        this.voTableVisitor.setFilename(new File(catalogueDatafile).getName());
        this.voTableVisitor.setFailFast(mode != Mode.VALIDATE_ONLY);
        try
        {
            table.accept(this.voTableVisitor);
        }
        catch (AbstractCatalogueVoTableVisitor.MalformedVoTableException ex)
        {
            throw new CatalogueParser.MalformedFileException(ex);
        }
        List<Throwable> exceptions = this.voTableVisitor.getErrors();
        if (CollectionUtils.isNotEmpty(exceptions))
        {
            throw new CatalogueParser.MalformedFileException(exceptions);
        }
    }

}
