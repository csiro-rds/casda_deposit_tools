package au.csiro.casda.datadeposit.rtcnotifier;

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
 * Helper class to support command line parameter parsing for NotifierCommandLineImporter.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class NotifierCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<NotifierCommandLineArgumentsParser.CommandLineArguments>
{

    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see JCommander <p>
     *      Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Notify the RTC that deposit of an observation has been successfully completed")
    public static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-infile", description = "the actual path to RTC source folder", required = true)
        private String infile;

        @Parameter(names = "-sbid", description = "the scheduling block id of the Observation", required = true)
        private String sbid;

        /**
         * @return the infile argument (if supplied)
         */
        public String getInfile()
        {
            return infile;
        }

        /**
         * @return the sbid argument (if supplied)
         */
        public Integer getSbid()
        {
            return Integer.parseInt(sbid);
        }

        private String getRawSbid()
        {
            return sbid;
        }
    }

    /**
     * Constructs a NotifierCommandLineArgumentsParser
     */
    public NotifierCommandLineArgumentsParser()
    {
        super(Notifier.TOOL_NAME, new CommandLineArguments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate() throws ParameterException
    {
        try
        {
            getArgs().getSbid();
        }
        catch (NumberFormatException e)
        {
            throw new ParameterException("Parameter sbid must be an integer");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder)
    {
        if (this.getArgs().getRawSbid() == null || this.getArgs().getRawSbid().isEmpty())
        {
            builder.add("NOT-SPECIFIED");
        }
        else
        {
            builder.add(this.getArgs().getRawSbid());
        }
    }
}
