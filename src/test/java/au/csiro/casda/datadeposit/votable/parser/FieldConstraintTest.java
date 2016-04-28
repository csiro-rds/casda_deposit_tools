package au.csiro.casda.datadeposit.votable.parser;

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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import net.ivoa.vo.CoordinateSystem;
import net.ivoa.vo.DataType;
import net.ivoa.vo.Field;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/**
 * Tests the FieldConstraint class.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 *
 */
@RunWith(Enclosed.class)
public class FieldConstraintTest
{
    public static class TestValidateField
    {
        @Rule
        public ExpectedException thrown = ExpectedException.none();

        @Test
        public void testValidateFieldEmpty() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field = new Field();
            fieldDescription.validateField(field);
        }

        @Test
        public void testRefAttributeIgnoredIfNotSpecified() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field = createField(RandomStringUtils.random(50), null, null, null, null, null);
            fieldDescription.validateField(field);
        }

        @Test
        public void testDatatypeAttributeIgnoredIfNotSpecified() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field =
                    createField(null, null, DataType.values()[RandomUtils.nextInt(0, DataType.values().length - 1)],
                            null, null, null);
            fieldDescription.validateField(field);
        }

        @Test
        public void testUnitAttributeIgnoredIfNotSpecified() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field = createField(null, null, null, RandomStringUtils.random(50), null, null);
            fieldDescription.validateField(field);
        }

        @Test
        public void testArraysizeAttributeIgnoredIfNotSpecified() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field = createField(null, RandomStringUtils.random(50), null, "unit", null, null);
            fieldDescription.validateField(field);
        }

        @Test
        public void testWidthAttributeIgnoredIfNotSpecified() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field =
                    createField(null, null, null, "unit", new BigInteger(RandomStringUtils.randomNumeric(50)), null);
            fieldDescription.validateField(field);
        }

        @Test
        public void testPrecisionAttributeIgnoredIfNotSpecified() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field = createField(null, null, null, "unit", null, RandomStringUtils.random(50));
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldDescriptionEmptyFieldFull() throws FieldFormatException
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            Field field = createField("reference", "arraySize", DataType.CHAR, "unit", BigInteger.TEN, "precision");
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldAllChecked() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "4");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "4");
            // this should pass validation because all expected fields are present and they match
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchRef() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'ref' ('different') must be 'reference'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "*", "char", "unit", "10", "");

            Field field = createField("different", "arraySize", DataType.CHAR, "unit", BigInteger.TEN, "precision");

            // this should fail because the ref field is different
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldNullRef() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'ref' is required and must be 'reference'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "*", "char", "unit", "10", "");

            Field field = createField(null, "arraySize", DataType.CHAR, "unit", BigInteger.TEN, "precision");
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldEmptyRef() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'ref' ('') must be 'reference'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "*", "char", "unit", "10", "");

            Field field = createField("", "arraySize", DataType.CHAR, "unit", BigInteger.TEN, "precision");
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldNullArraysize() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", null, DataType.CHAR, "unit", BigInteger.TEN, "3");

            /*
             * Don't expect any failure here because the arraysize is optional on the field and absence does not mean it
             * fails the maxarraysize on the descriptor.
             */
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldEmptyArraysize() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "", DataType.CHAR, "unit", BigInteger.TEN, "3");

            /*
             * Don't expect any failure here because the arraysize is optional on the field and absence does not mean it
             * fails the maxarraysize on the descriptor.
             */
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldArraysizePurelyVariable() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "*", DataType.CHAR, "unit", BigInteger.TEN, "3");

            /*
             * Don't expect any failure here because the arraysize is optional on the field and absence does not mean it
             * fails the maxarraysize on the descriptor.
             */
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldArraysizeVariableUpToLimit() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            /*
             * Don't expect any failure here because the arraysize is optional on the field and absence does not mean it
             * fails the maxarraysize on the descriptor.
             */
            Field field = createField("reference", "20*", DataType.CHAR, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldArraysizeVariableOverLimit() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'arraysize' ('21*') exceeds maximum of '20'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "21*", DataType.CHAR, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldArraysizeFixedUpToLimit() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldArraysizeFixedOverLimit() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'arraysize' ('21') exceeds maximum of '20'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "21", DataType.CHAR, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldNullDatatype() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'datatype' is required and must be 'char'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", null, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingDatatype() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'datatype' ('boolean') must be 'char'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", DataType.BOOLEAN, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldNullUnit() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'unit' is required and must be 'units'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "units", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, null, BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldEmptyUnit() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'unit' ('') must be 'units'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "units", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingUnit() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'unit' ('different') must be 'units'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "units", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "different", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingWidth() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Arraysize 'width' ('17') is greater than maximum of '10'");

            FieldConstraint fieldDescription = createFieldDescription("reference", "30", "char", "unit", "10", "4");

            Field field = createField("reference", "30", DataType.CHAR, "unit", BigInteger.valueOf(17), "4");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldNullWidth() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", null, "3");

            /*
             * Don't expect any failure here because the width is optional on the field and absence does not mean it
             * fails the maxwidth on the descriptor.
             */
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldNullPrecision() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, null);

            /*
             * Don't expect any failure here because the precision is optional on the field and absence does not mean it
             * fails the maxPrecision on the descriptor.
             */
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldIncompatiblePrecisions1() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'precision' ('E3') must specify a number of decimal places");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "F3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "E3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldIncompatiblePrecisions2() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'precision' ('F3') must specify a number of significant digits");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "E3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "F3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldEmptyPrecision() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, null);

            /*
             * Don't expect any failure here because the precision is optional on the field and absence does not mean it
             * fails the maxPrecision on the descriptor.
             */
            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingNumDecimalDigitsPrecisionUpToLimit() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingNumDecimalDigitsPrecisionOverLimit() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'precision' ('4') is more precise than maximum 3 decimal places");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "4");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingNumDecimalDigitsPrecisionUpToLimitWithQualifier()
                throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "F3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingNumDecimalDigitsPrecisionOverLimitWithQualifier()
                throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'precision' ('4') is more precise than maximum 3 decimal places");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "F3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "4");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingPrecisionUpToLimit() throws FieldFormatException
        {
            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "E3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "E3");

            fieldDescription.validateField(field);
        }

        @Test
        public void testValidateFieldMismatchingPrecision() throws FieldFormatException
        {
            thrown.expect(FieldFormatException.class);
            thrown.expectMessage("Attribute 'precision' ('E4') is more precise than maximum 3 significant digits");

            FieldConstraint fieldDescription = createFieldDescription("reference", "20", "char", "unit", "10", "E3");

            Field field = createField("reference", "20", DataType.CHAR, "unit", BigInteger.TEN, "E4");

            fieldDescription.validateField(field);
        }

        private FieldConstraint createFieldDescription(String reference, String arraySize, String datatype,
                String unit, String width, String precision)
        {
            FieldConstraint fieldDescription = new FieldConstraint();
            fieldDescription.setName("fieldname");
            fieldDescription.setRef(reference);
            fieldDescription.setMaxarraysize(arraySize);
            fieldDescription.setDatatype(datatype);
            fieldDescription.setUnit(unit);
            fieldDescription.setMaxwidth(width);
            fieldDescription.setMaxprecision(precision);
            return fieldDescription;
        }

        private Field createField(String ref, String arraySize, DataType dataType, String unit, BigInteger width,
                String precision)
        {
            Field field = new Field();
            field.setName("fieldname");
            CoordinateSystem coordSystem = new CoordinateSystem();
            coordSystem.setID(ref);
            field.setRef(coordSystem);
            field.setArraysize(arraySize);
            field.setDatatype(dataType);
            field.setUnit(unit);
            field.setWidth(width);
            field.setPrecision(precision);
            return field;
        }
    }

    public static class TestIsApplicableToInstance
    {
        @Test
        public void testRef()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setRef("some ref");

            Field field = new Field();
            CoordinateSystem coordSystem = new CoordinateSystem();
            coordSystem.setID("some ref");
            field.setRef(coordSystem);

            assertFalse(constraint.isApplicableToInstance(field));

        }

        @Test
        public void testUnit()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setUnit("some unit");

            Field field = new Field();
            field.setUnit("some unit");

            assertFalse(constraint.isApplicableToInstance(field));

        }

        @Test
        public void testWidth()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setMaxwidth("20");

            Field field = new Field();
            field.setDatatype(DataType.INT);
            field.setWidth(new BigInteger("20"));

            assertFalse(constraint.isApplicableToInstance(field));

        }

        @Test
        public void testPrecision()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setMaxprecision("E5");

            Field field = new Field();
            field.setDatatype(DataType.DOUBLE);
            field.setPrecision("E5");

            assertFalse(constraint.isApplicableToInstance(field));

        }

        @Test
        public void testArraysize()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setMaxarraysize("20*");

            Field field = new Field();
            field.setDatatype(DataType.CHAR);
            field.setArraysize("20*");

            assertFalse(constraint.isApplicableToInstance(field));
        }

        @Test
        public void testNameByItself()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setName("foo");

            Field field = new Field();
            field.setName("foo");

            assertTrue(constraint.isApplicableToInstance(field));
        }

        @Test
        public void testUcdByItself()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setUcd("foo");

            Field field = new Field();
            field.setUcd("foo");

            assertTrue(constraint.isApplicableToInstance(field));
        }

        @Test
        public void testNameAndUcdBothMatch()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setName("foo");
            constraint.setUcd("bar");

            Field field = new Field();

            assertFalse(constraint.isApplicableToInstance(field));

            field.setName("foo");
            field.setUcd("");
            assertFalse(constraint.isApplicableToInstance(field));

            field.setName("");
            field.setUcd("bar");
            assertFalse(constraint.isApplicableToInstance(field));

            field.setName("foo");
            field.setUcd("bar");
            assertTrue(constraint.isApplicableToInstance(field));
        }

        @Test
        public void testDatatypeByItself()
        {
            FieldConstraint constraint = new FieldConstraint();
            Field field = new Field();
            for (DataType datatype : DataType.values())
            {
                constraint.setDatatype(datatype.value());

                for (DataType testDatatype : DataType.values())
                {
                    field.setDatatype(testDatatype);
                    assertThat(constraint.isApplicableToInstance(field), Matchers.equalTo(datatype == testDatatype));
                }
            }
        }

        @Test
        public void testDatatypeIgnoredIfNameSet()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setName("foo");
            constraint.setDatatype("char");

            Field field = new Field();
            field.setDatatype(DataType.CHAR);

            assertFalse(constraint.isApplicableToInstance(field));

            field.setName("foo");
            assertTrue(constraint.isApplicableToInstance(field));

            field.setDatatype(DataType.BIT);
            assertTrue(constraint.isApplicableToInstance(field));
        }

        @Test
        public void testDatatypeIgnoredIfUcdSet()
        {
            FieldConstraint constraint = new FieldConstraint();
            constraint.setUcd("foo");
            constraint.setDatatype("char");

            Field field = new Field();
            field.setDatatype(DataType.CHAR);

            assertFalse(constraint.isApplicableToInstance(field));

            field.setUcd("foo");
            assertTrue(constraint.isApplicableToInstance(field));

            field.setDatatype(DataType.BIT);
            assertTrue(constraint.isApplicableToInstance(field));
        }
    }
}
