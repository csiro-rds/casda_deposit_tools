package au.csiro.casda.datadeposit.catalogue;

import org.springframework.context.ApplicationContext;

import au.csiro.casda.datadeposit.catalogue.continuum.ContinuumComponentCatalogueParser;
import au.csiro.casda.datadeposit.catalogue.continuum.ContinuumIslandCatalogueParser;
import au.csiro.casda.datadeposit.catalogue.continuum.PolarisationComponentCatalogueParser;
import au.csiro.casda.datadeposit.catalogue.level7.Level7CatalogueParser;
import au.csiro.casda.datadeposit.catalogue.spectralline.SpectralLineAbsorptionCatalogueParser;
import au.csiro.casda.datadeposit.catalogue.spectralline.SpectralLineEmissionCatalogueParser;

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
 * Enumeration of all possible catalogue types.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public enum CatalogueType
{
    /**
     * The 'continuum island' catalogue type.
     */
    CONTINUUM_ISLAND
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends CatalogueParser> getParserClass()
        {
            return ContinuumIslandCatalogueParser.class;
        }
    },

    /**
     * The 'continuum component' catalogue type.
     */
    CONTINUUM_COMPONENT
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends CatalogueParser> getParserClass()
        {
            return ContinuumComponentCatalogueParser.class;
        }
    },

    /**
     * The 'polarisation component' catalogue type.
     */
    POLARISATION_COMPONENT
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends CatalogueParser> getParserClass()
        {
            return PolarisationComponentCatalogueParser.class;
        }
    },
    
    /**
     * The 'spectral line absorption' catalogue type.
     */
    SPECTRAL_LINE_ABSORPTION
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends CatalogueParser> getParserClass()
        {
            return SpectralLineAbsorptionCatalogueParser.class;
        }
    },
    
    /**
     * The 'spectral line emission' catalogue type.
     */
    SPECTRAL_LINE_EMISSION
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends CatalogueParser> getParserClass()
        {
            return SpectralLineEmissionCatalogueParser.class;
        }
    },

    /**
     * The 'Level 7' catalogue type.
     */
    DERIVED_CATALOGUE
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends CatalogueParser> getParserClass()
        {
            return Level7CatalogueParser.class;
        }
    };

    /**
     * Creates a specific CatalogueParser to parse a catalogue file of this type.
     * 
     * @param context
     *            a Spring ApplicationContext that will be used to instantiate the right parser class.
     * @return a CatalogueParser
     */
    public CatalogueParser createParser(ApplicationContext context)
    {
        return (CatalogueParser) context.getAutowireCapableBeanFactory().createBean(this.getParserClass());
    }

    /**
     * Template method for enumeration values to implement
     * 
     * @return the CatalogueParser subclass that can be used to parse catalogue files of this type
     */
    protected abstract Class<? extends CatalogueParser> getParserClass();

    /**
     * @return a human-readable description of this catalogue type
     */
    public String getDescription()
    {
        return this.toString().toLowerCase().replace("_", " ");
    }
}
