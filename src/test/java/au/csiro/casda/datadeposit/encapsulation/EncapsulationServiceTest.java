package au.csiro.casda.datadeposit.encapsulation;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.casda.datadeposit.encapsulation.EncapsulationService.EncapsulationException;
import au.csiro.casda.datadeposit.observation.jpa.repository.EncapsulationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.service.InlineScriptException;
import au.csiro.casda.datadeposit.service.InlineScriptService;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.jobmanager.JobManager.JobMonitor;
import au.csiro.casda.jobmanager.ProcessJob;
import au.csiro.casda.jobmanager.ProcessJobBuilder.ProcessJobFactory;

/*
 * #%L
 * CSIRO Data Access Portal
 * %%
 * Copyright (C) 2010 - 2016 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * Validate the EncapsulationService code.
 * <p>
 * Copyright 2016, CSIRO Australia. All rights reserved.
 */
public class EncapsulationServiceTest
{

    private EncapsulationService service;

    @Mock
    private InlineScriptService inlineScriptService;

    @Mock
    private ProcessJobFactory processJobFactory;

    @Mock
    private EncapsulationFileRepository encapsulationFileRepository;
    
    @Mock
    private EvaluationFileRepository evaluationFileRepository;
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        service = new EncapsulationService("{\"/bin/bash\",\"-c\",\"cat <pattern> > <infile>\"}", "", "",
                processJobFactory, inlineScriptService, encapsulationFileRepository, evaluationFileRepository);
    }

    /**
     * Test that the checksum verification passes for a good checksum
     */
    @Test
    public void testVerifyChecksumsGood() throws IOException, EncapsulationException, InlineScriptException
    {
        String inFile = tempFolder.newFile("encaps-spectrum-1.tar").getAbsolutePath();
        File spectrumFile = tempFolder.newFile("spec_1.fits");
        File checksumFile = tempFolder.newFile("spec_1.fits.checksum");
        String checksum = "0 0 0";
        try (PrintWriter writer = new PrintWriter(checksumFile, CharEncoding.UTF_8))
        {
            writer.println(checksum);
        }

        when(inlineScriptService.callScriptInline(any(String.class), any(String.class))).thenReturn(checksum);

        service.verifyChecksums(inFile, "myFile.tar", "spec_*.fits", 1234, false);

        verify(inlineScriptService).callScriptInline(any(String.class), eq(spectrumFile.getAbsolutePath()));

    }

    /**
     * Test that the checksum verification passes for a non matching checksum
     */
    @Test
    public void testVerifyChecksumsBad() throws IOException, EncapsulationException, InlineScriptException
    {
        String inFile = tempFolder.newFile("encaps-spectrum-1.tar").getAbsolutePath();
        File spectrumFile = tempFolder.newFile("spec_1.fits");
        File checksumFile = tempFolder.newFile("spec_1.fits.checksum");
        String checksum = "0 0 0";
        try (PrintWriter writer = new PrintWriter(checksumFile, CharEncoding.UTF_8))
        {
            writer.println(checksum);
        }

        when(inlineScriptService.callScriptInline(any(String.class), any(String.class))).thenReturn("Something else");

        expectedException.expect(EncapsulationException.class);
        expectedException.expectMessage("Checksum was invalid for " + spectrumFile.getAbsolutePath()
                + ". Excepted 0 0 0 but got Something else.");

        service.verifyChecksums(inFile, "myFile.tar", "spec_*.fits", 1234, false);

    }

    /**
     * Test method for createEncapsulation.
     */
    @Test
    public void testCreateEncapsulation() throws Exception
    {
        // Create two files matching the pattern and one not
        String inFile = tempFolder.newFile("encaps-spectrum-1.tar").getAbsolutePath();
        File spectrumFile = tempFolder.newFile("spec_1.fits");
        File checksumFile = tempFolder.newFile("spec_1.fits.checksum");
        File spectrum2File = tempFolder.newFile("spec_2.fits");
        File checksum2File = tempFolder.newFile("spec_2.fits.checksum");
        File thumbnail1File = tempFolder.newFile("spec_1.png");
        File checksum3File = tempFolder.newFile("spec_1.png.checksum");

        when(processJobFactory.createJobProcess(any(String.class), any(String.class), any(String.class), any(Map.class),
                any(String[].class))).thenReturn(new ProcessJob("42", inFile, inFile, null, null)
                {
                    
                    @Override
                    public void run(JobMonitor monitor)
                    {
                        monitor.jobSucceeded(this, "");
                    }
                });
        when(inlineScriptService.callScriptInline(any(String.class), any(String.class))).thenReturn("0 0 0");

        int numFiles = service.createEncapsulation(1000, "", inFile, "spec_*.fits", false);
        assertThat(numFiles, is(2));
        
    }

    @Test
    public void testCreateEvaluationEncapsulation() throws Exception
    {
    	List<EvaluationFile> results = new ArrayList<EvaluationFile>();
    	EvaluationFile file1 = new EvaluationFile();
    	file1.setFilename("hello.pdf");
    	results.add(file1);
    	String inFile = tempFolder.newFile("encaps-evaluation-1.tar").getAbsolutePath();
        when(evaluationFileRepository.findByObservationSbidAndEncapsulationFileFilename(any(Integer.class),
                any(String.class))).thenReturn(results);
        when(processJobFactory.createJobProcess(any(String.class), any(String.class), any(String.class), any(Map.class),
                any(String[].class))).thenReturn(new ProcessJob("42", inFile, inFile, null, null)
                {
                    
                    @Override
                    public void run(JobMonitor monitor)
                    {
                        monitor.jobSucceeded(this, "");
                    }
                });
        when(inlineScriptService.callScriptInline(any(String.class), any(String.class))).thenReturn("0 0 0");
        
        int numFiles = service.createEncapsulation(1000, "", inFile, null, true);
        assertThat(numFiles, is(1));
    }
}
