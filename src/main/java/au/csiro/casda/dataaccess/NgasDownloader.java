package au.csiro.casda.dataaccess;

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
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.service.NgasService;
import au.csiro.casda.datadeposit.service.NgasService.ServiceCallException;
import au.csiro.casda.datadeposit.service.NgasService.Status;
import au.csiro.casda.logging.CasdaEvent;
import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.DataLocation;
import au.csiro.casda.logging.LogEvent;

/**
 * NGAS downloader implements a command line process that issues a downloading command to NGAS, waits for its
 * completion, reports success or failure in the DB and terminates.
 * 
 * Copyright 2015, CSIRO Australia All rights reserved.
 */
@Import(AppConfig.class)
@Component
public class NgasDownloader extends
        ArgumentsDrivenCommandLineTool<DownloaderCommandLineArgumentsParser.CommandLineArguments>
{
    private static final Logger logger = LoggerFactory.getLogger(NgasDownloader.class);

    /** downloading command name */
    public static final String TOOL_NAME = "ngas_download";

    private String propertiesVersion;

    private MiddlewareClient middlewareClient;

    private NgasService ngasService;

    private DownloaderCommandLineArgumentsParser commandLineArgumentsParser;

    /**
     * Constructor with args. Also instantiates the command line args parser.
     * 
     * @param propertiesVersion
     *            the properties used
     * @param middlewareClient
     *            the middleware client (ngas)
     * @param ngasService
     *            the ngas service
     */
    @Autowired
    public NgasDownloader(@Value("${properties.version}") String propertiesVersion, MiddlewareClient middlewareClient,
            NgasService ngasService)
    {
        super();
        this.propertiesVersion = propertiesVersion;
        this.middlewareClient = middlewareClient;
        this.ngasService = ngasService;
        this.commandLineArgumentsParser = new DownloaderCommandLineArgumentsParser();
    }

    /**
     * Main
     * 
     * @param args
     *            -fileId - file id of the file to download -name - full name of the destination file
     */
    public static void main(String[] args)
    {
        runCommandLineTool(NgasDownloader.class, logger, args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.boot.CommandLineRunner#run(java.lang.String[])
     */
    @Override
    public void run(String... args)
    {
        logger.debug("Started NGAS downloader, properties version: {}, args: {}", propertiesVersion,
                Arrays.toString(args));

        parseCommandLineArguments(logger, args);

        String fileId = getCommandLineArgumentsParser().getArgs().getFileId();
        String name = getCommandLineArgumentsParser().getArgs().getName();
        boolean downloadChecksumFile = getCommandLineArgumentsParser().getArgs().downloadChecksumFile();

        Instant startTime = Instant.now();

        boolean success = downloadFileAndCreateChecksumFromNgas(fileId, name, downloadChecksumFile);

        if (success)
        {
            Instant endTime = Instant.now();

            long filesizeInBytes = FileUtils.sizeOf(new File(this.getCommandLineArgumentsParser().getArgs().getName()));

            DataDepositMessageBuilder messageBuilder = CasdaDataAccessEvents.E134.messageBuilder() //
                    .add(commandLineArgumentsParser.getArgs().getFileId()) //
                    .addStartTime(startTime) //
                    .addEndTime(endTime) //
                    .addSource(DataLocation.ARCHIVE) //
                    .addDestination(DataLocation.DATA_ACCESS) //
                    .addVolumeBytes(filesizeInBytes) //
                    .addFileId(this.getCommandLineArgumentsParser().getArgs().getFileId());
            logger.info(messageBuilder.toString());
        }
        System.exit(success ? 0 : 1);
    }

    /**
     * Downloads file from NGAS and creates the associated checksum file, if requested, by querying the status of the
     * file from NGAS.
     * 
     * @param fileId
     *            the NGAS file id of the file to download
     * @param name
     *            the full path to the destination filename
     * @param downloadChecksumFile
     *            if true, creates a checksum file for the downloaded file, called name.checksum which will put it in
     *            the same folder as the downloaded file
     * @return boolean if successfully downloads file and creates checksum file
     */
    protected boolean downloadFileAndCreateChecksumFromNgas(String fileId, String name, boolean downloadChecksumFile)
    {
        boolean success = true;

        try
        {
            downloadFileFromNgas(fileId, name);
        }
        catch (MiddlewareClientException e)
        {
            logger.error(CasdaDataAccessEvents.E030.messageBuilder().add("file").add(fileId).toString());
            success = false;
        }

        if (downloadChecksumFile)
        {
            try
            {
                createChecksumFile(fileId, name);
            }
            catch (IOException | ServiceCallException e)
            {
                logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(LogEvent.UNKNOWN_EVENT).toString(), e);
                success = false;
            }
        }

        return success;

    }

    /**
     * Downloads file with the given id from NGAS.
     * 
     * 
     * @param fileId
     *            file id of the file to download
     * @param name
     *            full name of the destination file
     * @throws MiddlewareClientException
     *             if there is a problem downloading the file from NGAS
     */
    protected void downloadFileFromNgas(String fileId, String name) throws MiddlewareClientException
    {
        logger.debug("Downloading file {} to {}", fileId, name);
        Retriever retriever = new Retriever(new File(name));
        middlewareClient.retrieve(fileId, retriever);

    }

    /**
     * Creates a checksum file with the value retrieved from NGAS for a given fileId. The checksum filename will be
     * fileId.checksum
     * 
     * @param fileId
     *            file id of the file to create the checksum for
     * @param name
     *            full name of the destination file
     * 
     * @throws ServiceCallException
     *             if there is a problem querying the status of the file from NGAS
     * @throws IOException
     *             if there is a problem writing the checksum file
     */
    protected void createChecksumFile(String fileId, String name) throws ServiceCallException, IOException
    {

        Status status = ngasService.getStatus(fileId);
        String checksumFileId = name + ".checksum";
        if (status.wasSuccess())
        {
            String checksum = status.getChecksum();
            if (StringUtils.isBlank(checksum))
            {
                throw new ServiceCallException(String.format("Checksum empty for %s: %s", fileId, status.toString()));
            }
            createFile(checksumFileId, checksum);
        }
        else
        {
            throw new ServiceCallException(String.format("Status check for %s was unsuccessful: %s", fileId,
                    status.toString()));
        }
    }

    /**
     * Creates a file at the given location with the given contents.
     * 
     * @param path
     *            the location where the file is created
     * @param contents
     *            the contents of the file
     * @throws IOException
     *             if there is any problem creating the file
     */
    protected void createFile(String path, String contents) throws IOException
    {
        FileUtils.writeStringToFile(new File(path), contents);
    }

    @Override
    protected DownloaderCommandLineArgumentsParser getCommandLineArgumentsParser()
    {
        return commandLineArgumentsParser;
    }

    @Override
    protected CasdaEvent getMalformedParametersEvent()
    {
        return CasdaDataAccessEvents.E100;
    }
}
