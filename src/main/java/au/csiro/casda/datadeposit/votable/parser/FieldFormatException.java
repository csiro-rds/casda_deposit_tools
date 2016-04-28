package au.csiro.casda.datadeposit.votable.parser;

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
 * Exception thrown when a field's format is inconsistent.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class FieldFormatException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a FieldFormatException with the given message
     * 
     * @param message
     *            a String
     */
    public FieldFormatException(String message)
    {
        super(message);
    }
}
