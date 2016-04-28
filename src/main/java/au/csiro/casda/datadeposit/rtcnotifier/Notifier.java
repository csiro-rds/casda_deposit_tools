package au.csiro.casda.datadeposit.rtcnotifier;

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

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.logging.CasdaDataDepositEvents;

/**
 * Notifier implements a Spring CommandLineRunner that can be used to create an RTC notification 'DONE' file to signal
 * successful completion of data import task to RTC
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class Notifier extends ArgumentsDrivenCommandLineTool<NotifierCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "rtc_notify";

    private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

    /**
     * The DONE file name
     */
    public static final String DONE_FILE = "DONE";

    private NotifierCommandLineArgumentsParser commandLineArgumentsParser = new NotifierCommandLineArgumentsParser();

    /**
     * main method used to run this CommandLineImporter
     * 
     * @param args
     *            the command-line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(Notifier.class, logger, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {
        parseCommandLineArguments(logger, args);

        process(getCommandLineArgumentsParser().getArgs().getInfile(), getCommandLineArgumentsParser().getArgs()
                .getSbid());

        logger.info(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(CasdaDataDepositEvents.E054)
                .add(getCommandLineArgumentsParser().getArgs().getSbid()).toString());

        System.exit(0);
    }

    /**
     * Places 'DONE' file in the dataset directory (passed as a command line argument) to notify RTC that this dataset
     * has been successfully processed.
     * 
     * 
     * @param inPath
     *            Name of directory to place the 'DONE' file to
     * @param sbid
     *            Numeric scheduling block id needed for reporting purposes only
     */
    public void process(String inPath, int sbid)
    {

        try
        {
            Path dirPath = Paths.get(inPath); // will throw Invalid Path exception if invalid path

            // This should throw I/O exception if can't read attributes of the directory
            if (!(boolean) Files.getAttribute(dirPath, "basic:isDirectory"))
            {
                logger.error(CasdaDataDepositEvents.E010.messageBuilder().add(sbid).add(inPath)
                        .addCustomMessage(String.format("'infile' is not a directory")).toString());
                System.exit(1);
            }

            Path doneFilePath = dirPath.resolve(DONE_FILE);

            try
            {
                this.createFile(doneFilePath);
            }
            catch (FileAlreadyExistsException e)
            {
                // This is actually a 'success' but we just warn about the condition
                logger.warn(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(CasdaDataDepositEvents.E008).add(sbid)
                        .toString());
            }
            catch (UnsupportedOperationException | IOException | SecurityException e) // other reasons
            {
                logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(CasdaDataDepositEvents.E010)
                        .add(sbid).add(inPath).toString(), e);
                System.exit(1);
            }

        }
        catch (IOException | InvalidPathException e)
        {
            logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(CasdaDataDepositEvents.E010).add(sbid)
                    .add(inPath).toString(), e);
            System.exit(1);
        }
    }

    /**
     * This is a proxy for Files.create(path) for testing purposes. Static methods can be mocked with PowerMockito, but
     * PowerMockito class loading seems to be in conflict with Spring.
     * 
     * @param file
     *            path of the file to create
     * 
     * @return created file path
     * 
     * @throws FileAlreadyExistsException
     *             when DONE file already exists
     * @throws UnsupportedOperationException
     *             - should never happen
     * @throws IOException
     *             when having a filesystem problem
     * @throws SecurityException
     *             - should never happen
     */
    public Path createFile(Path file) throws FileAlreadyExistsException, UnsupportedOperationException, IOException,
            SecurityException
    {
        return Files.createFile(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NotifierCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return this.commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        return CasdaDataDepositEvents.E069;
    }
}
