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
 * This enum maps Stokes values to equivalent values from a FITS header.
 * 
 * see http://www.astron.nl/casacore/trunk/casacore/doc/html/classcasa_1_1Stokes.html
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public enum StokesPolarisationMapping
{
    /** Stokes type 'I' which is '1' in the FITS header */
    I("I", 1),

    /** Stokes type 'Q' which is '2' in the FITS header */
    Q("Q", 2),

    /** Stokes type 'U' which is '3' in the FITS header */
    U("U", 3),

    /** Stokes type 'V' which is '4' in the FITS header */
    V("V", 4),

    /** Undefined Stokes type which will default to 0 */
    UNDEFINED("UNDEFINED", 0);

    /** The stokes value of the enum */
    private final String stokesValue;

    /** The value that we get from the FITS header */
    private final int fitsValue;

    /**
     * Create a new AskapFitsKey
     * 
     * @param stokesValue
     *            The stokes value of the enum
     * @param fitsValue
     *            The value that we get from the FITS header
     * 
     */
    StokesPolarisationMapping(String stokesValue, int fitsValue)
    {
        this.stokesValue = stokesValue;
        this.fitsValue = fitsValue;
    }

    /**
     * @return the stokesValue
     */
    public String getStokesValue()
    {
        return stokesValue;
    }

    /**
     * @return the fitsValue
     */
    public int getFitsValue()
    {
        return fitsValue;
    }

    /**
     * Gets an instance of this enum using the int value from the FITS header. If there is no corresponding value then
     * returns UNDEFINED.
     * 
     * @param fitsValue
     *            the int value
     * @return StokesPolarisationMapping corresponding to the value passed in.
     */
    public static StokesPolarisationMapping getFromFitsValue(int fitsValue)
    {
        for (StokesPolarisationMapping enumInstance : StokesPolarisationMapping.values())
        {
            if (enumInstance.fitsValue == fitsValue)
            {
                return enumInstance;
            }
        }
        return UNDEFINED;
    }

}
