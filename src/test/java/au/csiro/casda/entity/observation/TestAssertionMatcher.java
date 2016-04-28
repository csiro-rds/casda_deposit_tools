package au.csiro.casda.entity.observation;

import java.util.function.Consumer;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

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
 * A Matcher that accepts a Consumer (lambda) that can be used to check additional test conditions.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class TestAssertionMatcher extends BaseMatcher<Object>
{
    public static TestAssertionMatcher assertions(Consumer<Object> assertions)
    {
        return new TestAssertionMatcher(assertions);
    }

    private Consumer<Object> assertions;

    public TestAssertionMatcher(Consumer<Object> assertions)
    {
        this.assertions = assertions;
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(Object item)
    {
        /*
         * Any exceptions thrown within the assertions Consumer, either explicitly or through methods like Assert.*,
         * will propagate out of this method. For Assert.* methods this is exactly what we want as it will end up
         * displaying as an actual assertion failure rather than a failure of this matcher.
         */
        assertions.accept(item);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description)
    {
        /*
         * Fairly meaningless description as a) it's hard to get one out of a lambda, and b) because when used with
         * Assert.* methods in the lambda the message will not actually get shown.
         */
        description.appendText(assertions.toString());
    }
}
