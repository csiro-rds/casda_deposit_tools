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
 * Precision test cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class PrecisionTest
{

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testConstructorWithNull()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected value 'null' to match 'F?(\\d+)|E(\\d+)");
        new Precision(null);
    }

    @Test
    public void testConstructorWithEmptyString()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected value '' to match 'F?(\\d+)|E(\\d+)'");

        new Precision("");
    }

    @Test
    public void testConstructorWithBogusValue()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected value 'E-3' to match 'F?(\\d+)|E(\\d+)'");

        new Precision("E-3");
    }

    @Test
    public void testConstructorWithSimpleNumber()
    {
        Precision precision = new Precision("3");
        assertThat(precision.getValue(), equalTo("3"));
        assertFalse(precision.usesSignificantDigits());
        assertThat(precision.getNumSignificantDigits(), nullValue());
        assertThat(precision.getNumDecimalPlaces(), equalTo(new BigInteger("3")));
        assertThat(precision.getDegree(), equalTo(new BigInteger("3")));
        assertThat(precision.getKindDescription(), equalTo("decimal places"));
        assertThat(precision.getDescription(), equalTo("3 decimal places"));
    }

    @Test
    public void testConstructorWithDecimalPlacesQualifier()
    {
        Precision precision = new Precision("F4");
        assertThat(precision.getValue(), equalTo("F4"));
        assertFalse(precision.usesSignificantDigits());
        assertThat(precision.getNumSignificantDigits(), nullValue());
        assertThat(precision.getNumDecimalPlaces(), equalTo(new BigInteger("4")));
        assertThat(precision.getDegree(), equalTo(new BigInteger("4")));
        assertThat(precision.getKindDescription(), equalTo("decimal places"));
        assertThat(precision.getDescription(), equalTo("4 decimal places"));
    }

    @Test
    public void testConstructorWithSignificantDigitsQualifier()
    {
        Precision precision = new Precision("E5");
        assertThat(precision.getValue(), equalTo("E5"));
        assertTrue(precision.usesSignificantDigits());
        assertThat(precision.getNumSignificantDigits(), equalTo(new BigInteger("5")));
        assertThat(precision.getNumDecimalPlaces(), nullValue());
        assertThat(precision.getDegree(), equalTo(new BigInteger("5")));
        assertThat(precision.getKindDescription(), equalTo("significant digits"));
        assertThat(precision.getDescription(), equalTo("5 significant digits"));
    }

    @Test
    public void testComparableTo()
    {
        Precision p1 = new Precision("F3");
        Precision p2 = new Precision("3");
        Precision p3 = new Precision("E3");
        Precision p4 = new Precision("E4");

        assertTrue(p1.comparableTo(p1));
        assertTrue(p1.comparableTo(p2));
        assertFalse(p1.comparableTo(p3));
        assertFalse(p1.comparableTo(p4));

        assertTrue(p2.comparableTo(p1));
        assertTrue(p2.comparableTo(p2));
        assertFalse(p2.comparableTo(p3));
        assertFalse(p2.comparableTo(p4));

        assertFalse(p3.comparableTo(p1));
        assertFalse(p3.comparableTo(p2));
        assertTrue(p3.comparableTo(p3));
        assertTrue(p3.comparableTo(p4));

        assertFalse(p4.comparableTo(p1));
        assertFalse(p4.comparableTo(p2));
        assertTrue(p4.comparableTo(p3));
        assertTrue(p4.comparableTo(p4));
    }

    @Test
    public void testIsMorePreciseThanComparingSignificantDigitsToDecimalPlaces()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected precisions to be comparable but one uses significant digits "
                + "and the other decimal places.");

        new Precision("F3").isMorePreciseThan(new Precision("E3"));
    }

    @Test
    public void testIsMorePreciseThanComparingDecimalPlacesToSignificantDigits()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Expected precisions to be comparable but one uses significant digits "
                + "and the other decimal places.");

        new Precision("E3").isMorePreciseThan(new Precision("F3"));
    }

    @Test
    public void testIsMorePreciseThanComparingDecimalPlaces()
    {
        assertFalse(new Precision("F3").isMorePreciseThan(new Precision("F4")));
        assertFalse(new Precision("F3").isMorePreciseThan(new Precision("F3")));
        assertTrue(new Precision("F3").isMorePreciseThan(new Precision("F2")));
    }

    @Test
    public void testIsMorePreciseThanComparingSignificantDigits()
    {
        assertFalse(new Precision("E3").isMorePreciseThan(new Precision("E4")));
        assertFalse(new Precision("E3").isMorePreciseThan(new Precision("E3")));
        assertTrue(new Precision("E3").isMorePreciseThan(new Precision("E2")));
    }

}
