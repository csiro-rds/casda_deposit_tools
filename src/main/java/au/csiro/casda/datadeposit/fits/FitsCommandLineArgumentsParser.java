package au.csiro.casda.datadeposit.fits;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.logging.CasdaMessageBuilder;
import au.csiro.util.CasdaStringUtils;

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
        private static String getValidFitsObjectTypeValuesDescription()
        {
            String[] fitsObjectTypeValues = new String[FitsObjectType.values().length];
            for (int i = 0; i < fitsObjectTypeValues.length; i++)
            {
                fitsObjectTypeValues[i] = FitsObjectType.values()[i].toString().toLowerCase().replace("_", "-");
            }
            return CasdaStringUtils.joinStringsForDisplay(fitsObjectTypeValues, ",", "or");
        }

        @Parameter(names = "-infile", description = "the actual path to the fits data file to import", required = true)
        private String infile;

        @Parameter(names = "-fitsFilename",
                description = "the filename of the fits file (used for looking up an image cube or spectrum)",
                required = true)
        private String fitsFilename;

        @Parameter(names = "-fits-type", description = 
        		"the kind of FITS file to import (image-cube, moment-map, cubelet or spectrum)", required = true)
        private String fitsType;

        @Parameter(names = "-parent-id", description = "the scheduling block id of the Observation", required = true)
        private String parentId;

        @Parameter(names = "-parent-type", description = "the kind of deposit to stage "
                + "(observation or derived-catalogue) ", required = true)
        private String parentType;

        @Parameter(names = "-refresh", description = "Indicates that this is a refrsh of an existing FITS object.",
                required = false)
        private boolean refresh = false;

        /**
         * @return the infile argument (if supplied)
         */
        public String getInfile()
        {
            return infile;
        }

        /**
         * @return the fits filename argument (if supplied)
         */
        public String getFitsFilename()
        {
            return fitsFilename;
        }

        /**
         * Convert from the command-line value (see the description for the fitsType @Parameter above) to an
         * enumeration value.
         * @return the converted value
         */
        public FitsObjectType getFitsObjectType()
        {
            return FitsObjectType.valueOf(fitsType.toUpperCase().replace("-", "_"));
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

        public boolean isRefresh()
        {
            return refresh;
        }
    }

    /**
     * Constructs a FitsCommandLineArgumentsParser
     */
    FitsCommandLineArgumentsParser()
    {
        super(FitsCommandLineImporter.TOOL_NAME, new CommandLineArguments());
    }

    @Override
    protected void validate() throws ParameterException
    {
        super.validate();
        
        try
        {
            getArgs().getFitsObjectType();
        }
        catch (Exception e)
        {
            throw new ParameterException("Parameter fits-type must be either "
                    + CommandLineArguments.getValidFitsObjectTypeValuesDescription());
        }
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
            throw new ParameterException("Parameter parent-type must be either 'derived-catalogue' or 'observation'");
        }

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
