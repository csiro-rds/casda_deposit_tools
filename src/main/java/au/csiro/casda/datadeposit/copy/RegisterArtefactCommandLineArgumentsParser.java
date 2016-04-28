package au.csiro.casda.datadeposit.copy;

import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.logging.CasdaMessageBuilder;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
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
 * Package-level helper class to support command line parameter parsing for CopyArtefactCommandLineTool.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class RegisterArtefactCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<RegisterArtefactCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see com.beust.jcommander.JCommander <p>
     *      Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Register a depositable artefact with NGAS. "
            + "(The artefact must already have been staged to the NGAS staging area.)")
    static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-parent-id", 
                description = "the scheduling block id of the Observation or level7 collection id", required = true)
        private String parentId;

        @Parameter(names = "-infile", description = "the full file path of the depositable artefact", required = true)
        private String infile;

        @Parameter(names = "-staging_volume",
                description = "the 'volume' to which the depositable artefact has been staged", required = true)
        private String stagingVolume;

        @Parameter(names = "-file_id", description = "the file_id of the file in NGAS", required = true)
        private String fileId;

        /**
         * @return the parentId argument (if supplied)
         */
        public Integer getParentId()
        {
            return Integer.parseInt(getRawParentId());
        }

        /**
         * @return the type argument (if supplied)
         */
        public String getStagingVolume()
        {
            return stagingVolume;
        }

        /**
         * @return the filename argument (if supplied)
         */
        public String getInfile()
        {
            return infile;
        }

        /**
         * @return the fileId argument (if supplied)
         */
        public String getFileId()
        {
            return fileId;
        }

        private String getRawParentId()
        {
            return parentId;
        }

    }

    /**
     * Constructs a RegisterArtefactCommandLineArgumentsParser
     */
    RegisterArtefactCommandLineArgumentsParser()
    {
        super(RegisterArtefactCommandLineTool.TOOL_NAME, new CommandLineArguments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder)
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate() throws ParameterException
    {
        try
        {
            getArgs().getParentId();
        }
        catch (NumberFormatException e)
        {
            throw new ParameterException("Parameter parent-id must be an integer");
        }
    }
}
