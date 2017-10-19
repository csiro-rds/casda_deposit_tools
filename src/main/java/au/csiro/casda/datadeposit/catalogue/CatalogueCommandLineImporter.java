package au.csiro.casda.datadeposit.catalogue;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.catalogue.CatalogueCommandLineArgumentsParser.CommandLineArguments;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.logging.DataLocation;
import au.csiro.logging.CasdaDataDepositEvents;

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
 * CommandLineImporter implements a Spring CommandLineRunner that can be used to import RTC continuum catalog data as
 * referenced in the RTC Metadata XML file.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@Import(AppConfig.class)
public class CatalogueCommandLineImporter extends
        ArgumentsDrivenCommandLineTool<CatalogueCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "catalogue_import";

    private static final Logger logger = LoggerFactory.getLogger(CatalogueCommandLineImporter.class);

    private CatalogueCommandLineArgumentsParser commandLineArgumentsParser = new CatalogueCommandLineArgumentsParser();

    @Autowired
    private ApplicationContext context;

    /**
     * main method used to run this CommandLineImporter
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(CatalogueCommandLineImporter.class, logger, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {

        parseCommandLineArguments(logger, args);

        Instant startTime = Instant.now();

        CommandLineArguments arguments = commandLineArgumentsParser.getArgs();
        CatalogueType catalogueType = arguments.getCatalogueType();

        CatalogueParser parser = createParserForCatalogueType(catalogueType);
        Catalogue catalogue = null;
        try
        {
            catalogue = parser.parseFile(arguments.getParentId(), arguments.getCatalogueFilename(),
                    arguments.getInfile(), arguments.getMode(), arguments.getDcCommonId());
        }
        catch (FileNotFoundException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E056.messageBuilder().add(catalogueType.getDescription())
                            .add(arguments.getInfile()).add(String.join(" ", args))
                            .toString(), e);
            System.exit(1);
        }
        catch (CatalogueParser.MalformedFileException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E057.messageBuilder().add(catalogueType.getDescription())
                            .add(arguments.getInfile()).add(String.join(" ", args))
                            .toString(), e);
            System.exit(1);
        }
        catch (CatalogueParser.DatabaseException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E058.messageBuilder().add(catalogueType.getDescription())
                            .add(arguments.getInfile()).add(String.join(" ", args))
                            .toString(), e);
            System.exit(1);
        }
        catch (CatalogueParser.ValidationModeSignal s)
        {
            if (!this.getCommandLineArgumentsParser().getArgs().isValidateOnly())
            {
                throw new RuntimeException("ValidationModeSignal received when not in validation-only mode.", s);
            }
            for (String message : s.getValidationFailureMessages())
            {
                System.out.println(message);
            }
            // exit successfully because we are in validation mode
            System.exit(0);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOf(new File(this.getCommandLineArgumentsParser().getArgs().getInfile()));
        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E059.messageBuilder() //
                .add(catalogueType.getDescription()) //
                .add(arguments.getInfile()) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.RTC) //
                .addDestination(DataLocation.CASDA_DB) //
                .addVolumeBytes(filesizeInBytes) //
                .addFileId(catalogue.getFileId());
        logger.info(messageBuilder.toString());

        System.exit(0);
    }

    /**
     * Factory method that returns the correct CatalogueParser implementation for the given CatalogueType. (This method
     * exists primarily to facilitate testing.)
     * 
     * @param catalogueType
     *            a CatalogueType
     * @return a CatalogueParser
     */
    protected CatalogueParser createParserForCatalogueType(CatalogueType catalogueType)
    {
        return catalogueType.createParser(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CatalogueCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        return CasdaDataDepositEvents.E055;
    }

}
