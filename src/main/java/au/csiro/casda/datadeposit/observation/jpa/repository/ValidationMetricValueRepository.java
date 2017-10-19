package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.repository.CrudRepository;

import au.csiro.casda.entity.observation.EvaluationFile;
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
import au.csiro.casda.entity.validation.ValidationMetricValue;

/**
 * JPA Repository declaration.
 * <p>
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
public interface ValidationMetricValueRepository extends CrudRepository<ValidationMetricValue, Long>
{
    /**
     * Returns the number of validation metric values associated with the given evaluationFile
     * 
     * @param evaluationFile
     *            an evaluationFile
     * @return a number
     */
    long countByEvaluationFile(EvaluationFile evaluationFile);
}