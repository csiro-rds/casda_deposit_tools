package au.csiro.casda.datadeposit.copy;

import java.nio.file.Path;

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
 * Signals that the deposit checksum could not be found for an artefacT.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class ChecksumMissingException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ChecksumMissingException for the given artefact details.
     * 
     * @param sbid
     *            the sbid of the Observation owning the artefact missing the checksum file
     * @param filename
     *            the filename of the artefact missing the checksum file
     * @param depositChecksumFilePath
     *            the expected location of the checksum file
     */
    ChecksumMissingException(Integer sbid, String filename, Path depositChecksumFilePath)
    {
        super(String.format(
                "Could not stage artefact with filename '%s' for observation %d because checksum file %s is "
                        + "missing", filename, sbid, depositChecksumFilePath.toString()));
    }
}
