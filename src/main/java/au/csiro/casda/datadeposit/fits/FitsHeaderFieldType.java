package au.csiro.casda.datadeposit.fits;

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
 * List of the possible content types for a FITS header field.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public enum FitsHeaderFieldType
{
    /** Whole number */
    INTEGER,

    /** Real number */
    DOUBLE,

    /** Text */
    STRING
}
