package au.csiro.casda.datadeposit.encapsulation;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.logging.CasdaMessageBuilder;

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
 * Helper class to support command line parameter parsing for EncapsaulationCommandLineImporter.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class EncapsulationCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<EncapsulationCommandLineArgumentsParser.CommandLineArguments>
{

    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see JCommander <p>
     */
    @Parameters(commandDescription = "Encapsulate a set of smaller files")
    static class CommandLineArguments extends CommonCommandLineArguments
    {

        @Parameter(names = "-infile", description = "the actual path to the encapsulation file to create", required = true)
        private String infile;

        @Parameter(names = "-encapsFilename",
                description = "the filename of the tar file (used for looking up the object)",
                required = true)
        private String encapsFilename;

        @Parameter(names = "-pattern",
                description = "the pattern of the small files to be encapsulated", required = false)
        private String pattern = null;

        @Parameter(names = "-parent-id", description = "the scheduling block id of the Observation", required = true)
        private String parentId;

        @Parameter(names = "-parent-type", description = "the kind of deposit to stage "
                + "(observation or derived-catalogue) ", required = true)
        private String parentType;
        
        @Parameter(names = "-eval", description = "The switch which signifies that this encapsulation is for evaluation"
        		+ " files", required = false)
        private boolean eval = false;

        /**
         * @return the infile argument (if supplied)
         */
        public String getInfile()
        {
            return infile;
        }

        /**
         * @return the encapsulation filename argument (if supplied)
         */
        public String getEncapsFilename()
        {
            return encapsFilename;
        }

        public String getPattern()
        {
            return pattern;
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
        
		public boolean getEval() 
		{
			return eval;
		}

		public void setEval(boolean eval) 
		{
			this.eval = eval;
		}
    }

    /**
     * Constructs a FitsCommandLineArgumentsParser
     */
    EncapsulationCommandLineArgumentsParser()
    {
        super(EncapsulationCommandLineImporter.TOOL_NAME, new CommandLineArguments());
    }

    @Override
    protected void validate() throws ParameterException
    {
        super.validate();

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
