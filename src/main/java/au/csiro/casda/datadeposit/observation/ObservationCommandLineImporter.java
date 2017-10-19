package au.csiro.casda.datadeposit.observation;



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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Instant;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.observation.ObservationParser.MalformedFileException;
import au.csiro.casda.datadeposit.observation.ObservationParser.RepositoryException;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.DataLocation;
import au.csiro.logging.CasdaDataDepositEvents;

/**
 * CommandLineImporter implements a Spring CommandLineRunner that can be used to import RTC observation data contained
 * in an observation XML file.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class ObservationCommandLineImporter extends
        ArgumentsDrivenCommandLineTool<ObservationCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "observation_import";

    private static final Logger logger = LoggerFactory.getLogger(ObservationCommandLineImporter.class);

    private au.csiro.casda.datadeposit.observation.ObservationParser observationParser;

    private ObservationCommandLineArgumentsParser commandLineArgumentsParser =
            new ObservationCommandLineArgumentsParser();

    /**
     * main method used to run this CommandLineImporter
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(ObservationCommandLineImporter.class, logger, args);
    }

    /**
     * Constructor
     * 
     * @param observationParser
     *            a au.csiro.casda.datadeposit.observation.ObservationParser used to perform the import of an
     *            Observation metadata file.
     */
    @Autowired
    public ObservationCommandLineImporter(au.csiro.casda.datadeposit.observation.ObservationParser observationParser)
    {
        super();
        this.observationParser = observationParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {
        // use a JCommander to process the command line arguments
        parseCommandLineArguments(logger, args);

        logger.debug("Starting import of RTC observation metadata xml file");

        String infile = commandLineArgumentsParser.getArgs().getInfile();
        Integer sbid = commandLineArgumentsParser.getArgs().getSbid();
        boolean redeposit = commandLineArgumentsParser.getArgs().isRedeposit();

        Instant startTime = Instant.now();

        Observation observation = null;
        try
        {
            observation = observationParser.parseFile(sbid, infile, redeposit);
        }
        catch (FileNotFoundException | MalformedFileException | RepositoryException | 
                DataIntegrityViolationException e)
        {
            logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(CasdaDataDepositEvents.E003).add(sbid)
                    .toString(), e);
            
            //skip this if file or directory does not exist
            if (!(e instanceof FileNotFoundException))
            {
                File obsDirectory = new File(infile).getParentFile();
                if (obsDirectory != null)
                {
                    try
                    {
                        OutputStreamWriter output =
                                new OutputStreamWriter(new FileOutputStream(obsDirectory + "/ERROR"), Charsets.UTF_8);
                        output.write(e.getMessage());
                        output.close();
                    }
                    catch (IOException ioe)
                    {
                        logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(CasdaDataDepositEvents.E151)
                                .add(sbid).add(obsDirectory).toString(), ioe);
                    }
                }
            }
            
            System.exit(1);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOf(new File(this.getCommandLineArgumentsParser().getArgs().getInfile()));

        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E012.messageBuilder() //
                .add(sbid) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.RTC) //
                .addDestination(DataLocation.CASDA_DB) //
                .addVolumeBytes(filesizeInBytes) //
                .addFileId(observation.getUniqueIdentifier().replace("/", "-"));
        logger.info(messageBuilder.toString());

        System.exit(0);
    }

    /**
     * Gets the arguments parser. Mostly useful for unit testing and keeping the field prooivate with a getter keeps
     * checkstyle happy.
     * 
     * @return ObservationCommandLineArgumentsParser the parser
     */
    public ObservationCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        return CasdaDataDepositEvents.E033;
    }
}
