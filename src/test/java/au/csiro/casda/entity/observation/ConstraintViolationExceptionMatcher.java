package au.csiro.casda.entity.observation;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

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
 * A Matcher that can be used to check that an Exception is a javax.validation.ConstraintViolationException and that it
 * has specific fields in that exception.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class ConstraintViolationExceptionMatcher extends TypeSafeDiagnosingMatcher<ConstraintViolationException>
{

    /**
     * Shortcut method to create a ConstraintViolationExceptionMatcher
     * 
     * @param objectClass
     *            the expected class of the object when matching a ConstraintViolationException
     * @param propertyPath
     *            the expected propertyPath when matching a ConstraintViolationException
     * @param violationMessage
     *            the expected violation message when matching a ConstraintViolationException
     * @return a ConstraintViolationExceptionMatcher
     */
    public static ConstraintViolationExceptionMatcher constraintViolation(Class<?> objectClass, String propertyPath,
            String violationMessage)
    {
        return new ConstraintViolationExceptionMatcher(objectClass, propertyPath, violationMessage);
    }

    private Class<?> objectClass;
    private String propertyPath;
    private String violationMessage;

    /**
     * Constructor
     * 
     * @param objectClass
     *            the expected class of the object when matching a ConstraintViolationException
     * @param propertyPath
     *            the expected propertyPath when matching a ConstraintViolationException
     * @param violationMessage
     *            the expected violation message when matching a ConstraintViolationException
     */
    public ConstraintViolationExceptionMatcher(Class<?> objectClass, String propertyPath, String violationMessage)
    {
        super();
        this.objectClass = objectClass;
        this.propertyPath = propertyPath;
        this.violationMessage = violationMessage;
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description)
    {
        description.appendText("a ConstraintViolationException with one violation for ").appendValue(this.propertyPath)
                .appendText(" ").appendValue(this.violationMessage);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(ConstraintViolationException item, Description mismatchDescription)
    {
        if (item.getConstraintViolations().size() != 1)
        {
            return false;
        }
        ConstraintViolation<?> violation = item.getConstraintViolations().iterator().next();
        return violation.getRootBeanClass() == this.objectClass
                && this.propertyPath.equals(violation.getPropertyPath().toString())
                && this.violationMessage.equals(violation.getMessage());
    }
}
