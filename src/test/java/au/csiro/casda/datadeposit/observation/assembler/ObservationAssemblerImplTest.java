package au.csiro.casda.datadeposit.observation.assembler;

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


import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import au.csiro.TestUtils;
import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.parser.XmlObservation;
import au.csiro.casda.datadeposit.observation.parser.XmlObservationParser;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.Observation;

/**
 * Tests for the ObservationAssemblerImpl.
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@ActiveProfiles("local")
public class ObservationAssemblerImplTest
{
	@Autowired
	@Value("${fileIdMaxSize}")
	private int fileIdMaxSize;
	
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ObservationRepository observationRepository;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(observationRepository.findBySbid(any(Integer.class))).thenReturn(null);
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testNoMeasurementSets() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/good/metadata-v2-good08.xml"));

        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset,fileIdMaxSize);
        assertEquals(0, observation.getMeasurementSets().size());
        assertEquals(0, observation.getEvaluationFiles().size());
        assertEquals(Integer.valueOf(12351), observation.getSbid());
        Catalogue catalogue = observation.getCatalogues().get(0);
        assertNotNull(catalogue);
        assertEquals(56615.104166666664, catalogue.getTimeObsMjd(), 0.000001);
        assertEquals("2013-11-19T02:30:00.000Z", catalogue.getTimeObs().toString());
    }

    @Test
    public void testEmptyMeasurementSets() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("observation/good/metadata-v2-good07.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset,fileIdMaxSize);
        assertEquals(0, observation.getMeasurementSets().size());
        assertEquals(0, observation.getEvaluationFiles().size());
        assertEquals(Integer.valueOf(12351), observation.getSbid());
        Catalogue catalogue = observation.getCatalogues().get(0);
        assertNotNull(catalogue);
        assertEquals(56615.104166666664, catalogue.getTimeObsMjd(), 0.000001);
        assertEquals("2013-11-19T02:30:00.000Z", catalogue.getTimeObs().toString());
    }
    
    @Test
    public void testCataloguesCreatedWithCatalogueType() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils
                        .getResource("observation/good/multiple-catalogue-types.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset,fileIdMaxSize);
        @SuppressWarnings("serial")
        Map<CatalogueType, List<String>> testConfig = new HashMap<CatalogueType, List<String>>()
        {
            {
                put(CatalogueType.CONTINUUM_ISLAND, Arrays.asList("file1.xml", "file2.xml"));
                put(CatalogueType.CONTINUUM_COMPONENT, Arrays.asList("file3.xml", "file4.xml"));
            }
        };
        for (CatalogueType catalogueType : testConfig.keySet())
        {
            for (String filename : testConfig.get(catalogueType))
            {
                List<Catalogue> catalogues = observation.getCatalogues().stream().filter((c) -> {
                    return c.getFilename().matches(filename);
                }).collect(Collectors.toList());
                assertThat(catalogues.size(), equalTo(1));
                assertThat(catalogues.get(0).getCatalogueType(), equalTo(catalogueType));
            }
        }
    }
    
    public void setFileIdMaxSize(int fileIdMaxSize)
    {
    	this.fileIdMaxSize = fileIdMaxSize;
    }
}
