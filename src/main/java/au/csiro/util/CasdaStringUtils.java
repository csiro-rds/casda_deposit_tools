package au.csiro.util;

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


import java.util.Collection;

/**
 * Common utilities strings.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class CasdaStringUtils
{
    /**
     * Joins a collection of strings using the given separator and conjunction.
     * 
     * @param strings
     *            an array of Strings to join
     * @param separator
     *            the separator to use between all elements except the last two
     * @param conjunction
     *            the separator to use between the last two elements.
     * @return a String
     */
    public static String joinStringsForDisplay(String[] strings, String separator, String conjunction)
    {
        if (strings == null)
        {
            throw new IllegalArgumentException("Expected strings != null");
        }
        switch (strings.length)
        {
        case 0:
            return "";
        case 1:
            return strings[0];
        default:
            if (separator == null)
            {
                separator = ",";
            }
            if (conjunction == null)
            {
                conjunction = "and";
            }
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < strings.length - 1; i++)
            {
                if (i > 0)
                {
                    buf.append(separator);
                    buf.append(" ");
                }
                buf.append(strings[i]);
            }
            buf.append(" ");
            buf.append(conjunction);
            buf.append(" ");
            buf.append(strings[strings.length - 1]);
            return buf.toString();
        }
    }

    /**
     * Joins a collection of strings using the given separator and conjunction.
     * 
     * @param strings
     *            a Collection of Strings to join
     * @param separator
     *            the separator to use between all elements except the last two
     * @param conjunction
     *            the separator to use between the last two elements.
     * @return a String
     */
    public static String joinStringsForDisplay(Collection<String> strings, String separator, String conjunction)
    {
        return joinStringsForDisplay(strings != null ? strings.toArray(new String[0]) : null, separator, conjunction);
    }
}
