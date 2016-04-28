package au.csiro.casda.datadeposit.votable.parser;

import java.math.BigInteger;
import java.util.function.Function;

import net.ivoa.vo.Field;

import org.apache.commons.lang3.StringUtils;

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
 * Enumeration of possible Field datatypes with behaviour to validate a Field's attributes and value.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public enum FieldDatatype
{
    /**
     * CHAR DATATYPE enum
     */
    CHAR
    {
        /**
         * {@inheritDoc}
         */
        @Override
        protected void validateArraysizeAttribute(Field field, FieldConstraint constraint) throws FieldFormatException
        {
            if (StringUtils.isEmpty(field.getArraysize()))
            {
                return;
            }

            /*
             * arraysize for CHAR types means something a little different to other types. From the IVOA VOTABLE spec:
             * 
             * --------------------------------------------------------------------------------------------------------
             * Strings, which are defined as a set of characters, can therefore be represented in VOTable as a fixed- or
             * variable-length array of characters:
             * 
             * <FIELD name="unboundedString" datatype="char" arraysize="*"/>
             * --------------------------------------------------------------------------------------------------------
             */
            try
            {
                new Arraysize(field.getArraysize());
            }
            catch (IllegalArgumentException ex)
            {
                throw new FieldFormatException(String.format("Attribute 'arraysize' ('%s') does not match '%s'",
                        field.getArraysize(), Arraysize.ARRAYSIZE_REGEX.toString()));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            FieldKey.ARRAYSIZE.validateFieldAttributeValue(field, constraint, value);
        }

        /** {@inheritDoc} */
        @Override
        public String getWidthForField(VisitableVoTableField field, FieldConstraint constraint)
        {
            String result = null;
            if (StringUtils.isNotEmpty(field.getArraysize()))
            {
                Arraysize arraysize = new Arraysize(field.getArraysize());
                if (arraysize.hasMaximum())
                {
                    result = arraysize.getMaximum().toString();
                }
            }
            if (result == null && constraint != null && FieldKey.ARRAYSIZE.getValueForObject(constraint) != null)
            {
                Arraysize maxArraysize = new Arraysize(FieldKey.ARRAYSIZE.getValueForObject(constraint));
                if (maxArraysize.hasMaximum())
                {
                    result = maxArraysize.getMaximum().toString();
                }
            }
            return result;

        }
    },
    /**
     * BOOLEAN DATATYPE enum
     */
    BOOLEAN
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            /*
             * From the IVOA VOTABLE spec:
             * 
             * If the value of the datatype attribute specifies data type "boolean", the contents of the field shall
             * consist of the BINARY/BINARY2 serialization of ASCII "T", "t", or "1" indicating true, and ASCII "F",
             * "f", or "0" indicating false. The null value is indicated by an ascii NULL [0x00], a space [0x20] or a
             * question mark "?" [0x3f]. The acceptable representations in the TABLEDATA serialization also include any
             * capitalisation variation of the strings "true" and "false" (e.g. "tRUe" or "FalsE").
             */
            if (!value.toLowerCase().matches("0|1|t|f|true|false| |\\?|\u0000"))
            {
                throw new FieldValidationException(String.format("Value '%s' is not a '%s'", value, this.name()
                        .toLowerCase()));
            }
        }

        @Override
        public String convertFieldValue(String originalValue)
        {
            if (StringUtils.isNotBlank(originalValue))
            {
                char firstCharLower = originalValue.toLowerCase().charAt(0);
                if (firstCharLower == '1' || firstCharLower == 't')
                {
                    return "t";
                }
                if (firstCharLower == '0' || firstCharLower == 'f')
                {
                    return "f";
                }
            }
            // Anything else is null
            return null;
        }

    },
    /**
     * SHORT DATATYPE enum
     */
    SHORT
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            validateWholeNumberValue(field, constraint, value, (v) -> Short.parseShort(v));
        }
    },
    /**
     * INT DATATYPE enum
     */
    INT
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            validateWholeNumberValue(field, constraint, value, (v) -> Integer.parseInt(v));
        }
    },
    /**
     * LONG DATATYPE enum
     */
    LONG
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            validateWholeNumberValue(field, constraint, value, (v) -> Long.parseLong(v));
        }
    },
    /**
     * FLOAT DATATYPE enum
     */
    FLOAT
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            validateRealNumberValue(field, constraint, value, (v) -> {
                /*
                 * We've deliberately decided not to try and do any more complex parsing of the String into a Float,
                 * such as checking that the indicated precision is actually preserved.
                 */
                return Float.parseFloat(v);
            });
        }
    },
    /**
     * DOUBLE DATATYPE enum
     */
    DOUBLE
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            validateRealNumberValue(field, constraint, value, (v) -> {
                /*
                 * We've deliberately decided not to try and do any more complex parsing of the String into a Double,
                 * such as checking that the indicated precision is actually preserved.
                 */
                return Double.parseDouble(v);
            });

        }

    },

    /**
     * Bit array DATATYPE enum. Bit arrays are unbounded in VOTABLE so we store them using Postgres' bit varying type.
     * We allow spaces to separate binary values but strip them before handing the value to Postgres.
     */
    BIT
    {
        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            if (!value.matches("[01 ]*"))
            {
                throw new FieldValidationException(String.format("Value '%s' is not a '%s'", value, this.name()
                        .toLowerCase()));
            }
        }

        @Override
        public String convertFieldValue(String originalValue)
        {
            return originalValue.replace(" ", "");
        }

        @Override
        public String getWidthForField(VisitableVoTableField field, FieldConstraint constraint)
        {
            if (field.getWidth() != null && BigInteger.ZERO.compareTo(field.getWidth()) < 0)
            {
                return String.valueOf(field.getWidth());
            }
            else
            {
                return null;
            }
        }
    },

    /**
     * unsigned byte DATATYPE enum. An 8 bit positive number..
     */
    UNSIGNEDBYTE
    {
        private final static int MAX_UNSIGNED_BYTE = 255;

        @Override
        public void validateFieldValue(Field field, FieldConstraint constraint, String value)
                throws FieldValidationException
        {
            validateWholeNumberValue(field, constraint, value, (v) -> parseUnsignedByte(v), 0, MAX_UNSIGNED_BYTE);
        }

        @Override
        public String convertFieldValue(String originalValue)
        {
            return String.valueOf(parseUnsignedByte(originalValue));
        }
    };

    /**
     * Returns a FieldDatatype for the given Field.
     * 
     * @param field
     *            a Field
     * @return a FieldDatatype
     * @throws FieldFormatException
     *             if the field's datatype is not supported
     */
    public static FieldDatatype getFieldDatatypeForField(Field field) throws FieldFormatException
    {
        try
        {
            return FieldDatatype.valueOf(field.getDatatype().value().toUpperCase());
        }
        catch (IllegalArgumentException ex)
        {
            throw new FieldFormatException(String.format("Datatype '%s' is not supported", field.getDatatype()));
        }
    }

    /**
     * Validate the attributes of the given Field, ensuring that the attributes are constrained by the given constraint
     * (if present), and that any arraysize, width, and precision attributes (if present) have sensible values.
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @throws FieldFormatException
     *             if the field attributes are invalid
     */
    public void validateFieldAttributes(Field field, FieldConstraint constraint) throws FieldFormatException
    {
        if (constraint != null)
        {
            constraint.validateField(field);
        }
        validateArraysizeAttribute(field, constraint); // Common to all fields
        validateWidthAttribute(field, constraint); // Common to all fields
        validatePrecisionAttribute(field, constraint); // Common to all fields
    }

    /**
     * Template method with default implementation that checks the arraysize attribute on the given field (if present)
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @throws FieldFormatException
     *             if the field attributes are invalid
     */
    protected void validateArraysizeAttribute(Field field, FieldConstraint constraint) throws FieldFormatException
    {
        /*
         * Default behaviour is to reject fields with an arraysize because for all other types than CHAR it means the
         * data values will be an actual array of values embedded in the string value and differntiated with some
         * separator. For numeric types this would then imply we have to check the type, width, and precision of each
         * value in the array. There would also be serious storage ramifications for such fields. In the future we might
         * like to handle non-CHAR fields with arraysize as actual CHAR fields. Incidentally, CHAR has its own special
         * interpretation of arraysize.
         */
        if (StringUtils.isNotEmpty(field.getArraysize()))
        {
            throw new FieldFormatException(
                    String.format("Attribute 'arraysize' ('%s') for datatype '%s' is not supported",
                            field.getArraysize(), this.name()));
        }
    }

    /**
     * Template method with default implementation that checks the width attribute on the given field (if present)
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @throws FieldFormatException
     *             if the field attributes are invalid
     */
    protected void validateWidthAttribute(Field field, FieldConstraint constraint) throws FieldFormatException
    {
        /*
         * Although the width attribute only really makes sense for numeric types, we allow it to exist on any field.
         */
        if (field.getWidth() != null && field.getWidth().signum() == -1)
        {
            throw new FieldFormatException(String.format(
                    "Attribute 'width' attribute cannot be negative for datatype '%s'", this.name()));
        }
    }

    /**
     * Template method with default implementation that checks the precision attribute on the given field (if present)
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @throws FieldFormatException
     *             if the field attributes are invalid
     */
    protected void validatePrecisionAttribute(Field field, FieldConstraint constraint) throws FieldFormatException
    {
        /*
         * Although the precision attribute only really makes sense for real-number types, we allow it to exist on any
         * field.
         */
        if (StringUtils.isNotEmpty(field.getPrecision()))
        {
            try
            {
                // In theory this should never fail because the value is constrained in the schema.
                new Precision(field.getPrecision());
            }
            catch (IllegalArgumentException ex)
            {
                throw new FieldFormatException(String.format("Attribute 'precision' does not match '%s'",
                        Precision.PRECISION_REGEX.toString()));
            }
        }
    }

    /**
     * Requests the datatype to validate the given field/param's value against the datatype and the field (or
     * constraint's) attributes (eg: numeric types will check a 'width' if it is present on the field or as a maxwidth
     * on the constraint).
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @param value
     *            the value
     * @throws FieldValidationException
     *             if the value does not match the requirements of the field an (optional) constraint
     */
    public abstract void validateFieldValue(Field field, FieldConstraint constraint, String value)
            throws FieldValidationException;

    /**
     * Utility method for use by whole-number subclasses to check a field value
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @param value
     *            the value
     * @param converter
     *            a Function<String, Object> that is used to convert the value to a Java object whose type is
     *            appropriate for the specific FieldDatatype (eg: Short for SHORT)
     * @throws FieldValidationException
     *             if the value does not match the requirements of the field an (optional) constraint
     */
    protected void validateWholeNumberValue(Field field, FieldConstraint constraint, String value,
            Function<String, Object> converter) throws FieldValidationException
    {
        try
        {
            converter.apply(value);
        }
        catch (NumberFormatException e)
        {
            throw new FieldValidationException(String.format("Value '%s' is not a '%s'", value, this.name()
                    .toLowerCase()));
        }
        FieldKey.WIDTH.validateFieldAttributeValue(field, constraint, value);
    }

    /**
     * Utility method for use by whole-number subclasses to check a field value
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @param value
     *            the value
     * @param converter
     *            a Function<String, Object> that is used to convert the value to a Java object whose type is
     *            appropriate for the specific FieldDatatype (eg: Short for SHORT)
     * @param min
     *            The smallest number allowed for the numeric type.
     * @param max
     *            The largest number allowed for the numeric type.
     * @throws FieldValidationException
     *             if the value does not match the requirements of the field an (optional) constraint
     */
    protected void validateWholeNumberValue(Field field, FieldConstraint constraint, String value,
            Function<String, Object> converter, long min, long max) throws FieldValidationException
    {
        validateWholeNumberValue(field, constraint, value, converter);

        Number numericValue = (Number) converter.apply(value);

        if (numericValue.longValue() < min || numericValue.longValue() > max)
        {
            throw new FieldValidationException(String.format("Value '%s' is not a '%s'", value, this.name()
                    .toLowerCase()));
        }
    }

    /**
     * Utility method for use by real-number subclasses to check a field value
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @param value
     *            the value
     * @param converter
     *            a Function<String, Object> that is used to convert the value to a Java object whose type is
     *            appropriate for the specific FieldDatatype (eg: Double for DOUBLE)
     * @throws FieldValidationException
     *             if the value does not match the requirements of the field an (optional) constraint
     */
    protected void validateRealNumberValue(Field field, FieldConstraint constraint, String value,
            Function<String, Object> converter) throws FieldValidationException
    {
        try
        {
            converter.apply(value);
        }
        catch (NumberFormatException e)
        {
            throw new FieldValidationException(String.format("Value '%s' is not a '%s'", value, this.name()
                    .toLowerCase()));
        }
        FieldKey.WIDTH.validateFieldAttributeValue(field, constraint, value);
        FieldKey.PRECISION.validateFieldAttributeValue(field, constraint, value);
    }

    /**
     * Parse an unsigned integer value that may be a decimal or a hexadecimal number. Hexadecimal numbers are prefixed
     * by 0x.
     * 
     * @param value
     *            The value to be parsed.
     * @return The numeric value of the string.
     */
    protected int parseUnsignedByte(String value)
    {
        if (StringUtils.isNotBlank(value) && value.startsWith("0x"))
        {
            final int HEXADECIMAL_RADIX = 16;
            return Integer.parseUnsignedInt(value.substring(2), HEXADECIMAL_RADIX);
        }
        return Integer.parseUnsignedInt(value);
    }

    /**
     * Converts the supplied value to a standard format. Useful for types such as boolean and byte which have unusual
     * optional representations.
     * 
     * @param originalValue
     *            The value to be converted
     * @return The value on a standard form
     */
    public String convertFieldValue(String originalValue)
    {
        return originalValue == null ? null : originalValue.trim();
    }

    /**
     * Returns a String representing the 'width' of the given field if the given field has one and if such a concept
     * makes sense for the this datatype (or null otherwise)
     * 
     * @param field
     *            a Field
     * @param constraint
     *            an optional FieldConstraint
     * @return a String
     */
    public String getWidthForField(VisitableVoTableField field, FieldConstraint constraint)
    {
        return null;
    }
}
