package au.csiro;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

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
 * Wrapper class that converts a Matcher<CharSequence> to a Matcher<String>.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public final class CharSequenceMatcherToStringMatcher extends BaseMatcher<String>
{
    private Matcher<CharSequence> wrappedMatcher;

    /**
     * Factory method to create a Matcher<String> from a Matcher<CharSequence>
     * 
     * @param matcher
     *            a Matcher<CharSequence>
     * @return a Matcher<String>
     */
    public static Matcher<String> toStringMatcher(Matcher<CharSequence> matcher)
    {
        return new CharSequenceMatcherToStringMatcher(matcher);
    }

    private CharSequenceMatcherToStringMatcher(Matcher<CharSequence> wrappedMatcher)
    {
        this.wrappedMatcher = wrappedMatcher;
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(Object argument)
    {
        return this.wrappedMatcher.matches(argument);
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description)
    {
        this.wrappedMatcher.describeTo(description);
    }

    /** {@inheritDoc} */
    @Override
    public void describeMismatch(Object item, Description mismatchDescription)
    {
        this.wrappedMatcher.describeMismatch(item, mismatchDescription);
    }

}
