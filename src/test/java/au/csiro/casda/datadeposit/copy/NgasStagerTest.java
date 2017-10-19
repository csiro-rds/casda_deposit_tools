package au.csiro.casda.datadeposit.copy;

import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.Utils;
import au.csiro.casda.datadeposit.service.NgasService.ServiceCallException;
import au.csiro.casda.jobmanager.JavaProcessJobFactory;

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
 * NgasStager Test
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@ActiveProfiles("local")
public class NgasStagerTest
{
    @Autowired
    @Value("${failing.copy.command}")
    private String failingCopyCommand;

    @Autowired
    @Value("${existence.copy.command}")
    private String existenceCopyCommand;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    String depositObservationParentDirectory;
    String ngasServerName;
    String ngasStagingDirectory;
    NgasStager stager;
    Integer sbid;
    String filename;
    String volume;
    String fileId;

    @Before
    public void setUp() throws Exception
    {
        // Manual equivalent to @RunWith(SpringJUnit4ClassRunner.class)
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        depositObservationParentDirectory = "src/test/resources/deposit/";
        ngasServerName = RandomStringUtils.randomAlphabetic(20);
        ngasStagingDirectory = RandomStringUtils.randomAlphabetic(20);
        sbid = new Integer(11111);
        filename = depositObservationParentDirectory + sbid + "/observation.xml";
        volume = RandomStringUtils.randomAlphabetic(20);
        fileId = RandomStringUtils.randomAlphabetic(20);
    }

    @Test
    public void missingSourceFileShouldThrowNoSuchFileException() throws NoSuchFileException, ChecksumMissingException,
            StagingException
    {
        sbid = Integer.parseInt(RandomStringUtils.randomNumeric(6));
        filename = depositObservationParentDirectory + sbid + "/" + RandomStringUtils.randomAlphabetic(20);

        stager =
                new NgasStager(ngasServerName, ngasStagingDirectory,
                        existenceCopyCommand, new JavaProcessJobFactory());

        exception.expect(NoSuchFileException.class);
        exception
                .expectMessage(String.format("Artefact with filename '%s' for observation %d not found at path: %s",
                        filename, sbid,
                        Paths.get(filename)));

        stager.stageArtefactToNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void copyToStagingFailureShouldThrowNgasStagingCopyException() throws NoSuchFileException,
            ChecksumMissingException, StagingException
    {
        stager =
                new NgasStager(ngasServerName, ngasStagingDirectory,
                        failingCopyCommand, new JavaProcessJobFactory());

        exception.expect(StagingException.class);
        exception.expectMessage(String.format(
                "Could not stage artefact with filename '%s' for observation %d to NGAS staging path %s. "
                        + "Cause: Failed to run:\n%s\n%s", filename, sbid, ngasStagingDirectory,
                StringUtils.join(Utils.elStringToArray(failingCopyCommand), " "),
                "(Process failure cause unavailable.)"));

        stager.stageArtefactToNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void checksumFileMissingShouldThrowChecksumMissingException() throws ServiceCallException,
            NoSuchFileException, ChecksumMissingException, StagingException
    {
        sbid = 22223;
        filename = depositObservationParentDirectory + sbid + "/image.i.clean.restored.fits";
        stager =
                new NgasStager(ngasServerName, ngasStagingDirectory,
                        existenceCopyCommand, new JavaProcessJobFactory());

        exception.expect(ChecksumMissingException.class);
        exception.expectMessage(String.format(
                "Could not stage artefact with filename '%s' for observation %d because checksum file %s is missing",
                filename, sbid, Paths.get(filename + ".checksum")
                        .toString()));

        stager.stageArtefactToNgas(sbid, filename, volume, fileId);
    }

    @Test
    public void checksumFileExistsShouldNotThrowChecksumMissingException() throws ServiceCallException,
            NoSuchFileException, ChecksumMissingException, StagingException
    {
        sbid = 11111;
        filename = depositObservationParentDirectory + "/" + sbid + "/observation.xml";
        stager =
                new NgasStager(ngasServerName, ngasStagingDirectory,
                        existenceCopyCommand, new JavaProcessJobFactory());

        stager.stageArtefactToNgas(sbid, filename, volume, fileId);
    }
    
    @Test
    public void artifactEmptyShouldThrowStagingException() throws ServiceCallException,
            NoSuchFileException, ChecksumMissingException, StagingException
    {
        sbid = 22226;
        filename = depositObservationParentDirectory + "/" + sbid + "/evaluations.pdf";
        stager = new NgasStager(ngasServerName, ngasStagingDirectory,
                        existenceCopyCommand, new JavaProcessJobFactory());

        exception.expect(StagingException.class);
        exception.expectMessage(String.format(
                "Could not stage artefact with filename '%s' for observation %d to NGAS staging path %s."
                + " Cause: Artefact with filename '%s' is empty.", filename, sbid, ngasStagingDirectory, filename));

        stager.stageArtefactToNgas(sbid, filename, volume, fileId);
    }

}
