package au.csiro.casda.datadeposit.copy;

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
 * Signals that the checksums of the deposit and archive versions of a depositable artefact do not match.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class ChecksumVerificationFailedException extends ChecksumVerificationException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ChecksumVerificationFailedException for the given artefact's details
     * 
     * @param sbid
     *            the sbid of the Observation owning the artefact that failed the checksum verification
     * @param filename
     *            the filename of the artefact that failed the checksum verification
     * @param depositChecksum
     *            the checksum value of the deposited artefact
     * @param archiveChecksum
     *            the checksum value of the archived artefact
     */
    ChecksumVerificationFailedException(Integer sbid, String filename, String depositChecksum, String archiveChecksum)
    {
        super(String.format("Checksum verification failed for artefact with filename '%s' for observation %d. "
                + "Deposit checksum: %s. Archive checksum: %s", filename, sbid, depositChecksum, archiveChecksum));
    }
}
