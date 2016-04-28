package au.csiro.casda.datadeposit.observation.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import au.csiro.casda.entity.observation.Observation;

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
public interface ObservationRepository extends CrudRepository<Observation, Long>
{
    /**
     * Find an Observation by sbid.
     * 
     * @param sbid
     *            the sbid to look for
     * @return an Observation (or null if no matching Observation exists)
     */
    Observation findBySbid(Integer sbid);

    /**
     * Gets observations by that are currently being deposited, ie. not in DEPOSITED or FAILED states
     * 
     * @return A List of observations that are currently depositing.
     */
    @Query("FROM Observation WHERE deposit_state <> 'DEPOSITED' and deposit_state <> 'FAILED' order by id asc")
    List<Observation> findDepositingObservations();
}
