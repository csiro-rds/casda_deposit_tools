package au.csiro.casda.datadeposit;

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


import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Test;

import au.csiro.util.TimeConversion;

/**
 * 
 * Tests the logging formatter methods.
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 *
 */
public class LoggingFormatterTest
{
    /**
     * Tests that duration is converted as expected - not truncated.
     */
    @Test
    public void testDurationToMillis()
    {
        assertEquals(1, TimeConversion.durationToMillis(Duration.ofNanos(1000000)));
        assertEquals(2, TimeConversion.durationToMillis(Duration.ofNanos(1900000)));
        assertEquals(1, TimeConversion.durationToMillis(Duration.ofNanos(1499999)));
        assertEquals(2, TimeConversion.durationToMillis(Duration.ofNanos(1500000)));
    }
}
