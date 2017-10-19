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
 * Enumeration of all possible FITS object types.
 * <p>
 * Copyright 2016, CSIRO Australia. All rights reserved.
 */
public enum FitsObjectType
{
    /**
     * The image cube object type - a 2D, 3D or 4D FITS image with the first two axes representing RA and Dec.
     */
    IMAGE_CUBE,

    /**
     * The moment map object type - a 2 dimensional array of wavelength related data.
     */
    MOMENT_MAP,

    /**
     * The spectrum object type - a 1 dimensional array of wavelength related data.
     */
    SPECTRUM,

    /**
     * The cubelet object type - a smaller cutout of an image cube.
     */
    CUBELET;

    /**
     * @return a human-readable description of this FITS object type
     */
    public String getDescription()
    {
        return this.toString().toLowerCase().replace("_", " ");
    }
}
