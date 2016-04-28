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


import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Common utilities for Spring.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class SpringUtils
{
    /**
     * Convenience method that takes an EL string and converts it to an array; uses elStringToList.
     * 
     * @param listString
     *            the EL String
     * @return an array of Strings
     */
    public static String[] elStringToArray(String listString)
    {
        return elStringToList(listString).toArray(new String[0]);
    }

    /**
     * Converts a Spring EL string into a List. The list must be formatted as {&quot;1&quot;, &quot;2&quot;, etc}
     * 
     * @param listString
     *            the EL String
     * @return a List of Strings
     */
    @SuppressWarnings("rawtypes")
    public static List<String> elStringToList(String listString)
    {
        ExpressionParser parser = new SpelExpressionParser();
        Object parsedValue = parser.parseExpression(listString).getValue();
        List<String> resultList = new ArrayList<>();
        if (parsedValue instanceof List)
        {
            for (Object object : (List) parsedValue)
            {
                resultList.add(object.toString());
            }
        }
        else if (parsedValue instanceof String)
        {
            resultList.add((String) parsedValue);
        }
        else
        {
            throw new IllegalArgumentException(String.format(
                    "Could not parse '%s' into either a single String or a list of Strings", listString));
        }
        return resultList;
    }
}
