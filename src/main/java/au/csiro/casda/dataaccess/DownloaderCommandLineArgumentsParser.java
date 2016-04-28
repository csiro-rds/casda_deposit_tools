package au.csiro.casda.dataaccess;

import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.logging.CasdaMessageBuilder;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

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
 * Helper class to support command line parameter parsing for DownloaderCommandLineImporter.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class DownloaderCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<DownloaderCommandLineArgumentsParser.CommandLineArguments>
{

    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see JCommander <p>
     *      Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Download a file from NGAS to target location")
    public static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-fileId", description = "NGAS file id", required = true)
        private String fileId;

        @Parameter(names = "-name", description = "full destination file name", required = true)
        private String name;

        @Parameter(names = "-checksum", description = "Download checksum file", required = false)
        private boolean checksum;

        String getFileId()
        {
            return fileId;
        }

        String getName()
        {
            return name;
        }

        /**
         * Whether the checksum for the requested file should also be downloaded.
         * 
         * @return true if the checksum file should be downloaded.
         */
        boolean downloadChecksumFile()
        {
            return checksum;
        }

        void setFileId(String fileId)
        {
            this.fileId = fileId;
        }

        void setName(String name)
        {
            this.name = name;
        }
    }

    /**
     * Default constructor
     */
    public DownloaderCommandLineArgumentsParser()
    {
        super(NgasDownloader.TOOL_NAME, new CommandLineArguments());
    }

    /**
     * (non-Javadoc)
     * 
     * @see au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser#validate()
     */
    protected void validate()
    {
    }

    /**
     * (non-Javadoc)
     * 
     * @see AbstractCommandLineArgumentsParser#addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder)
     * @param builder
     *            casda message builder
     */
    @Override
    public void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder)
    {
        
        if (this.getArgs().getFileId() == null || this.getArgs().getFileId().isEmpty())
        {
            builder.add("NOT-SPECIFIED");
        }
        else
        {
            builder.add(this.getArgs().getFileId());
        }
        if (this.getArgs().getName() == null || this.getArgs().getName().isEmpty())
        {
            builder.add("NOT-SPECIFIED");
        }
        else
        {
            builder.add(this.getArgs().getName());
        }

    }
}
