package au.csiro.casda.datadeposit.copy;

import org.apache.commons.lang3.StringUtils;

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
 * Signals that the checksums of the deposit and archive versions of a depositable artefact could not be checked due to
 * an unexpected condition.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class UnexpectedChecksumVerificationException extends ChecksumVerificationException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a UnexpectedChecksumVerificationException for the given artefact details
     * 
     * @param sbid
     *            the sbid of the Observation owning the artefact
     * @param filename
     *            the filename of the artefact
     * @param cause
     *            the cause of the exception
     */
    UnexpectedChecksumVerificationException(Integer sbid, String filename, String cause)
    {
        super(String.format("Checksum could not be verified for artefact with filename '%s' for observation %d.%s",
                filename, sbid, (StringUtils.isBlank(cause) ? "" : " Cause: " + cause)));
    }

    /**
     * Constructs a UnexpectedChecksumVerificationException for the given artefact details
     * 
     * @param sbid
     *            the sbid of the Observation owning the artefact
     * @param filename
     *            the filename of the artefact
     * @param t
     *            the cause of the exception
     */
    UnexpectedChecksumVerificationException(Integer sbid, String filename, Throwable t)
    {
        super(String.format("Checksum could not be verified for artefact with filename '%s' for observation %d due to "
                + "unexpected exception.", filename, sbid), t);
    }
}
