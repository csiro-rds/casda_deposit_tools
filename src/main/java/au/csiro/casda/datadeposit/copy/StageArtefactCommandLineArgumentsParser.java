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
 * Package-level helper class to support command line parameter parsing for StageArtefactCommandLineTool.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class StageArtefactCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<StageArtefactCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see com.beust.jcommander.JCommander <p>
     *      Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Stage a depositable artefact to the NGAS staging area (ready to be registered).")
    static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-infile", description = "full file path of the depositable artefact", required = true)
        private String infile;
        
        @Parameter(names = "-parent-id", description = " Obeservation sbid or Level 7 Collection id", required = true)
        private String parentId;

        @Parameter(names = "-parent-type", description = "the kind of deposit to stage "
                    + "(observation or level7 ", required = true)
        private String parentType;

        @Parameter(names = "-staging_volume", description = "the 'volume' to which to stage the depositable artefact",
                required = true)
        private String stagingVolume;

        @Parameter(names = "-file_id", description = "the file_id of the file in NGAS", required = true)
        private String fileId;

        /**
         * @return the type argument (if supplied)
         */
        public String getStagingVolume()
        {
            return stagingVolume;
        }

        /**
         * @return the infile argument (if supplied)
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

        public Integer getParentId()
        {
            return Integer.parseInt(getRawParentId());
        }

        public String getParentType()
        {
            return parentType;
        }
        
        private String getRawParentId()
        {
            return parentId;
        }
    }

    /**
     * Constructs a StageArtefactCommandLineArgumentsParser.
     */
    StageArtefactCommandLineArgumentsParser()
    {
        super(StageArtefactCommandLineTool.TOOL_NAME, new CommandLineArguments());
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
        if(! CommonCommandLineArguments.LEVEL7_PARENT_TYPE.equalsIgnoreCase(getArgs().getParentType()) &&
                ! CommonCommandLineArguments.OBSERVATION_PARENT_TYPE.equalsIgnoreCase(getArgs().getParentType()))
        {
            throw new ParameterException("Parameter parent-type must be either 'level7' or 'observation'");
        }
    }
}
