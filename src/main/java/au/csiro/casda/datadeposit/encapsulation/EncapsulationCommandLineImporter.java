package au.csiro.casda.datadeposit.encapsulation;

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
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.ParentType;
import au.csiro.casda.datadeposit.encapsulation.EncapsulationCommandLineArgumentsParser.CommandLineArguments;
import au.csiro.casda.datadeposit.encapsulation.EncapsulationService.EncapsulationException;
import au.csiro.casda.datadeposit.exception.CreateChecksumException;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.logging.DataLocation;
import au.csiro.logging.CasdaDataDepositEvents;

/**
 * CommandLineImporter implements a Spring CommandLineRunner that can be used to encapsulate a set of smaller files into
 * a single tar file for efficient processing and storage.
 * <p>
 * Copyright 2016, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class EncapsulationCommandLineImporter
        extends ArgumentsDrivenCommandLineTool<EncapsulationCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "encapsulate";

    private static final Logger logger = LoggerFactory.getLogger(EncapsulationCommandLineImporter.class);

    /**
     * The parser for the arguments for this CommandLineRunner.
     */
    private EncapsulationCommandLineArgumentsParser commandLineArgumentsParser =
            new EncapsulationCommandLineArgumentsParser();

    private EncapsulationService encapsulationService;

    /**
     * The arguments passed in to the program from the command line by the operator
     * 
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(EncapsulationCommandLineImporter.class, logger, args);
    }

    
    /**
     * Create a new EncapsulationCommandLineImporter instance
     * 
     * @param encapsulationService
     *            The service instance that will do the encapsulation processing.
     */
    @Autowired
    public EncapsulationCommandLineImporter(EncapsulationService encapsulationService)
    {
        this.encapsulationService = encapsulationService;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {

        parseCommandLineArguments(logger, args);

        Instant startTime = Instant.now();

        int numFiles = 0;

        CommandLineArguments parsedArgs = commandLineArgumentsParser.getArgs();
        try
        {
            String inFile = parsedArgs.getInfile();
            String encapsFilename = parsedArgs.getEncapsFilename();
            int parentId = parsedArgs.getParentId();
            ParentType parentType = ParentType.findParentType(parsedArgs.getParentType());
            String pattern = parsedArgs.getPattern();
            boolean eval = parsedArgs.getEval();

            numFiles = produceEncapsulation(parentId, encapsFilename, inFile, pattern, parentType, eval);
        }
        catch (EncapsulationException | CreateChecksumException e)
        {
            /*
             * if the encapsulation file cannot be created
             */
            logger.error(CasdaDataDepositEvents.E155.messageBuilder().add(parsedArgs.getInfile())
                    .add(parsedArgs.getParentId()).toString(), e);
            System.exit(1);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOf(new File(parsedArgs.getInfile()));
        String fileId = getFileId();

        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E154.messageBuilder() //
                .add(numFiles) //
                .add(parsedArgs.getPattern()) //
                .add(parsedArgs.getInfile()) //
                .add(parsedArgs.getParentId()) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.RTC) //
                .addDestination(DataLocation.RTC) //
                .addVolumeBytes(filesizeInBytes) //
                .addFileId(fileId);
        logger.info(messageBuilder.toString());

        System.exit(0);
    }

    /**
     * Create and verify an encapsulation file.
     * 
     * @param parentId
     *            The primary scheduling block id.
     * @param encapsulationFilename
     *            The name of the file to be created
     * @param inFile
     *            The full path to the file to be created
     * @param pattern
     *            The pattern of files that should be in the encapsulation.
     * @param parentType 
     *            the type of parent object for this encapsulation
     * @param eval 
     *            boolean denoting if this encapsulation file is for evaluation files
     * @return The number of files (apart from checksums) that were encapsulated.
     * 
     * @throws EncapsulationException
     *             If the encapsulation file cannot be created, or is invalid.
     * @throws CreateChecksumException
     *             If the checksum for the encapsulation file cannot be created.
     */
    int produceEncapsulation(int parentId, String encapsulationFilename, String inFile, String pattern,
            ParentType parentType, boolean eval) throws EncapsulationException, CreateChecksumException
    {
    	encapsulationService.verifyChecksums(inFile, encapsulationFilename, pattern, parentId, eval);

        int numFiles = encapsulationService.createEncapsulation(parentId, encapsulationFilename, inFile, pattern, eval);

        encapsulationService.updateEncapsulationFileWithMetadata(parentId, encapsulationFilename, new File(inFile),
                parentType);

        return numFiles;
    }

    private String getFileId()
    {
        CommandLineArguments args = commandLineArgumentsParser.getArgs();

        EncapsulationFile encaps = new EncapsulationFile();
        encaps.setFilename(args.getEncapsFilename());
        if (CommonCommandLineArguments.LEVEL7_PARENT_TYPE.equalsIgnoreCase(args.getParentType()))
        {
            encaps.setParent(new Level7Collection(args.getParentId()));
        }
        else
        {
            encaps.setParent(new Observation(args.getParentId()));
        }
        return encaps.getFileId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractCommandLineArgumentsParser<CommandLineArguments> getCommandLineArgumentsParser()
    {
        return this.commandLineArgumentsParser;
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
