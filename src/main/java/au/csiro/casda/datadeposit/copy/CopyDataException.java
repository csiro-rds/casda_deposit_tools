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
 * Represents a failure in copying a level 7 folder to the staging area.
 * <p>
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
public final class CopyDataException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a CopyDataException for the given artefact details.
     * 
     * @param dataCollectionId
     *            the DAP collection id of the level 7 collection that could not be staged
     * @param sourcePath
     *            the path to the folder being copied from
     * @param targetPath
     *            the path to the folder being copied to
     * @param failureCause
     *            the cause of the failure
     */
    public CopyDataException(long dataCollectionId, String sourcePath, String targetPath, String failureCause)
    {
        super(String.format("Could not copy level 7 data for collection %d from '%s' to '%s'.%s", dataCollectionId,
                sourcePath, targetPath, StringUtils.isBlank(failureCause) ? "" : (" Cause: " + failureCause)));
    }
}
