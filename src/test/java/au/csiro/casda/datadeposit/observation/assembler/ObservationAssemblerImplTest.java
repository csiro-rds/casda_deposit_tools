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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
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
import au.csiro.casda.datadeposit.DepositState.Type;
import au.csiro.casda.datadeposit.DepositStateImpl;
import au.csiro.casda.datadeposit.observation.ObservationParser.MalformedFileException;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.parser.XmlObservation;
import au.csiro.casda.datadeposit.observation.parser.XmlObservationParser;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Spectrum;

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
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("observation/good"), true);
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
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("observation/good"), true);
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
        XmlObservation dataset = XmlObservationParser
                .parseDataSetXML(TestUtils.getResource("observation/good/multiple-catalogue-types.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("observation/good"), true);
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

    @Test
    public void testSpectraPresent() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("spectra/good/spectra-present.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("spectra/good"), true);
        assertEquals(0, observation.getMeasurementSets().size());
        assertEquals(0, observation.getEvaluationFiles().size());
        assertEquals(0, observation.getCatalogues().size());
        assertEquals(Integer.valueOf(610), observation.getSbid());
        assertEquals(1, observation.getImageCubes().size());
        List<Spectrum> spectra = observation.getSpectra();
        assertNotNull(spectra);
        assertEquals(4, spectra.size());
        for (Spectrum spectrum : spectra)
        {
            assertEquals("fits", spectrum.getFormat());
            assertEquals("AS031", spectrum.getProject().getOpalCode());
            assertEquals("beta.image2.fits", spectrum.getImageCube().getFilename());
            assertEquals(610, Integer.parseInt(spectrum.getParent().getUniqueId()));
            switch (spectrum.getFilename())
            {
            case "beta.image2.001.spr.fits":
                assertEquals("spectral_peak_restored", spectrum.getType());
                assertEquals("beta.image2.001.spr.png", spectrum.getThumbnail().getFilename());
                assertEquals("encaps-spectra-1.tar", spectrum.getEncapsulationFile().getFilename());
                assertEquals("encaps-thumb-2.tar", spectrum.getThumbnail().getEncapsulationFile().getFilename());
                break;

            case "beta.image2.002.spr.fits":
                assertEquals("spectral_peak_restored", spectrum.getType());
                assertEquals("beta.image2.002.spr.png", spectrum.getThumbnail().getFilename());
                assertEquals("encaps-spectra-1.tar", spectrum.getEncapsulationFile().getFilename());
                assertEquals("encaps-thumb-2.tar", spectrum.getThumbnail().getEncapsulationFile().getFilename());
                break;

            case "beta.image2.001.snr.fits":
                assertEquals("spectral_noise_restored", spectrum.getType());
                assertEquals("beta.image2.001.snr.png", spectrum.getThumbnail().getFilename());
                assertEquals("encaps-spectra-3.tar", spectrum.getEncapsulationFile().getFilename());
                assertEquals("encaps-thumb-4.tar", spectrum.getThumbnail().getEncapsulationFile().getFilename());
                break;

            case "beta.image2.002.snr.fits":
                assertEquals("spectral_noise_restored", spectrum.getType());
                assertEquals("beta.image2.002.snr.png", spectrum.getThumbnail().getFilename());
                assertEquals("encaps-spectra-3.tar", spectrum.getEncapsulationFile().getFilename());
                assertEquals("encaps-thumb-4.tar", spectrum.getThumbnail().getEncapsulationFile().getFilename());
                break;

            default:
                fail("Unexpected filename " + spectrum.getFilename());
                break;
            }
        }

        List<EncapsulationFile> encapsulations = observation.getEncapsulationFiles();
        assertEquals("encaps-spectra-1.tar", encapsulations.get(0).getFilename());
        assertEquals("encaps-thumb-2.tar", encapsulations.get(1).getFilename());
        assertEquals("encaps-spectra-3.tar", encapsulations.get(2).getFilename());
        assertEquals("encaps-thumb-4.tar", encapsulations.get(3).getFilename());
        assertEquals(4, encapsulations.size());
        for (EncapsulationFile encapsulationFile : encapsulations)
        {
            assertEquals("tar", encapsulationFile.getFormat());
            assertEquals("610", encapsulationFile.getParent().getUniqueId());
        }
    }

    @Test
    public void testSpectraAbsent() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("spectra/good/spectra-absent.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("spectra/good"), true);
        assertEquals(0, observation.getMeasurementSets().size());
        assertEquals(0, observation.getEvaluationFiles().size());
        assertEquals(2, observation.getImageCubes().size());
        assertEquals(0, observation.getSpectra().size());
        assertEquals(0, observation.getMomentMaps().size());
        assertEquals(0, observation.getCubelets().size());
    }

    @Test
    public void testNotEnoughSpectra() throws Exception
    {
        exception.expect(MalformedFileException.class);
        exception.expectMessage("Expected 2 spectra matching beta.image2.*.sir.fits but found 1.");

        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("spectra/bad/spectra-missing.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("spectra/bad"), true);
    }

    @Test
    public void testNotEnoughMomentMaps() throws Exception
    {
        exception.expect(MalformedFileException.class);
        exception.expectMessage("Expected 2 moment maps matching mom0_*.fits but found 1.");

        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("momentmap/bad/momentmap-missing.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("momentmap/bad"), true);
    }
    
    @Test
    public void testNotEnoughCubelets() throws Exception
    {
        exception.expect(MalformedFileException.class);
        exception.expectMessage("Expected 2 cubelets matching cubelet_*.fits but found 1.");

        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("cubelet/bad/cubelet-missing.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("cubelet/bad"), true);
    }

    @Test
    public void testMissingThumbnails() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("spectra/good/thumbnail-missing.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("spectra/good"), true);
        assertEquals(0, observation.getMeasurementSets().size());
        assertEquals(0, observation.getEvaluationFiles().size());
        assertEquals(1, observation.getImageCubes().size());
        List<Spectrum> spectra = observation.getSpectra();
        assertNotNull(spectra);
        assertEquals(4, spectra.size());
        for (Spectrum spectrum : spectra)
        {
            assertEquals("fits", spectrum.getFormat());
            assertEquals("AS031", spectrum.getProject().getOpalCode());
            assertEquals("beta.image2.fits", spectrum.getImageCube().getFilename());
            assertEquals("610", spectrum.getParent().getUniqueId());
            switch (spectrum.getFilename())
            {
            case "beta.image2.001.spr.fits":
                assertEquals("spectral_peak_restored", spectrum.getType());
                assertEquals("encaps-spectra-1.tar", spectrum.getEncapsulationFile().getFilename());
                assertNull(spectrum.getThumbnail());
                break;

            case "beta.image2.002.spr.fits":
                assertEquals("spectral_peak_restored", spectrum.getType());
                assertEquals("encaps-spectra-1.tar", spectrum.getEncapsulationFile().getFilename());
                assertNull(spectrum.getThumbnail());
                break;

            case "beta.image2.001.sir.fits":
                assertEquals("spectral_integrated_restored", spectrum.getType());
                assertEquals("encaps-spectra-3.tar", spectrum.getEncapsulationFile().getFilename());
                assertEquals("beta.image2.001.sir.png", spectrum.getThumbnail().getFilename());
                assertEquals("encaps-thumb-4.tar", spectrum.getThumbnail().getEncapsulationFile().getFilename());
                break;

            case "beta.image2.002.sir.fits":
                assertEquals("spectral_integrated_restored", spectrum.getType());
                assertEquals("encaps-spectra-3.tar", spectrum.getEncapsulationFile().getFilename());
                assertNull(spectrum.getThumbnail());
                break;

            default:
                fail("Unexpected filename " + spectrum.getFilename());
                break;
            }

        }

        List<EncapsulationFile> encapsulations = observation.getEncapsulationFiles();
        assertEquals("encaps-spectra-1.tar", encapsulations.get(0).getFilename());
        assertEquals("encaps-spectra-3.tar", encapsulations.get(1).getFilename());
        assertEquals("encaps-thumb-4.tar", encapsulations.get(2).getFilename());
        assertEquals(3, encapsulations.size());
        for (EncapsulationFile encapsulationFile : encapsulations)
        {
            assertEquals("tar", encapsulationFile.getFormat());
            assertEquals("610", encapsulationFile.getParent().getUniqueId());
        }
    }

    @Test
    public void testGetNextEncapsPairNum()
    {
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);

        assertThat(observationAssembler.getNextEncapsPairNum(new ArrayList<>()), is(0));

        List<EncapsulationFile> encapsulations = new ArrayList<>();
        encapsulations.add(createNamedEncapsulationFile("encaps-spectra-1.tar"));
        encapsulations.add(createNamedEncapsulationFile("encaps-thumb-2.tar"));
        assertThat(observationAssembler.getNextEncapsPairNum(encapsulations), is(1));

        encapsulations.add(createNamedEncapsulationFile("encaps-spectra-3.tar"));
        assertThat(observationAssembler.getNextEncapsPairNum(encapsulations), is(2));

        encapsulations.add(createNamedEncapsulationFile("encaps-mom-7.tar"));
        assertThat(observationAssembler.getNextEncapsPairNum(encapsulations), is(4));
    }

    @Test
    public void testEncapsForDuplicatesNotAdded() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("spectra/good/thumbnail-missing.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);

        // do initial deposit of observation
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("spectra/good"), true);
        List<EncapsulationFile> encapsulations = observation.getEncapsulationFiles();
        assertEquals("encaps-spectra-1.tar", encapsulations.get(0).getFilename());
        assertEquals("encaps-spectra-3.tar", encapsulations.get(1).getFilename());
        assertEquals("encaps-thumb-4.tar", encapsulations.get(2).getFilename());
        assertEquals(3, encapsulations.size());

        // Now redeposit with no changes
        observation.setDepositState(new DepositStateImpl(Type.DEPOSITED, observation));
        observation.setMetadataFileDepositStateType(Type.DEPOSITED);
        assertEquals(0, observation.getObservationMetadataFileDepositable().getDepositFailureCount());
        when(observationRepository.findBySbid(observation.getSbid())).thenReturn(observation);
        Observation updatedObservation = observationAssembler.updateObservationFromParsedObservation(dataset,
                fileIdMaxSize, TestUtils.getResourceAsFile("spectra/good"), true);
        // Ensure there haven't been any new encapsulation files added.
        encapsulations = updatedObservation.getEncapsulationFiles();
        assertEquals("encaps-spectra-1.tar", encapsulations.get(0).getFilename());
        assertEquals("encaps-spectra-3.tar", encapsulations.get(1).getFilename());
        assertEquals("encaps-thumb-4.tar", encapsulations.get(2).getFilename());
        assertEquals(3, encapsulations.size());
        // Check the observation.xml has been prepped for redeposit
        assertEquals(Type.UNDEPOSITED, observation.getMetadataFileDepositStateType());
        assertEquals(1, observation.getObservationMetadataFileDepositable().getDepositFailureCount());

    }

    @Test
    public void testRedepositNewImageThumbnails() throws Exception
    {
        XmlObservation dataset =
                XmlObservationParser.parseDataSetXML(TestUtils.getResource("image/good/image-no-thumbnails.xml"));
        ObservationAssemblerImpl observationAssembler =
                new ObservationAssemblerImpl(projectRepository, observationRepository);

        // do initial deposit of observation
        Observation observation = observationAssembler.createObservationFromParsedObservation(dataset, fileIdMaxSize,
                TestUtils.getResourceAsFile("image/good"), true);
        List<ImageCube> images = observation.getImageCubes();
        assertEquals("emu.image.fits", images.get(0).getFilename());
        assertNull(images.get(0).getLargeThumbnail());
        assertNull(images.get(0).getSmallThumbnail());
        List<EncapsulationFile> encapsulations = observation.getEncapsulationFiles();
        assertEquals(0, encapsulations.size());

        // Now redeposit with no changes
        observation.setDepositState(new DepositStateImpl(Type.DEPOSITED, observation));
        when(observationRepository.findBySbid(observation.getSbid())).thenReturn(observation);
        dataset = XmlObservationParser.parseDataSetXML(TestUtils.getResource("image/good/image-with-thumbnails.xml"));
        Observation updatedObservation = observationAssembler.updateObservationFromParsedObservation(dataset,
                fileIdMaxSize, TestUtils.getResourceAsFile("image/good"), true);
        // Check that the new thumbnails have been added
        images = updatedObservation.getImageCubes();
        assertEquals("emu.image.fits", images.get(0).getFilename());
        assertEquals("img_large.png", images.get(0).getLargeThumbnail().getFilename());
        assertEquals("img_small.png", images.get(0).getSmallThumbnail().getFilename());
        encapsulations = updatedObservation.getEncapsulationFiles();
        assertEquals(0, encapsulations.size());
    }

    private EncapsulationFile createNamedEncapsulationFile(String name)
    {
        EncapsulationFile eFile = new EncapsulationFile();
        eFile.setFilename(name);
        return eFile;
    }

    public void setFileIdMaxSize(int fileIdMaxSize)
    {
        this.fileIdMaxSize = fileIdMaxSize;
    }
}
