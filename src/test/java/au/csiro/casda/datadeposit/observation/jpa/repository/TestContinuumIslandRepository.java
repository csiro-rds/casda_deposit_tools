package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.sourcedetect.ContinuumIsland;

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
 * Extension of ContinuumIslandRepository JPA Repository with additional methods for test cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@Repository
public interface TestContinuumIslandRepository extends ContinuumIslandRepository
{
    /**
     * Returns a Page of ContinuumIslands associated with given Catalogue ordered by Id asc
     * 
     * @param catalogue
     *            a Catalogue
     * @param pageable
     *            parameters for the paged query
     * @return a Page of ContinuumIslands
     */
    Page<ContinuumIsland> findByCatalogueOrderByIdAsc(Catalogue catalogue, Pageable pageable);
}
