package au.csiro.casda.datadeposit.catalogue.continuum;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.casda.datadeposit.DepositState;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.PolarisationComponentRepository;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Project;
import de.bechte.junit.runners.context.HierarchicalContextRunner;

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
 * Tests the PolarisationComponentCatalogueParser
 * <p>
 * Copyright 2015, CSIRO Australia All rights reserved.
 */
@RunWith(HierarchicalContextRunner.class)
public class PolarisationComponentCatalogueParserTest
{
    private Catalogue catalogue;

    @Mock
    private ObservationRepository observationRepository;

    @Mock
    private CatalogueRepository catalogueRepository;

    @Mock
    private PolarisationComponentRepository polarisationComponentRepository;

    private PolarisationComponentCatalogueParser parser;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        parser =
                new PolarisationComponentCatalogueParser(observationRepository, catalogueRepository,
                        polarisationComponentRepository, new PolarisationComponentVoTableVisitor(
                                polarisationComponentRepository));

    }

    public class ValidCases
    {
        @Test
        public void simple() throws Exception
        {
            String catalogueFilename = getPathForGoodTestCase("sample-polarisation");
            configureMockRepositories(catalogueFilename, "src/test/resources/image/good/validFile.fits");

            parser.parseFile(12345, catalogueFilename, catalogueFilename, CatalogueParser.Mode.NORMAL, null);
            // this would throw an exception if parsing failed, but we expect it to be ok

            ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
            verify(catalogue).setDepositState(queryCaptor.capture());
            assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.PROCESSED));
        }

    }

    private String getPathForGoodTestCase(String testCase)
    {
        return "src/test/resources/catalogue/good/polarisation/" + testCase + ".xml";
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
