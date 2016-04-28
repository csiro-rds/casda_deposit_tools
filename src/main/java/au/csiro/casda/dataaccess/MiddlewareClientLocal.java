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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.LogEvent;


/**
 * A version of the middleware client to run data access application locally without using file archive (ngas). It is
 * only a test implementation that creates a file with the fileId as it's contents.
 * 
 * Copyright 2015, CSIRO Australia All rights reserved.
 * 
 */
@Profile("local")
@Component
public class MiddlewareClientLocal implements MiddlewareClient
{
    private static final Logger logger = LoggerFactory.getLogger(MiddlewareClientLocal.class);

    /** Length of number field in file id, if any */
    private static final int NUM_LEN = 4 ;

    /** Mills in a second */
    private static final int MILLS = 1000 ;

    @Override
    public File retrieve(String fileId, RetrieveHandler callback) throws MiddlewareClientException
    {
        // if fileId starts with a digit, this is a number of seconds to sleep
        try
        {
            int seconds = Integer.parseInt(fileId.substring(0, NUM_LEN).trim());
            Thread.sleep(MILLS * seconds);
        }
        catch (NumberFormatException | InterruptedException e) // do nothing
        {
            @SuppressWarnings("unused")
            int i = 0  ; // stop checkstyle complains
        }
        if (fileId.contains("fail")) // If this file must fail
        {
            logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(LogEvent.UNKNOWN_EVENT).add(
                    "Failed as requested").toString());
            throw new MiddlewareClientException("File download failed as requested");
        }
        if (fileId.contains("crash")) // If this file must crash the JVM
        {
            System.exit(1);
        }

        return callback.onRetrieve(IOUtils.toInputStream(fileId, Charsets.UTF_8), fileId);
    }

}
