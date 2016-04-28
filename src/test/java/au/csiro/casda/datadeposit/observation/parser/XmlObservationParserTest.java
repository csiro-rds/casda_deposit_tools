package au.csiro.casda.datadeposit.observation.parser;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import au.csiro.TestUtils;
import au.csiro.casda.datadeposit.observation.jaxb.Scan;

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
 * Tests that XML is unmarshalled correctly.
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 * 
 */
public class XmlObservationParserTest
{
    public static Matcher<Throwable> unmarshallExceptionCausedByException(
            @SuppressWarnings("rawtypes") Class exceptionClass, String expectedMessage)
    {
        return new TypeSafeMatcher<Throwable>()
        {

            @Override
            public void describeTo(Description description)
            {
                description.appendText("UnmarshallException with a linked " + exceptionClass.getName() + " matching /"
                        + expectedMessage + "/");
            }

            @Override
            protected boolean matchesSafely(Throwable item)
            {
                if (!(item instanceof UnmarshalException))
                {
                    return false;
                }

                if (!(((UnmarshalException) item).getLinkedException().getClass().equals(exceptionClass)))
                {
                    return false;
                }

                String message = ((UnmarshalException) item).getLinkedException().getMessage();
                return message.matches(".*" + expectedMessage + ".*");
            }
        };
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Tests that perfectly fine xml can be ingested without issues.
     * 
     * @throws Exception
     */
    @Test
    public void testXMLGood() throws Exception
    {
        XmlObservation dataset = getDataset("observation/good/metadata-v2-good01.xml");

        assertEquals("12345", dataset.getIdentity().getSbid());
        assertEquals("ASKAP", dataset.getIdentity().getTelescope());
        assertEquals("test", dataset.getIdentity().getObsprogram());

        Scan firstScan = dataset.getMeasurementSets().get(0).getScans().getScan().get(0);
        assertEquals("2013-11-19T02:30:00Z[UTC]", firstScan.getScanstart().toString());
        assertEquals(0, firstScan.getId());
        assertEquals("Fornax", firstScan.getFieldname());

        assertEquals("2013-11-19T02:30:00Z[UTC]", dataset.getObservation().getObsstart().toString());
    }

    @Test
    public void testXMLGoodEmptyMeasurementSetsAndEvaluations() throws Exception
    {
        XmlObservation dataset = getDataset("observation/good/metadata-v2-good07.xml");
        assertEquals("12351", dataset.getIdentity().getSbid());
        assertEquals("test", dataset.getIdentity().getObsprogram());
        assertEquals(0, dataset.getMeasurementSets().size());
        assertEquals(0, dataset.getEvaluationFiles().size());
    }

    @Test
    public void testXMLGoodNoMeasurementSetsAndEvaluations() throws Exception
    {
        XmlObservation dataset = getDataset("observation/good/metadata-v2-good08.xml");
        assertEquals("12351", dataset.getIdentity().getSbid());
        assertEquals("test", dataset.getIdentity().getObsprogram());
        assertThat(dataset.getMeasurementSets(), is(empty()));
        assertThat(dataset.getEvaluationFiles(), is(empty()));
    }

    /**
     * Tests that faulty XML cannot be parsed and the correct exception is thrown.
     * 
     * <!-- error01 - 'unknown' element not in content model --> <unknown>Error</unknown>
     * 
     * @throws Exception
     */
    @Test
    public void testXMLBad01() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Invalid content was found starting with element "
                        + "'unknown'. No child element is expected at this point."));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-error01.xml"));
    }

    /**
     * Tests that faulty XML cannot be parsed and the correct exception is thrown.
     * 
     * missing element - obsprogram.
     * 
     * @throws Exception
     */
    @Test
    public void testXMLBad02() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "The content of element 'identity' is not " + "complete. One of .* is expected."));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-error02.xml"));
    }

    @Test
    public void testBlankFilenameElement() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value '' with length = '0' is not facet-valid "
                        + "with respect to minLength '1' for type 'nonEmptyString'."));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-blank_filename.xml"));
    }

    @Test
    public void testBlankFormatElement() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value '' with length = '0' is not facet-valid "
                        + "with respect to minLength '1' for type 'nonEmptyString'."));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-blank_format.xml"));
    }

    @Test
    public void testBlankTelescopeElement() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value '' with length = '0' is not facet-valid "
                        + "with respect to minLength '1' for type 'nonEmptyString'."));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-blank_telescope.xml"));
    }

    @Test
    public void testBlankObsProgramElement() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value '' with length = '0' is not facet-valid "
                        + "with respect to minLength '1' for type 'nonEmptyString'."));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-blank_obsprogram.xml"));
    }

    @Test
    public void testTooManyPolarisations() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "'\\[XX, XY, YX, YY, q, u, v\\]' is not facet-valid " + "with respect to pattern"));
        XmlObservationParser.parseDataSetXML(TestUtils
                .getResource("observation/bad/metadata-v2-invalid_polarisations.xml"));
    }

    @Test
    public void testManyPolarisations() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils
                        .getResource("observation/good/metadata-v2-two_polarisation_values.xml"));
        assertEquals("111", dataset.getIdentity().getSbid());
    }

    @Test
    public void testBlankSbid() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "'' is not a valid value"));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-sbid_blank.xml"));
    }

    @Test
    public void testNonNumericSbid() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "'abc' is not a valid value"));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-sbid_non_numeric.xml"));
    }

    @Test
    public void testNegativeSbid() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "'-1' is not a valid value"));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-sbid_negative.xml"));
    }

    @Test
    public void testBlankProjectName() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value '' is not facet-valid with respect to "
                        + "pattern '\\(\\[A-Za-z0-9\\]\\)\\+' for type '#AnonType_project'."));
        XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/bad/metadata-v2-project_blank.xml"));
    }

    @Test
    public void testProjectNameWithSpaces() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value 'AS 007' is not facet-valid with respect to "
                        + "pattern '\\(\\[A-Za-z0-9\\]\\)\\+' for type '#AnonType_project'."));
        XmlObservationParser.parseDataSetXML(TestUtils
                .getResource("observation/bad/metadata-v2-project_with_spaces.xml"));
    }

    @Test
    public void testProjectNameWithNonAlphanumeric() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value 'AS_007' is not facet-valid with respect to "
                        + "pattern '\\(\\[A-Za-z0-9\\]\\)\\+' for type '#AnonType_project'."));
        XmlObservationParser.parseDataSetXML(TestUtils
                .getResource("observation/bad/metadata-v2-project_with_non_alphanumerics.xml"));
    }

    @Test
    public void testDoubleOrNothingFieldsWithNonNumeric() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(
                NumberFormatException.class, "text-ShouldBeNumeric"));
        XmlObservationParser.parseDataSetXML(TestUtils
                .getResource("observation/bad/metadata-v2-doubleOrNothingFieldsWithNonNumeric.xml"));
    }

    @Test
    public void testInvalidFieldCentre() throws Exception
    {
        expectedException.expect(XmlObservationParserTest.unmarshallExceptionCausedByException(SAXParseException.class,
                "Value '\\[-3.02785\\]' is not facet-valid with respect to pattern "));

        XmlObservationParser.parseDataSetXML(TestUtils
                .getResource("observation/bad/metadata-v2-invalid-field-centre.xml"));
    }

    private XmlObservation getDataset(String filename) throws JAXBException, SAXException, IOException
    {
        XmlObservation dataset = XmlObservationParser.parseDataSetXML(TestUtils.getResource(filename));
        assertNotNull(dataset);
        assertNotNull(dataset.getCatalogues());
        assertNotNull(dataset.getIdentity());
        assertNotNull(dataset.getImages());
        assertNotNull(dataset.getObservation());
        return dataset;
    }
}
