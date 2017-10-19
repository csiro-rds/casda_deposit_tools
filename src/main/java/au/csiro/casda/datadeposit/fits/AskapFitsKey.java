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
 * The available keys for FITS header fields in ASKAP image cube files.
 * 
 * For more details on each of the headers see http://heasarc.gsfc.nasa.gov/docs/fcg/standard_dict.html
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public enum AskapFitsKey
{
    /** Name of observed object */
    OBJECT("OBJECT", FitsHeaderFieldType.STRING),

    /** Beam type */
    BTYPE("BTYPE", FitsHeaderFieldType.STRING),

    /** Beam unit */
    BUNIT("BUNIT", FitsHeaderFieldType.STRING),

    /** Bean width - major axis of the ellipse */
    BMAJ("BMAJ", FitsHeaderFieldType.DOUBLE),

    /** Beam width - minor axis of the ellipse */
    BMIN("BMIN", FitsHeaderFieldType.DOUBLE),

    /** Start time of the observation in MJD */
    TMIN("TMIN", FitsHeaderFieldType.DOUBLE),

    /** Stop time of the observation in MJD */
    TMAX("TMAX", FitsHeaderFieldType.DOUBLE),

    /** The duration of the observation */
    INTIME("INTIME", FitsHeaderFieldType.DOUBLE),

    /** name of telescope used to acquire the data */
    TELESCOP("TELESCOP", FitsHeaderFieldType.STRING),

    /** name of the instrument used to acquire the data */
    INSTRUME("INSTRUME", FitsHeaderFieldType.STRING),

    /** name of the project involved with acquiring the data */
    PROJECT("PROJECT", FitsHeaderFieldType.STRING),
	
    /** the rest frequency of a fits object */
    REST_FREQUENCY("RESTFRQ", FitsHeaderFieldType.DOUBLE);
    
    /** The key of the enum */
    private final String keyword;

    /** The type of the enum */
    private final FitsHeaderFieldType fieldType;

    /**
     * Create a new AskapFitsKey
     * 
     * @param keyword
     *            FITS keyword identifier
     * @param fieldType
     *            the data type of the field
     * 
     */
    AskapFitsKey(String keyword, FitsHeaderFieldType fieldType)
    {
        this.keyword = keyword;
        this.fieldType = fieldType;
    }

    /**
     * Gets the key of this enum
     * 
     * @return the keyword
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Gets the datatype of the field
     * 
     * @return the fieldType
     */
    public FitsHeaderFieldType getFieldType()
    {
        return fieldType;
    }

}
