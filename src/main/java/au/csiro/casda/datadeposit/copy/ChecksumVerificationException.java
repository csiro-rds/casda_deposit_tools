package au.csiro.casda.datadeposit.copy;

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
 * Base class for checksum verification exceptions.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public abstract class ChecksumVerificationException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ChecksumVerificationException with the given message.
     * 
     * @param message
     *            a String
     */
    ChecksumVerificationException(String message)
    {
        super(message);
    }

    /**
     * Constructs a ChecksumVerificationException with the given message and cause.
     * 
     * @param message
     *            a String
     * @param t
     *            the Exception cause
     */
    ChecksumVerificationException(String message, Throwable t)
    {
        super(message, t);
    }

}
