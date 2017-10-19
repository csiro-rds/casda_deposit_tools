package au.csiro.casda.datadeposit.validationmetric;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.casda.datadeposit.DepositState;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ValidationMetricRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ValidationMetricValueRepository;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Project;
import de.bechte.junit.runners.context.HierarchicalContextRunner;

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
 * Tests the ContinuumCatalogueParser
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@RunWith(HierarchicalContextRunner.class)
public class ValidationMetricParserTest
{
    private EvaluationFile evaluationFile;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private EvaluationFileRepository evaluationFileRepository;

    @Mock
    private ValidationMetricValueRepository validationMetricValueRepository;

    @Mock
    private ValidationMetricRepository validationMetricRepository;
    
    private ValidationMetricParser parser;
    
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        parser = new ValidationMetricParser(observationRepository, validationMetricValueRepository, 
       		 evaluationFileRepository, validationMetricRepository);
    }
    
    public class ValidCases
    {
        @Test
        public void paramExtra() throws Exception
        {
            String filename = getPathForGoodTestCase("param.extra");
            configureMockRepositories(filename);

            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
            // this would throw an exception if parsing failed, but we expect it to be ok
        }

        @Test
        public void fieldExtra() throws Exception
        {
            String filename = getPathForGoodTestCase("field.extra");
            configureMockRepositories(filename);

            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
            // this would throw an exception if parsing failed, but we expect it to be ok
        }

        @Test
        public void paramAttributesValueLessThan() throws Exception
        {
            String filename = getPathForGoodTestCase("valid");
            configureMockRepositories(filename);

            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);

            ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
            verify(evaluationFile).setDepositState(queryCaptor.capture());
            assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.ENCAPSULATING));
        }
    }
    
    public class FileMissing
    {
        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Test
        public void testFileNotFoundExceptionThrown() throws Exception
        {
            String filename = RandomStringUtils.randomAlphabetic(30);
            expectedException.expect(FileNotFoundException.class);
            expectedException.expectMessage(containsString(filename));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }
    }
    
    public class ObservationMissing
    {
        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void setUp()
        {
            doReturn(null).when(observationRepository).findBySbid(anyInt());
        }

        @Test
        public void testDatabaseExceptionThrown() throws Exception
        {
            Integer sbid = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            String filename = "src/test/resources/log4j2.xml"; // Just need something that exists.
            String imageCubeFilename = getPathForGoodTestCase("valid");
            expectedException.expect(CatalogueParser.DatabaseException.class);
            expectedException.expectMessage(
                    containsString("Could not find matching Observation record for sbid '" + sbid.toString() + "'"));
            parser.parseFile(sbid, filename, imageCubeFilename, CatalogueParser.Mode.NORMAL);
        }
    }
    
    public class EvaluationFileMissing
    {
        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Before
        public void setUp()
        {
            Observation observation = mock(Observation.class);
            doReturn(observation).when(observationRepository).findBySbid(anyInt());
            doReturn(new ArrayList<>(0)).when(observation).getCatalogues();
        }

        @Test
        public void testDatabaseExceptionThrown() throws Exception
        {
            Integer sbid = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            String filename = getPathForTestCase("malformed"); // Just need something that exists.
            String imageCubeFilename = getPathForGoodTestCase("valid");
            expectedException.expect(CatalogueParser.DatabaseException.class);
            expectedException.expectMessage(containsString("Could not find matching Evaluation File record for filename '"
                    + filename + "' on Observation '" + sbid.toString() + "'"));
            parser.parseFile(sbid, filename, imageCubeFilename, CatalogueParser.Mode.NORMAL);
        }
    }
    
    public class BadFile
    {
        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Test
        public void malformedFile() throws Exception
        {
            String filename = getPathForTestCase("malformed");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Cannot find the declaration of element 'AVOTABLE'"));

            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramMissing() throws Exception
        {
            String filename = getPathForTestCase("param.missing");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Error in TABLE : Missing PARAM matching name: 'project', datatype: 'char'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramAttributesDifferent() throws Exception
        {
            String filename = getPathForTestCase("param.attributes.different");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Attribute 'ucdDIFFERENT' is not allowed to appear in element 'PARAM'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);

        }

        @Test
        public void paramAttributesExtra() throws Exception
        {
            String filename = getPathForTestCase("param.attributes.extra");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException
                    .expectMessage(containsString("Attribute 'extra' is not allowed to appear in element 'PARAM'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramRequiredAttributeMissing() throws Exception
        {
            String filename = getPathForTestCase("param.attributes.required.missing");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Attribute 'datatype' must appear on element 'PARAM'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldMissing() throws Exception
        {
            String filename = getPathForTestCase("field.missing");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in TABLE : " + "Missing FIELD matching name: 'metric_description', datatype: 'char'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldAttributesDifferent() throws Exception
        {
            String filename = getPathForTestCase("field.attributes.different");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Attribute 'ucdDIFFERENT' is not allowed to appear in element 'FIELD'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldAttributesExtra() throws Exception
        {
            String filename = getPathForTestCase("field.attributes.extra");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException
                    .expectMessage(containsString("Attribute 'extra' is not allowed to appear in element 'FIELD'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldRequiredAttributeMissingDatatype() throws Exception
        {
            String filename = getPathForTestCase("field.attributes.required.missing");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Attribute 'datatype' must appear on element 'FIELD'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellExtra() throws Exception
        {
            String filename = getPathForTestCase("cell.extra");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 1st TR : Additional TD"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellMissing() throws Exception
        {
            String filename = getPathForTestCase("cell.missing");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 1st TR : Missing TD"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueCharToolong() throws Exception
        {
            String filename = getPathForTestCase("cell.value.char.toolong");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);

            expectedException.expectMessage(containsString("Error in 1st TD (FIELD 'metric_name') of 2nd TR : "
                    + "Value 'this is my new metric, but steve says "
                    + "this name is way way way way way way way way way way way way way way "
                    + "way way way way way way way way way way way way way way way way way way "
                    + "way way way way way too long for this field' is wider than 100 chars"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueIntInvalidCharacters() throws Exception
        {
            String filename = getPathForTestCase("cell.value.int.invalidcharacters");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Error in 3rd TD (FIELD 'metric_status') of 5th TR : " + "Value 'OOPS' is not a 'int'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueIntTooWide() throws Exception
        {
            String filename = getPathForTestCase("cell.value.int.toowide");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in 2nd TD (FIELD 'metric_value') of 3rd TR : " + "Value '123.456' is wider than 6 chars"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueDecimalInvalidCharacters() throws Exception
        {
            String filename = getPathForTestCase("cell.value.decimal.invalidcharacters");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in 2nd TD (FIELD 'metric_value') of 4th TR : " + "Value '0.O9' is not a 'double'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueDecimalTooWide() throws Exception
        {
            String filename = getPathForTestCase("cell.value.decimal.toowide");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 2nd TD (FIELD 'metric_value') of 3rd TR : "
                    + "Value '0.82123' is wider than 6 chars"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueDecimalTooMuchPrecision() throws Exception
        {
            String filename = getPathForTestCase("cell.value.decimal.toomuchprecision");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 2nd TD (FIELD 'metric_value') of 4th TR : "
                    + "Value '0.191' is more precise than 2 decimal places"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void badMidTableCell() throws Exception
        {
            String filename = getPathForTestCase("bad.mid.table.cell");
            configureMockRepositories(filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in 3rd TD (FIELD 'metric_status') of 4th TR : " + "Value 'OOPS' is not a 'int'"));
            parser.parseFile(12345, filename, filename, CatalogueParser.Mode.NORMAL);
        }

    }
    
    private String getPathForTestCase(String testCase)
    {
        return "src/test/resources/validationmetric/bad/validationMetric." + testCase + ".xml";
    }

    private String getPathForGoodTestCase(String testCase)
    {
        return "src/test/resources/validationmetric/good/validationMetric." + testCase + ".xml";
    }
    
    private void configureMockRepositories(String filename)
    {
        evaluationFile = mock(EvaluationFile.class);
        doReturn(filename).when(evaluationFile).getFilename();

        Observation observation = mock(Observation.class);
        doReturn(observation).when(observationRepository).findBySbid(anyInt());
        doReturn(observation).when(evaluationFile).getParent();
        doReturn(Arrays.asList(evaluationFile)).when(observation).getEvaluationFiles();

        Project project = mock(Project.class);
        doReturn(project).when(evaluationFile).getProject();
        doReturn(123L).when(project).getId();
    }
}
