package au.csiro.casda.datadeposit.votable.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import net.ivoa.vo.Field;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
 * Test cases for FieldDatatype
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@RunWith(Enclosed.class)
public class FieldDatatypeTest
{
    @RunWith(Parameterized.class)
    public static class ArraysizeRejectedTest
    {
        @Rule
        public ExpectedException thrown = ExpectedException.none();

        private String fieldName = RandomStringUtils.random(10);
        private FieldDatatype datatype;

        @Parameters
        public static Collection<Object[]> data()
        {
            return Arrays.asList(FieldDatatype.values()).stream()
                    .filter((dt) -> !Arrays.asList(FieldDatatype.CHAR).contains(dt)).map((dt) -> {
                        return new FieldDatatype[] { dt };
                    }).collect(Collectors.toList());
        }

        public ArraysizeRejectedTest(FieldDatatype datatype)
        {
            this.datatype = datatype;
        }

        @Test
        public void testArraysizeAndMaxarraysizeUsed() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'arraysize' ('0') for datatype '" + this.datatype.toString()
                    + "' is not supported");

            Field field = new Field();
            field.setName(fieldName);
            field.setArraysize("0");

            this.datatype.validateFieldAttributes(field, null);
        }
    }

    @RunWith(Enclosed.class)
    public static class CharTest
    {
        @Rule
        public ExpectedException thrown = ExpectedException.none();

        private String fieldName = RandomStringUtils.random(10);

        @Test
        public void testArraysizeRejectedOnCharWhenMalformed() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("FIELD '" + fieldName + "' has 'arraysize' that does not match '(\\d+)?(\\*)?'");

            Field field = new Field();
            field.setName(fieldName);
            field.setArraysize("abc");

            FieldDatatype.CHAR.validateFieldAttributes(field, null);
        }

        @RunWith(Parameterized.class)
        public static class CharArraysizeTest
        {
            private String fieldName = RandomStringUtils.random(10);
            private String arraysize;
            private String maxarraysize;
            private String value;
            private String fieldFormatExceptionWidthDescription;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] {
                        // Parameters are: arraysize, maxarraysize, value, exception-expected
                        // * No maxarraysize:
                        // ** Good values:
                        { null, null, RandomStringUtils.random(10000), null }, // No constraints
                        { null, "", RandomStringUtils.random(10000), null }, // No constraints
                        { "", null, RandomStringUtils.random(10000), null }, // No constraints
                        { "", "", RandomStringUtils.random(10000), null }, // No constraints
                        { "*", null, RandomStringUtils.random(10000), null }, // No constraints
                        { "*", "", RandomStringUtils.random(10000), null }, // No constraints
                        { "0", null, "", null }, //
                        { "0", "", "", null }, //
                        { "0*", null, "", null }, // variable
                        { "0*", "", "", null }, // variable
                        { "1", null, "A", null }, //
                        { "1", "", "A", null }, //
                        { "1*", null, "A", null }, // variable
                        { "1*", "", "A", null }, // variable
                        { "10", null, "ABCDEFGHIJ", null }, //
                        { "10", "", "ABCDEFGHIJ", null }, //
                        { "10*", null, "ABCDEFGHIJ", null }, // variable
                        { "10*", "", "ABCDEFGHIJ", null }, // variable
                        // ** Bad values:
                        { "0", null, "A", "0" }, //
                        { "0", "", "A", "0" }, //
                        { "0*", null, "A", "0*" }, // variable
                        { "0*", "", "A", "0*" }, // variable
                        { "1", null, "AB", "1" }, //
                        { "1", "", "AB", "1" }, //
                        { "1*", null, "AB", "1*" }, //
                        { "1*", "", "AB", "1*" }, //
                        { "10", null, "ABCDEFGHIJK", "10" }, //
                        { "10", "", "ABCDEFGHIJK", "10" }, //
                        { "10*", null, "ABCDEFGHIJK", "10*" }, // variable
                        { "10*", "", "ABCDEFGHIJK", "10*" }, // variable
                        // * With ignored maxarraysize:
                        // ** Good values:
                        { "0", "0", "", null }, //
                        { "0*", "0", "", null }, // variable
                        { "0", "0*", "", null }, //
                        { "0*", "0*", "", null }, // variable
                        { "1", "1", "A", null }, //
                        { "1*", "1", "A", null }, // variable
                        { "1", "1*", "A", null }, //
                        { "1*", "1*", "A", null }, // variable
                        { "10", "10", "ABCDEFGHIJ", null }, //
                        { "10*", "10", "ABCDEFGHIJ", null }, // variable
                        { "10", "10*", "ABCDEFGHIJ", null }, //
                        { "10*", "10*", "ABCDEFGHIJ", null }, // variable
                        // ** Bad values:
                        { "0", "0", "A", "0" }, //
                        { "0*", "0", "A", "0*" }, // variable
                        { "1", "1", "AB", "1" }, //
                        { "1*", "1", "AB", "1*" }, // variable
                        { "1", "1*", "AB", "1" }, //
                        { "1*", "1*", "AB", "1*" }, // variable
                        { "10", "0", "ABCDEFGHIJK", "10" }, //
                        { "10*", "0", "ABCDEFGHIJK", "10*" }, // variable
                        // * With maxarraysize:
                        // ** Good values:
                        { null, "*", RandomStringUtils.random(10000), null }, // No constraints
                        { "", "*", RandomStringUtils.random(10000), null }, // No constraints
                        { null, "0", "", null }, //
                        { "", "0", "", null }, //
                        { "*", "0", "", null }, { null, "0*", "", null }, // variable
                        { "", "0*", "", null }, // variable
                        { "*", "0*", "", null }, // variable
                        { null, "1", "A", null }, //
                        { "", "1", "A", null }, //
                        { "*", "1", "A", null }, { null, "1*", "A", null }, // variable
                        { "", "1*", "A", null }, // variable
                        { "*", "1*", "A", null }, // variable
                        { null, "10", "ABCDEFGHIJ", null }, //
                        { "", "10", "ABCDEFGHIJ", null }, //
                        { "*", "10", "ABCDEFGHIJ", null }, { null, "10*", "ABCDEFGHIJ", null }, // variable
                        { "", "10*", "ABCDEFGHIJ", null }, // variable
                        { "*", "10*", "ABCDEFGHIJ", null }, // variable
                        // ** Bad values:
                        { null, "0", "A", "maximum 0" }, //
                        { "", "0", "A", "maximum 0" }, //
                        { "*", "0", "A", "maximum 0" }, { null, "0*", "A", "maximum 0*" }, // variable
                        { "", "0*", "A", "maximum 0*" }, // variable
                        { "*", "0*", "A", "maximum 0*" }, // variable
                        { null, "1", "AB", "maximum 1" }, //
                        { "", "1", "AB", "maximum 1" }, //
                        { "*", "1", "AB", "maximum 1" }, { null, "1*", "AB", "maximum 1*" }, // variable
                        { "", "1*", "AB", "maximum 1*" }, // variable
                        { "*", "1*", "AB", "maximum 1*" }, // variable
                        { null, "10", "ABCDEFGHIJK", "maximum 10" }, //
                        { "", "10", "ABCDEFGHIJK", "maximum 10" }, //
                        { "*", "10", "ABCDEFGHIJK", "maximum 10" }, //
                        { null, "10*", "ABCDEFGHIJK", "maximum 10*" }, // variable
                        { "", "10*", "ABCDEFGHIJK", "maximum 10*" }, // variable
                        { "*", "10*", "ABCDEFGHIJK", "maximum 10*" } // variable
                        });
            }

            public CharArraysizeTest(String arraysize, String maxarraysize, String value,
                    String fieldFormatExceptionWidthDescription)
            {
                this.arraysize = arraysize;
                this.maxarraysize = maxarraysize;
                this.value = value;
                this.fieldFormatExceptionWidthDescription = fieldFormatExceptionWidthDescription;
            }

            @Test
            public void testArraysizeAndMaxarraysizeUsed()
            {

                Field field = new Field();
                field.setName(this.fieldName);
                field.setArraysize(this.arraysize);

                FieldConstraint fieldDescription = null;
                if (this.maxarraysize != null)
                {
                    fieldDescription = new FieldConstraint();
                    fieldDescription.setMaxarraysize(this.maxarraysize);
                }

                try
                {
                    FieldDatatype.CHAR.validateFieldValue(field, fieldDescription, this.value);
                    if (this.fieldFormatExceptionWidthDescription != null)
                    {
                        fail("Expected FieldValidationException");
                    }
                }
                catch (FieldValidationException ex)
                {
                    if (this.fieldFormatExceptionWidthDescription == null)
                    {
                        fail("Unexpected FieldValidationException: " + ex.getMessage());
                    }
                    assertThat(ex.getMessage(), equalTo("Value '" + this.value + "' is wider than "
                            + fieldFormatExceptionWidthDescription + " chars"));
                }
            }
        }

    }

    @RunWith(Parameterized.class)
    public static class NumericValuesOutOfBoundsTest
    {
        private String fieldName = RandomStringUtils.random(10);
        private FieldDatatype fieldDatatype;
        private String value;
        private String fieldFormatExceptionTypeDescription;

        @Parameters
        public static Collection<Object[]> data()
        {
            return Arrays
                    .asList(new Object[][] {
                            // Parameters are: FieldDataType, value, fieldFormatExceptionTypeDescription
                            { FieldDatatype.SHORT, Short.toString(Short.MIN_VALUE), null }, //
                            { FieldDatatype.SHORT, Short.toString(Short.MAX_VALUE), null }, //
                            {
                                    FieldDatatype.SHORT,
                                    new BigInteger(Short.toString(Short.MIN_VALUE)).subtract(BigInteger.ONE).toString(),
                                    "short" }, //
                            { FieldDatatype.SHORT,
                                    new BigInteger(Short.toString(Short.MAX_VALUE)).add(BigInteger.ONE).toString(),
                                    "short" },

                            { FieldDatatype.INT, Integer.toString(Integer.MIN_VALUE), null }, //
                            { FieldDatatype.INT, Integer.toString(Integer.MAX_VALUE), null }, //
                            {
                                    FieldDatatype.INT,
                                    new BigInteger(Integer.toString(Integer.MIN_VALUE)).subtract(BigInteger.ONE)
                                            .toString(), "int" }, //
                            { FieldDatatype.INT,
                                    new BigInteger(Integer.toString(Integer.MAX_VALUE)).add(BigInteger.ONE).toString(),
                                    "int" },

                            { FieldDatatype.LONG, Long.toString(Long.MIN_VALUE), null }, //
                            { FieldDatatype.LONG, Long.toString(Long.MAX_VALUE), null }, //
                            { FieldDatatype.LONG,
                                    new BigInteger(Long.toString(Long.MIN_VALUE)).subtract(BigInteger.ONE).toString(),
                                    "long" }, //
                            { FieldDatatype.LONG,
                                    new BigInteger(Long.toString(Long.MAX_VALUE)).add(BigInteger.ONE).toString(),
                                    "long" },

                            { FieldDatatype.FLOAT, Float.toString(Float.MIN_VALUE), null }, //
                            { FieldDatatype.FLOAT, Float.toString(Float.MAX_VALUE), null }, //
                            { FieldDatatype.FLOAT,
                                    // We know Integer.MAX_VALUE will be rounded when converted to a float but we've
                                    // decided not to deal with these situations.
                                    new BigDecimal(Integer.toString(Integer.MAX_VALUE)).toString(), null }, //

                            { FieldDatatype.DOUBLE, Double.toString(Double.MIN_VALUE), null }, //
                            { FieldDatatype.DOUBLE, Double.toString(Double.MAX_VALUE), null }, //
                            { FieldDatatype.DOUBLE,
                                    // We know Long.MAX_VALUE will be rounded when converted to a double but we've
                                    // decided not to deal with these situations.
                                    new BigDecimal(Long.toString(Long.MAX_VALUE)).toString(), null } //
                    });
        }

        public NumericValuesOutOfBoundsTest(FieldDatatype fieldDatatype, String value,
                String fieldFormatExceptionTypeDescription)
        {
            this.fieldDatatype = fieldDatatype;
            this.value = value;
            this.fieldFormatExceptionTypeDescription = fieldFormatExceptionTypeDescription;
        }

        @Test
        public void testArraysizeAndMaxarraysizeUsed()
        {

            Field field = new Field();
            field.setName(this.fieldName);

            try
            {
                this.fieldDatatype.validateFieldValue(field, null, this.value);
                if (this.fieldFormatExceptionTypeDescription != null)
                {
                    fail("Expected FieldValidationException");
                }
            }
            catch (FieldValidationException ex)
            {
                if (this.fieldFormatExceptionTypeDescription == null)
                {
                    fail("Unexpected FieldValidationException: " + ex.getMessage());
                }
                assertThat(ex.getMessage(), equalTo("Value '" + this.value
                        + "' is not a '" + this.fieldFormatExceptionTypeDescription + "'"));
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class NumericValuesTooWideTest
    {
        private String fieldName = RandomStringUtils.random(10);
        private FieldDatatype fieldDatatype;
        private String value;
        private String fieldFormatExceptionTypeDescription;
        private String width;
        private String maxwidth;

        @Parameters
        public static Collection<Object[]> data()
        {
            ArrayList<Object[]> data = new ArrayList<>();

            // No constraints
            String[] alwaysValidWidthCombinations = new String[] { null, "" };
            for (String width : alwaysValidWidthCombinations)
            {
                for (String maxwidth : alwaysValidWidthCombinations)
                {
                    for (String value : new String[] { "0", "1", "-1" })
                    {
                        data.add(new Object[] { FieldDatatype.SHORT, width, maxwidth, value, null });
                    }
                    for (String value : new String[] { "0", "1", "-1" })
                    {
                        data.add(new Object[] { FieldDatatype.INT, width, maxwidth, value, null });
                    }
                    for (String value : new String[] { "0", "1", "-1" })
                    {
                        data.add(new Object[] { FieldDatatype.LONG, width, maxwidth, value, null });
                    }
                    for (String value : new String[] { "0", "0.0", "1", "1.0", "-1", "-1.0" })
                    {
                        data.add(new Object[] { FieldDatatype.FLOAT, width, maxwidth, value, null });
                    }
                    for (String value : new String[] { "0", "0.0", "1", "1.0", "-1", "-1.0" })
                    {
                        data.add(new Object[] { FieldDatatype.DOUBLE, width, maxwidth, value, null });
                    }
                }
            }
            // Good values, no maxwidth
            for (String maxwidth : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.SHORT, FieldDatatype.INT,
                        FieldDatatype.LONG })
                {
                    for (Object[] widthAndValues : new Object[][] { //
                    { "1", new String[] { "0", "1" } }, //
                            { "2", new String[] { "0", "1", "10", "99", "-9" } }, //
                            { "3", new String[] { "0", "1", "10", "999", "-09", "-99" } } //
                    })
                    {
                        String width = (String) widthAndValues[0];
                        String[] values = (String[]) widthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, null });
                        }
                    }
                }
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] widthAndValues : new Object[][] { //
                    { "1", new String[] { "0", "1" } }, //
                            { "2", new String[] { "0", "1", "10", "99", "-9" } }, //
                            { "3", new String[] { "0", "0.0", "1", "1.0", "10", "999", "-09", "-99" } }, //
                            { "4", new String[] { //
                                    "0", "0.0", "-0.0", "1", "1.0", "-1.0", "10", "999", "-09", "-99", "9999", "-999" //
                                    } } //
                    })
                    {
                        String width = (String) widthAndValues[0];
                        String[] values = (String[]) widthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, null });
                        }
                    }
                }
            }
            // Bad values, no maxwidth
            for (String maxwidth : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.SHORT, FieldDatatype.INT,
                        FieldDatatype.LONG })
                {
                    for (Object[] widthAndValues : new Object[][] { //
                    { "1", new String[] { "00", "10" } }, //
                            { "2", new String[] { "000", "110", "-10" } }, //
                            { "3", new String[] { "0000", "9999", "-011" } } //
                    })
                    {
                        String width = (String) widthAndValues[0];
                        String[] values = (String[]) widthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, width });
                        }
                    }
                }
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] widthAndValues : new Object[][] { //
                    { "1", new String[] { "01", "-1" } }, //
                            { "2", new String[] { "0.0", "100", "101", "-99", "-009" } }, //
                            { "3", new String[] { "0000", "0.000", "-0.00", "1.00", "1000", "-999" } } //
                    })
                    {
                        String width = (String) widthAndValues[0];
                        String[] values = (String[]) widthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, width });
                        }
                    }
                }
            }
            // Good values, using maxwidth
            for (String width : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.SHORT, FieldDatatype.INT,
                        FieldDatatype.LONG })
                {
                    for (Object[] maxwidthAndValues : new Object[][] { //
                    { "1", new String[] { "0", "1" } }, //
                            { "1", new String[] { "0", "1" } }, //
                            { "2", new String[] { "0", "1", "10", "99", "-9" } }, //
                            { "3", new String[] { "0", "1", "10", "999", "-09", "-99" } } //
                    })
                    {
                        String maxwidth = (String) maxwidthAndValues[0];
                        String[] values = (String[]) maxwidthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, null });
                        }
                    }
                }
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] maxwidthAndValues : new Object[][] { //
                    { "1", new String[] { "0", "1" } }, //
                            { "2", new String[] { "0", "1", "10", "99", "-9" } }, //
                            { "3", new String[] { "0", "0.0", "1", "1.0", "10", "999", "-09", "-99" } }, //
                            { "4", new String[] { //
                                    "0", "0.0", "-0.0", "1", "1.0", "-1.0", "10", "999", "-09", "-99", "9999", "-999" //
                                    } } //
                    })
                    {
                        String maxwidth = (String) maxwidthAndValues[0];
                        String[] values = (String[]) maxwidthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, null });
                        }
                    }
                }
            }
            // Bad values, using maxwidth
            for (String width : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.SHORT, FieldDatatype.INT,
                        FieldDatatype.LONG })
                {
                    for (Object[] maxwidthAndValues : new Object[][] { //
                    { "1", new String[] { "00", "10" } }, //
                            { "2", new String[] { "000", "110", "-10" } }, //
                            { "3", new String[] { "0000", "9999", "-011" } } //
                    })
                    {
                        String maxwidth = (String) maxwidthAndValues[0];
                        String[] values = (String[]) maxwidthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, "maximum " + maxwidth });
                        }
                    }
                }
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] maxwidthAndValues : new Object[][] { //
                    { "1", new String[] { "01", "-1" } }, //
                            { "2", new String[] { "0.0", "100", "101", "-99", "-009" } }, //
                            { "3", new String[] { "0000", "0.000", "-0.00", "1.00", "1000", "-999" } } //
                    })
                    {
                        String maxwidth = (String) maxwidthAndValues[0];
                        String[] values = (String[]) maxwidthAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, width, maxwidth, value, "maximum " + maxwidth });
                        }
                    }
                }
            }
            return data;
        }

        public NumericValuesTooWideTest(FieldDatatype fieldDatatype, String width, String maxwidth, String value,
                String fieldFormatExceptionTypeDescription)
        {
            this.fieldDatatype = fieldDatatype;
            this.width = width;
            this.maxwidth = maxwidth;
            this.value = value;
            this.fieldFormatExceptionTypeDescription = fieldFormatExceptionTypeDescription;
        }

        @Test
        public void testWidthAndMaxwidthUsed()
        {

            Field field = new Field();
            field.setName(this.fieldName);
            if (StringUtils.isNotEmpty(this.width))
            {
                field.setWidth(new BigInteger(this.width));
            }

            FieldConstraint fieldDescription = null;
            if (this.maxwidth != null)
            {
                fieldDescription = new FieldConstraint();
                fieldDescription.setMaxwidth(this.maxwidth);
            }

            try
            {
                this.fieldDatatype.validateFieldValue(field, fieldDescription, this.value);
                if (this.fieldFormatExceptionTypeDescription != null)
                {
                    fail("Expected FieldValidationException for " + this.fieldDatatype + " with width " + this.width
                            + " with maxwidth " + this.maxwidth + " with value " + this.value);
                }
            }
            catch (FieldValidationException ex)
            {
                if (this.fieldFormatExceptionTypeDescription == null)
                {
                    fail("Unexpected FieldValidationException: " + ex.getMessage());
                }
                assertThat(ex.getMessage(), equalTo("Value '" + this.value
                        + "' is wider than " + fieldFormatExceptionTypeDescription + " chars"));
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class RealValuesTooPreciseTest
    {
        private String fieldName = RandomStringUtils.random(10);
        private FieldDatatype fieldDatatype;
        private String value;
        private String fieldFormatExceptionTypeDescription;
        private String precision;
        private String maxprecision;

        @Parameters
        public static Collection<Object[]> data()
        {
            ArrayList<Object[]> data = new ArrayList<>();

            // No constraints
            String[] alwaysValidPrecisionCombinations = new String[] { null, "" };
            for (String precision : alwaysValidPrecisionCombinations)
            {
                for (String maxprecision : alwaysValidPrecisionCombinations)
                {
                    for (String value : new String[] { "0.0", "0.0932320", "1", "32231.0", "-12233", "-1.00000000000" })
                    {
                        data.add(new Object[] { FieldDatatype.FLOAT, precision, maxprecision, value, null });
                    }
                    for (String value : new String[] { "0.0", "0.0932320", "1", "32231.0", "-12233", "-1.00000000000" })
                    {
                        data.add(new Object[] { FieldDatatype.DOUBLE, precision, maxprecision, value, null });
                    }
                }
            }
            // Good values, no maxprecision
            for (String maxprecision : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] precisionAndValues : new Object[][] { //
                    { "0", new String[] { "0", "01", "1", "-1" } }, //
                            { "F0", new String[] { "0", "01", "1", "-1", "-01" } }, //
                            { "E1", new String[] { "0", "01", "1", "-1", "-01" } }, //
                            { "2", new String[] { "0", "1", "-1", "10.0", "099.09", "-0.08" } }, //
                            { "F2", new String[] { "0", "1", "-1", "010.0", "99.09", "-0.08" } }, //
                            { "E2", new String[] { "0", "1", "-01", "1.0", "00.9", "-0.8" } } //
                    })
                    {
                        String precision = (String) precisionAndValues[0];
                        String[] values = (String[]) precisionAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, precision, maxprecision, value, null });
                        }
                    }
                }
            }
            // Bad values, no maxprecision
            for (String maxprecision : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] precisionAndValues : new Object[][] { //
                            { "0", "0 decimal places", new String[] { "0.0", "01.0", "1.0", "-1.0" } }, //
                            { "F0", "0 decimal places", new String[] { "0.0", "01.0", "1.9", "-1.0", "-01.9" } }, //
                            { "E1", "1 significant digits", new String[] { "012", "1.1", "-1.0", "-011" } }, //
                            { "2", "2 decimal places", new String[] { "0.021", "01.222", "-1.003" } }, //
                            { "F2", "2 decimal places", new String[] { "0.021", "01.222", "-1.003" } }, //
                            { "E2", "2 significant digits",
                                    new String[] { "0111", "101", "-01.01", "1.01", "00.900", "-0.801" } } //
                    })
                    {
                        String precision = (String) precisionAndValues[0];
                        String precisionDescription = (String) precisionAndValues[1];
                        String[] values = (String[]) precisionAndValues[2];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, precision, maxprecision, value, precisionDescription });
                        }
                    }
                }
            }
            // Good values, using maxprecision
            for (String precision : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] maxprecisionAndValues : new Object[][] { //
                    { "0", new String[] { "0", "01", "1", "-1" } }, //
                            { "F0", new String[] { "0", "01", "1", "-1", "-01" } }, //
                            { "E1", new String[] { "0", "01", "1", "-1", "-01" } }, //
                            { "2", new String[] { "0", "1", "-1", "10.0", "099.09", "-0.08" } }, //
                            { "F2", new String[] { "0", "1", "-1", "010.0", "99.09", "-0.08" } }, //
                            { "E2", new String[] { "0", "1", "-01", "1.0", "00.9", "-0.8" } } //
                    })
                    {
                        String maxprecision = (String) maxprecisionAndValues[0];
                        String[] values = (String[]) maxprecisionAndValues[1];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, precision, maxprecision, value, null });
                        }
                    }
                }
            }
            // Bad values, using maxprecision
            for (String precision : new String[] { null, "" })
            {
                for (FieldDatatype datatype : new FieldDatatype[] { FieldDatatype.FLOAT, FieldDatatype.DOUBLE })
                {
                    for (Object[] maxprecisionAndValues : new Object[][] { //
                            { "0", "0 decimal places", new String[] { "0.0", "01.0", "1.0", "-1.0" } }, //
                            { "F0", "0 decimal places", new String[] { "0.0", "01.0", "1.9", "-1.0", "-01.9" } }, //
                            { "E1", "1 significant digits", new String[] { "012", "1.1", "-1.0", "-011" } }, //
                            { "2", "2 decimal places", new String[] { "0.021", "01.222", "-1.003" } }, //
                            { "F2", "2 decimal places", new String[] { "0.021", "01.222", "-1.003" } }, //
                            { "E2", "2 significant digits",
                                    new String[] { "0111", "101", "-01.01", "1.01", "00.900", "-0.801" } } //
                    })
                    {
                        String maxprecision = (String) maxprecisionAndValues[0];
                        String precisionDescription = (String) maxprecisionAndValues[1];
                        String[] values = (String[]) maxprecisionAndValues[2];
                        for (String value : values)
                        {
                            data.add(new Object[] { datatype, precision, maxprecision, value,
                                    "maximum " + precisionDescription });
                        }
                    }
                }
            }
            return data;
        }

        public RealValuesTooPreciseTest(FieldDatatype fieldDatatype, String precision, String maxprecision,
                String value, String fieldFormatExceptionTypeDescription)
        {
            this.fieldDatatype = fieldDatatype;
            this.precision = precision;
            this.maxprecision = maxprecision;
            this.value = value;
            this.fieldFormatExceptionTypeDescription = fieldFormatExceptionTypeDescription;
        }

        @Test
        public void testPrecisionAndMaxPrecisionUsed()
        {

            Field field = new Field();
            field.setName(this.fieldName);
            if (StringUtils.isNotEmpty(this.precision))
            {
                field.setPrecision(this.precision);
            }

            FieldConstraint fieldDescription = null;
            if (this.maxprecision != null)
            {
                fieldDescription = new FieldConstraint();
                fieldDescription.setMaxprecision(this.maxprecision);
            }

            try
            {
                this.fieldDatatype.validateFieldValue(field, fieldDescription, this.value);
                if (this.fieldFormatExceptionTypeDescription != null)
                {
                    fail("Expected FieldValidationException for " + this.fieldDatatype + " with precision "
                            + this.precision + " with maxprecision " + this.maxprecision + " with value " + this.value);
                }
            }
            catch (FieldValidationException ex)
            {
                if (this.fieldFormatExceptionTypeDescription == null)
                {
                    fail("Unexpected FieldValidationException: " + ex.getMessage());
                }
                assertThat(ex.getMessage(), equalTo("Value '" + this.value
                        + "' is more precise than " + fieldFormatExceptionTypeDescription));
            }
        }
    }

    @RunWith(Enclosed.class)
    public static class BooleanTest
    {

        @RunWith(Parameterized.class)
        public static class ValidValueTest
        {

            private String fieldName = RandomStringUtils.randomAlphanumeric(10);
            private String value;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] { { "0" }, { "1" }, { "T" }, { "F" }, { "t" }, { "f" }, { "trUE" },
                        { "FaLsE" }, { "TRUE" }, { " " }, { "?" }, { "\u0000" } });
            }

            public ValidValueTest(String value)
            {
                this.value = value;
            }

            @Test
            public void testValueIsValid() throws Exception
            {
                Field field = new Field();
                field.setName(this.fieldName);

                FieldConstraint fieldDescription = null;

                FieldDatatype.BOOLEAN.validateFieldValue(field, fieldDescription, value);
            }
        }

        @RunWith(Parameterized.class)
        public static class InvalidValueTest
        {
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            private String fieldName = RandomStringUtils.randomAlphanumeric(10);
            private String value;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] { { "2" }, { "a" }, { "wibble" }, { "y" }, { "N" }, { "712" },
                        { "" }, { "00" }, { "01" }, { "truey" }, { "falsed" } });
            }

            public InvalidValueTest(String value)
            {
                this.value = value;
            }

            @Test
            public void testValueInvalid() throws Exception
            {
                thrown.expect(FieldValidationException.class);
                thrown.expectMessage("Value '" + value + "' is not a 'boolean'");

                Field field = new Field();
                field.setName(this.fieldName);

                FieldConstraint fieldDescription = null;

                FieldDatatype.BOOLEAN.validateFieldValue(field, fieldDescription, value);
            }
        }
    }

    @RunWith(Enclosed.class)
    public static class BitTest
    {

        @RunWith(Parameterized.class)
        public static class ValidValueTest
        {

            private String fieldName = RandomStringUtils.randomAlphanumeric(10);
            private String value;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] { { "0" }, { "1" }, { "0010" }, { "1111111" }, { "1000000" },
                        { "101010" }, { "100000000000000" }, { "1000000 000000000" }, { "   1 1 0 1" },
                        { "11111111 00000000 11111111 00000000 10101010 00001111" }, { "" } });
            }

            public ValidValueTest(String value)
            {
                this.value = value;
            }

            @Test
            public void testValueIsValid() throws Exception
            {
                Field field = new Field();
                field.setName(this.fieldName);

                FieldConstraint fieldDescription = null;

                FieldDatatype.BIT.validateFieldValue(field, fieldDescription, value);
            }
        }

        @RunWith(Parameterized.class)
        public static class InvalidValueTest
        {
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            private String fieldName = RandomStringUtils.randomAlphanumeric(10);
            private String value;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] { { "2" }, { "wibble" }, { "101012" } });
            }

            public InvalidValueTest(String value)
            {
                this.value = value;
            }

            @Test
            public void testValueInValid() throws Exception
            {
                thrown.expect(FieldValidationException.class);
                thrown.expectMessage("Value '" + value + "' is not a 'bit'");

                Field field = new Field();
                field.setName(this.fieldName);

                FieldConstraint fieldDescription = null;

                FieldDatatype.BIT.validateFieldValue(field, fieldDescription, value);
            }
        }

        @RunWith(Parameterized.class)
        public static class ConvertValueTest
        {

            private String value;
            private String expected;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] { { "0", "0" }, { "1", "1" }, { "0010", "0010" },
                        { "1111111", "1111111" }, { "1000000", "1000000" }, { "10 10 10", "101010" },
                        { "10000000 0000001", "100000000000001" }, { "", "" }, { " grue ", "grue" } });
            }

            public ConvertValueTest(String value, String expected)
            {
                this.value = value;
                this.expected = expected;
            }

            @Test
            public void testConvertValue()
            {
                assertThat("Conversion of " + value + " gave the incorrect result",
                        FieldDatatype.BIT.convertFieldValue(value), is(expected));
            }
        }
    }

    @RunWith(Enclosed.class)
    public static class ByteTest
    {

        @RunWith(Parameterized.class)
        public static class ValidValueTest
        {

            private String fieldName = RandomStringUtils.randomAlphanumeric(10);
            private String value;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] { { "0" }, { "1" }, { "128" }, { "255" }, { "0x01" }, { "0xff" },
                        { "0x1f" }, { "0x0" } });
            }

            public ValidValueTest(String value)
            {
                this.value = value;
            }

            @Test
            public void testValueIsValid() throws Exception
            {
                Field field = new Field();
                field.setName(this.fieldName);

                FieldConstraint fieldDescription = null;

                FieldDatatype.UNSIGNEDBYTE.validateFieldValue(field, fieldDescription, value);
            }
        }

        @RunWith(Parameterized.class)
        public static class InvalidValueTest
        {
            @Rule
            public ExpectedException thrown = ExpectedException.none();

            private String fieldName = RandomStringUtils.randomAlphanumeric(10);
            private String value;

            @Parameters
            public static Collection<Object[]> data()
            {
                return Arrays.asList(new Object[][] { { "256" }, { "-1" }, { "wibble" }, { "0x100" }, { "0xfg" } });
            }

            public InvalidValueTest(String value)
            {
                this.value = value;
            }

            @Test
            public void testValueInValid() throws Exception
            {
                thrown.expect(FieldValidationException.class);
                thrown.expectMessage("Value '" + value + "' is not a 'unsignedbyte'");

                Field field = new Field();
                field.setName(this.fieldName);

                FieldConstraint fieldDescription = null;

                FieldDatatype.UNSIGNEDBYTE.validateFieldValue(field, fieldDescription, value);
            }
        }
    }
}
