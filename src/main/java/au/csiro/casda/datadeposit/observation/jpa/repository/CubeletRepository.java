package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import au.csiro.casda.entity.observation.Cubelet;

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
 * JPA Repository declaration.
 * <p>
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
@Repository
public interface CubeletRepository extends CrudRepository<Cubelet, Long> 
{
    /**
     * Returns the first Cubelet associated with the given Observation (by sbid) and filename
     * 
     * @param sbid
     *            the sbid of an Observation
     * @param filename
     *            a String
     * @return a Cubelet
     */
    public Cubelet findByObservationSbidAndFilename(Integer sbid, String filename);

    /**
     * Returns the first Cubelet associated with the given Level7CollectionId and filename
     * 
     * @param level7CollectionId
     *            the level7CollectionId of a level 7 Collection
     * @param filename
     *            a String
     * @return a Cubelet
     */
    public Cubelet findByLevel7CollectionDapCollectionIdAndFilename(long level7CollectionId, String filename);
}
