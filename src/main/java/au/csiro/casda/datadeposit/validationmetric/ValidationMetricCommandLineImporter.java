package au.csiro.casda.datadeposit.validationmetric;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.validationmetric.ValidationMetricCommandLineArgumentsParser.CommandLineArguments;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.logging.DataLocation;
import au.csiro.logging.CasdaDataDepositEvents;

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
 * CommandLineImporter implements a Spring CommandLineRunner that can be used to parse the validation metric file
 * and store the contents in the database
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class ValidationMetricCommandLineImporter extends
ArgumentsDrivenCommandLineTool<ValidationMetricCommandLineArgumentsParser.CommandLineArguments>
{
	/**
     * The name of this tool.
     */
	public static final String TOOL_NAME = "validation_metric_import";
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationMetricCommandLineImporter.class);

    private ValidationMetricCommandLineArgumentsParser commandLineArgumentsParser = 
    		new ValidationMetricCommandLineArgumentsParser();
    
    private ValidationMetricParser validationMetricParser;
    
    /**
     * main method used to run this CommandLineImporter
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(ValidationMetricCommandLineImporter.class, logger, args);
    }
    
    /**
     * Constructor
     * 
     * @param validationMetricParser the validation metric parser
     */
    @Autowired
    public ValidationMetricCommandLineImporter(ValidationMetricParser validationMetricParser)
    {
        super();
        this.validationMetricParser = validationMetricParser;
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
        
        EvaluationFile evaluationFile = null;
    	
        try
        {
        	evaluationFile = validationMetricParser.parseFile(arguments.getParentId(), arguments.getFilename(),
                    arguments.getInfile(), arguments.getMode());
        }
        catch (FileNotFoundException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E187.messageBuilder().add(arguments.getInfile()).add(String.join(" ", args))
                            .toString(), e);
            System.exit(1);
        }
        catch (CatalogueParser.MalformedFileException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E188.messageBuilder().add(arguments.getInfile()).add(String.join(" ", args))
                            .toString(), e);
            System.exit(1);
        }
        catch (CatalogueParser.DatabaseException e)
        {
            logger.error(CasdaDataDepositEvents.E189.messageBuilder().add(arguments.getInfile()).toString(), e);
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
        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E190.messageBuilder() //
                .add(arguments.getInfile()) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.RTC) //
                .addDestination(DataLocation.CASDA_DB) //
                .addVolumeBytes(filesizeInBytes) //
                .addFileId(evaluationFile.getFileId());
        logger.info(messageBuilder.toString());

        System.exit(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationMetricCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        return CasdaDataDepositEvents.E186;
    }
}
