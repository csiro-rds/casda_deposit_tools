package au.csiro.casda.datadeposit.validationmetric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import au.csiro.casda.datadeposit.observation.jpa.repository.ValidationMetricRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ValidationMetricValueRepository;
import au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor;
import au.csiro.casda.datadeposit.votable.parser.FieldConstraint;
import au.csiro.casda.datadeposit.votable.parser.ParamConstraint;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableField;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableParam;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.validation.ValidationMetric;
import au.csiro.casda.entity.validation.ValidationMetricValue;

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
 * class for visiting a Validation Metric VOTABLE in preparation for writing to a database.
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
public class ValidationMetricVoTableVisitor extends AbstractVoTableElementVisitor
{
    private static final String VALIDATION_METRIC_METADATA_RESOURCE_PATH = "schemas/validation_metric_metadata.yml";

    private static final List<ParamConstraint> PARAM_CONSTRAINTS = new ArrayList<>();

    private static final List<FieldConstraint> FIELD_CONSTRAINTS = new ArrayList<>();

    static
    {
        loadConstraintsFile(VALIDATION_METRIC_METADATA_RESOURCE_PATH, PARAM_CONSTRAINTS, FIELD_CONSTRAINTS);
    }
    
    private static final Map<String, BiConsumer<ValidationMetricValue, String>> APPLIERS;
    static
    {
        APPLIERS = new HashMap<>();
        APPLIERS.put("metric_name", (validationMetricValue, value) -> 
        											validationMetricValue.getValidationMetric().setMetricName(value));
        APPLIERS.put("metric_value", (validationMetricValue, value) -> 
        											validationMetricValue.setMetricValue(Double.parseDouble(value)));
        APPLIERS.put("metric_status", (validationMetricValue, value) -> 
        											validationMetricValue.setStatus(Short.parseShort(value)));
        APPLIERS.put("metric_description", (validationMetricValue, value) -> 
        											validationMetricValue.getValidationMetric().setDescription(value));
    }
    
	private EvaluationFile evaluationFile;
	private ValidationMetricValueRepository validationMetricValueRepository;
	private ValidationMetricRepository validationMetricRepository;
	
    /**
     * Constructs a ValidationMetricVoTableVisitor that uses the given EvaluationFileRepository to perform any
     * operations associated with EvaluationFile persistence.
     * 
     * @param validationMetricValueRepository the validationMetricValueRepository
     * @param validationMetricRepository the validationMetricRepository
     */
    public ValidationMetricVoTableVisitor(ValidationMetricValueRepository validationMetricValueRepository, 
    		ValidationMetricRepository validationMetricRepository)
    {
        this.validationMetricValueRepository = validationMetricValueRepository;
        this.validationMetricRepository = validationMetricRepository;
    }
    
    /**
     * Implementation of template method
     * {@link au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor#getParamConstraints()} that provides
     * metadata about the specific VOTABLE Param's that are acceptable for the ValidationMetric data.
     * 
     * @return a Map of {@link VoTableParamDescription>s keyed by the description's name
     */
    @Override
    protected List<ParamConstraint> getParamConstraints()
    {
        return PARAM_CONSTRAINTS;
    }

    /**
     * Implementation of template method
     * {@link au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor#getFieldConstraints()} that provides
     * metadata about the specific VOTABLE Field's that are acceptable for the ValidationMetric data.
     * 
     * @return a Map of {@link VoTableFieldDescription>s keyed by the description's name
     */
    @Override
    protected List<FieldConstraint> getFieldConstraints()
    {
        return FIELD_CONSTRAINTS;
    }
	
    /**
     * Sets the EvaluationFile that the validation metrics belong to.
     * 
     * @param evaluationFile
     *            a evaluationFile
     */
    public void setEvaluationFile(EvaluationFile evaluationFile)
    {
        if (evaluationFile == null)
        {
            throw new IllegalArgumentException("expected evaluationFile != null");
        }
        long validationMetricValueCount = getValidationMetricValueCount(evaluationFile);
        if (validationMetricValueCount > 0)
        {
            throw new IllegalArgumentException("expected evaluationFile " + evaluationFile + " to have no entries but had "
                    + validationMetricValueCount);
        }
        this.evaluationFile = evaluationFile;
    }

	@Override
	protected void processFields(List<VisitableVoTableField> fields) 
	{
		// We don't really care about the fields as long as they match the defined ones.
	}

	@Override
	protected void processRow(Map<VisitableVoTableField, String> row)
	{
		ValidationMetricValue validationMetricValue = new ValidationMetricValue();
		validationMetricValue.setEvaluationFile(evaluationFile);
		validationMetricValue.setProjectId(evaluationFile.getProject().getId());
		validationMetricValue.setObservationId(evaluationFile.getParent().getId());
		validationMetricValue.setValidationMetric(new ValidationMetric());
		
        for (VisitableVoTableField field : row.keySet())
        {
            if (row.get(field) != null)
            {
                BiConsumer<ValidationMetricValue, String> applier = APPLIERS.get(field.getName());
                if (applier != null) // Not all fields will be written to the ContinuumComponent
                {
                    applier.accept(validationMetricValue, row.get(field));
                }
            }
        }

        ValidationMetric existingMetric = validationMetricRepository.findFirstByMetricNameAndDescription(
        		validationMetricValue.getValidationMetric().getMetricName(), 
        		validationMetricValue.getValidationMetric().getDescription());
        
        if(existingMetric != null)
        {
        	//if this combination of name and description already exist overwrite the new validation metric with the 
        	//existing one.
        	validationMetricValue.setValidationMetric(existingMetric);
        }
        
        validationMetricValueRepository.save(validationMetricValue);
	}

	
	private long getValidationMetricValueCount(EvaluationFile evaluationFile2)
	{
		return this.validationMetricValueRepository.countByEvaluationFile(evaluationFile);
	}

	@Override
	protected void processParams(Collection<VisitableVoTableParam> params) 
	{
		//nothing needed here.
	}
}
