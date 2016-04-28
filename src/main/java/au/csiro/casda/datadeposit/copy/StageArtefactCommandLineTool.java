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
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
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
 * Command-line tool used to copy depositable artefacts into the NGAS staging area (prior to registration with NGAS).
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class StageArtefactCommandLineTool extends
        ArgumentsDrivenCommandLineTool<StageArtefactCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "stage_artefact";

    private static final Logger logger = LoggerFactory.getLogger(StageArtefactCommandLineTool.class);

    private StageArtefactCommandLineArgumentsParser commandLineArgumentsParser =
            new StageArtefactCommandLineArgumentsParser();

    private NgasStager stager;

    /**
     * main method used to run this CommandLineImporter
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(StageArtefactCommandLineTool.class, logger, args);
    }

    /**
     * Constructor.
     * 
     * @param stager
     *            the staging service used to stage artefacts.
     */
    @Autowired
    public StageArtefactCommandLineTool(NgasStager stager)
    {
        this.stager = stager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {
        parseCommandLineArguments(logger, args);
        String parentType = this.getCommandLineArgumentsParser().getArgs().getParentType();
        CasdaDataDepositEvents copy_started;
        CasdaDataDepositEvents file_missing;
        CasdaDataDepositEvents checksum_missing;
        CasdaDataDepositEvents error_during_staging;
        CasdaDataDepositEvents copy_finished;
        if (CommonCommandLineArguments.OBSERVATION_PARENT_TYPE.equalsIgnoreCase(parentType))
        {
            copy_started = CasdaDataDepositEvents.E047;
            file_missing = CasdaDataDepositEvents.E015;
            checksum_missing = CasdaDataDepositEvents.E007;
            error_during_staging = CasdaDataDepositEvents.E001;
            copy_finished = CasdaDataDepositEvents.E091;
        }
        else
        {
            copy_started = CasdaDataDepositEvents.E128;
            file_missing = CasdaDataDepositEvents.E129;
            checksum_missing = CasdaDataDepositEvents.E130;
            error_during_staging = CasdaDataDepositEvents.E131;
            copy_finished = CasdaDataDepositEvents.E132;

        }
        logger.info(copy_started.messageBuilder().add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                .add(this.getCommandLineArgumentsParser().getArgs().getFileId())
                .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).toString());

        Instant startTime = Instant.now();

        try
        {
            stager.stageArtefactToNgas(this.getCommandLineArgumentsParser().getArgs().getParentId(), this
                    .getCommandLineArgumentsParser().getArgs().getInfile(), this.getCommandLineArgumentsParser()
                    .getArgs().getStagingVolume(), this.getCommandLineArgumentsParser().getArgs().getFileId());
        }
        catch (NoSuchFileException e)
        {
            logger.error(file_missing.messageBuilder().add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                    .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).toString(), e);
            System.exit(1);
        }
        catch (ChecksumMissingException e)
        {
            logger.error(
                    checksum_missing.messageBuilder().add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                            .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).toString(), e);
            System.exit(1);
        }
        catch (StagingException e)
        {
            logger.error(
                    error_during_staging.messageBuilder()
                            .add(this.getCommandLineArgumentsParser().getArgs().getInfile())
                            .add(this.getCommandLineArgumentsParser().getArgs().getParentId()).toString(), e);
            System.exit(1);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOf(new File(this.getCommandLineArgumentsParser().getArgs().getInfile()));

        DataDepositMessageBuilder messageBuilder = copy_finished.messageBuilder() //
                .add(this.getCommandLineArgumentsParser().getArgs().getInfile()) //
                .add(this.getCommandLineArgumentsParser().getArgs().getFileId()) //
                .add(this.getCommandLineArgumentsParser().getArgs().getParentId()) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.RTC) //
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
    public StageArtefactCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        return CasdaDataDepositEvents.E081;
    }

}
