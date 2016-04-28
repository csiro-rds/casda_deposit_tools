package au.csiro.casda.datadeposit.observation.jpa.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import au.csiro.casda.entity.observation.MeasurementSet;

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
 * JPA Repository declaration. Copyright 2014, CSIRO Australia All rights reserved.
 */
@Repository
public interface MeasurementSetRepository extends CrudRepository<MeasurementSet, Long>
{
    /**
     * Gets MeasurementSet objects that are related to the specified Observation
     * 
     * @param observationId
     *            the foreign key into the Observation table
     * @return a List of MeasurementSet objects
     */
    public List<MeasurementSet> findByObservationId(Long observationId);

    /**
     * Returns the first MeasurementSet associated with the given Observation (by sbid) and filename
     * 
     * @param sbid
     *            the sbid of an Observation
     * @param filename
     *            a String
     * @return an MeasurementSet
     */
    public MeasurementSet findByObservationSbidAndFilename(Integer sbid, String filename);
}
