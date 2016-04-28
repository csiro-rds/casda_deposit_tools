package au.csiro.casda.datadeposit.observation.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.joda.time.DateTime;
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
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@RunWith(Enclosed.class)
public class ObservationTimeTest
{

    @RunWith(Parameterized.class)
    public static class StringConstructorBadValuesTest
    {

        @Parameters
        public static Collection<Object[]> data()
        {
            return Arrays.asList(new Object[][] { { null }, { "a" }, { "1923-13-11T08:33" }
            // TODO: Include more examples (I will ask Brendan Sullivan for a few more of these)
                    });
        }

        @Rule
        public ExpectedException thrown = ExpectedException.none();

        private String value;

        public StringConstructorBadValuesTest(String value)
        {
            this.value = value;
        }

        @Test
        public void testStringConstructorWithNull()
        {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage(badValueExpectedMessage(value));

            new ObservationTime(value);
        }

        private String badValueExpectedMessage(String badValue)
        {
            return String.format("expected "
                    + "Pattern.matches('\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d+)?', %s) " + "|| "
                    + "Pattern.matches('\\d+(\\.\\d+)?', %s)", badValue, badValue);
        }
    }

    @RunWith(Parameterized.class)
    public static class StringConstructorTest
    {

        @Parameters
        public static Collection<Object[]> data()
        {
            return Arrays.asList(new Object[][] {
                    { "2013-11-19T02:30:00.00", "2013-11-19T02:30:00Z", 56615.104166666664, 1384828200000L },
                    { "2013-11-19T02:30:00", "2013-11-19T02:30:00Z", 56615.104166666664, 1384828200000L },
                    { "123.2213456", "1859-03-20T05:18:44.25984Z", 123.2213456, -3496070475741L },
                    { "1859-03-20T05:18:44.25984", "1859-03-20T05:18:44.25984Z", 123.2213456, -3496070475741L },
                    { "123.22134559027778", "1859-03-20T05:18:44.259Z", 123.22134559027778, -3496070475741L },
                    { "1859-03-20T05:18:44.259", "1859-03-20T05:18:44.259Z", 123.22134559027778, -3496070475741L }
            // TODO: Include more examples (I have asked Brendan Sullivan for a few more of these)
                    });
        }

        private String value;
        private String resultTimeString;
        private double resultMjd;
        private long timestamp;

        public StringConstructorTest(String value, String resultTimeString, double resultMjd, long timestamp)
        {
            this.value = value;
            this.resultTimeString = resultTimeString;
            this.resultMjd = resultMjd;
            this.timestamp = timestamp;
        }

        @Test
        public void testStringConstructor()
        {
            ObservationTime it = new ObservationTime(value);
            assertTrue(timestamp == it.asTimestamp().getTime());
            assertEquals(resultTimeString + "[UTC]", it.toString());
            assertEquals(resultMjd, it.asModifiedJulianDate(), 0.0);
        }
    }
    
    @RunWith(Parameterized.class)
    public static class DateTimeToModifiedJulianDateTest
    {

        @Parameters
        public static Collection<Object[]> data()
        {
            return Arrays.asList(new Object[][] {
                    { "2013-11-19T02:30:00Z", 56615.104166666664 },
                    { "2013-11-19T02:30:00Z", 56615.104166666664 },
                    { "1859-03-20T05:18:44.25984Z", 123.22134559027778 } 
            // TODO: Include more examples (I have asked Brendan Sullivan for a few more of these)
                    });
        }

        private DateTime value;
        private double resultMjd;

        public DateTimeToModifiedJulianDateTest(String value, double resultMjd)
        {
            this.value = DateTime.parse(value);
            this.resultMjd = resultMjd;
        }

        @Test
        public void testIt()
        {
            assertEquals(resultMjd, ObservationTime.dateTimeToModifiedJulianDate(value), 0.0);
        }
    }
    
}
