package au.csiro.casda.datadeposit.fits;

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
 * Helper class to support command line parameter parsing for FitsCommandLineImporter.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class FitsCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<FitsCommandLineArgumentsParser.CommandLineArguments>
{

    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see JCommander <p>
     *      Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Import a FITS data file")
    static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-infile", description = "the actual path to the fits data file to import", required = true)
        private String infile;

        @Parameter(names = "-imageCubeFilename",
                description = "the filename of the fits file (used for looking up an image cube) "
                        + "if different from the infile argument", required = true)
        private String imageCubeFilename;

        @Parameter(names = "-sbid", description = "the scheduling block id of the Observation", required = true)
        private int sbid;

        /**
         * @return the infile argument (if supplied)
         */
        public String getInfile()
        {
            return infile;
        }

        /**
         * @return the filename argument (if supplied)
         */
        public String getImageCubeFilename()
        {
            return imageCubeFilename;
        }

        /**
         * @return the sbid argument (if supplied)
         */
        public int getSbid()
        {
            return sbid;
        }
    }

    /**
     * Constructs a FitsCommandLineArgumentsParser
     */
    FitsCommandLineArgumentsParser()
    {
        super(FitsCommandLineImporter.TOOL_NAME, new CommandLineArguments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder)
    {
        // Nothing to add.
    }
}
