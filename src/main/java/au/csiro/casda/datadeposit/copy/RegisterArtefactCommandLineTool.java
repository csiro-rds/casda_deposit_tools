package au.csiro.casda.datadeposit.copy;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
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
 * Command-line tool used to register staged depositable artefacts with NGAS.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class RegisterArtefactCommandLineTool extends
        ArgumentsDrivenCommandLineTool<RegisterArtefactCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "register_artefact";

    private static final Logger logger = LoggerFactory.getLogger(RegisterArtefactCommandLineTool.class);

    private RegisterArtefactCommandLineArgumentsParser commandLineArgumentsParser =
            new RegisterArtefactCommandLineArgumentsParser();

    private NgasRegistrar registrar;

    /**
     * main method used to run this CommandLineImporter
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(RegisterArtefactCommandLineTool.class, logger, args);
    }

    /**
     * Constructor
     * 
     * @param registrar
     *            the service object used to perform registration of an artefact
     */
    @Autowired
    public RegisterArtefactCommandLineTool(NgasRegistrar registrar)
    {
        this.registrar = registrar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {
        parseCommandLineArguments(logger, args);

        logger.info(CasdaDataDepositEvents.E090.messageBuilder()
                .add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                .add(this.getCommandLineArgumentsParser().getArgs().getFileId())
                .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).toString());

        Instant startTime = Instant.now();

        try
        {
            registrar.registerArtefactWithNgas(this.getCommandLineArgumentsParser().getArgs().getParentId(), this
                    .getCommandLineArgumentsParser().getArgs().getInfile(), this.getCommandLineArgumentsParser()
                    .getArgs().getStagingVolume(), this.getCommandLineArgumentsParser().getArgs().getFileId());
        }
        catch (NoSuchFileException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E007.messageBuilder()
                            .add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                            .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).toString(), e);
            System.exit(1);
        }
        catch (RegisterException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E080.messageBuilder()
                            .add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                            .add(this.getCommandLineArgumentsParser().getArgs().getFileId())
                            .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).add("register")
                            .toString(), e);
            System.exit(1);
        }
        catch (ChecksumVerificationException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E006.messageBuilder()
                            .add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                            .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).toString(), e);
            System.exit(1);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOf(new File(this.getCommandLineArgumentsParser().getArgs().getInfile()));

        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E043.messageBuilder() //
                .add(this.getCommandLineArgumentsParser().getArgs().getInfile()) //
                .add(this.getCommandLineArgumentsParser().getArgs().getFileId()) //
                .add(this.getCommandLineArgumentsParser().getArgs().getParentId()) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.NGAS) //
                .addDestination(DataLocation.NGAS) //
                .addVolumeBytes(filesizeInBytes) //
                .addFileId(this.getCommandLineArgumentsParser().getArgs().getFileId());
        logger.info(messageBuilder.toString());

        System.exit(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegisterArtefactCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        return CasdaDataDepositEvents.E092;
    }

}
