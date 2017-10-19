package au.csiro.casda.datadeposit.fits.service;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import au.csiro.casda.datadeposit.DepositState;
import au.csiro.casda.datadeposit.ParentType;
import au.csiro.casda.datadeposit.fits.assembler.FitsImageAssembler;
import au.csiro.casda.datadeposit.observation.jpa.repository.CubeletRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.MomentMapRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectrumRepository;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Spectrum;

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
 * Test case for FitsImageService
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class FitsImageServiceTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private SpectrumRepository spectrumRepository = mock(SpectrumRepository.class);
    private MomentMapRepository momentMapRepository = mock(MomentMapRepository.class);
    private ImageCubeRepository imageCubeRepository = mock(ImageCubeRepository.class);
    private CubeletRepository cubeletRepository = mock(CubeletRepository.class);

    @Test
    public void testUpdateImageCubeWithFitsMetadataForUnknownImageCube() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "imageCube.fits";

        File fitsFile = mock(File.class);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(null);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);
        expectedException.expect(FitsImageService.FitsObjectNotFoundException.class);
        expectedException
                .expectMessage("FITS file could not be found using parentId 12345 and fitsFilename imageCube.fits"
                        + " for parent type of OBSERVATION");

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateImageCubeWithFitsMetadata(sbid, imageCubeFilename, fitsFile, ParentType.OBSERVATION, false);

    }

    @Test
    public void testUpdateImageCubeWithFitsMetadataForKnownImageCube() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "imageCube.fits";

        File fitsFile = mock(File.class);
        ImageCube imageCube = mock(ImageCube.class);
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(imageCube.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateImageCubeWithFitsMetadata(sbid, imageCubeFilename, fitsFile, ParentType.OBSERVATION, false);

        ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
        verify(imageCube).setDepositState(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.PROCESSED));

        verify(assembler).populateFitsObject(imageCube, fitsFile);
        verify(imageCubeRepository).save(imageCube);
    }

    @Test
    public void testUpdateImageCubeWithFitsMetadataForRefresh() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "imageCube.fits";

        File fitsFile = mock(File.class);
        ImageCube imageCube = mock(ImageCube.class);
        ImageCubeRepository imageCubeRepository = mock(ImageCubeRepository.class);
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(imageCube.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateImageCubeWithFitsMetadata(sbid, imageCubeFilename, fitsFile, ParentType.OBSERVATION, true);

        verify(imageCube, never()).setDepositState(any(DepositState.class));

        verify(assembler).populateFitsObject(imageCube, fitsFile);
        verify(imageCubeRepository).save(imageCube);
    }
    
    
    @Test
    public void testUpdateSpectrumWithFitsMetadataForUnknownSpectrum() throws Exception
    {
        Integer sbid = 12345;
        String spectrumFilename = "spectrum.fits";

        File fitsFile = mock(File.class);
        when(spectrumRepository.findByObservationSbidAndFilename(sbid, spectrumFilename)).thenReturn(null);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);
        expectedException.expect(FitsImageService.FitsObjectNotFoundException.class);
        expectedException
                .expectMessage("FITS file could not be found using parentId 12345 and fitsFilename spectrum.fits"
                        + " for parent type of OBSERVATION");

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateSpectraWithFitsMetadata(sbid, spectrumFilename, fitsFile, ParentType.OBSERVATION, false);
    }

    @Test
    public void testUpdateSpectrumWithFitsMetadataForKnownImageCube() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "cube.fits";
        String spectrumFilename = "spectrum.fits";

        File parentFolder = tempFolder.newFile("12345");
        ImageCube imageCube = mock(ImageCube.class);
        Spectrum spectrum = mock(Spectrum.class);
        when(spectrum.getFilename()).thenReturn(spectrumFilename);
        List<Spectrum> spectrumList = new ArrayList<>();
        spectrumList.add(spectrum);
        when(imageCube.getSpectra()).thenReturn(spectrumList);
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(spectrum.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateSpectraWithFitsMetadata(sbid, imageCubeFilename, parentFolder, ParentType.OBSERVATION, false);

        ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
        verify(spectrum).setDepositState(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.ENCAPSULATING));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<Spectrum> spectrumCaptor = ArgumentCaptor.forClass(Spectrum.class);
        verify(assembler).populateFitsObject(spectrumCaptor.capture(), fileCaptor.capture());
        assertThat(spectrumCaptor.getValue(), is(spectrum));
        assertThat(fileCaptor.getValue().toString(), endsWith(spectrumFilename));
        verify(spectrumRepository).save(spectrum);
    }

    @Test
    public void testUpdateSpectrumWithFitsMetadataForLevel7() throws Exception
    {
        Integer level7CollectionId = 12345;
        Long dapCollectionId = 999L;
        String spectrumFilename = "spectrum.fits";

        File parentFolder = tempFolder.newFile("12345");
        File parentPath = new File(parentFolder, spectrumFilename); 
        Spectrum spectrum = mock(Spectrum.class);
        when(spectrum.getFilename()).thenReturn(spectrumFilename);
        List<Spectrum> spectrumList = new ArrayList<>();
        spectrumList.add(spectrum);
        Level7Collection level7 = mock(Level7Collection.class);
        when(level7.getId()).thenReturn(new Long(level7CollectionId));
        when(level7.getDapCollectionId()).thenReturn(dapCollectionId);
        when(spectrum.getParent()).thenReturn(level7);
        when(spectrumRepository.findByLevel7CollectionDapCollectionIdAndFilename(level7CollectionId, spectrumFilename))
                .thenReturn(spectrum);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateSpectraWithFitsMetadata(level7CollectionId, spectrumFilename, parentPath,
                        ParentType.LEVEL_7_COLLECTION, false);

        ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
        verify(spectrum).setDepositState(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.ENCAPSULATING));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<Spectrum> spectrumCaptor = ArgumentCaptor.forClass(Spectrum.class);
        verify(assembler).populateFitsObject(spectrumCaptor.capture(), fileCaptor.capture());
        assertThat(spectrumCaptor.getValue(), is(spectrum));
        assertThat(fileCaptor.getValue().toString(), is(parentPath.getPath()));
        verify(spectrumRepository).save(spectrum);
    }

    @Test
    public void testUpdateSpectrumWithFitsMetadataForRefresh() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "cube.fits";
        String spectrumFilename = "spectrum.fits";

        File parentFolder = tempFolder.newFile(String.valueOf(sbid));
        ImageCube imageCube = mock(ImageCube.class);
        Spectrum spectrum = mock(Spectrum.class);
        when(spectrum.getFilename()).thenReturn(spectrumFilename);
        List<Spectrum> spectrumList = new ArrayList<>();
        spectrumList.add(spectrum);
        when(imageCube.getSpectra()).thenReturn(spectrumList);
        
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(spectrum.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);
        //when(spectrumRepository.findByObservationSbidAndFilename(sbid, spectrumFilename)).thenReturn(spectrum);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateSpectraWithFitsMetadata(sbid, imageCubeFilename, parentFolder, ParentType.OBSERVATION, true);

        verify(spectrum, never()).setDepositState(any(DepositState.class));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<Spectrum> spectrumCaptor = ArgumentCaptor.forClass(Spectrum.class);
        verify(assembler).populateFitsObject(spectrumCaptor.capture(), fileCaptor.capture());
        assertThat(spectrumCaptor.getValue(), is(spectrum));
        assertThat(fileCaptor.getValue().toString(), endsWith(spectrumFilename));
        verify(spectrumRepository).save(spectrum);
    }
    
    
    @Test
    public void testUpdateMomentMapWithFitsMetadataForUnknownMomentMap() throws Exception
    {
        Integer sbid = 12345;
        String momentMapFilename = "mom0.fits";

        File fitsFile = mock(File.class);
        when(momentMapRepository.findByObservationSbidAndFilename(sbid, momentMapFilename)).thenReturn(null);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);
        expectedException.expect(FitsImageService.FitsObjectNotFoundException.class);
        expectedException.expectMessage("FITS file could not be found using parentId 12345 and fitsFilename mom0.fits"
                + " for parent type of OBSERVATION");

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateMomentMapsWithFitsMetadata(sbid, momentMapFilename, fitsFile, ParentType.OBSERVATION, false);
    }

    @Test
    public void testUpdateMomentMapWithFitsMetadataForKnownImageCube() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "cube.fits";
        String momentMapFilename = "mom0.fits";

        
        File parentFolder = tempFolder.newFile("12345");
        ImageCube imageCube = mock(ImageCube.class);
        MomentMap momentMap = mock(MomentMap.class);
        when(momentMap.getFilename()).thenReturn(momentMapFilename);
        List<MomentMap> momentMapList = new ArrayList<>();
        momentMapList.add(momentMap);
        when(imageCube.getMomentMaps()).thenReturn(momentMapList);
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(momentMap.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateMomentMapsWithFitsMetadata(sbid, imageCubeFilename, parentFolder, ParentType.OBSERVATION, false);

        ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
        verify(momentMap).setDepositState(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.ENCAPSULATING));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<MomentMap> momentMapCaptor = ArgumentCaptor.forClass(MomentMap.class);
        verify(assembler).populateFitsObject(momentMapCaptor.capture(), fileCaptor.capture());
        assertThat(momentMapCaptor.getValue(), is(momentMap));
        assertThat(fileCaptor.getValue().toString(), endsWith(momentMapFilename));
        verify(momentMapRepository).save(momentMap);
    }

    @Test
    public void testUpdateMomentMapWithFitsMetadataForLevel7() throws Exception
    {
        Integer level7CollectionId = 12345;
        Long dapCollectionId = 999L;
        String momentMapFilename = "mom0.fits";

        File parentFolder = tempFolder.newFile("12345");
        File parentPath = new File(parentFolder, momentMapFilename); 
        MomentMap momentMap = mock(MomentMap.class);
        when(momentMap.getFilename()).thenReturn(momentMapFilename);
        List<MomentMap> momentMapList = new ArrayList<>();
        momentMapList.add(momentMap);
        Level7Collection level7 = mock(Level7Collection.class);
        when(level7.getId()).thenReturn(new Long(level7CollectionId));
        when(level7.getDapCollectionId()).thenReturn(dapCollectionId);
        when(momentMap.getParent()).thenReturn(level7);
        when(momentMapRepository.findByLevel7CollectionDapCollectionIdAndFilename(level7CollectionId,
                momentMapFilename)).thenReturn(momentMap);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateMomentMapsWithFitsMetadata(level7CollectionId, momentMapFilename, parentPath,
                        ParentType.LEVEL_7_COLLECTION, false);

        ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
        verify(momentMap).setDepositState(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.ENCAPSULATING));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<MomentMap> momentMapCaptor = ArgumentCaptor.forClass(MomentMap.class);
        verify(assembler).populateFitsObject(momentMapCaptor.capture(), fileCaptor.capture());
        assertThat(momentMapCaptor.getValue(), is(momentMap));
        assertThat(fileCaptor.getValue().toString(), is(parentPath.getPath()));
        verify(momentMapRepository).save(momentMap);
    }

    @Test
    public void testUpdateMomentMapWithFitsMetadataForRefresh() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "cube.fits";
        String momentMapFilename = "mom0.fits";

        File parentFolder = tempFolder.newFile(String.valueOf(sbid));
        ImageCube imageCube = mock(ImageCube.class);
        MomentMap momentMap = mock(MomentMap.class);
        when(momentMap.getFilename()).thenReturn(momentMapFilename);
        List<MomentMap> momentMapList = new ArrayList<>();
        momentMapList.add(momentMap);
        when(imageCube.getMomentMaps()).thenReturn(momentMapList);
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(momentMap.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateMomentMapsWithFitsMetadata(sbid, imageCubeFilename, parentFolder, ParentType.OBSERVATION, true);

        verify(momentMap, never()).setDepositState(any(DepositState.class));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<MomentMap> momentMapCaptor = ArgumentCaptor.forClass(MomentMap.class);
        verify(assembler).populateFitsObject(momentMapCaptor.capture(), fileCaptor.capture());
        assertThat(momentMapCaptor.getValue(), is(momentMap));
        assertThat(fileCaptor.getValue().toString(), endsWith(momentMapFilename));
        verify(momentMapRepository).save(momentMap);
    }

    @Test
    public void testUpdateCubeletWithFitsMetadataForUnknownImageCube() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "cube.fits";

        File fitsFile = mock(File.class);
        when(momentMapRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(null);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);
        expectedException.expect(FitsImageService.FitsObjectNotFoundException.class);
        expectedException.expectMessage("FITS file could not be found using parentId 12345 and fitsFilename cube.fits"
                + " for parent type of OBSERVATION");

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateMomentMapsWithFitsMetadata(sbid, imageCubeFilename, fitsFile, ParentType.OBSERVATION, false);
    }

    @Test
    public void testUpdateCubeletWithFitsMetadataForKnownImageCube() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "cube.fits";
        String cubeletFilename = "cubelet.fits";

        File parentFolder = tempFolder.newFile(String.valueOf(sbid));
        ImageCube imageCube = mock(ImageCube.class);
        Cubelet cubelet = mock(Cubelet.class);
        when(cubelet.getFilename()).thenReturn(cubeletFilename);
        List<Cubelet> cubeletList = new ArrayList<>();
        cubeletList.add(cubelet);
        when(imageCube.getCubelets()).thenReturn(cubeletList);
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(cubelet.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateCubeletsWithFitsMetadata(sbid, imageCubeFilename, parentFolder, ParentType.OBSERVATION, false);

        ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
        verify(cubelet).setDepositState(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.ENCAPSULATING));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<Cubelet> cubeletCaptor = ArgumentCaptor.forClass(Cubelet.class);
        verify(assembler).populateFitsObject(cubeletCaptor.capture(), fileCaptor.capture());
        assertThat(cubeletCaptor.getValue(), is(cubelet));
        assertThat(fileCaptor.getValue().toString(), endsWith(cubeletFilename));
        verify(cubeletRepository).save(cubelet);
    }

    @Test
    public void testUpdateCubeletWithFitsMetadataForLevel7() throws Exception
    {
        Integer level7CollectionId = 12345;
        Long dapCollectionId = 999L;
        String cubeletFilename = "mom0.fits";

        File parentFolder = tempFolder.newFile("12345");
        File parentPath = new File(parentFolder, cubeletFilename); 
        Cubelet cubelet = mock(Cubelet.class);
        when(cubelet.getFilename()).thenReturn(cubeletFilename);
        List<Cubelet> cubeletList = new ArrayList<>();
        cubeletList.add(cubelet);
        Level7Collection level7 = mock(Level7Collection.class);
        when(level7.getId()).thenReturn(new Long(level7CollectionId));
        when(level7.getDapCollectionId()).thenReturn(dapCollectionId);
        when(cubelet.getParent()).thenReturn(level7);
        when(cubeletRepository.findByLevel7CollectionDapCollectionIdAndFilename(level7CollectionId,
                cubeletFilename)).thenReturn(cubelet);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateCubeletsWithFitsMetadata(level7CollectionId, cubeletFilename, parentPath,
                        ParentType.LEVEL_7_COLLECTION, false);

        ArgumentCaptor<DepositState> queryCaptor = ArgumentCaptor.forClass(DepositState.class);
        verify(cubelet).setDepositState(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getType(), is(DepositState.Type.ENCAPSULATING));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<Cubelet> cubeletCaptor = ArgumentCaptor.forClass(Cubelet.class);
        verify(assembler).populateFitsObject(cubeletCaptor.capture(), fileCaptor.capture());
        assertThat(cubeletCaptor.getValue(), is(cubelet));
        assertThat(fileCaptor.getValue().toString(), is(parentPath.getPath()));
        verify(cubeletRepository).save(cubelet);
    }

    @Test
    public void testUpdateCubeletWithFitsMetadataForRefresh() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "cube.fits";
        String cubeletFilename = "cubelet.fits";

        File parentFolder = tempFolder.newFile(String.valueOf(sbid));
        ImageCube imageCube = mock(ImageCube.class);
        Cubelet cubelet = mock(Cubelet.class);
        when(cubelet.getFilename()).thenReturn(cubeletFilename);
        List<Cubelet> cubeletList = new ArrayList<>();
        cubeletList.add(cubelet);
        when(imageCube.getCubelets()).thenReturn(cubeletList);
        Observation observation = mock(Observation.class);
        when(observation.getSbid()).thenReturn(12345);
        when(cubelet.getParent()).thenReturn(observation);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(imageCube);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);

        new FitsImageService(imageCubeRepository, spectrumRepository, momentMapRepository, cubeletRepository, assembler)
                .updateCubeletsWithFitsMetadata(sbid, imageCubeFilename, parentFolder, ParentType.OBSERVATION, true);

        verify(cubelet, never()).setDepositState(any(DepositState.class));

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<Cubelet> cubeletCaptor = ArgumentCaptor.forClass(Cubelet.class);
        verify(assembler).populateFitsObject(cubeletCaptor.capture(), fileCaptor.capture());
        assertThat(cubeletCaptor.getValue(), is(cubelet));
        assertThat(fileCaptor.getValue().toString(), endsWith(cubeletFilename));
        verify(cubeletRepository).save(cubelet);
    }
}
