package au.csiro.casda.datadeposit.fits;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import org.springframework.stereotype.Component;

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
 * Class for reading ASKAP FITS files.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Component
public class FitsFileParser
{

    /**
     * The block index of the header within the FITS file.
     */
    private static final int HEADER_TABLE_INDEX = 0;

    /** Constant for header values which are double but must default to null instead of zero */
    private static final List<String> DOUBLE_EXCEPTIONS = new ArrayList<String>(Arrays.asList("TMIN", "TMAX"));

    /**
     * A FITS file handle.
     */
    private Fits fitsFile;

    /**
     * no-arg Constructor for bean initialisation
     */
    public FitsFileParser()
    {
    }

    /**
     * Retrieve the AKAP Image Cube header values of interest from the FITS file.
     * 
     * @return The map of header values.
     * @throws IOException
     *             If the file cannot be opened
     * @throws FitsException
     *             If the file cannot be processed as a FITS file
     */
    public Map<AskapFitsKey, Object> getHeaderValues() throws FitsException, IOException
    {
        Map<AskapFitsKey, Object> valueMap = new HashMap<>();
        BasicHDU basicHDU = fitsFile.getHDU(HEADER_TABLE_INDEX);
        Header basicHeader = basicHDU.getHeader();

        for (AskapFitsKey field : AskapFitsKey.values())
        {
            FitsHeaderFieldType cls = field.getFieldType();
            switch (cls)
            {
            case DOUBLE:
                // because unlike other Doubles TMIN & TMAX should default to null instead of 0.0
                Double value = basicHeader.getDoubleValue(field.getKeyword());

                if (DOUBLE_EXCEPTIONS.contains(field.getKeyword()) && !basicHeader.containsKey(field.getKeyword()))
                {
                    value = null;
                }
                valueMap.put(field, value);
                break;
            case INTEGER:
                valueMap.put(field, basicHeader.getIntValue(field.getKeyword()));
                break;
            case STRING:
                valueMap.put(field, basicHeader.getStringValue(field.getKeyword()));
                break;
            default:
                break;

            }
        }
        return valueMap;
    }

    /**
     * Sets the dataFile to be parsed.
     * 
     * @param dataFile
     *            a File
     * @throws FitsException
     *             if the file is not a Fits file
     * @throws IOException
     *             if the file could not be found or read
     */
    public void setFitsFile(File dataFile) throws FitsException, IOException
    {
        if (!dataFile.exists())
        {
            throw new FileNotFoundException("File passed to FitsFileParser does not exist!");
        }
        this.fitsFile = new Fits(dataFile);
    }
}
