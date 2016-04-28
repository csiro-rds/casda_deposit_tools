package au.csiro.casda.datadeposit.catalogue;

import org.apache.commons.lang3.StringUtils;

import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.Mode;
import au.csiro.casda.logging.CasdaMessageBuilder;
import au.csiro.util.CasdaStringUtils;

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
 * Package-level helper class to support command line parameter parsing for CatalogueCommandLineImporter.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
class CatalogueCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<CatalogueCommandLineArgumentsParser.CommandLineArguments>
{

    /**
     * Describes and holds argument values. See {@link com.beust.jcommander.JCommander}
     * <p>
     * Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Import a continuum catalogue data file")
    static class CommandLineArguments extends CommonCommandLineArguments
    {
        private static String getValidCatalogueTypeValuesDescription()
        {
            String[] catalogueTypeValues = new String[CatalogueType.values().length];
            for (int i = 0; i < catalogueTypeValues.length; i++)
            {
                catalogueTypeValues[i] = CatalogueType.values()[i].toString().toLowerCase().replace("_", "-");
            }
            return CasdaStringUtils.joinStringsForDisplay(catalogueTypeValues, ",", "or");
        }

        @Parameter(names = "-parent-id", description = "the scheduling block id of the Observation, "
                + "or the Level 7 Collection id", required = true)
        private String parentId;

        @Parameter(names = "-catalogue-type", description = "the kind of catalogue to import (continuum-island, "
               + "or continuum-component, polarisarion-component, spectral-line-absorption, spectral-line-emission, "
               + "or level7", required = true)
        private String catalogueType;

        @Parameter(names = "-catalogue-filename", description = "the filename of the catalogue file "
                + "(used for looking up the catalogue) " + "if different from the infile argument", required = true)
        private String catalogueFilename;

        @Parameter(names = "-infile", description = "the catalog data file to import", required = true)
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

        public CatalogueType getCatalogueType()
        {
            /*
             * Convert from the command-line value (see the description for the catalogueType @Parameter above) to an
             * enumeration value.
             */
            return CatalogueType.valueOf(catalogueType.toUpperCase().replace("-", "_"));
        }

        /**
         * @return the catalogueFilename argument (if supplied)
         */
        public String getCatalogueFilename()
        {
            return catalogueFilename;
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
    CatalogueCommandLineArgumentsParser()
    {
        super(CatalogueCommandLineImporter.TOOL_NAME, new CommandLineArguments());
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
        try
        {
            getArgs().getCatalogueType();
        }
        catch (Exception e)
        {
            throw new ParameterException("Parameter catalogue-type must be either "
                    + CommandLineArguments.getValidCatalogueTypeValuesDescription());
        }
    }

    /**
     * {@inheritDoc}
     */
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
