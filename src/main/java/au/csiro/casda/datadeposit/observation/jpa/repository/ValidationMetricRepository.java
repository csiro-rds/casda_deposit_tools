package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.repository.CrudRepository;

import au.csiro.casda.entity.validation.ValidationMetric;

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
public interface ValidationMetricRepository  extends CrudRepository<ValidationMetric, Long>
{
	/**
	 * returns the matching validationMetric (if any) which match the passed name and description
	 * @param metricName the metric name
	 * @param description the metric description
	 * @return the matching metric or null if none found
	 */
	public ValidationMetric findFirstByMetricNameAndDescription(String metricName, String description);
}
