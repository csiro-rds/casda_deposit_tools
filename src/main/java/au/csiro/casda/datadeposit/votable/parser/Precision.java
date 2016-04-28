package au.csiro.casda.datadeposit.votable.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Represents the precision attribute found on VOTABLE FIELDs and PARAMs.
 * <p>
 * From the IVOA VOTABLE spec:
 * <p>
 * The precision attribute is meant to express the number of significant digits, either as a number of decimal places
 * (e.g. precision=" F2" or equivalently precision="2" to express 2 significant figures after the decimal point), or as
 * a number of significant figures (e.g. precision=" E5" indicates a relative precision of 10-5).
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class Precision
{
    /**
     * The Regular Expression describing valid precisions.
     */
    public static final Pattern PRECISION_REGEX = Pattern.compile("F?(\\d+)|E(\\d+)");
    private static final int PRECISION_REGEX_DECIMAL_DIGITS_GROUP = 1;
    private static final int PRECISION_REGEX_EXPONENT_GROUP = 2;
    private Matcher valueMatcher;
    private String value;

    /**
     * Creates an Precision object from the given value. If the value does not match the PRECISION_REGEX then an
     * IllegalArgumentException will be thrown.
     * 
     * @param value
     *            a String
     */
    public Precision(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            throw new IllegalArgumentException(String.format("Expected value '%s' to match '%s'", value,
                    PRECISION_REGEX.toString()));
        }
        this.valueMatcher = PRECISION_REGEX.matcher(value);
        if (!this.valueMatcher.matches())
        {
            throw new IllegalArgumentException(String.format("Expected value '%s' to match '%s'", value,
                    PRECISION_REGEX.toString()));
        }
        this.value = value;
    }

    /**
     * @return the original value used to create this Precision
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * Returns whether this Precision is more precise that another Precision. An IllegalArgumentException will be thrown
     * if the Precisions are not compatible as defined by comparableTo.
     * 
     * @param precision
     *            a Precision
     * @return a boolean
     */
    public boolean isMorePreciseThan(Precision precision)
    {
        if (!this.comparableTo(precision))
        {
            throw new IllegalArgumentException("Expected precisions to be comparable but one uses significant digits "
                    + "and the other decimal places.");
        }
        if (this.usesSignificantDigits())
        {
            return this.getNumSignificantDigits().compareTo(precision.getNumSignificantDigits()) > 0;
        }
        else
        {
            return this.getNumDecimalPlaces().compareTo(precision.getNumDecimalPlaces()) > 0;
        }
    }

    /**
     * @return the number of significant digits represented by this precision, or null if this precision is defined as a
     *         constraint on a number of decimal places
     */
    public BigInteger getNumSignificantDigits()
    {
        if (!this.usesSignificantDigits())
        {
            return null;
        }
        else
        {
            return new BigInteger(this.valueMatcher.group(PRECISION_REGEX_EXPONENT_GROUP));
        }
    }

    /**
     * @return the number of decimal places represented by this precision, or null if this precision is defined as a
     *         constraint on a number of significant digits
     */
    public BigInteger getNumDecimalPlaces()
    {
        if (this.usesSignificantDigits())
        {
            return null;
        }
        else
        {
            return new BigInteger(this.valueMatcher.group(PRECISION_REGEX_DECIMAL_DIGITS_GROUP));
        }
    }

    /**
     * @return whether this Precision is defined as a constraint on the number of significant digits
     */
    public boolean usesSignificantDigits()
    {
        return this.valueMatcher.group(PRECISION_REGEX_EXPONENT_GROUP) != null;
    }

    /**
     * Returns whether the given value exceeds the maximum precision of this Precision.
     * 
     * @param value
     *            a String
     * @return a boolean
     */
    public boolean exceededByValue(String value)
    {
        if (this.usesSignificantDigits())
        {
            BigDecimal numericValue = new BigDecimal(value);
            BigInteger valuePrecision = new BigInteger(Integer.toString(numericValue.precision()));
            return valuePrecision.compareTo(this.getNumSignificantDigits()) > 0;
        }
        else
        {
            BigInteger valuePrecision;
            if (value.contains("."))
            {
                valuePrecision = new BigInteger(Integer.toString(value.split("\\.")[1].length()));
            }
            else
            {
                valuePrecision = BigInteger.ZERO;
            }
            return valuePrecision.compareTo(this.getNumDecimalPlaces()) > 0;
        }
    }

    /**
     * @param precision
     *            another Precision
     * @return whether this Precisions can be compared to another - this will be true if they both constrain using
     *         decimal places or both constrain using significant digits, and false otherwise.
     */
    public boolean comparableTo(Precision precision)
    {
        return (this.usesSignificantDigits() && precision.usesSignificantDigits())
                || (!this.usesSignificantDigits() && !precision.usesSignificantDigits());
    }

    /**
     * @return a description of the kind of this Precision
     */
    public String getKindDescription()
    {
        return usesSignificantDigits() ? "significant digits" : "decimal places";
    }

    /**
     * @return the degree of the Precision (either a number of significant digits or a number of decimal places)
     */
    public BigInteger getDegree()
    {
        return usesSignificantDigits() ? getNumSignificantDigits() : getNumDecimalPlaces();
    }

    /**
     * @return a description of the Precision
     */
    public String getDescription()
    {
        BigInteger precisionValue = getDegree();
        String precisionKind = getKindDescription();

        String description = precisionValue.toString() + " " + precisionKind;
        return description;
    }

}
