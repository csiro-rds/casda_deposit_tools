package au.csiro.casda.datadeposit.observation.assembler;

import au.csiro.casda.datadeposit.observation.ObservationParser.MalformedFileException;
import au.csiro.casda.datadeposit.observation.parser.XmlObservation;
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
 * Service used to create an Observation from a parsed observation.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public interface ObservationAssembler
{
    /**
     * Exception thrown if an Observation already exists when an ObservationAssembler is asked to create on.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static class ObservationAlreadyExistsException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Cosntructs an ObservationAlreadyExistsException for the given sbid
         * 
         * @param sbid
         *            an SBID (String)
         */
        public ObservationAlreadyExistsException(String sbid)
        {
            super(String.format("Observation with sbid '%s' already exists.", sbid));
        }
    }

    /**
     * Creates an Observation from a parsed observation.
     * 
     * @param parsedObservation
     *            the JAXB root element for the parsed observation.
     * @param fileIdMaxSize
     *            allowed max file Id.
     * @return a new Observation
     * @throws ObservationAlreadyExistsException
     *             if an Observation already exists for the parsed observation's SBID.
     * @throws MalformedFileException 
     * 			   Thrown when parse fails due to errors in xml file
     */
    public Observation createObservationFromParsedObservation(XmlObservation parsedObservation, int fileIdMaxSize)
            throws ObservationAlreadyExistsException, MalformedFileException;
}
