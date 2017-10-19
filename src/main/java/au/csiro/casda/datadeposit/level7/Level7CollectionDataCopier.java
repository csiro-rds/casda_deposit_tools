package au.csiro.casda.datadeposit.level7;

import java.io.File;

import org.apache.commons.io.FileUtils;

import au.csiro.casda.datadeposit.copy.CopyDataException;
import au.csiro.casda.datadeposit.exception.CreateChecksumException;
import au.csiro.casda.entity.observation.Level7Collection;

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
 * Defines an interface to enable copying of level 7 data files to a staging area and registration of those fies.
 * <p>
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
public interface Level7CollectionDataCopier
{

    /**
     * Exception class used to notify clients of any datafile-related issues encountered when parsing a datafile.
     * <p>
     * Copyright 2014, CSIRO Australia All rights reserved.
     */
    public static class MalformedFileException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructs a MalformedFileException with the given message.
         * 
         * @param message
         *            a String
         */
        public MalformedFileException(String message)
        {
            super(message);
        }

        /**
         * Constructs a MalformedFileException with the given cause.
         * 
         * @param cause
         *            another Throwable
         */
        public MalformedFileException(Throwable cause)
        {
            super(cause);
        }

        /**
         * Constructs a MalformedFileException with the given message and cause.
         * 
         * @param message
         *            a String
         * @param cause
         *            another Throwable
         */
        public MalformedFileException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Copies the level 7 data and registers each file of relevance.
     * 
     * @param dataCollectionId
     *            the id in DAP for the level 7 collection.
     * @param collectionFolder
     *            the base folder in which the collection's data will be staged
     * @return the Level7Collection
     * @throws CopyDataException
     *            if the data could not be copied. 
     * @throws CreateChecksumException
     *            if a checksum could not be created for a file. 
     */
    public Level7Collection copyData(long dataCollectionId, String collectionFolder)
            throws CopyDataException, CreateChecksumException;

    /**
     * Calculates the size of a file, in kilobytes.
     * 
     * @param file
     *            the file
     * @return long the file size, in kilobytes
     */
    public static long calculateFileSizeKb(File file)
    {
        return (long) Math.ceil(((double) file.length()) / FileUtils.ONE_KB);
    }
}
