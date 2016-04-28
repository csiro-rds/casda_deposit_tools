package au.csiro.casda.dataaccess;

import java.io.File;

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
 * Defines the capabilities to be provided by the middleware client.
 * 
 * 
 */
public interface MiddlewareClient
{

    /**
     * Retrieve a given file.
     * 
     * @param fileId
     *            The file id of the file
     * @param callback
     *            A callback to execute when the file has been retrieved
     * @return a reference to the created file
     * @throws MiddlewareClientException
     *             If the retrieve command fails
     */
    public File retrieve(String fileId, RetrieveHandler callback) throws MiddlewareClientException;

}
