package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import au.csiro.casda.entity.observation.MomentMap;

/*
 * #%L
 * CSIRO ASKAP Science Data Archive
 * %%
 * Copyright (C) 2016 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * JPA Repository declaration.
 * <p>
 * Copyright 2016, CSIRO Australia. All rights reserved.
 */
@Repository
public interface MomentMapRepository extends CrudRepository<MomentMap, Long>
{
    /**
     * Returns the first MomentMap associated with the given Observation (by sbid) and filename
     * 
     * @param sbid
     *            the sbid of an Observation
     * @param filename
     *            a String
     * @return a MomentMap
     */
    public MomentMap findByObservationSbidAndFilename(Integer sbid, String filename);

    /**
     * Returns the first MomentMap associated with the given Level7CollectionId and filename
     * 
     * @param level7CollectionId
     *            the level7CollectionId of a level 7 Collection
     * @param filename
     *            a String
     * @return a MomentMap
     */
    public MomentMap findByLevel7CollectionDapCollectionIdAndFilename(long level7CollectionId, String filename);
}
