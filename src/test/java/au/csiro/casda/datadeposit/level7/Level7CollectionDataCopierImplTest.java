package au.csiro.casda.datadeposit.level7;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.when;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.service.InlineScriptService;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.Spectrum;
import au.csiro.casda.entity.observation.Thumbnail;
import au.csiro.casda.jobmanager.ProcessJobBuilder.ProcessJobFactory;

/*
 * #%L
 * CSIRO Data Access Portal
 * %%
 * Copyright (C) 2010 - 2017 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * Test class for Level7CollectionDataCopierImpl
 * <p>
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
public class Level7CollectionDataCopierImplTest
{

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private Level7CollectionRepository level7CollectionRepository;

    @Mock
    private ProcessJobFactory processJobFactory;

    @Mock
    private InlineScriptService inlineScriptService;

    private Level7CollectionDataCopierImpl dataCopier;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        dataCopier = new Level7CollectionDataCopierImpl(level7CollectionRepository, processJobFactory, "{}", "",
                inlineScriptService);

    }
    @Test
    public void testGetEncapsulationFile()
    {
        Map<String, EncapsulationFile> encapsulationMap = new HashMap<>();
        Level7Collection level7Collection = new Level7Collection(1234);
        EncapsulationFile spectrumEncpas =
                dataCopier.getEncapsulationFile("spectra/foo.fits", encapsulationMap, level7Collection, "spectrum", 0);
        assertThat(spectrumEncpas.getFilename(), is("encaps-spectrum-1.tar"));
        assertThat(encapsulationMap, hasEntry("spectrum|spectra|fits", spectrumEncpas));
        EncapsulationFile spectrumEncpas2 =
                dataCopier.getEncapsulationFile("spectra/bar.fits", encapsulationMap, level7Collection, "spectrum", 0);
        assertThat(spectrumEncpas2.getFilename(), is("encaps-spectrum-1.tar"));
        assertThat(encapsulationMap, hasEntry("spectrum|spectra|fits", spectrumEncpas2));
        assertThat(encapsulationMap.size(), is(1));

        EncapsulationFile thumbEncpas =
                dataCopier.getEncapsulationFile("spectra/foo.png", encapsulationMap, level7Collection, "thumb", 0);
        assertThat(thumbEncpas.getFilename(), is("encaps-thumb-2.tar"));
        assertThat(encapsulationMap, hasEntry("thumb|spectra|png", thumbEncpas));

        thumbEncpas =
                dataCopier.getEncapsulationFile("spectra/foo.jpeg", encapsulationMap, level7Collection, "thumb", 0);
        assertThat(thumbEncpas.getFilename(), is("encaps-thumb-3.tar"));
        assertThat(encapsulationMap, hasEntry("thumb|spectra|jpeg", thumbEncpas));

        EncapsulationFile thumbEncpasMM =
                dataCopier.getEncapsulationFile("momentmap/foo.png", encapsulationMap, level7Collection, "thumb", 2);
        assertThat(thumbEncpasMM.getFilename(), is("encaps-thumb-6.tar"));
        assertThat(encapsulationMap, hasEntry("thumb|momentmap|png", thumbEncpasMM));
    }

    @Test
    public void testCreateImageCubesForFiles() throws Exception
    {
        Level7Collection level7Collection = new Level7Collection(1234);
        String imageCubeFolderName = "imageCubes";
        File icFolder = tempFolder.newFolder(imageCubeFolderName);
        createFileOfSize(icFolder, "ic1.fits", 3200);
        createFileOfSize(icFolder, "ic1.png",800);
        createFileOfSize(icFolder, "ic2.fits", 1024);
        when(inlineScriptService.callScriptInline(anyVararg())).thenReturn("1 2 3");
        

        dataCopier.createImageCubesForFiles(level7Collection, icFolder.getParent(), imageCubeFolderName);
        List<ImageCube> imageCubes = level7Collection.getImageCubes();
        assertThat(imageCubes.size(), is(2));
        for (ImageCube imageCube : imageCubes)
        {
            assertThat(imageCube.getFormat(), is("fits"));
            if (imageCube.getFilename().endsWith("ic1.fits"))
            {
                assertThat(imageCube.getFilesize(), is(4L));
                Thumbnail thumb = imageCube.getLargeThumbnail();
                assertThat(thumb, is(not(nullValue())));
                assertThat(imageCube.getSmallThumbnail(), is(thumb));
                assertThat(thumb.getFilesize(), is(1L));
            }
            else if (imageCube.getFilename().endsWith("ic2.fits"))
            {
                assertThat(imageCube.getFilesize(), is(1L));
                assertThat(imageCube.getSmallThumbnail(), is(nullValue()));
                assertThat(imageCube.getLargeThumbnail(), is(nullValue()));
            }
            else
            {
                fail("Unexpected image cube name; " + imageCube.getFilename());
            }
        }
    }

    @Test
    public void testCreateSpectraForFiles() throws Exception
    {
        Level7Collection level7Collection = new Level7Collection(1234);
        String spectraFolderName = "spectra";
        File specFolder = tempFolder.newFolder(spectraFolderName);
        createFileOfSize(specFolder, "spec1.fits", 2089);
        File thumb1 = createFileOfSize(specFolder, "spec1.png",800);
        File thumb2 = createFileOfSize(specFolder, "unmatched.png",800);
        createFileOfSize(specFolder, "spec2.fits", 1024);
        when(inlineScriptService.callScriptInline(anyVararg())).thenReturn("1 2 3");
        

        dataCopier.createSpectraForFiles(level7Collection, specFolder.getParent(), spectraFolderName, 0);
        List<Spectrum> spectra = level7Collection.getSpectra();
        assertThat(spectra.size(), is(2));
        for (Spectrum spectrum : spectra)
        {
            assertThat(spectrum.getFormat(), is("fits"));
            if (spectrum.getFilename().endsWith("spec1.fits"))
            {
                assertThat(spectrum.getFilesize(), is(3L));
                Thumbnail thumb = spectrum.getThumbnail();
                assertThat(thumb, is(not(nullValue())));
                assertThat(thumb.getFilesize(), is(1L));
            }
            else if (spectrum.getFilename().endsWith("spec2.fits"))
            {
                assertThat(spectrum.getFilesize(), is(1L));
                assertThat(spectrum.getThumbnail(), is(nullValue()));
            }
            else
            {
                fail("Unexpected spectrum name; " + spectrum.getFilename());
            }
        }
        
        // Check the thumbnail deletion
        assertThat("Used thumbnail image retained", thumb1.exists(),  is(true));
        assertThat("Unused thumbnail image deleted", thumb2.exists(),  is(false));
    }

    @Test
    public void testCreateMomentMapsForFiles() throws Exception
    {
        Level7Collection level7Collection = new Level7Collection(1234);
        String momentMapsFolderName = "momentMaps";
        File momFolder = tempFolder.newFolder(momentMapsFolderName);
        createFileOfSize(momFolder, "mom1.fits", 2089);
        File thumb1 = createFileOfSize(momFolder, "mom1.png",800);
        File thumb2 = createFileOfSize(momFolder, "unmatched.png",800);
        File thumb3 = createFileOfSize(momFolder, "mom1.jpeg",800);
        createFileOfSize(momFolder, "mom2.fits", 1024);
        createFileOfSize(momFolder, "mom2.fits.checksum", 1);
        when(inlineScriptService.callScriptInline(anyVararg())).thenReturn("1 2 3");

        dataCopier.createMomentMapsForFiles(level7Collection, momFolder.getParent(), momentMapsFolderName, 0);
        List<MomentMap> momentMaps = level7Collection.getMomentMaps();
        assertThat(momentMaps.size(), is(2));
        for (MomentMap momentMap : momentMaps)
        {
            assertThat(momentMap.getFormat(), is("fits"));
            if (momentMap.getFilename().endsWith("mom1.fits"))
            {
                assertThat(momentMap.getFilesize(), is(3L));
                Thumbnail thumb = momentMap.getThumbnail();
                assertThat(thumb, is(not(nullValue())));
                assertThat(thumb.getFilesize(), is(1L));
            }
            else if (momentMap.getFilename().endsWith("mom2.fits"))
            {
                assertThat(momentMap.getFilesize(), is(1L));
                assertThat(momentMap.getThumbnail(), is(nullValue()));
            }
            else
            {
                fail("Unexpected moment map name; " + momentMap.getFilename());
            }
        }
        
        // Check the thumbnail deletion
        assertThat("Used thumbnail image retained", thumb1.exists(),  is(true));
        assertThat("Unused thumbnail image deleted", thumb2.exists(),  is(false));
        assertThat("Unused thumbnail image deleted", thumb3.exists(),  is(false));
    }
    
    @Test
    public void testCreateCubeletsForFiles() throws Exception
    {
        Level7Collection level7Collection = new Level7Collection(1234);
        String cubeletsFolderName = "cubelets";
        File cubeletsFolder = tempFolder.newFolder(cubeletsFolderName);
        createFileOfSize(cubeletsFolder, "cube1.fits", 2089);
        File thumb1 = createFileOfSize(cubeletsFolder, "cube1.png",800);
        File thumb2 = createFileOfSize(cubeletsFolder, "unmatched.png",800);
        File thumb3 = createFileOfSize(cubeletsFolder, "cube1.jpeg",800);
        createFileOfSize(cubeletsFolder, "cube2.fits", 1024);
        createFileOfSize(cubeletsFolder, "cube2.fits.checksum", 1);
        when(inlineScriptService.callScriptInline(anyVararg())).thenReturn("1 2 3");

        dataCopier.createCubeletsForFiles(level7Collection, cubeletsFolder.getParent(), cubeletsFolderName, 0);
        List<Cubelet> cubelets = level7Collection.getCubelets();
        assertThat(cubelets.size(), is(2));
        for (Cubelet cubelet : cubelets)
        {
            assertThat(cubelet.getFormat(), is("fits"));
            if (cubelet.getFilename().endsWith("cube1.fits"))
            {
                assertThat(cubelet.getFilesize(), is(3L));
                Thumbnail thumb = cubelet.getThumbnail();
                assertThat(thumb, is(not(nullValue())));
                assertThat(thumb.getFilesize(), is(1L));
            }
            else if (cubelet.getFilename().endsWith("cube2.fits"))
            {
                assertThat(cubelet.getFilesize(), is(1L));
                assertThat(cubelet.getThumbnail(), is(nullValue()));
            }
            else
            {
                fail("Unexpected cubelet name; " + cubelet.getFilename());
            }
        }
        
        // Check the thumbnail deletion
        assertThat("Used thumbnail image retained", thumb1.exists(),  is(true));
        assertThat("Unused thumbnail image deleted", thumb2.exists(),  is(false));
        assertThat("Unused thumbnail image deleted", thumb3.exists(),  is(false));
    }
    
    private File createFileOfSize(File folder, String name, int size) throws IOException
    {
        File file = new File(folder, name);
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        f.setLength(size);
        f.close();
        return file;
    }
}
