package au.csiro.casda.datadeposit.observation.jpa.repository;

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


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.SpectralLineEmission;

public interface TestSpectralLineEmissionRepository extends SpectralLineEmissionRepository
{
    /**
     * Returns a Page of spectral line emission associated with given Catalogue ordered by Id asc
     * 
     * @param catalogue
     *            a Catalogue
     * @param pageable
     *            parameters for the paged query
     * @return a Page of spectral line emissions
     */
    Page<SpectralLineEmission> findByCatalogueOrderByIdAsc(Catalogue catalogue, Pageable pageable);
}
