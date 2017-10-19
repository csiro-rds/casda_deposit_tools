package au.csiro.casda.datadeposit.validationmetric;

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import javax.transaction.Transactional;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Before;
import org.junit.Test;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import au.csiro.casda.datadeposit.FunctionalTestBase;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.observation.Observation;
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
 * Functional test for validation metric data import.
 * <p>
 * Copyright 2017, CSIRO Australia All rights reserved.
 */
public class ValidationMetricImportFunctionalTest extends FunctionalTestBase
{
	private ValidationMetricCommandLineImporter validationMetricCommandLineImporter;
	private ValidationMetricParser validationMetricParser;

    public ValidationMetricImportFunctionalTest() throws Exception
    {
        super();
    }
    
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        validationMetricParser = context.getBeanFactory().createBean(ValidationMetricParser.class);
        
        validationMetricCommandLineImporter = new ValidationMetricCommandLineImporter(validationMetricParser);
    }

    @Test
    @Transactional
    public void testValidationMetricDatafileLoadedAndPersistedToDatabase()
    {
    	String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String validationMetricDatafile = "valid_validation_metric_file.xml";
        try
        {
        	validationMetricCommandLineImporter.run("-parent-id", sbid, "-filename", validationMetricDatafile, "-infile",
                    depositDir + validationMetricDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Validation Metric data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        Observation observation = observationRepository.findAll().iterator().next();
        EvaluationFile evaluationFile = null;
        for(EvaluationFile file : observation.getEvaluationFiles())
        {
        	if(file.getFilename().equals(validationMetricDatafile))
        	{
        		evaluationFile = file;
        		break;
        	}
        }

        assertEquals("valid_validation_metric_file.xml", evaluationFile.getFilename());
        assertEquals(2.0, evaluationFile.getFilesize(), 1.0);
        assertEquals("validation-metrics", evaluationFile.getFormat());
        assertEquals(evaluationFile.getProject().getId(), evaluationFile.getValidationMetricValues().get(0).getProjectId());
        assertEquals(6, evaluationFile.getValidationMetricValues().size());
        
        for(ValidationMetricValue value :evaluationFile.getValidationMetricValues())
        {
        	if(value.getValidationMetric().getMetricName().equals("RMS"))
        	{
        		assertEquals("Measure/Predicted r.m.s", value.getValidationMetric().getDescription());
        		assertEquals(3, value.getStatus());
        		assertEquals(1.0, value.getMetricValue(), 0.01);
        	}
        	else if(value.getValidationMetric().getMetricName().equals("Flux Ratio"))
        	{
        		assertEquals("Average flux density ratio of all detected sources when compared to a standard catalogue",
        				value.getValidationMetric().getDescription());
        		assertEquals(1, value.getStatus());
        		assertEquals(0.97, value.getMetricValue(), 0.01);
        	}
        	else if(value.getValidationMetric().getMetricName().equals("Positional Offset"))
        	{
        		assertEquals("Positional offset (arcsec) (SUMSS - ASKAP)", value.getValidationMetric().getDescription());
        		assertEquals(1, value.getStatus());
        		assertEquals(0.82, value.getMetricValue(), 0.01);
        	}
        	else if(value.getValidationMetric().getMetricName().equals("Resolved Fraction"))
        	{
        		assertEquals("Resolved Fraction from int/peak flux (ASKAP)", value.getValidationMetric().getDescription());
        		assertEquals(1, value.getStatus());
        		assertEquals(0.19, value.getMetricValue(), 0.01);
        	}
        	else if(value.getValidationMetric().getMetricName().equals("Spectral Index"))
        	{
        		assertEquals("Spectral Index (ASKAP-SUMSS)", value.getValidationMetric().getDescription());
        		assertEquals(3, value.getStatus());
        		assertEquals(-1.48, value.getMetricValue(), 0.01);
        	}
        	else if(value.getValidationMetric().getMetricName().equals("Source Counts"))
        	{
        		assertEquals("Source Counts Chi_red^2 (ASKAP)", value.getValidationMetric().getDescription());
        		assertEquals(3, value.getStatus());
        		assertEquals(9.63, value.getMetricValue(), 0.01);
        	}
        	else
        	{
        		//nothing should reach here
        		fail();
        	}
        }

    }
    
    @Test
    @Transactional
    public void testReloadingTheSameValidationMetricShouldFail()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String validationMetricDatafile = "valid_validation_metric_file.xml";
        try
        {
        	validationMetricCommandLineImporter.run("-parent-id", sbid, "-filename", validationMetricDatafile, "-infile",
                    depositDir + validationMetricDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        long count = validationMetricValueRepository.count();

        try
        {
        	validationMetricCommandLineImporter.run("-parent-id", sbid, "-filename", validationMetricDatafile, "-infile",
                    depositDir + validationMetricDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Validation metric data import should not have succeeded - please check log for details", 1,
                    e.getStatus().intValue());
        }

        assertEquals(count, validationMetricValueRepository.count());
    }
    
    @Test
    @Transactional
    public void testValidationMode() throws Exception
    {
    	String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String evaluationDatafile = "valid_validation_metric_file.xml";
        try
        {
        	validationMetricCommandLineImporter.run("-parent-id", sbid, "-filename", evaluationDatafile, "-infile",
                    depositDir + evaluationDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Validation metric data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }
        long count = validationMetricValueRepository.count();
        
        assertEquals(6, count);
        validationMetricCommandLineImporter = context.getBeanFactory().createBean(ValidationMetricCommandLineImporter.class);
        try
        {
        	validationMetricCommandLineImporter.run("-parent-id", sbid, "-filename", evaluationDatafile, "-infile",
                    depositDir + evaluationDatafile, "-validate-only");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Validation Metric data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(count, validationMetricValueRepository.count());
        assertThat(out.toString(CharEncoding.UTF_8).split(System.lineSeparator()),
                arrayContaining("Evaluation File record for filename '" + evaluationDatafile + "' on Observation '"
                        + sbid + "' already has validation metric entries"));
    }
}
