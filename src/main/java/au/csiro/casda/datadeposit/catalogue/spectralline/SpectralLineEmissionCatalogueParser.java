package au.csiro.casda.datadeposit.catalogue.spectralline;

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


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueParser;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectralLineEmissionRepository;
import au.csiro.casda.entity.observation.SpectralLineEmission;

/**
 * Instances of this class are Spring components that orchestrate the parsing of a spectral line emission catalogue
 * datafile, in a VOTABLE format, and the persistence of the parsed data to the database.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SpectralLineEmissionCatalogueParser extends AbstractCatalogueParser<SpectralLineEmission>
{
    /**
     * @param observationRepository
     *            an ObservationRepository to used to find the Observation for a parsed catalogue file
     * @param catalogueRepository
     *            a CatalogueRepository to used to persist the parsed catalogue file
     * @param spectralLineEmissionRepository
     *            a spectralLineEmissionRepository to be used to manage the persistent spectralLinEmissions 
     *            associated with the parsed catalogue
     * @param spectralLineEmissionVoTableVisitor
     *            an spectralLineEmissionVoTableVisitor which will be used to visit a parsed catalogue file
     */
    public SpectralLineEmissionCatalogueParser(ObservationRepository observationRepository,
            CatalogueRepository catalogueRepository, SpectralLineEmissionRepository spectralLineEmissionRepository,
            SpectralLineEmissionVoTableVisitor spectralLineEmissionVoTableVisitor)
    {
        super(catalogueRepository, observationRepository, 
                spectralLineEmissionVoTableVisitor, spectralLineEmissionRepository);
        
    }
    
    /**
     * Creates a SpectralLineEmissionCatalogueParser which will use the given ObservationRepository and
     * CatalogueRepository for persistent record access.
     * 
     * @param observationRepository
     *            an ObservationRepository to used to find the Observation for a parsed catalogue file
     * @param catalogueRepository
     *            a CatalogueRepository to used to persist the parsed catalogue file
     * @param spectralLineEmissionRepository
     *            a SpectralLineEmissionRepository to be used to manage the persistent SpectralLine Emissions
     *            associated with the parsed catalogue
     */
    @Autowired
    public SpectralLineEmissionCatalogueParser(ObservationRepository observationRepository,
            CatalogueRepository catalogueRepository, SpectralLineEmissionRepository spectralLineEmissionRepository)
    {
        super(catalogueRepository, observationRepository, 
          new SpectralLineEmissionVoTableVisitor(spectralLineEmissionRepository), spectralLineEmissionRepository);

    }
}
