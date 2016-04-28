package au.csiro.casda.datadeposit.observation.service;

import org.springframework.stereotype.Service;

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
 * Service for test cases that allows the clearing of all Metadata records.
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@Service
public interface RepositoryTestHelper
{
    /**
     * Clears the database;
     */
    public void deleteAll();

    public Long getProjectCount();

    public Long getObservationCount();

    public Long getCatalogueCount();

    public Long getImageCubeCount();

    public Long getMeasurementSetCount();

    public Long getEvaluationFileCount();

    public Long getScanCount();
    
    public Long getLevel7CollectionCount();
}
