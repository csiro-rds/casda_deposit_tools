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
 * Represents a failure in executing an NGAS REGISTER request.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public final class RegisterException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a RegisterException for the given artefact details
     * 
     * @param sbid
     *            the sbid of the Observation owning the artefact that could not be registered
     * @param filename
     *            the filename of the artefact that could not be registered
     * @param ngasStagingPath
     *            the path on the NGAS server where the artefact had been 'staged'
     * @param fileId
     *            the NGAS fileId of the artefact
     * @param failureCause
     *            the cause of the failure
     */
    RegisterException(Integer sbid, String filename, String ngasStagingPath, String fileId, String failureCause)
    {
        super(String.format("Could not register artefact with filename '%s' for observation %d with NGAS at staging "
                + "path %s/%s.%s", filename, sbid, ngasStagingPath, fileId, StringUtils.isBlank(failureCause) ? ""
                : (" Cause: " + failureCause)));
    }

    /**
     * Constructs a RegisterException for the given artefact details
     * 
     * @param sbid
     *            the sbid of the Observation owning the artefact that could not be registered
     * @param filename
     *            the filename of the artefact that could not be registered
     * @param ngasStagingPath
     *            the path on the NGAS server where the artefact had been 'staged'
     * @param fileId
     *            the NGAS fileId of the artefact
     * @param t
     *            the cause of the failure
     */
    RegisterException(Integer sbid, String filename, String ngasStagingPath, String fileId, Throwable t)
    {
        super(String.format(
                "Could not register artefact with filename '%s' for observation %d with NGAS at staging path %s/%s due"
                        + " to unexpected exception", filename, sbid, ngasStagingPath, fileId), t);
    }
}
