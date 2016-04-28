package au.csiro.casda.dataaccess;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.Log4JTestAppender;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.service.NgasService;
import au.csiro.casda.datadeposit.service.NgasService.ServiceCallException;
import au.csiro.casda.datadeposit.service.NgasService.Status;
import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.LogEvent;

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
 * Tests the ngas downloader tool.
 * 
 * Copyright 2015, CSIRO Australia All rights reserved.
 */
public class NgasDownloaderTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    @Mock
    private NgasService mockNgasService;

    private NgasDownloader downloader;

    private MiddlewareClient middlewareClient;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        middlewareClient = spy(new MiddlewareClientLocal());
        downloader = spy(new NgasDownloader("propertiesVersion", middlewareClient, mockNgasService));

        testAppender = Log4JTestAppender.createAppender();
    }

    @Test
    public void testSuccessfulRunExits() throws Exception
    {
        // by expecting a system exit, any asserts/verifications will not be run after the command that exits.
        // that functionality will need to be tested elsewhere.
        exit.expectSystemExitWithStatus(0);

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();
        FileUtils.touch(new File(name));

        doReturn(true).when(downloader).downloadFileAndCreateChecksumFromNgas(anyString(), anyString(), anyBoolean());

        downloader.run("-fileId", fileId, "-name", name);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfulRunLogsE134() throws Exception
    {
        exit.expectSystemExit();

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();
        FileUtils.touch(new File(name));

        doReturn(true).when(downloader).downloadFileAndCreateChecksumFromNgas(anyString(), anyString(), anyBoolean());

        runAndCheckConditionsIgnoringExit(() -> {
            downloader.run("-fileId", fileId, "-name", name);
        }, () -> {
            String logMessage = CasdaDataAccessEvents.E134.messageBuilder().add(fileId).toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: ARCHIVE\\].*"), matchesPattern(".*\\[destination: DATA_ACCESS\\].*"),
                    matchesPattern(".*\\[volumeKB: 0\\].*"), matchesPattern(".*\\[fileId: " + fileId + "\\].*")),
                    sameInstance((Throwable) null));
        });
    }

    @Test
    public void testSuccessfulRunWithChecksumExits() throws Exception
    {
        // by expecting a system exit, any asserts/verifications will not be run after the command that exits.
        // that functionality will need to be tested elsewhere.
        exit.expectSystemExitWithStatus(0);

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();
        FileUtils.touch(new File(name));

        doReturn(true).when(downloader).downloadFileAndCreateChecksumFromNgas(anyString(), anyString(), anyBoolean());

        downloader.run("-fileId", fileId, "-name", name, "-checksum");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfulRunWithChecksumLogsE134() throws Exception
    {
        exit.expectSystemExit();

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();
        FileUtils.touch(new File(name));

        doReturn(true).when(downloader).downloadFileAndCreateChecksumFromNgas(anyString(), anyString(), anyBoolean());

        runAndCheckConditionsIgnoringExit(() -> {
            downloader.run("-fileId", fileId, "-name", name, "-checksum");
        }, () -> {
            String logMessage = CasdaDataAccessEvents.E134.messageBuilder().add(fileId).toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: ARCHIVE\\].*"), matchesPattern(".*\\[destination: DATA_ACCESS\\].*"),
                    matchesPattern(".*\\[volumeKB: 0\\].*"), matchesPattern(".*\\[fileId: " + fileId + "\\].*")),
                    sameInstance((Throwable) null));
        });
    }

    @Test
    public void testHelpExits() throws Exception
    {
        // by expecting a system exit, any asserts/verifications will not be run after the command that exits.
        // that functionality will need to be tested elsewhere.
        exit.expectSystemExitWithStatus(0);

        downloader.run("-help");
    }

    @Test
    public void testInvalidArgsExits() throws Exception
    {
        // by expecting a system exit, any asserts/verifications will not be run after the command that exits.
        // that functionality will need to be tested elsewhere.
        exit.expectSystemExitWithStatus(1);

        downloader.run("-invalid", "-fileId", "file.id", "name", "name");
    }

    @Test
    public void testMiddlewareClientExceptionOnDownloadExits() throws Exception
    {
        // by expecting a system exit, any asserts/verifications will not be run after the command that exits.
        // that functionality will need to be tested elsewhere.
        exit.expectSystemExitWithStatus(1);

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        doThrow(new MiddlewareClientException("problem")).when(downloader).downloadFileFromNgas(anyString(),
                anyString());

        downloader.run("-fileId", fileId, "-name", name);
    }

    @Test
    public void testServiceCallExceptionOnChecksumExits() throws Exception
    {
        // by expecting a system exit, any asserts/verifications will not be run after the command that exits.
        // that functionality will need to be tested elsewhere.
        exit.expectSystemExitWithStatus(1);

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        doNothing().when(downloader).downloadFileFromNgas(anyString(), anyString());

        doThrow(new NgasService.ServiceCallException("problem")).when(downloader).createChecksumFile(anyString(),
                anyString());

        downloader.run("-fileId", fileId, "-name", name, "-checksum");
    }

    @Test
    public void testIOExceptionOnChecksumExits() throws Exception
    {
        // by expecting a system exit, any asserts/verifications will not be run after the command that exits.
        // that functionality will need to be tested elsewhere.
        exit.expectSystemExitWithStatus(1);

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        doNothing().when(downloader).downloadFileFromNgas(anyString(), anyString());

        doThrow(new IOException("problem")).when(downloader).createChecksumFile(anyString(), anyString());

        downloader.run("-fileId", fileId, "-name", name, "-checksum");

    }

    @Test
    public void testRuntimeExceptionThrown() throws Exception
    {
        // by expecting an exception, any asserts/verifications will not be run after the command that throws the
        // exception. that functionality will need to be tested elsewhere
        exception.expect(RuntimeException.class);
        exception.expectMessage("Some message");

        doThrow(new RuntimeException("Some message")).when(downloader).downloadFileAndCreateChecksumFromNgas(
                anyString(), anyString(), anyBoolean());

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        downloader.run("-fileId", fileId, "-name", name);
    }

    @Test
    public void testRuntimeExceptionThrownOnChecksum() throws Exception
    {
        // by expecting an exception, any asserts/verifications will not be run after the command that throws the
        // exception. that functionality will need to be tested elsewhere
        exception.expect(RuntimeException.class);
        exception.expectMessage("Some message");

        doThrow(new RuntimeException("Some message")).when(downloader).downloadFileAndCreateChecksumFromNgas(
                anyString(), anyString(), anyBoolean());

        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        downloader.run("-fileId", fileId, "-name", name, "-checksum");
    }

    @Test
    public void testDownloadCreatesFileMiddlewareClientLocal() throws Exception
    {
        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        boolean success = downloader.downloadFileAndCreateChecksumFromNgas(fileId, name, false);
        assertTrue(success);

        // check that the file is at the destination (created by MiddlewareClientLocal)
        assertTrue(Files.exists(Paths.get(name)));

        verify(downloader, times(1)).downloadFileFromNgas(eq(fileId), eq(name));
        verify(downloader, never()).createChecksumFile(anyString(), anyString());
    }

    @Test
    public void testDownloadCreatesFileMiddlewareClientLocalAndChecksum() throws Exception
    {
        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        Status status = mock(Status.class);
        when(mockNgasService.getStatus(fileId)).thenReturn(status);
        when(status.wasSuccess()).thenReturn(true);
        when(status.getChecksum()).thenReturn("123-abc checksum value");

        boolean success = downloader.downloadFileAndCreateChecksumFromNgas(fileId, name, true);

        assertTrue(success);
        verify(downloader, times(1)).downloadFileFromNgas(eq(fileId), eq(name));
        verify(downloader, times(1)).createChecksumFile(eq(fileId), eq(name));

        // check that the file is at the destination (created by MiddlewareClientLocal)
        assertTrue(Files.exists(FileSystems.getDefault().getPath(name)));

        // check that the checksum file is at the destination
        Path checksumFile = Paths.get(name + ".checksum");
        assertTrue(Files.exists(checksumFile));
        List<String> lines = Files.readAllLines(checksumFile, Charsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals("123-abc checksum value", lines.get(0));
    }

    @Test
    public void testFailedDownloadMiddlewareClientExceptionLogsError() throws Exception
    {
        String fileId = "fail destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        doThrow(new MiddlewareClientException("download problem")).when(downloader).downloadFileFromNgas(eq(fileId),
                any());

        boolean success = downloader.downloadFileAndCreateChecksumFromNgas(fileId, name, false);
        // check that the log contains an expected string
        testAppender.verifyLogMessage(Level.ERROR, CasdaDataAccessEvents.E030.messageBuilder().add("file").add(fileId)
                .toString());
        verify(downloader, times(1)).downloadFileFromNgas(eq(fileId), eq(name));
        verify(downloader, never()).createChecksumFile(eq(fileId), eq(name));
        assertFalse(success);
    }

    @Test
    public void testFailedCreateChecksumServiceCallExceptionLogsError() throws Exception
    {
        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        doNothing().when(downloader).downloadFileFromNgas(anyString(), anyString());
        doThrow(new NgasService.ServiceCallException("service call exception")).when(downloader).createChecksumFile(
                anyString(), anyString());

        boolean success = downloader.downloadFileAndCreateChecksumFromNgas(fileId, name, true);

        // check that the log contains an expected string
        testAppender.verifyLogMessage(Level.ERROR,
                CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(LogEvent.UNKNOWN_EVENT).toString(),
                ServiceCallException.class, "service call exception");
        verify(downloader, times(1)).downloadFileFromNgas(eq(fileId), eq(name));
        verify(downloader, times(1)).createChecksumFile(eq(fileId), eq(name));

        assertFalse(success);
    }

    @Test
    public void testFailedCreateChecksumIOExceptionLogsError() throws Exception
    {
        String fileId = "destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        doNothing().when(downloader).downloadFileFromNgas(anyString(), anyString());
        doThrow(new IOException("problem writing")).when(downloader).createChecksumFile(anyString(), anyString());

        boolean success = downloader.downloadFileAndCreateChecksumFromNgas(fileId, name, true);

        // check that the log contains an expected string
        testAppender.verifyLogMessage(Level.ERROR,
                CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(LogEvent.UNKNOWN_EVENT).toString(),
                IOException.class, "problem writing");
        verify(downloader, times(1)).downloadFileFromNgas(eq(fileId), eq(name));
        verify(downloader, times(1)).createChecksumFile(eq(fileId), eq(name));

        assertFalse(success);
    }

    @Test
    public void testFailedDownloadThrowsException() throws Exception
    {
        exception.expect(MiddlewareClientException.class);
        exception.expectMessage("download problem");

        String fileId = "fail destination.dat";
        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        doThrow(new MiddlewareClientException("download problem")).when(middlewareClient).retrieve(eq(fileId), any());

        downloader.downloadFileFromNgas(fileId, name);
    }

    @Test
    public void testCreateChecksumThrowsExceptionIfGetStatusUnsuccessful() throws Exception
    {
        exception.expect(ServiceCallException.class);
        exception.expectMessage("Status check for destination.dat was unsuccessful: status.as.string");

        String fileId = "destination.dat";

        Status status = mock(Status.class);
        when(mockNgasService.getStatus(fileId)).thenReturn(status);
        when(status.wasSuccess()).thenReturn(false);
        doReturn("status.as.string").when(status).toString();

        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        downloader.createChecksumFile(fileId, name);
    }

    @Test
    public void testCreateChecksumThrowsExceptionIfNullChecksum() throws Exception
    {
        exception.expect(ServiceCallException.class);
        exception.expectMessage("Checksum empty for destination.dat: status.as.string");

        String fileId = "destination.dat";

        Status status = mock(Status.class);
        when(mockNgasService.getStatus(fileId)).thenReturn(status);
        when(status.wasSuccess()).thenReturn(true);
        when(status.getChecksum()).thenReturn(null);
        doReturn("status.as.string").when(status).toString();

        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        downloader.createChecksumFile(fileId, name);
    }

    @Test
    public void testCreateChecksumThrowsExceptionIfBlankChecksum() throws Exception
    {
        exception.expect(ServiceCallException.class);
        exception.expectMessage("Checksum empty for destination.dat: status.as.string");

        String fileId = "destination.dat";

        Status status = mock(Status.class);
        when(mockNgasService.getStatus(fileId)).thenReturn(status);
        when(status.wasSuccess()).thenReturn(true);
        when(status.getChecksum()).thenReturn("  ");
        doReturn("status.as.string").when(status).toString();

        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        downloader.createChecksumFile(fileId, name);
    }

    @Test
    public void testCreateChecksumThrowsExceptionIfGetStatusThrowsException() throws Exception
    {
        exception.expect(ServiceCallException.class);
        exception.expectMessage("sample exception");

        String fileId = "destination.dat";

        Exception sampleException = new NgasService.ServiceCallException("sample exception");
        when(mockNgasService.getStatus(fileId)).thenThrow(sampleException);

        String name = new File(tempFolder.getRoot(), fileId).getCanonicalPath();

        downloader.createChecksumFile(fileId, name);

    }

    @Test
    public void testCreateChecksumThrowsExceptionIfCantCreateFile() throws Exception
    {
        exception.expect(IOException.class);
        exception.expectMessage("couldn't create file");

        String fileId = "destination.dat";

        Status mockStatus = mock(Status.class);
        when(mockNgasService.getStatus(fileId)).thenReturn(mockStatus);
        when(mockStatus.wasSuccess()).thenReturn(true);
        when(mockStatus.getChecksum()).thenReturn("checksum string");

        String name = new File("/invalid/folder", fileId).getCanonicalPath();

        doThrow(new IOException("couldn't create file")).when(downloader).createFile(eq(name + ".checksum"),
                eq("checksum string"));

        downloader.createChecksumFile(fileId, name);

    }

    @Test
    public void malformedParametersTest() throws IOException
    {
        // NOTE: this test does not verify building a proper event message.
        // It only uses event format to verify that parameters are passed to the message builder.
        // Arguments set
        DownloaderCommandLineArgumentsParser parser = new DownloaderCommandLineArgumentsParser();
        parser.getArgs().setFileId("fileId");
        parser.getArgs().setName("name");
        DataDepositMessageBuilder builder = CasdaDataAccessEvents.E100.messageBuilder();
        parser.addArgumentValuesToMalformedParametersEvent(builder);
        String msg = builder.toString();
        assertTrue(msg.contains("fileId") && msg.contains("name"));

        // Arguments are not set
        parser = new DownloaderCommandLineArgumentsParser();
        builder = CasdaDataAccessEvents.E100.messageBuilder();
        parser.addArgumentValuesToMalformedParametersEvent(builder);
        msg = builder.toString();
        assertTrue(msg.contains("NOT-SPECIFIED") && !msg.contains("name") && !msg.contains("fileId"));

        // FieldId is not set
        parser = new DownloaderCommandLineArgumentsParser();
        parser.getArgs().setName("name");
        builder = CasdaDataAccessEvents.E100.messageBuilder();
        parser.addArgumentValuesToMalformedParametersEvent(builder);
        msg = builder.toString();
        assertTrue(msg.contains("NOT-SPECIFIED") && msg.contains("name") && !msg.contains("fileId"));

        // FieldId is empty
        parser = new DownloaderCommandLineArgumentsParser();
        parser.getArgs().setName("name");
        parser.getArgs().setFileId("");
        builder = CasdaDataAccessEvents.E100.messageBuilder();
        parser.addArgumentValuesToMalformedParametersEvent(builder);
        msg = builder.toString();
        assertTrue(msg.contains("NOT-SPECIFIED") && msg.contains("name") && !msg.contains("fileId"));

        // Name is not set
        parser = new DownloaderCommandLineArgumentsParser();
        parser.getArgs().setFileId("fileId");
        builder = CasdaDataAccessEvents.E100.messageBuilder();
        parser.addArgumentValuesToMalformedParametersEvent(builder);
        msg = builder.toString();
        assertTrue(msg.contains("NOT-SPECIFIED") && !msg.contains("name") && msg.contains("fileId"));

        // Name is empty
        parser = new DownloaderCommandLineArgumentsParser();
        parser.getArgs().setFileId("fileId");
        parser.getArgs().setName("");
        builder = CasdaDataAccessEvents.E100.messageBuilder();
        parser.addArgumentValuesToMalformedParametersEvent(builder);
        msg = builder.toString();
        assertTrue(msg.contains("NOT-SPECIFIED") && !msg.contains("name") && msg.contains("fileId"));
    }

    @Override
    protected ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter()
    {
        return new NgasDownloader("", middlewareClient, mockNgasService);
    }
}
