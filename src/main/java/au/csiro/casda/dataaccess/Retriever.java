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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.LogEvent;

/**
 * Handles the response from a request to retrieve from the archive
 * 
 * Copyright 2015, CSIRO Australia All rights reserved.
 * 
 */
public class Retriever implements RetrieveHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MiddlewareClientLocal.class);

    private File destination;

    /**
     * @param destination
     *            full file name of the destination file
     */
    public Retriever(File destination)
    {
        this.destination = destination;
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.casda.archive.RetrieveHandler#onRetrieve(java.io.InputStream, java.lang.String,
     * org.apache.http.entity.ContentType, long)
     */
    @Override
    public File onRetrieve(InputStream inputStream, String filename) throws MiddlewareClientException
    {
        try
        {
            filename = filename.replaceAll("\\:", "_");
            filename = filename.replaceAll("/", "_");

            File dir = destination.getParentFile() ;
            dir.mkdirs() ;
            File outFile = new File(dir, filename);

            logger.debug( "Saving retrieved file to {}", outFile.getCanonicalPath());
            try (FileOutputStream outStream = new FileOutputStream(outFile))
            {
                IOUtils.copyLarge(inputStream, outStream);
            }
            return outFile;
        }
        catch (IOException e)
        {
            logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(LogEvent.UNKNOWN_EVENT)
                    .add(e.getLocalizedMessage()).toString());
            throw new MiddlewareClientException(e);
        }

    }

}
