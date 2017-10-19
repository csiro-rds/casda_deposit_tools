package au.csiro.casda.datadeposit.validationmetric;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.Mode;
import au.csiro.casda.logging.CasdaMessageBuilder;

/*
 * #%L
 * CSIRO ASKAP Science Data Archive
 * %%
 * Copyright (C) 2017 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * Package-level helper class to support command line parameter parsing for ValidationMetricCommandLineImporter.
 * <p>
 * Copyright 2017, CSIRO Australia All rights reserved.
 */
class ValidationMetricCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<ValidationMetricCommandLineArgumentsParser.CommandLineArguments>
{
	/**
     * Describes and holds argument values. See {@link com.beust.jcommander.JCommander}
     * <p>
     * Copyright 2017, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Import a validation metric data file")
    static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-parent-id", description = "the scheduling block id of the Observation, "
                + "or the Level 7 Collection id", required = true)
        private String parentId;

        @Parameter(names = "-filename", description = "the filename of the validation metric file "
                + "if different from the infile argument", required = true)
        private String filename;

        @Parameter(names = "-infile", description = "the validation metric data file to import", required = true)
        private String infile;

        @Parameter(names = "-validate-only", description = "Validation mode", required = false)
        private boolean validateOnly = false;


        /**
         * @return the parent-id argument (if supplied)
         */
        public Integer getParentId()
        {
            return Integer.parseInt(parentId);
        }

        /**
         * @return the catalogueFilename argument (if supplied)
         */
        public String getFilename()
        {
            return filename;
        }

        /**
         * @return the infile argument (if supplied)
         */
        public String getInfile()
        {
            return infile;
        }

        private String getRawParentId()
        {
            return parentId;
        }

        /**
         * @return whether the 'validate-only' flag was supplied
         */
        public boolean isValidateOnly()
        {
            return this.validateOnly;
        }

        /**
         * @return the mode associated with the validate-only setting: VALIDATE_ONLY or NORMAL
         */
        public Mode getMode()
        {
            return isValidateOnly() ? CatalogueParser.Mode.VALIDATE_ONLY : CatalogueParser.Mode.NORMAL;
        }
    }
	
    /**
     * Package-visible Constructor.
     */
	ValidationMetricCommandLineArgumentsParser()
    {
        super(ValidationMetricCommandLineImporter.TOOL_NAME, new CommandLineArguments());
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

	@Override
	public void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder) 
	{
        if (StringUtils.isBlank(this.getArgs().getRawParentId()))
        {
            builder.add("NOT-SPECIFIED");
        }
        else
        {
            builder.add(this.getArgs().getRawParentId());
        }
	}
}