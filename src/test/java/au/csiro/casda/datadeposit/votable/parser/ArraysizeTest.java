package au.csiro.casda.datadeposit.votable.parser;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
 * Arraysize test cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class ArraysizeTest
{

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testConstructorWithNull()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected value 'null' to match '(\\d+)?(\\*)?'");
        new Arraysize(null);
    }

    @Test
    public void testConstructorWithEmptyString()
    {
        Arraysize arraysize = new Arraysize("");
        assertThat(arraysize.getValue(), equalTo(""));
        assertThat(arraysize.getMaximum(), nullValue());
        assertFalse(arraysize.isVariable());
        assertFalse(arraysize.hasMaximum());
    }

    @Test
    public void testConstructorWithVotableSeparatorString()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected value '*10s,' to match '(\\d+)?(\\*)?'");
        new Arraysize("*10s,");
    }

    @Test
    public void testConstructorWithFitsSeparatorString()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected value '20A:SSTR50/032' to match '(\\d+)?(\\*)?'");
        new Arraysize("20A:SSTR50/032");
    }

    @Test
    public void testConstructorWithPurelyUnboundedValue()
    {
        Arraysize arraysize = new Arraysize("*");
        assertThat(arraysize.getValue(), equalTo("*"));
        assertThat(arraysize.getMaximum(), nullValue());
        assertTrue(arraysize.isVariable());
        assertFalse(arraysize.hasMaximum());
    }

    @Test
    public void testConstructorWithBoundedValue()
    {
        Arraysize arraysize = new Arraysize("30");
        assertThat(arraysize.getValue(), equalTo("30"));
        assertThat(arraysize.getMaximum(), equalTo(new BigInteger("30")));
        assertFalse(arraysize.isVariable());
        assertTrue(arraysize.hasMaximum());
    }

    @Test
    public void testConstructorWithPurelyBundedVariableValue()
    {
        Arraysize arraysize = new Arraysize("40*");
        assertThat(arraysize.getValue(), equalTo("40*"));
        assertThat(arraysize.getMaximum(), equalTo(new BigInteger("40")));
        assertTrue(arraysize.isVariable());
        assertTrue(arraysize.hasMaximum());
    }

    @Test
    public void testPurelyUnboundedDoesNotDefinitelyExceedPurelyUnboundedArraysize()
    {
        assertFalse(new Arraysize("*").definitelyExceedsMaxarraysize(new Arraysize("*")));
    }

    @Test
    public void testPurelyUnboundedDoesNotDefinitelyExceedBoundedArraysize()
    {
        assertFalse(new Arraysize("*").definitelyExceedsMaxarraysize(new Arraysize("0")));
    }

    @Test
    public void testPurelyUnboundedDoesNotDefinitelyExceedBoundedVariableArraysize()
    {
        assertFalse(new Arraysize("*").definitelyExceedsMaxarraysize(new Arraysize("0*")));
    }

    @Test
    public void testBoundedDoesNotDefinitelyExceedPurelyUnboundedArraysize()
    {
        assertFalse(new Arraysize("10").definitelyExceedsMaxarraysize(new Arraysize("*")));
    }

    @Test
    public void testDefinitelyExceedsMaxarraysizeForBoundedVsBoundedArraysize()
    {
        assertFalse(new Arraysize("10").definitelyExceedsMaxarraysize(new Arraysize("11")));
        assertFalse(new Arraysize("10").definitelyExceedsMaxarraysize(new Arraysize("10")));
        assertTrue(new Arraysize("10").definitelyExceedsMaxarraysize(new Arraysize("9")));
    }

    @Test
    public void testDefinitelyExceedsMaxarraysizeForVariableBoundedVsBoundedArraysize()
    {
        assertFalse(new Arraysize("10*").definitelyExceedsMaxarraysize(new Arraysize("11")));
        assertFalse(new Arraysize("10*").definitelyExceedsMaxarraysize(new Arraysize("10")));
        assertTrue(new Arraysize("10*").definitelyExceedsMaxarraysize(new Arraysize("9")));
    }

    @Test
    public void testDefinitelyExceedsMaxarraysizeForBoundedVsVariableBoundedArraysize()
    {
        assertFalse(new Arraysize("10").definitelyExceedsMaxarraysize(new Arraysize("11*")));
        assertFalse(new Arraysize("10").definitelyExceedsMaxarraysize(new Arraysize("10*")));
        assertTrue(new Arraysize("10").definitelyExceedsMaxarraysize(new Arraysize("9*")));
    }

    @Test
    public void testDefinitelyExceedsMaxarraysizeForVariableBoundedVsVariableBoundedArraysize()
    {
        assertFalse(new Arraysize("10*").definitelyExceedsMaxarraysize(new Arraysize("11*")));
        assertFalse(new Arraysize("10*").definitelyExceedsMaxarraysize(new Arraysize("10*")));
        assertTrue(new Arraysize("10*").definitelyExceedsMaxarraysize(new Arraysize("9*")));
    }

}
