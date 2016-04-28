package au.csiro.casda.datadeposit.votable.parser;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.ivoa.vo.Field;
import net.ivoa.vo.Param;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

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
 * Describes a constraint on one or more FIELDs (and, via the ParamConstraint subclass, PARAMs) in a VOTABLE.
 * <p>
 * Field/Param Constraints consist of a set of Field/Param attribute constraints. The possible attributes that can be
 * constrained are defined in the {@link FieldKey} enum.
 * <p>
 * Whether a particular Field/Param Constraint applies to a particular field depends on the particular set of attribute
 * constraints. Not all attributes can be matched - the {@link FieldKey#isIdentifying()} defines whether this is the
 * case or not. A constraint applies to a Field/Param if all of the 'identifying' attributes match. For example,
 * specifying a 'datatype' attribute constraint of 'char' will match any Field/Param with a datatype of 'char'. Adding
 * an additional attribute constraint for, say, 'name' will reduce the matching Fields/Params to those matching both the
 * name and a 'char' datatype. The method {@link #isApplicableToInstance(Field)} returns whether a constraint is
 * applicable to a particular Field/Param.
 * <p>
 * Non-'identifying' constraints represent constraints to be applied to and Fields/Params matched by the identifying
 * attribute constraints. For example, a 'arraysize' constraint can be used to limit the maximum arraysize of a matching
 * Field/Param.
 * <p>
 * There is a subset of identifying attribute constraints that are known as 'required' constraints (see
 * {@link FieldKey#isRequired()}). Specifying a FieldConstraint with one of these 'required' attribute constraints
 * implies that the VOTABLE must contain a Field/Param that matches the constraint.
 * <p>
 * It is common for more than one Field/Param Constraint to apply to a given Field/Param, eg: a specific constraint on a
 * mandatory field (say by 'name' and 'unit') and a general constraint on, say, a general datatype's maxarraysize. The
 * method {@link #mergeInOtherConstraintForField(FieldConstraint, Field)} can be used to incrementally build-up a
 * combined Field/Param constraint.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class FieldConstraint
{
    private Map<FieldKey, String> constraints;

    /**
     * Constructor
     */
    public FieldConstraint()
    {
        this.constraints = new HashMap<>();
    }

    /**
     * Returns an attribute constraint for the given FieldKey, or null if no such constraint has been defined.
     * 
     * @param fieldKey
     *            a FieldKey
     * @return the attribute constraint value (a String)
     */
    public String getConstraintForFieldKey(FieldKey fieldKey)
    {
        return this.constraints.get(fieldKey);
    }

    /**
     * Sets the value of the attribute constraint defined by the given FieldKey. Supplying a null or blank value will
     * remove any existing attribute constraint for the FieldKey - any other value will update it.
     * 
     * @param value
     *            the value (a String) to set
     * @param fieldKey
     *            a FieldKey
     */
    public void setConstraintForKey(String value, FieldKey fieldKey)
    {
        if (StringUtils.isNotBlank(value))
        {
            this.constraints.put(fieldKey, value.trim());
        }
        else
        {
            if (this.constraints.containsKey(fieldKey))
            {
                this.constraints.remove(fieldKey);
            }
        }
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.NAME. Typically used by code that relies on a
     * bean-like interface (eg: yaml deserialisation).
     * 
     * @param name
     *            a String
     */
    public void setName(String name)
    {
        setConstraintForKey(name, FieldKey.NAME);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.ID. Typically used by code that relies on a
     * bean-like interface (eg: yaml deserialisation).
     * 
     * @param id
     *            a String
     */
    public void setId(String id)
    {
        setConstraintForKey(id, FieldKey.ID);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.UCD. Typically used by code that relies on a
     * bean-like interface (eg: yaml deserialisation).
     * 
     * @param ucd
     *            a String
     */
    public void setUcd(String ucd)
    {
        setConstraintForKey(ucd, FieldKey.UCD);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.REF. Typically used by code that relies on a
     * bean-like interface (eg: yaml deserialisation).
     * 
     * @param ref
     *            a String
     */
    public void setRef(String ref)
    {
        setConstraintForKey(ref, FieldKey.REF);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.DATATYPE. Typically used by code that relies on
     * a bean-like interface (eg: yaml deserialisation).
     * 
     * @param datatype
     *            a String
     */
    public void setDatatype(String datatype)
    {
        this.setConstraintForKey(StringUtils.isBlank(datatype) ? null : datatype.trim().toLowerCase(),
                FieldKey.DATATYPE);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.ARRAYSIZE, representing the maximum arraysize a
     * constrained Field/Param may have. Typically used by code that relies on a bean-like interface (eg: yaml
     * deserialisation). Will throw an IllegalArgumentException if the given value is not a valid Arraysize.
     * 
     * @param maxarraysize
     *            a String
     */
    public void setMaxarraysize(String maxarraysize)
    {
        this.setConstraintForKey(
                StringUtils.isBlank(maxarraysize) ? null : new Arraysize(maxarraysize.trim()).getValue(),
                FieldKey.ARRAYSIZE);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.UNIT. Typically used by code that relies on a
     * bean-like interface (eg: yaml deserialisation).
     * 
     * @param unit
     *            a String
     */
    public void setUnit(String unit)
    {
        this.setConstraintForKey(unit, FieldKey.UNIT);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.WIDTH, representing the maximum width a
     * constrained Field/Param may have. Typically used by code that relies on a bean-like interface (eg: yaml
     * deserialisation). Will throw an IllegalArgumentException if the given value is not a valid BigInteger.
     * 
     * @param maxwidth
     *            a String
     */
    public void setMaxwidth(String maxwidth)
    {
        this.setConstraintForKey(StringUtils.isBlank(maxwidth) ? null : new BigInteger(maxwidth.trim()).toString(),
                FieldKey.WIDTH);
    }

    /**
     * A helper method used to set an attribute constraint for FieldKey.PRECISION, representing the maximum precision a
     * constrained Field/Param may have. Typically used by code that relies on a bean-like interface (eg: yaml
     * deserialisation). Will throw an IllegalArgumentException if the given value is not a valid Precision.
     * 
     * @param maxprecision
     *            a String
     */
    public void setMaxprecision(String maxprecision)
    {
        this.setConstraintForKey(
                StringUtils.isBlank(maxprecision) ? null : new Precision(maxprecision.trim()).getValue(),
                FieldKey.PRECISION);
    }

    /**
     * Returns whether this constraint is applicable to the given field/param.
     * <p>
     * Applicability is determined using the following criteria:
     * <ul>
     * <li>an empty constraint will always be applicable (though it will have no effect as a constraint)</li>
     * <li>if there is any attribute constraints that are both identifying and required then they will be used solely to
     * determine applicabilty - in this case if all those identifying and required attribute constraints match then the
     * constraint is appliable, otherwise it isn't. This allows us to detect 'missing' fields/params.</li>
     * <li>otherwise, applicability is determined by the remaining identifying but not-required attribute constraints -
     * though in this case the constraint is applicable if any of these match.</li>
     * </ul>
     * 
     * @param field
     *            a Field or Param
     * @return a boolean
     */
    public boolean isApplicableToInstance(Field field)
    {
        if (this.constraints.isEmpty())
        {
            /*
             * An empty constraint will match anything but will also have no constraining effect.
             */
            return true;
        }

        if (isMatchingFieldRequired())
        {
            return EnumSet
                    .allOf(FieldKey.class)
                    .stream()
                    .filter((fieldKey) -> fieldKey.isIdentifying() && fieldKey.isRequired()
                            && StringUtils.isNotBlank(fieldKey.getValueForObject(this)))
                    .allMatch((fieldKey) -> fieldKey.getValueForObject(this).equals(fieldKey.getValueForObject(field)));
        }
        else
        // match using non-required identifying fields
        {
            return EnumSet
                    .allOf(FieldKey.class)
                    .stream()
                    .filter((fieldKey) -> fieldKey.isIdentifying() && !fieldKey.isRequired()
                            && StringUtils.isNotBlank(fieldKey.getValueForObject(this)))
                    .anyMatch((fieldKey) -> fieldKey.getValueForObject(this).equals(fieldKey.getValueForObject(field)));
        }
    }

    /**
     * @return whether this constraint must have a corresponding Field/Param in a VOTABLE
     */
    public boolean isMatchingFieldRequired()
    {
        return EnumSet
                .allOf(FieldKey.class)
                .stream()
                .anyMatch(
                        (fieldKey) -> {
                            return fieldKey.isIdentifying() && fieldKey.isRequired()
                                    && StringUtils.isNotBlank(fieldKey.getValueForObject(this));
                        });
    }

    /**
     * @return a simple description of this object
     */
    public String getSimpleFieldDescription()
    {
        return EnumSet.allOf(FieldKey.class).stream()
                .filter((fk) -> fk.isIdentifying() && StringUtils.isNotBlank(fk.getValueForObject(this)))
                .map((fk) -> fk.toString().toLowerCase() + ": '" + fk.getValueForObject(this) + "'")
                .collect(Collectors.joining(", "));
    }

    /**
     * Validates the attributes of the given Field to determine if they match this constraint's attribute constraint.
     * 
     * @param field
     *            a Field
     * @throws FieldFormatException
     *             if the Field does not match
     */
    public void validateField(Field field) throws FieldFormatException
    {
        if (!this.isApplicableToInstance(field))
        {
            throw new IllegalArgumentException("Expected this.isApplicableToInstance(field)");
        }
        /*
         * NOTE: There is no checking as to whether all FieldKeys 'make sense' for a particular datatype (eg: width only
         * pertains to numeric types) - we just make sure that if they are defined in this constraint then any
         * corresponding value on the field is valid in comparison.
         */
        for (FieldKey fieldKey : EnumSet.allOf(FieldKey.class))
        {
            if (StringUtils.isBlank(fieldKey.getValueForObject(this)))
            {
                continue;
            }
            fieldKey.validateAttributeConstraint(field, this);
        }
    }

    /**
     * Merges the settings in the other constraint into this one. If the other constraint has incompatible attribute
     * constraints then an IllegalArgumentException will be thrown.
     * 
     * @param other
     *            another FieldConstraint
     * @param fieldOrParamTypeDescription
     *            a String describing whether we are merging constraints for Field or Param validation (used only for
     *            exception reporting)
     * @param fieldName
     *            the name of the field for which we merging constraints for Field/Param validation (used only for
     *            exception reporting)
     */
    public void mergeInOtherConstraintForField(FieldConstraint other, String fieldOrParamTypeDescription,
            String fieldName)
    {
        for (FieldKey fieldKey : EnumSet.allOf(FieldKey.class))
        {
            fieldKey.mergeAttributeConstraintForFieldConstraints(this, other, fieldOrParamTypeDescription, fieldName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        ToStringBuilder toStringBuilder = new ToStringBuilder(this);
        for (FieldKey fieldKey : EnumSet.allOf(FieldKey.class))
        {
            String value = this.constraints.get(fieldKey) == null ? "" : this.constraints.get(fieldKey);
            toStringBuilder.append(fieldKey.toString().toLowerCase(), value);
        }
        return toStringBuilder.toString();
    }

    /**
     * Template method used to determine if the given field is of the type described by this constraint.
     * 
     * @param field
     *            a Field
     * @return a boolean
     */
    protected boolean isFieldOfDescribedType(Field field)
    {
        return !(field instanceof Param);
    }

}
