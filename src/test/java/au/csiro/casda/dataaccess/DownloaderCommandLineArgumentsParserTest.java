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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.beust.jcommander.ParameterException;

/**
 * Tests the ngas downloader argument parser.
 * 
 * Copyright 2015, CSIRO Australia All rights reserved.
 * 
 */
public class DownloaderCommandLineArgumentsParserTest
{

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Test
    public void testCommandLineArgs()
    {
        DownloaderCommandLineArgumentsParser argsParser = new DownloaderCommandLineArgumentsParser();
        String[] args = {"-fileId", "file.id.value", "-name", "dest.file.name"};
        argsParser.parse(args);
        assertEquals("file.id.value", argsParser.getArgs().getFileId());
        assertEquals("dest.file.name", argsParser.getArgs().getName());
        assertFalse(argsParser.getArgs().downloadChecksumFile());
    }
    
    @Test
    public void testCommandLineArgsChecksum()
    {
        DownloaderCommandLineArgumentsParser argsParser = new DownloaderCommandLineArgumentsParser();
        String[] args = {"-fileId", "file.id.value", "-name", "dest.file.name", "-checksum"};
        argsParser.parse(args);
        assertEquals("file.id.value", argsParser.getArgs().getFileId());
        assertEquals("dest.file.name", argsParser.getArgs().getName());
        assertTrue(argsParser.getArgs().downloadChecksumFile());
    }
    
    @Test
    public void testInvalidFirstCommandLineArgs()
    {
        exception.expect(ParameterException.class);
        exception.expectMessage("Unknown option: -file");
        
        DownloaderCommandLineArgumentsParser argsParser = new DownloaderCommandLineArgumentsParser();
        String[] args = {"-file", "file.id.value", "-name", "dest.file.name"};
        argsParser.parse(args);
    }
    
    @Test
    public void testInvalidSecondCommandLineArgs()
    {
        exception.expect(ParameterException.class);
        exception.expectMessage("Unknown option: -invalid");
        
        DownloaderCommandLineArgumentsParser argsParser = new DownloaderCommandLineArgumentsParser();
        String[] args = {"-fileId", "file.id.value", "-invalid", "dest.file.name"};
        argsParser.parse(args);
    }
    
    @Test
    public void testInvalidExtraCommandLineArgs()
    {
        exception.expect(ParameterException.class);
        exception.expectMessage("Unknown option: -invalid");
        
        DownloaderCommandLineArgumentsParser argsParser = new DownloaderCommandLineArgumentsParser();
        String[] args = {"-fileId", "file.id.value", "-name", "dest.file.name", "-invalid"};
        argsParser.parse(args);
    }
    
    @Test
    public void testMultipleFiles()
    {
        DownloaderCommandLineArgumentsParser argsParser = new DownloaderCommandLineArgumentsParser();
        String[] args = {"-fileId", "file.id.value", "-name", "dest.file.name"};
        argsParser.parse(args);
        assertEquals("file.id.value", argsParser.getArgs().getFileId());
        assertEquals("dest.file.name", argsParser.getArgs().getName());
    }

}
