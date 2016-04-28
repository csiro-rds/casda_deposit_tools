package au.csiro.casda.datadeposit.observation;

import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.io.FileUtils;

import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.entity.observation.Observation;

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
 * <add description here>
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public interface ObservationParser
{
    /**
     * Exception class used to notify clients of any database-related issues encountered when parsing a datafile.
     * <p>
     * Copyright 2014, CSIRO Australia All rights reserved.
     */
    public static class RepositoryException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructs a DatabaseException with the given message.
         * 
         * @param message
         *            a String
         */
        public RepositoryException(String message)
        {
            super(message);
        }

        /**
         * Constructs a DatabaseException with the given cause.
         * 
         * @param cause
         *            another Throwable
         */
        public RepositoryException(Throwable cause)
        {
            super(cause);
        }

    }

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
     * Parses the given datafile into an Observation and persists it to the database.
     * 
     * @param sbid
     *            the scheduling block id of the observation for which to import the catalogue data
     * @param observationMetadataFile
     *            the file to parse
     * @return the Observation
     * @throws FileNotFoundException
     *             if the specified file could not be found
     * @throws MalformedFileException
     *             if the specified file does not match the expected file format
     * @throws RepositoryException
     *             if there were any issues persisting the observation data to the database
     */
    public Observation parseFile(Integer sbid, String observationMetadataFile) throws FileNotFoundException,
            MalformedFileException, RepositoryException;

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
    
    /**
     * overwrites the repsoitory with a mock one for junit testing
     * @param simpleJdbcRepository the mock jdbc repository
     */
    public void setSimpleJdbcRepository(SimpleJdbcRepository simpleJdbcRepository);
}
