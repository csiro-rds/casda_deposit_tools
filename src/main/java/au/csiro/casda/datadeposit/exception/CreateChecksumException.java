package au.csiro.casda.datadeposit.exception;



/**
 * A general exception for unrecoverable exceptions creating a checksum file
 * <p>
 * Copyright 2017, CSIRO Australia All rights reserved.
 */
public class CreateChecksumException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Create a new CreateChecksumException with a message and a cause.
     * 
     * @param message
     *            The description of the cause of the exception.
     */
    public CreateChecksumException(String message)
    {
        super(message);
    }

    /**
     * Create a new CreateChecksumException with a cause.
     * 
     * @param cause
     *            The Exception or Error that caused the problem.
     */
    public CreateChecksumException(Throwable cause)
    {
        super(cause);
    }
}