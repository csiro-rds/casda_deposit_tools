package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import au.csiro.casda.entity.observation.Catalogue;
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
 * JPA Repository declaration.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 * @param <T> the data object type to be used with these generic functions
 */
public interface AbstractCatalogueEntryRepository<T>
{
    /**
     * Returns the number of catalogue Entities associated with the given Catalogue
     * 
     * @param catalogue
     *            a Catalogue
     * @return a number
     */
    long countByCatalogue(Catalogue catalogue);

    /**
     * Returns a Page of catalogue Entities associated with given Catalogue
     * 
     * @param catalogue
     *            a Catalogue
     * @param pageable
     *            parameters for the paged query
     * @return a Page of Entities
     */
    Page<T> findByCatalogue(Catalogue catalogue, Pageable pageable);
}
