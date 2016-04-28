package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

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
 */
@Repository
public interface CatalogueRepository extends CrudRepository<Catalogue, Long>
{
    /**
     * Returns the Catalogue associated with the given Observation (by sbid) and catalogue filename
     * 
     * @param sbid
     *            the sbid of an Observation
     * @param filename
     *            a String
     * @return a Catalogue
     */
    public Catalogue findByObservationSbidAndFilename(Integer sbid, String filename);

    /**
     * Returns the catalogue associated with the given Level 7 Collection (by collection id) and catalogue filename
     * 
     * @param level7CollectionId
     *            level 7 collection id
     * @param filename
     *            the catalogue filename
     * @return the matching Catalogue record
     */
    public Catalogue findByLevel7CollectionIdAndFilename(Long level7CollectionId, String filename);
}
