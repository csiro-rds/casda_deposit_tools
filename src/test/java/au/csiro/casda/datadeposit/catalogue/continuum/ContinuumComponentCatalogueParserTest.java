package au.csiro.casda.datadeposit.catalogue.continuum;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ContinuumComponentRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.ImageCube;
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
public class ContinuumComponentCatalogueParserTest
{
    private Catalogue catalogue;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private CatalogueRepository catalogueRepository;

    @Mock
    private ContinuumComponentRepository continuumComponentRepository;

    private ContinuumComponentCatalogueParser parser;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        parser = new ContinuumComponentCatalogueParser(observationRepository, catalogueRepository,
                continuumComponentRepository, new ContinuumComponentVoTableVisitor(continuumComponentRepository));
    }

    public class ValidCases
    {
        @Test
        public void paramExtra() throws Exception
        {
            String catalogueFilename = getPathForGoodTestCase("param.extra");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
            // this would throw an exception if parsing failed, but we expect it to be ok
        }

        @Test
        public void fieldExtra() throws Exception
        {
            String catalogueFilename = getPathForGoodTestCase("field.extra");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
            // this would throw an exception if parsing failed, but we expect it to be ok
        }

        @Test
        public void paramAttributesValueLessThan() throws Exception
        {
            String catalogueFilename = getPathForGoodTestCase("param.attributes.value.lessthan");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
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
            String imageCubeFilename = getPathForGoodTestCase("singlerow");
            expectedException.expect(CatalogueParser.DatabaseException.class);
            expectedException.expectMessage(
                    containsString("Could not find matching Observation record for sbid '" + sbid.toString() + "'"));
            parser.parseFile(sbid, filename, imageCubeFilename, CatalogueParser.Mode.NORMAL);
        }
    }

    public class CatalogueMissing
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
            String imageCubeFilename = getPathForGoodTestCase("singlerow");
            expectedException.expect(CatalogueParser.DatabaseException.class);
            expectedException.expectMessage(containsString("Could not find matching Catalogue record for filename '"
                    + filename + "' on Observation '" + sbid.toString() + "'"));
            parser.parseFile(sbid, filename, imageCubeFilename, CatalogueParser.Mode.NORMAL);
        }
    }

    public class CatalogueAlreadyLoaded
    {
        @Ignore
        @Test
        public void testIt()
        {
            fail("Should be better tested.");
        }
    }

    public class BadFile
    {
        @Rule
        public ExpectedException expectedException = ExpectedException.none();

        @Test
        public void malformedFile() throws Exception
        {
            String catalogueFilename = getPathForTestCase("malformed");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Cannot find the declaration of element 'AVOTABLE'"));

            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramMissing() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.missing");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Error in TABLE : " + "Missing PARAM matching name: 'imageFile', datatype: 'char'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramAttributesDifferent() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.attributes.different");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Attribute 'ucdDIFFERENT' is not allowed to appear in element 'PARAM'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);

        }

        @Test
        public void paramAttributesExtra() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.attributes.extra");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException
                    .expectMessage(containsString("Attribute 'extra' is not allowed to appear in element 'PARAM'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramRequiredAttributeMissing() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.attributes.required.missing");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Attribute 'datatype' must appear on element 'PARAM'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramOptionalAttributeMissing() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.attributes.optional.missing");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            // optional attribute missing is now ok
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void paramAttributesValueGreater() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.attributes.value.greater");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in PARAM 'imageFile' : " + "Attribute 'arraysize' ('260') exceeds maximum of '255'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        @Ignore
        public void paramValueBooleanInvalidCharacters() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no mandatory 'boolean' PARAMS
        }

        @Test
        public void paramValueCharToolong() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.value.char.toolong");
            String filename = "src/test/resources/image/too/long/too/long/too/long/too/long/too/long/too/long/too/"
                    + "long/tooo/long/too/long/too/long/too/long/too/long/too/long/too/long/too/"
                    + "long/too/long/too/long/too/long/too/long/too/long/too/long/too/long/good/"
                    + "image.i.clean.restored.fits";

            configureMockRepositories(catalogueFilename, filename);

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException
                    .expectMessage(containsString("Error in PARAM 'imageFile' : Value 'src/test/resources/image/"
                            + "too/long/too/long/too/long/too/long/too/long/too/long/too/long/tooo/long/too/long/too/long/"
                            + "too/long/too/long/too/long/too/long/too/long/too/long/too/long/too/long/too/long/too/long/"
                            + "too/long/too/long/good/image.i.clean.restored.fits' is wider than 255 chars"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        @Ignore
        public void paramValueIntInvalidCharacters() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no mandatory 'int' PARAMS
        }

        @Test
        @Ignore
        public void paramValueIntTooWide() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no mandatory 'int' PARAMS
        }

        @Test
        @Ignore
        public void paramValueRealInvalidCharacters() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no mandatory 'real' PARAMS
        }

        @Test
        public void paramValueFloatInvalidCharacters() throws Exception
        {
            String catalogueFilename = getPathForTestCase("param.value.float.invalidcharacters");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Error in PARAM 'Reference frequency' : " + "Value 'OOPS9e+08' is not a 'float'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        @Ignore
        public void paramValueDecimalInvalidCharacters() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no 'decimal' PARAMS
        }

        @Test
        @Ignore
        public void paramValueDecimalTooWide() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no 'decimal' PARAMS with a width
        }

        @Test
        @Ignore
        public void paramValueDecimalTooMuchPrecision() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no 'decimal' PARAMS with a precision
        }

        @Test
        public void fieldMissing() throws Exception
        {
            String catalogueFilename = getPathForTestCase("field.missing");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in TABLE : " + "Missing FIELD matching name: 'ra_hms_cont', datatype: 'char'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldAttributesDifferent() throws Exception
        {
            String catalogueFilename = getPathForTestCase("field.attributes.different");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Attribute 'ucdDIFFERENT' is not allowed to appear in element 'FIELD'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldAttributesExtra() throws Exception
        {
            String catalogueFilename = getPathForTestCase("field.attributes.extra");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException
                    .expectMessage(containsString("Attribute 'extra' is not allowed to appear in element 'FIELD'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldRequiredAttributeMissingDatatype() throws Exception
        {
            String catalogueFilename = getPathForTestCase("field.attributes.required.missing");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Attribute 'datatype' must appear on element 'FIELD'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldRequiredAttributeMissingRef() throws Exception
        {
            String catalogueFilename = getPathForTestCase("field.attributes.required.missing.ref");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException
                    .expectMessage(containsString("Error in FIELD 'ra_hms_cont' : Attribute 'ref' is required"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void fieldOptionalAttributeMissing() throws Exception
        {
            String catalogueFilename = getPathForTestCase("field.attributes.optional.missing");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            // arraysize missing is no longer a problem - maxarraysize will check against the field width
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellExtra() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.extra");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 1st TR : Additional TD"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellMissing() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.missing");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 1st TR : Missing TD"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        @Ignore
        public void cellValueBooleanInvalidCharacters() throws Exception
        {
            // Can't test for Continuum Component Catalogue because there are no 'boolean' FIELDs
        }

        @Test
        public void cellValueCharToolong() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.value.char.toolong");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);

            expectedException.expectMessage(containsString("Error in 1st TD (FIELD 'island_id') of 1st TR : "
                    + "Value '_Images/SB_609_617_639_643_659_no617b6_withBeam_Freq_Stokes.fits_1 "
                    + "this file name is way way way way way way way way way way way way way way "
                    + "way way way way way way way way way way way way way way way way way way "
                    + "way way way way way too long for this field' is wider than 70 chars"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueIntInvalidCharacters() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.value.int.invalidcharacters");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(
                    containsString("Error in 31st TD (FIELD 'flag_c3') of 1st TR : " + "Value 'OOPS0' is not a 'int'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueIntTooWide() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.value.int.toowide");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in 32nd TD (FIELD 'flag_c4') of 1st TR : " + "Value '123456789' is wider than 8 chars"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        @Ignore
        public void cellValueRealInvalidCharacters() throws Exception
        {
            // Can't test for Continuum Component Catalogue because all 'float' FIELDs are 'decimals'
        }

        @Test
        public void cellValueDecimalInvalidCharacters() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.value.decimal.invalidcharacters");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in 6th TD (FIELD 'ra_deg_cont') of 1st TR : " + "Value '3OOPS36.689324' is not a 'double'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueDecimalTooWide() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.value.decimal.toowide");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 6th TD (FIELD 'ra_deg_cont') of 1st TR : "
                    + "Value '1234336.689324' is wider than 12 chars"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void cellValueDecimalTooMuchPrecision() throws Exception
        {
            String catalogueFilename = getPathForTestCase("cell.value.decimal.toomuchprecision");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString("Error in 6th TD (FIELD 'ra_deg_cont') of 1st TR : "
                    + "Value '33.689324123' is more precise than 6 decimal places"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

        @Test
        public void badMidTableCell() throws Exception
        {
            String catalogueFilename = getPathForTestCase("bad.mid.table.cell");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            expectedException.expect(CatalogueParser.MalformedFileException.class);
            expectedException.expectMessage(containsString(
                    "Error in 6th TD (FIELD 'ra_deg_cont') of 2nd TR : " + "Value 'OOPS' is not a 'double'"));
            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
        }

    }

    private String getPathForTestCase(String testCase)
    {
        return "src/test/resources/catalogue/bad/component/componentCatalogue." + testCase + ".xml";
    }

    private String getPathForGoodTestCase(String testCase)
    {
        return "src/test/resources/catalogue/good/component/componentCatalogue." + testCase + ".xml";
    }

    private void configureMockRepositories(String catalogueFilename, String imageCubeFilename)
    {
        catalogue = mock(Catalogue.class);
        doReturn(catalogueFilename).when(catalogue).getFilename();

        Observation observation = mock(Observation.class);
        doReturn(observation).when(observationRepository).findBySbid(anyInt());
        doReturn(observation).when(catalogue).getParent();
        doReturn(Arrays.asList(catalogue)).when(observation).getCatalogues();

        Project project = mock(Project.class);
        doReturn(project).when(catalogue).getProject();
        doReturn(123L).when(project).getId();

        ImageCube imageCube = mock(ImageCube.class);
        doReturn(imageCubeFilename).when(imageCube).getFilename();
        doReturn(Arrays.asList(imageCube)).when(observation).getImageCubes();
        doReturn(observation).when(imageCube).getParent();
    }

}
