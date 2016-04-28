package au.csiro.casda.datadeposit.votable.parser;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Represents the arraysize attribute found on VOTABLE FIELDs and PARAMs.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class Arraysize
{
    /**
     * The Regular Expression describing valid arraysizes. The unofficial extensions to arraysize to support arrays of
     * character strings, as described in the IVOA VOTABLE 1.3 spec are not supported.
     */
    public static final Pattern ARRAYSIZE_REGEX = Pattern.compile("(\\d+)?(\\*)?");
    private static final int ARRAYSIZE_REGEX_SIZE_INDEX = 1;
    private static final int ARRAYSIZE_REGEX_VARIABLITY_INDEX = 2;

    private Matcher valueMatcher;

    private String value;

    /**
     * Creates an Arraysize object from the given value. If the value does not match the ARRAYSIZE_REGEX then an
     * IllegalArgumentException will be thrown.
     * 
     * @param value
     *            a String
     */
    public Arraysize(String value)
    {
        if (value == null)
        {
            throw new IllegalArgumentException(String.format("Expected value '%s' to match '%s'", value,
                    ARRAYSIZE_REGEX.toString()));
        }
        this.valueMatcher = ARRAYSIZE_REGEX.matcher(value);
        if (!this.valueMatcher.matches())
        {
            throw new IllegalArgumentException(String.format("Expected value '%s' to match '%s'", value,
                    ARRAYSIZE_REGEX.toString()));
        }
        this.value = value;
    }

    /**
     * @return the original value used to create this Arraysize
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * Returns whether this Arraysize definitely exceeds a maximum Arraysize. This will be true only if both are bounded
     * and this arraysize's bound is less than or equal to the maximum's bound.
     * 
     * @param maxarraysize
     *            an Arraysize
     * @return a boolean
     */
    public boolean definitelyExceedsMaxarraysize(Arraysize maxarraysize)
    {
        if (!this.hasMaximum() || !maxarraysize.hasMaximum())
        {
            return false;
        }
        else
        {
            return this.getMaximum().compareTo(maxarraysize.getMaximum()) > 0;
        }
    }

    /**
     * @return whether this arraysize has a maximum bound
     */
    public boolean hasMaximum()
    {
        return valueMatcher.group(ARRAYSIZE_REGEX_SIZE_INDEX) != null;
    }

    /**
     * Returns whether the given value exceeds the maximum bounds of this Arraysize. An unbounded Arraysize will never
     * be exceeded by any value.
     * 
     * @param value
     *            a String
     * @return a boolean
     */
    public boolean exceededByValue(String value)
    {
        return this.hasMaximum() && this.getMaximum().compareTo(new BigInteger(Integer.toString(value.length()))) < 0;
    }

    /**
     * @return the maximum bound for this Arraysize (if one exists)
     */
    public BigInteger getMaximum()
    {
        if (!this.hasMaximum())
        {
            return null;
        }
        else
        {
            return new BigInteger(valueMatcher.group(ARRAYSIZE_REGEX_SIZE_INDEX));
        }
    }

    /**
     * @return whether this Arraysize is variable
     */
    public boolean isVariable()
    {
        return valueMatcher.group(ARRAYSIZE_REGEX_VARIABLITY_INDEX) != null;
    }

}
