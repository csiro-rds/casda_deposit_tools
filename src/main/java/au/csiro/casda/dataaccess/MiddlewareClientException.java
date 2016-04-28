package au.csiro.casda.dataaccess;

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
 * Generic exception thrown by the MiddlewareClient
 * 
 * 
 */
public class MiddlewareClientException extends Exception
{
    private static final long serialVersionUID = -2035904445507849905L;

    /**
     * Default constructor
     */
    public MiddlewareClientException()
    {
        super();
    }

    /**
     * Constructs a MiddlewareClientException with the given message and cause
     * 
     * @param message
     *            a String
     * @param cause
     *            cause
     */
    public MiddlewareClientException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructs a MiddlewareClientException with the given message
     * 
     * @param message
     *            a String
     */
    public MiddlewareClientException(String message)
    {
        super(message);
    }

    /**
     * Constructs a MiddlewareClientException with the given cause
     * 
     * @param cause
     *            cause
     */
    public MiddlewareClientException(Throwable cause)
    {
        super(cause);
    }

}
