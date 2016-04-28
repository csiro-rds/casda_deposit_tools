package au.csiro.casda.datadeposit.fits.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import au.csiro.casda.datadeposit.fits.assembler.FitsImageAssembler;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Observation;

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

    @Test
    public void testUpdateImageCubeWithFitsMetadataForUnknownImageCube() throws Exception
    {
        Integer sbid = 12345;
        String imageCubeFilename = "imageCube.fits";

        File fitsFile = mock(File.class);
        ImageCubeRepository imageCubeRepository = mock(ImageCubeRepository.class);
        when(imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename)).thenReturn(null);

        FitsImageAssembler assembler = mock(FitsImageAssembler.class);
        expectedException.expect(FitsImageService.ImageCubeNotFoundException.class);
        expectedException
                .expectMessage("Image Cube could not be found using sbid 12345 and imageCubeFilename imageCube.fits");

        new FitsImageService(imageCubeRepository, assembler).updateImageCubeWithFitsMetadata(sbid, imageCubeFilename,
                fitsFile);
    }

    @Test
    public void testUpdateImageCubeWithFitsMetadataForKnownImageCube() throws Exception
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

        new FitsImageService(imageCubeRepository, assembler).updateImageCubeWithFitsMetadata(sbid, imageCubeFilename,
                fitsFile);

        verify(assembler).populateImageCube(imageCube, fitsFile);
        verify(imageCubeRepository).save(imageCube);
    }
}
