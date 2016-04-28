package au.csiro.casda.datadeposit.observation.assembler;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.JulianFields;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
 * Value class that represents an ObservationTime as read from an observation (ie, dataset) XML file. An ObservationTime
 * is created using a String representation of a (observation) time, in either a standard time format or a Modified
 * Julian Date format. Once the object has been created it can be converted to a LocalDateTime or a Modified Julian Date
 * (as a double) as required.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class ObservationTime
{

    // Assume there are exactly 24 hours in a day for this calculation.
    // I think this is right but I will check with one of our Astronomers.
    private static final long HOURS_PER_DAY = 24;
    private static final long MINUTES_PER_HOUR = 60;
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR * SECONDS_PER_MINUTE;

    private static final double NANOSECONDS_PER_SECOND = 1e9;

    private static final int FRACTIONAL_GROUP_INDEX = 2;
    private static final int FRACTIONAL_PART_INDEX = 3;

    private ZonedDateTime time;

    /**
     * Creates an ObservationTime from the given String value.
     * 
     * @param value
     *            the time as either in the format 'yyyy-MM-ddTHH:mm:ss' (with optional fractional seconds) or as a
     *            Modified Julian Date (ie: a decimal)
     */
    public ObservationTime(String value)
    {
        if (value == null
                || !(Pattern.matches("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d+)?", value) || Pattern
                        .matches("\\d+(\\.\\d+)?", value)))
        {
            throw new IllegalArgumentException(String.format("expected "
                    + "Pattern.matches('\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d+)?', %s) " + "|| "
                    + "Pattern.matches('\\d+(\\.\\d+)?', %s)", value, value));
        }

        try
        {
            this.time = parseAsUTCZonedDateTime(value);
        }
        catch (DateTimeParseException e)
        {
            Pattern mjdFormat = Pattern.compile("(\\d+)(\\.(\\d+))?");

            Matcher matcher = mjdFormat.matcher(value);
            matcher.matches(); // Required to be able to call group(n) on matcher.

            this.time = parseAsUTCZonedDateTime("0000-01-01T00:00:00"); // Date is arbitrary but time must be midnight.

            this.time = this.time.with(JulianFields.MODIFIED_JULIAN_DAY, Long.parseLong(matcher.group(1)));

            double fractionOfDay;
            if (matcher.group(FRACTIONAL_GROUP_INDEX) != null)
            {
                fractionOfDay = Double.parseDouble("0." + matcher.group(FRACTIONAL_PART_INDEX));
            }
            else
            {
                fractionOfDay = 0.0;
            }
            long fractionOfDayInNanoseconds = (long) (fractionOfDay * SECONDS_PER_DAY * NANOSECONDS_PER_SECOND);
            this.time = this.time.with(ChronoField.NANO_OF_DAY, fractionOfDayInNanoseconds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.time.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * @return this object as a double representation of the Modified Julian Date
     */
    public double asModifiedJulianDate()
    {
        return this.time.getLong(JulianFields.MODIFIED_JULIAN_DAY) + this.time.getLong(ChronoField.NANO_OF_DAY)
                / (SECONDS_PER_DAY * NANOSECONDS_PER_SECOND);
    }

    /**
     * Returns the observation time as a timestamp
     * 
     * @return Timestamp the observation time as a timestamp
     */
    public Timestamp asTimestamp()
    {
        return Timestamp.from(this.time.toInstant());
    }

    /**
     * Parses the input value as a UTC Date/Time
     * 
     * @param value
     *            string representation of the time
     * @return ZonedDateTime time using UTC timezone
     */
    private ZonedDateTime parseAsUTCZonedDateTime(String value)
    {
        return ZonedDateTime.of(LocalDateTime.parse(value), ZoneId.of("UTC"));
    }

    /**
     * Converts a DateTime to a Modified Julian Date (a double).
     * 
     * @param dateTime
     *            a DateTime
     * @return the Modified Julian Date
     */
    public static double dateTimeToModifiedJulianDate(DateTime dateTime)
    {
        String dateTimeString = dateTime.toDateTime(DateTimeZone.UTC).toString();
        return new ObservationTime(dateTimeString.replaceFirst("Z$", "")).asModifiedJulianDate();
    }

}
