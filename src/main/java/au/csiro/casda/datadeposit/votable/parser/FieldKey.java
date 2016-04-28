package au.csiro.casda.datadeposit.votable.parser;

import java.math.BigInteger;
import java.util.function.Function;

import net.ivoa.vo.CoordinateSystem;
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
 * FieldKeys represent the possible attributes of a Field or Param in a VOTABLE that may be constrained by a
 * {@link FieldConstraint}.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public enum FieldKey
{
    /**
     * Represents the 'name' attribute of a Field/Param
     */
    NAME(true, true, (f) -> f.getName()),

    /**
     * Represents the 'id' attribute of a Field/Param
     */
    ID(true, true, (f) -> f.getID()),

    /**
     * Represents the 'ucd' attribute of a Field/Param
     */
    UCD(true, true, (f) -> f.getUcd()),

    /**
     * Represents the 'datatype' attribute of a Field/Param
     */
    DATATYPE(true, false, (f) -> f.getDatatype() == null ? null : f.getDatatype().value().toLowerCase()),

    /**
     * Represents the 'ref' attribute of a Field/Param
     */
    REF(false, false, (f) -> f.getRef().toString())
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateAttributeConstraint(Field field, FieldConstraint fieldConstraint)
                throws FieldFormatException
        {
            String expected = this.getValueForObject(fieldConstraint);
            Object ref = field.getRef();
            if (ref == null)
            {
                throw new FieldFormatException(String.format("Attribute 'ref' is required and must be '%s'", expected));
            }
            if (ref instanceof CoordinateSystem)
            {
                String fieldRef = ((CoordinateSystem) ref).getID();
                if (fieldRef == null)
                {
                    throw new FieldFormatException(String.format("Attribute 'ref' is required and must be '%s'",
                            expected));
                }
                else if (!expected.equals(fieldRef))
                {
                    throw new FieldFormatException(String.format("Attribute 'ref' ('%s') must be '%s'", fieldRef,
                            expected));
                }
            }
            else
            {
                throw new FieldFormatException("Parser does not know how to extract a ref 'name' from a "
                        + ref.getClass().getName());
            }
        }
    },

    /**
     * Represents the 'unit' attribute of a Field/Param
     */
    UNIT(false, false, (f) -> f.getUnit()),

    /**
     * Represents the 'arraysize' attribute of a Field/Param
     */
    ARRAYSIZE(false, false, (f) -> f.getArraysize())
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateAttributeConstraint(Field field, FieldConstraint fieldConstraint)
                throws FieldFormatException
        {
            /*
             * arraysize is optional according to the spec so an absent one on the field will match any max arraysize in
             * this field constraint by default.
             */
            String expected = this.getValueForObject(fieldConstraint);
            String actual = this.getValueForObject(field);
            if (actual != null && new Arraysize(actual).definitelyExceedsMaxarraysize(new Arraysize(expected)))
            {
                throw new FieldFormatException(String.format("Attribute 'arraysize' ('%s') exceeds maximum of '%s'",
                        actual, new Arraysize(expected).getMaximum().toString()));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mergeAttributeConstraintForFieldConstraints(FieldConstraint toFieldConstraint,
                FieldConstraint fromFieldConstraint, String fieldOrParamTypeDescription, String fieldName)
        {
            /*
             * Default behaviour is to override if there is no value set on the toFieldConstraint and to narrow the
             * value if the fromFieldConstraint has a tighter constraint.
             */
            String toValue = this.getValueForObject(toFieldConstraint);
            String fromValue = this.getValueForObject(fromFieldConstraint);
            if (toValue == null)
            {
                toFieldConstraint.setConstraintForKey(fromValue, this);
            }
            else if (toValue != null && fromValue != null)
            {
                Arraysize to = new Arraysize(toValue);
                Arraysize from = new Arraysize(fromValue);
                if (to.definitelyExceedsMaxarraysize(from))
                {
                    if (to.isVariable() || from.isVariable())
                    {
                        toFieldConstraint.setConstraintForKey("*" + from.getMaximum().toString(), this);
                    }
                    else
                    {
                        toFieldConstraint.setConstraintForKey(from.getMaximum().toString(), this);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldAttributeValue(Field field, FieldConstraint fieldConstraint, String value)
                throws FieldValidationException
        {
            boolean usingMaxarraysize = false;
            Arraysize arraysize = null;
            if (StringUtils.isNotEmpty(field.getArraysize()))
            {
                arraysize = new Arraysize(field.getArraysize());
            }
            boolean arraysizeIsUnusable = arraysize == null || (arraysize.isVariable() && !arraysize.hasMaximum());
            boolean fieldConstraintHasMaxArraysize =
                    fieldConstraint != null && this.getValueForObject(fieldConstraint) != null;
            if (arraysizeIsUnusable && fieldConstraintHasMaxArraysize)
            {
                arraysize = new Arraysize(this.getValueForObject(fieldConstraint));
                usingMaxarraysize = true;
            }
            if (arraysize == null)
            {
                /*
                 * arraysize is unspecified or unconstrained (explicitly or implicitly) so any value is valid
                 */
                return;
            }
            if (arraysize.exceededByValue(value))
            {
                throw new FieldValidationException(String.format("Value '%s' is wider than %s%s chars", value,
                        usingMaxarraysize ? "maximum " : "", arraysize.getValue().toString()));
            }
        }
    },

    /**
     * Represents the 'width' attribute of a Field/Param
     */
    WIDTH(false, false, (f) -> f.getWidth() == null ? null : f.getWidth().toString())
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateAttributeConstraint(Field field, FieldConstraint fieldConstraint)
                throws FieldFormatException
        {
            /*
             * width is optional according to the spec so an absent one will match any max width in the field constraint
             * by default.
             */
            String expected = this.getValueForObject(fieldConstraint);
            String actual = this.getValueForObject(field);
            if (actual != null && new BigInteger(expected).compareTo(new BigInteger(actual)) < 0)
            {
                throw new FieldFormatException(String.format(
                        "Arraysize 'width' ('%s') is greater than maximum of '%s'", actual.toString(), expected));
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mergeAttributeConstraintForFieldConstraints(FieldConstraint toFieldConstraint,
                FieldConstraint fromFieldConstraint, String fieldOrParamTypeDescription, String fieldName)
        {
            /*
             * Default behaviour is to override if there is no value set on the toFieldConstraint and to narrow the
             * value if the fromFieldConstraint has a tighter constraint.
             */
            String toValue = this.getValueForObject(toFieldConstraint);
            String fromValue = this.getValueForObject(fromFieldConstraint);
            if (toValue == null)
            {
                toFieldConstraint.setConstraintForKey(fromValue, this);
            }
            else if (toValue != null && fromValue != null)
            {
                BigInteger to = new BigInteger(toValue);
                BigInteger from = new BigInteger(fromValue);
                if (to.compareTo(from) > 0)
                {
                    toFieldConstraint.setConstraintForKey(fromValue, this);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldAttributeValue(Field field, FieldConstraint fieldConstraint, String value)
                throws FieldValidationException
        {
            boolean usingMaxwidth = false;
            BigInteger width = field.getWidth();
            if (width == null && fieldConstraint != null && this.getValueForObject(fieldConstraint) != null)
            {
                width = new BigInteger(this.getValueForObject(fieldConstraint));
                usingMaxwidth = true;
            }
            if (width != null)
            {
                /*
                 * From the IVOA VOTABLE spec:
                 * 
                 * The width attribute is meant to indicate to the application the number of characters to be used for
                 * input or output of the quantity.
                 */
                BigInteger valueWidth = new BigInteger(Integer.toString(value.length()));
                if (valueWidth.compareTo(width) > 0)
                {
                    throw new FieldValidationException(String.format("Value '%s' is wider than %s%s chars", value,
                            usingMaxwidth ? "maximum " : "", width));
                }
            }
        }
    },

    /**
     * Represents the 'precision' attribute of a Field/Param
     */
    PRECISION(false, false, (f) -> f.getPrecision())
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void validateAttributeConstraint(Field field, FieldConstraint fieldConstraint)
                throws FieldFormatException
        {
            /*
             * precision is optional according to the spec so an absent one will match any max precision in the field
             * constraint by default.
             */
            String expected = this.getValueForObject(fieldConstraint);
            String actual = this.getValueForObject(field);
            if (actual != null)
            {
                Precision maxPrecision = new Precision(expected);
                Precision fieldPrecision = new Precision(actual);
                if (!maxPrecision.comparableTo(fieldPrecision))
                {
                    throw new FieldFormatException(String.format(
                            "Attribute 'precision' ('%s') must specify a number of %s", fieldPrecision.getValue(),
                            maxPrecision.getKindDescription()));
                }
                if (fieldPrecision.isMorePreciseThan(maxPrecision))
                {
                    throw new FieldFormatException(String.format(
                            "Attribute 'precision' ('%s') is more precise than maximum %s", fieldPrecision.getValue(),
                            maxPrecision.getDescription()));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mergeAttributeConstraintForFieldConstraints(FieldConstraint toFieldConstraint,
                FieldConstraint fromFieldConstraint, String fieldOrParamTypeDescription, String fieldName)
        {
            /*
             * Default behaviour is to override if there is no value set on the toFieldConstraint and to narrow the
             * value if the fromFieldConstraint has a tighter constraint. However, if the to and from constraints have
             * different kinds of precision then we barf.
             */
            String toValue = this.getValueForObject(toFieldConstraint);
            String fromValue = this.getValueForObject(fromFieldConstraint);
            if (toValue == null)
            {
                toFieldConstraint.setConstraintForKey(fromValue, this);
            }
            else if (toValue != null && fromValue != null)
            {
                Precision to = new Precision(toValue);
                Precision from = new Precision(fromValue);
                if (!to.comparableTo(from))
                {
                    throw new IllegalArgumentException(String.format(
                            "Multiple field constraints matching %s '%s' have incompatible %s attributes",
                            fieldOrParamTypeDescription, fieldName, this.toString().toLowerCase()));
                }
                else if (from.isMorePreciseThan(to))
                {
                    if (to.usesSignificantDigits())
                    {
                        toFieldConstraint.setConstraintForKey("E" + from.getNumSignificantDigits().toString(), this);
                    }
                    else
                    {
                        toFieldConstraint.setConstraintForKey("F" + from.getNumDecimalPlaces().toString(), this);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void validateFieldAttributeValue(Field field, FieldConstraint fieldConstraint, String value)
                throws FieldValidationException
        {
            boolean usingMaxPrecision = false;
            Precision precision = null;
            if (StringUtils.isNotEmpty(field.getPrecision()))
            {
                precision = new Precision(field.getPrecision());
            }
            else if (fieldConstraint != null && this.getValueForObject(fieldConstraint) != null)
            {
                precision = new Precision(this.getValueForObject(fieldConstraint));
                usingMaxPrecision = true;
            }
            if (precision == null)
            {
                /*
                 * precision is unspecified or unconstrained (explicitly or implicitly) so any value is valid
                 */
                return;
            }
            if (precision.exceededByValue(value))
            {
                throw new FieldValidationException(String.format("Value '%s' is more precise than %s%s", value,
                        usingMaxPrecision ? "maximum " : "", precision.getDescription()));
            }
        }
    };

    private boolean isIdentifying;
    private boolean isRequired;
    private Function<FieldConstraint, String> fieldConstraintValueExtractor;
    private Function<Field, String> fieldValueExtractor;

    private FieldKey(boolean isIdentifying, boolean isRequired, Function<Field, String> fieldValueExtractor)
    {
        this.isIdentifying = isIdentifying;
        this.isRequired = isRequired;
        this.fieldConstraintValueExtractor = (fd) -> fd.getConstraintForFieldKey(this);
        this.fieldValueExtractor = fieldValueExtractor;
    }

    /**
     * @return whether the attribute represented by this key can be used to identify one or more Fields/Params in a
     *         VOTABLE
     */
    public boolean isIdentifying()
    {
        return this.isIdentifying;
    }

    /**
     * @return whether the attribute represented by this key would indicate a required Field/Param in a VOTABLE
     */
    public boolean isRequired()
    {
        return isRequired;
    }

    /**
     * @param fieldConstraint
     *            a FieldConstraint
     * @return the value of the attribute represented by this key in the given FieldConstraint
     */
    public String getValueForObject(FieldConstraint fieldConstraint)
    {
        return this.fieldConstraintValueExtractor.apply(fieldConstraint);
    }

    /**
     * 
     * @param field
     *            a Field
     * @return the value of the attribute represented by this key in the given Field
     */
    public String getValueForObject(Field field)
    {
        return this.fieldValueExtractor.apply(field);
    }

    /**
     * Merges and attribute constraints associated with this key from one field constraint into another.
     * <p>
     * The default implementation of this method simply treats attribute constraint values as Strings but specific
     * FieldKey values will provide a specialised implementation if the String values embed some other type's
     * representation.
     * 
     * @param to
     *            the FieldConstraint that will be modified to contain the combined attribute constraint
     * @param from
     *            the FieldConstraint to merge from
     * @param fieldOrParamTypeDescription
     *            a String describing whether we are merging constraints for Field or Param validation (used only for
     *            exception reporting)
     * @param fieldName
     *            the name of the field for which we merging constraints for Field/Param validation (used only for
     *            exception reporting)
     */
    public void mergeAttributeConstraintForFieldConstraints(FieldConstraint to, FieldConstraint from,
            String fieldOrParamTypeDescription, String fieldName)
    {
        /*
         * Default behaviour is to override if there is no value set on the toFieldConstraint and barf if there is
         * already one and we're trying to override it.
         */
        String toValue = this.getValueForObject(to);
        String fromValue = this.getValueForObject(from);
        if (toValue == null)
        {
            to.setConstraintForKey(fromValue, this);
        }
        else if (fromValue != null && !fromValue.equals(toValue))
        {
            throw new IllegalArgumentException(String.format(
                    "Multiple field constraints matching %s '%s' have incompatible %s attributes",
                    fieldOrParamTypeDescription, fieldName, this.toString().toLowerCase()));
        }
    }

    /**
     * Validates that any attribute constraint associated with this key in the given FieldConstraint is satisfied by the
     * same attribute on the given Field.
     * <p>
     * The default implementation of this method simply compares attribute values as Strings but specific FieldKey
     * values will provide a specialised implementation if the String values embed some other type's representation.
     * 
     * @param field
     *            a Field
     * @param fieldConstraint
     *            a FieldConstraint
     * @throws FieldFormatException
     *             if the field's attribute fails the constraint
     */
    public void validateAttributeConstraint(Field field, FieldConstraint fieldConstraint) throws FieldFormatException
    {
        String required = this.getValueForObject(fieldConstraint);
        String actual = this.getValueForObject(field);
        if (actual == null)
        {
            throw new FieldFormatException(String.format("Attribute '%s' is required and must be '%s'", this.toString()
                    .toLowerCase(), required));
        }
        else if (!StringUtils.equals(required, actual))
        {
            throw new FieldFormatException(String.format("Attribute '%s' ('%s') must be '%s'", this.toString()
                    .toLowerCase(), actual, required));
        }
    }

    /**
     * Validates that 'value' of a Field is valid according to any constraint imposed by this value of this attribute in
     * the given Field and/or FieldConstraint.
     * <p>
     * The default implementation of this method does nothing as most attributes impose no special constraints. FieldKey
     * values will override this method if they actual can constrain the field value.
     * 
     * @param field
     *            a Field
     * @param fieldConstraint
     *            a FieldConstraint
     * @param value
     *            the value being checked
     * @throws FieldValidationException
     *             if the value fails any specific constraint associated with this key's attribute
     */
    public void validateFieldAttributeValue(Field field, FieldConstraint fieldConstraint, String value)
            throws FieldValidationException
    {
        /*
         * Default behaviour is do nothing. Specific kinds of validatable FieldKeys will override this method.
         */
    }
}
