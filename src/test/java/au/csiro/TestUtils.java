package au.csiro;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.SystemUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

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
 * Common test utility methods
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public final class TestUtils
{
    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Gets a specific resource that is in the classpath (such as inside the jar file).
     * 
     * @param resName
     *            the name of the resource to look for
     * @return the InputStream for the resource
     * @throws IOException
     *             if there was an error while locating the resource
     */
    public static InputStream getResource(String resName) throws IOException
    {
        logger.debug("Getting resource from Classpath: " + System.getProperty("java.class.path"));
        Resource resource = new ClassPathResource(resName);
        InputStream inputStream = resource.getInputStream();
        return inputStream;
    }

    /**
     * Gets a specific resource that is in the classpath (such as inside the jar file). This version of the method
     * returns a File.
     * 
     * @param resName
     *            the name of the resource to look for
     * @return the InputStream for the resource
     * @throws IOException
     *             if there was an error while locating the resource
     */
    public static File getResourceAsFile(String resName) throws FileNotFoundException, IOException
    {
        logger.debug("Getting resource from Classpath: " + System.getProperty("java.class.path"));
        Resource resource = new ClassPathResource(resName);
        File file = resource.getFile();
        return file;
    }

    /**
     * Executes a simple SQL Query against the database underlying the given EntityManager
     * 
     * @param query
     *            a SQL query String
     */
    public static List<?> executeQuery(EntityManager entityManager, String query)
    {
        Session session = (Session) entityManager.getDelegate();
        return session.createSQLQuery(query).list();
    }

    /**
     * @param output
     *            the output of the external echo command
     * @return an EL string suitable for parsing by a SimpleToolJobProcessBuilder to create an ProcessJob that will
     *         execute an external echo command
     */
    public static String getCommandAndArgsElStringForEchoOutput(String output)
    {
        return getCommandAndArgsElStringForEchoOutput(output, false);
    }

    /**
     * @param output
     *            the output of the external echo command
     * @param fail
     *            whether the external command should fail
     * @return an EL string suitable for parsing by a SimpleToolJobProcessBuilder to create an ProcessJob that will
     *         execute an external echo command
     */
    public static String getCommandAndArgsElStringForEchoOutput(String output, boolean fail)
    {
        if (SystemUtils.IS_OS_WINDOWS)
        {
            return "{\"cmd\",\"/c\",\"\"\"echo " + output + "&& exit " + (fail ? 1 : 0) + "\"\"\"}";
        }
        else
        {
            return "{\"bash\",\"-c\",\"echo '" + output + "' && exit " + (fail ? 1 : 0) + "\"}";
        }
    }

    /**
     * @param filename
     *            the filename whose contents will be output
     * @return an EL string suitable for parsing by a SimpleToolJobProcessBuilder to create an ProcessJob that will
     *         execute an external 'cat' command
     */
    public static String getCommandAndArgsElStringForFileContentsOutput(String filename)
    {
        return getCommandAndArgsElStringForFileContentsOutput(filename, false);
    }

    /**
     * @param filename
     *            the filename whose contents will be output
     * @param fail
     *            whether the external command should fail
     * @return an EL string suitable for parsing by a SimpleToolJobProcessBuilder to create an ProcessJob that will
     *         execute an external 'cat' command
     */
    public static String getCommandAndArgsElStringForFileContentsOutput(String filename, boolean fail)
    {
        if (SystemUtils.IS_OS_WINDOWS)
        {
            return "{\"cmd\",\"/c\",\"\"\"type " + filename.replace("/", "\\\\") + "&& exit " + (fail ? 1 : 0)
                    + "\"\"\"}";
        }
        else
        {
            return "{\"bash\",\"-c\",\"cat '" + filename + "' && exit " + (fail ? 1 : 0) + "\"}";
        }
    }

}
