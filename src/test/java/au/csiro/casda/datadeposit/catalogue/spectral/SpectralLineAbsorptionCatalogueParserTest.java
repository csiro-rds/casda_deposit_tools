package au.csiro.casda.datadeposit.catalogue.spectral;

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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.TestUtils;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.ValidationModeSignal;
import au.csiro.casda.datadeposit.catalogue.spectralline.SpectralLineAbsorptionCatalogueParser;
import au.csiro.casda.datadeposit.catalogue.spectralline.SpectralLineAbsorptionVoTableVisitor;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectralLineAbsorptionRepository;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Project;
import de.bechte.junit.runners.context.HierarchicalContextRunner;

/**
 * Tests the SpectalLineAbsorptionCatalogueParser
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@RunWith(HierarchicalContextRunner.class)
public class SpectralLineAbsorptionCatalogueParserTest 
{
	 private Catalogue catalogue;
	
    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private CatalogueRepository catalogueRepository;

    @Mock
    private SpectralLineAbsorptionRepository spectralLineAbsorptionRepository;

    private SpectralLineAbsorptionCatalogueParser parser;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        parser = new SpectralLineAbsorptionCatalogueParser(observationRepository, catalogueRepository,
        		spectralLineAbsorptionRepository,
                        new SpectralLineAbsorptionVoTableVisitor(spectralLineAbsorptionRepository));
    }
    
    /**
     * simple parser test should succeed.
     * @throws Exception throws an exception on failure.
     */
    @Test
    public void parseValidSpectralLineAbsorption() throws Exception
    {
        String catalogueFilename = "src/test/resources/spectralline/good/spectralLineAbsorption.good.xml";
        configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

        parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL);
    }
    
    /**
     * parsing of invalid file, should return all problems in the file
     * currently returns 1 param error, 1 field error & 1 value error
     * @throws Exception an exception
     */
    @Test
    public void parseInvalidSpectralLineAbsorption() throws Exception
    {
        BufferedReader goldenMasterReader = new BufferedReader(new InputStreamReader(new FileInputStream(
                        TestUtils.getResourceAsFile("spectralline/bad/spectrallineAbsorptionFailureMessages.txt")),
                        Charset.forName(CharEncoding.UTF_8)));
    	
    	try
    	{
            String catalogueFilename = "src/test/resources/spectralline/bad/spectralLineAbsorption.bad.xml";
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.VALIDATE_ONLY);
    	}
    	catch(ValidationModeSignal vms)
    	{
            int i = 0;
            String goldenMasterLine = goldenMasterReader.readLine();
            while (goldenMasterLine != null)
            {
                String outputLine = vms.getValidationFailureMessages().get(i);
                Assert.assertNotNull("Output missing line at line" + i, outputLine);
                assertEquals("Difference in line " + i, goldenMasterLine, outputLine);
                goldenMasterLine = goldenMasterReader.readLine();
                i++;
            }
            assertEquals("Output has additional lines from line " + i, i, vms.getValidationFailureMessages().size());
    		
            goldenMasterReader.close();
    	}
    }

    private void configureMockRepositories(String catalogueFilename, String imageCubeFilename)
    {
        catalogue = mock(Catalogue.class);
        doReturn(catalogueFilename).when(catalogue).getFilename();

        Observation observation = mock(Observation.class);
        doReturn(observation).when(observationRepository).findBySbid(anyInt());
        doReturn(observation).when(catalogue).getParent();
        doReturn(Arrays.asList(catalogue)).when(observation).getCatalogues();

        Project project = mock(Project.class);
        doReturn(project).when(catalogue).getProject();
        doReturn(123L).when(project).getId();

        ImageCube imageCube = mock(ImageCube.class);
        doReturn(imageCubeFilename).when(imageCube).getFilename();
        doReturn(Arrays.asList(imageCube)).when(observation).getImageCubes();
        doReturn(observation).when(imageCube).getParent();
    }
}
