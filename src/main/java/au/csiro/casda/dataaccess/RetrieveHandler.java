package au.csiro.casda.dataaccess;

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


import java.io.File;
import java.io.InputStream;

import javax.annotation.Nonnull;

/**
 * Response for a Middleware Client Retrieve request
 * 
 */
public interface RetrieveHandler
{
    /**
     * Called when the retrieved file is ready to be consumed.
     * 
     * @param inputStream
     *            The input stream for the file. The implementation of RetrieveHandler is not expected to close the
     *            input stream as the Middleware Client should handle it.
     * @param filename
     *            The decoded file name as given by the server or "" if not supplied
     * @return a reference to the created file
     * @throws MiddlewareClientException
     *             if there is an problem using Ngas
     */
    public File onRetrieve(@Nonnull InputStream inputStream, @Nonnull String filename) throws MiddlewareClientException;
}
