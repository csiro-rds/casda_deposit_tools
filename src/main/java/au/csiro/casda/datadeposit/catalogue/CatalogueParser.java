package au.csiro.casda.datadeposit.catalogue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import au.csiro.casda.entity.observation.Catalogue;

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
 * Abstract type Catalogue datafile parsers.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public interface CatalogueParser
{
    /**
     * Enumeration that defines the different 'modes' (eg: validate-only, normal, etc.) that a CatalogueParser may run
     * in.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static enum Mode
    {
        /**
         * Indicates the MODE is VALIDATE_ONLY
         */
        VALIDATE_ONLY,

        /**
         * Indicates the MODE is NORMAL
         */
        NORMAL
    }

    /**
     * Exception class used to notify clients of any database-related issues encountered when parsing a datafile.
     * <p>
     * Copyright 2014, CSIRO Australia All rights reserved.
     */
    public static class DatabaseException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructs a DatabaseException with the given message.
         * 
         * @param message
         *            a String
         */
        public DatabaseException(String message)
        {
            super(message);
        }

        /**
         * Constructs a DatabaseException with the given cause.
         * 
         * @param cause
         *            another Throwable
         */
        public DatabaseException(Throwable cause)
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

        private List<String> causeMessages = new ArrayList<>();

        /**
         * Constructs a MalformedFileException with the cause message.
         * 
         * @param causeMessage
         *            a String
         */
        public MalformedFileException(String causeMessage)
        {
            super();
            this.causeMessages.add(causeMessage);
        }

        /**
         * Constructs a MalformedFileException with the given cause.
         * 
         * @param cause
         *            another Throwable
         */
        public MalformedFileException(Throwable cause)
        {
            super();
            this.causeMessages.add("Error in TABLE: " + getMessageForRootCause(cause));
        }

        /**
         * Constructs a MalformedFileException with the given list of causes
         * 
         * @param exceptions
         *            a List of exceptions
         */
        public MalformedFileException(List<Throwable> exceptions)
        {
            super();
            this.causeMessages.addAll(exceptions.stream().map((t) -> getMessageForRootCause(t))
                    .collect(Collectors.toList()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMessage()
        {
            return StringUtils.join(this.causeMessages, System.lineSeparator());
        }

        public List<String> getCauseMessages()
        {
            return this.causeMessages;
        }

        private String getMessageForRootCause(Throwable cause)
        {
            Throwable ex = cause;
            String message = ex.getMessage();
            while (message == null && ex.getCause() != null)
            {
                ex = ex.getCause();
                message = ex.getMessage();
            }
            return message;
        }
    }

    /**
     * Used by implementors of parseFile to signal results of the validation-only mode. Use of an exception rather than
     * returning a void or simple value allows implementors to make use of @Transactional blocks.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static class ValidationModeSignal extends Exception
    {
        private static final long serialVersionUID = 1L;

        private List<String> failureMessages;

        /**
         * Creates a ValidationModeSignal. Initially it will have no failure messages, indicating a successful
         * validation.
         */
        public ValidationModeSignal()
        {
            super();
            this.failureMessages = new ArrayList<>();
        }

        /**
         * Creates a ValidationModeSignal with the given failureMessages. If failureMessages is empty then the
         * validation was successful, otherwise it failed.
         * 
         * @param failureMessages
         *            a list of failure messages
         */
        public ValidationModeSignal(String... failureMessages)
        {
            this();
            for (String failureMessage : failureMessages)
            {
                this.failureMessages.add(failureMessage);
            }
        }

        /**
         * Adds a failure message to this signal.
         * 
         * @param message
         *            a String
         */
        public void addFailureMessage(String message)
        {
            this.failureMessages.add(message);
        }

        /**
         * @return a list of validation failure messages
         */
        public List<String> getValidationFailureMessages()
        {
            return this.failureMessages;
        }

    }

    /**
     * Parses the given datafile into a Catalogue and persists the object to the database.
     * 
     * @param sbid
     *            the scheduling block id of the observation for which to import the catalogue data
     * @param catalogueFilename
     *            the name of the catalogue filename
     * @param catalogueDatafile
     *            the file to parse
     * @param mode
     *            the CatalogueParser.MODE to use when parsing
     * @param dcCommonId
     *            The base collection id shared by all versions of this data collection.
     * @return the Catalogue
     * @throws FileNotFoundException
     *             if the specified file could not be found
     * @throws CatalogueParser.MalformedFileException
     *             if the specified file does not match the expected file format
     * @throws CatalogueParser.DatabaseException
     *             if there were any issues persisting the catalogue data to the database
     * @throws CatalogueParser.ValidationModeSignal
     *             if the mode was VALIDATE_ONLY - NOTE: this exception does NOT signal an error
     */
    public Catalogue parseFile(Integer sbid, String catalogueFilename, String catalogueDatafile, Mode mode,
            Integer dcCommonId) throws FileNotFoundException, CatalogueParser.MalformedFileException,
            CatalogueParser.DatabaseException, ValidationModeSignal;
}
