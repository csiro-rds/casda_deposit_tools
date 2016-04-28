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
 * Represents a failure in copying an artefact to the NGAS staging area.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public final class StagingException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a StagingException for the given artefact details.
     * 
     * @param sbid
     *            the sbid of the Observation owning the artefact that could not be staged
     * @param filename
     *            the filename of the artefact that could not be staged
     * @param ngasStagingPath
     *            the path on the NGAS server to which the artefact was attempted to be staged
     * @param fileId
     *            the NGAS fileId of the artefact
     * @param failureCause
     *            the cause of the failure
     */
    StagingException(Integer sbid, String filename, String ngasStagingPath, String fileId, String failureCause)
    {
        super(String.format(
                "Could not stage artefact with filename '%s' for observation %d to NGAS staging path %s.%s", filename,
                sbid, ngasStagingPath, StringUtils.isBlank(failureCause) ? "" : (" Cause: " + failureCause)));
    }
}
