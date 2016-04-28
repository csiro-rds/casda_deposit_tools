package au.csiro.casda.datadeposit.copy;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import au.csiro.casda.datadeposit.service.NgasService;
import au.csiro.casda.datadeposit.service.NgasService.ServiceCallException;

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
 * NgasRestRegistrar Test
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class NgasRegistrarTest
{
    /** Current checksum for the file deposit/12345/observation.xml */
    private static final String OBSERVATION_XML_CHECKSUM = "f110014d 4b509414162ca0008edb0df84d954630a6c726ef 8fd";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    String depositObservationParentDirectory;
    NgasService ngasService;
    String ngasServerName;
    String ngasStagingDirectory;
    NgasRegistrar registrar;
    Integer sbid;
    String filename;
    String volume;
    String fileId;

    @Before
    public void setUp() throws Exception
    {
        depositObservationParentDirectory = "src/test/resources/deposit/";
        ngasService = mock(NgasService.class);
        ngasServerName = RandomStringUtils.randomAlphabetic(20);
        ngasStagingDirectory = RandomStringUtils.randomAlphabetic(20);
        sbid = new Integer(11111);
        filename = depositObservationParentDirectory + sbid + "/observation.xml";
        volume = RandomStringUtils.randomAlphabetic(20);
        fileId = RandomStringUtils.randomAlphabetic(20);
    }

    @Test
    public void ngasRegisterFileErrorShouldThrowNgasRegisterException() throws ServiceCallException,
            NoSuchFileException, ChecksumVerificationException, RegisterException
    {
        registrar = new NgasRegistrar( ngasService, ngasStagingDirectory);

        NgasService.ServiceCallException ex =
                new NgasService.ServiceCallException(RandomStringUtils.randomAlphabetic(20));
        Mockito.when(ngasService.registerFile(any(), any())).thenThrow(ex);

        exception.expect(RegisterException.class);
        exception.expectMessage(String.format(
                "Could not register artefact with filename '%s' for observation %d with NGAS at staging path %s"
                        + " due to unexpected exception", filename, sbid, ngasStagingDirectory + "/" + fileId));
        exception.expectCause(Matchers.is(ex));

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void ngasRegisterFileReturningFailedStatusShouldNgasRegisterException() throws ServiceCallException,
            NoSuchFileException, ChecksumVerificationException, RegisterException
    {
        registrar = new NgasRegistrar( ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(false);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("12345");
        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        exception.expect(RegisterException.class);
        exception.expectMessage(String.format(
                "Could not register artefact with filename '%s' for observation %d with NGAS at staging path %s"
                        + ". Cause: unknown. Status: Mock for Status, hashCode: %s", filename, sbid,
                ngasStagingDirectory + "/" + fileId, registerFileStatus.hashCode()));

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void ngasGetStatusErrorShouldThrowUnexpectedChecksumVerificationException() throws ServiceCallException,
            NoSuchFileException, ChecksumVerificationException, RegisterException
    {
        registrar = new NgasRegistrar(ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("12345");
        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        NgasService.ServiceCallException ex =
                new NgasService.ServiceCallException(RandomStringUtils.randomAlphabetic(20));
        Mockito.when(ngasService.getStatus(any())).thenThrow(ex);

        exception.expect(UnexpectedChecksumVerificationException.class);
        exception.expectMessage(String.format(
                "Checksum could not be verified for artefact with filename '%s' for observation %d due to "
                        + "unexpected exception.", filename, sbid));
        exception.expectCause(Matchers.is(ex));

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void ngasGetStatusReturningFailedStatusShouldThrowUnexpectedChecksumVerificationException()
            throws ServiceCallException, NoSuchFileException, ChecksumVerificationException, RegisterException
    {
        registrar = new NgasRegistrar( ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("12345");

        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        NgasService.Status fileStatus = mock(NgasService.Status.class);
        Mockito.when(fileStatus.wasSuccess()).thenReturn(false);
        Mockito.when(fileStatus.getChecksum()).thenReturn(null);

        Mockito.when(ngasService.getStatus(any())).thenReturn(fileStatus);

        exception.expect(UnexpectedChecksumVerificationException.class);
        exception.expectMessage(String.format(
                "Checksum could not be verified for artefact with filename '%s' for observation %d. Cause: "
                        + "Unexpected NGAS status: Mock for Status, hashCode: %s", filename, sbid,
                fileStatus.hashCode()));

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void ngasGetStatusReturningNoChecksumShouldThrowChecksumVerificationFailedException()
            throws ServiceCallException, NoSuchFileException, RegisterException, ChecksumVerificationException
    {
        registrar = new NgasRegistrar(ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("12345");

        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        NgasService.Status fileStatus = mock(NgasService.Status.class);
        Mockito.when(fileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(fileStatus.getChecksum()).thenReturn("");

        Mockito.when(ngasService.getStatus(any())).thenReturn(fileStatus);

        exception.expect(ChecksumVerificationFailedException.class);
        exception.expectMessage(String.format(
                "Checksum verification failed for artefact with filename '%s' for observation %d. Deposit "
                        + "checksum: %s. Archive checksum: %s", filename, sbid, 
                        OBSERVATION_XML_CHECKSUM, ""));

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void invalidChecksumShouldThrowChecksumVerificationFailedException() throws ServiceCallException,
            NoSuchFileException, ChecksumVerificationException, RegisterException
    {
        registrar = new NgasRegistrar(ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("12345");

        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        NgasService.Status fileStatus = mock(NgasService.Status.class);
        Mockito.when(fileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(fileStatus.getChecksum()).thenReturn("12345");

        Mockito.when(ngasService.getStatus(any())).thenReturn(fileStatus);

        exception.expect(ChecksumVerificationFailedException.class);
        exception.expectMessage(String.format(
                "Checksum verification failed for artefact with filename '%s' for observation %d. Deposit "
                        + "checksum: %s. Archive checksum: %s", filename, sbid, 
                        OBSERVATION_XML_CHECKSUM, "12345"));

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void checksumFileMissingShouldThrowChecksumMissingException() throws ServiceCallException,
            NoSuchFileException, ChecksumVerificationException, RegisterException
    {
        sbid = 22223;
        String file = "image.i.clean.restored.fits";
        filename = depositObservationParentDirectory + sbid + "/" + file;
       
        registrar = new NgasRegistrar(ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("12345");

        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        exception.expect(NoSuchFileException.class);
        exception.expectMessage(String.format(
                "Checksum could not be compared for artefact with filename '%s' for observation %d because "
                        + "checksum file %s could not be found.", filename, sbid,
                Paths.get(depositObservationParentDirectory, sbid.toString(), file + ".checksum").toString()));

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void validChecksum() throws ServiceCallException, NoSuchFileException, ChecksumVerificationException,
            RegisterException
    {
        registrar = new NgasRegistrar(ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("12345");

        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        NgasService.Status fileStatus = mock(NgasService.Status.class);
        Mockito.when(fileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(fileStatus.getChecksum()).thenReturn(OBSERVATION_XML_CHECKSUM);

        Mockito.when(ngasService.getStatus(any())).thenReturn(fileStatus);

        registrar.registerArtefactWithNgas(11112, filename, volume, fileId);
    }
    
    @Test
    public void validChecksumWithNewlineInFile() throws ServiceCallException, NoSuchFileException,
            ChecksumVerificationException, RegisterException
    {
        registrar = new NgasRegistrar(ngasService, ngasStagingDirectory);

        NgasService.Status registerFileStatus = mock(NgasService.Status.class);
        Mockito.when(registerFileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(registerFileStatus.getChecksum()).thenReturn("123456");

        Mockito.when(ngasService.registerFile(any(), any())).thenReturn(registerFileStatus);

        NgasService.Status fileStatus = mock(NgasService.Status.class);
        Mockito.when(fileStatus.wasSuccess()).thenReturn(true);
        Mockito.when(fileStatus.getChecksum()).thenReturn(OBSERVATION_XML_CHECKSUM);

        Mockito.when(ngasService.getStatus(any())).thenReturn(fileStatus);

        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }
    
    @Test
    public void artifactEmptyShouldThrowRegisterException() throws ServiceCallException,
            NoSuchFileException, ChecksumMissingException, RegisterException, ChecksumVerificationException
    {
        sbid = 22226;
        filename = depositObservationParentDirectory + sbid + "/evaluations.pdf";
        registrar = new NgasRegistrar(ngasService, ngasStagingDirectory);
        
        String failureCause = String.format("Artefact with filename '%s' is empty.", filename);

        exception.expect(RegisterException.class);
        
        exception.expectMessage(String.format("Could not register artefact with filename '%s' for observation %d "
        		+ "with NGAS at staging path %s/%s.%s", filename, sbid, ngasStagingDirectory, fileId, " Cause: " 
        	    + failureCause));
        
        registrar.registerArtefactWithNgas(sbid, filename, volume, fileId);
    }
}
