package au.csiro.casda.datadeposit.observation;

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
 * Helper class to support command line parameter parsing for ObservationCommandLineImporter.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class ObservationCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<ObservationCommandLineArgumentsParser.CommandLineArguments>
{

    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see JCommander <p>
     *      Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Import an Observation metadata data file")
    public static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-infile", description = "the actual path to the fits data file to import", required = true)
        private String infile;

        @Parameter(names = "-sbid", description = "the scheduling block id of the Observation", required = true)
        private int sbid;

        @Parameter(names = "-redeposit",
                description = "this is a redeposit adding new data products to an already deposited Observation",
                required = false)
        private boolean redeposit = false;

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
        public int getSbid()
        {
            return sbid;
        }

        public boolean isRedeposit()
        {
            return redeposit;
        }
    }

    /**
     * Constructs a ObservationCommandLineArgumentsParser
     */
    public ObservationCommandLineArgumentsParser()
    {
        super(ObservationCommandLineImporter.TOOL_NAME, new CommandLineArguments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder)
    {
        // Nothing to add
    }
}
