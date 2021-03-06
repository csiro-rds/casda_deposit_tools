package au.csiro.casda.datadeposit.fits.assembler;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.hamcrest.junit.ExpectedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.vividsolutions.jts.geom.Polygon;

import au.csiro.TestUtils;
import au.csiro.casda.datadeposit.fits.FitsFileParser;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.FitsImportException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.ProjectCodeMismatchException;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.PGspoly;
import au.csiro.casda.entity.observation.Project;
import au.csiro.casda.entity.observation.Spectrum;
import au.csiro.casda.jobmanager.JavaProcessJobFactory;
import au.csiro.util.AstroConversion;
import au.csiro.util.SpringUtils;

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
 * Verify the function of the FitsImageAssembler class.
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class FitsImageAssemblerTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private String dontCareImageGeometryCommandAndArgsElString =
            TestUtils.getCommandAndArgsElStringForEchoOutput("don't care");

    private String defaultImageGeometryCommandAndArgsElString =
            TestUtils.getCommandAndArgsElStringForFileContentsOutput("src/test/resources/image/good/VAST.SIN.4x4.json");

    private ImageCube imageCube;

    private FitsImageAssembler assembler;

    private static final String HEADER_EXPECTED = "src/test/resources/image/good/header.txt";

    @Before
    public void setUp()
    {
        imageCube = new ImageCube();
        imageCube.setProject(new Project("VAST"));
    }

    @Test
    public void testPopulateFitsObjectFailsForMissingFitsFile() throws Exception
    {
        assembler = createFitsImageAssembler(dontCareImageGeometryCommandAndArgsElString);

        exception.expect(FileNotFoundException.class);
        exception.expectMessage("File passed to FitsFileParser does not exist!");

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "bad", "i_dont_exist"));
    }

    @Test
    public void testPopulateFitsObjectFailsForEmptyFitsFile() throws Exception
    {
        assembler = createFitsImageAssembler(dontCareImageGeometryCommandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectCause(Matchers.any(Throwable.class));

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "bad", "empty"));
    }

    @Test
    public void testPopulateFitsObjectFailsForInvalidFitsFile() throws Exception
    {
        assembler = createFitsImageAssembler(dontCareImageGeometryCommandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectCause(Matchers.any(Throwable.class));

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "bad", "invalid"));
    }

    @Test
    public void testPopulateFitsObjectFailsForProjectMismatch() throws Exception
    {
        imageCube = new ImageCube();
        imageCube.setProject(new Project("AS007"));

        assembler = createFitsImageAssembler(dontCareImageGeometryCommandAndArgsElString);

        exception.expect(ProjectCodeMismatchException.class);
        exception.expectMessage("Expected FITS file to have project code 'AS007' but was 'VAST'");

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "bad", "project.mismatch"));
    }

    @Test
    public void testPopulateFitsObjectFailsForFailingGeometryImport() throws Exception
    {
        String commandAndArgsElString = TestUtils.getCommandAndArgsElStringForEchoOutput("boom", true);

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage(String.format(
                "Could not determine geometry of image cube using command %s Output from command was: boom",
                StringUtils.join(SpringUtils.elStringToArray(commandAndArgsElString), " ")));

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "VAST.SIN.4x4"));
    }

    @Test
    public void testPopulateFitsObjectFailsForBadGeometryImportOutput() throws Exception
    {
        String commandAndArgsElString = TestUtils.getCommandAndArgsElStringForEchoOutput("wibble");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage(String.format(
                "Could not determine geometry of image cube using command %s Could not parse output "
                        + "from command (wibble) into a JSON map.",
                StringUtils.join(SpringUtils.elStringToArray(commandAndArgsElString), " ")));

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "VAST.SIN.4x4"));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersNoData() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.basic"));

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getSResolution(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.0));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertNull(imageCube.getRestFrequency());
        assertThat(imageCube.getTExptime(), equalTo(0.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersTargetName1() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.target_name.1"));

        assertThat(imageCube.getObjectName(), is(equalTo("foo")));
        assertThat(imageCube.getSResolution(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.0));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertNull(imageCube.getRestFrequency());
        assertThat(imageCube.getTExptime(), equalTo(0.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersTargetName2() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.target_name.2"));

        assertThat(imageCube.getObjectName(), is(equalTo("bar")));
        assertThat(imageCube.getSResolution(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.0));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getRestFrequency());
        assertNull(imageCube.getTMax());
        assertThat(imageCube.getTExptime(), equalTo(0.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersSResolution1() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.s_resolution.1"));

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getSResolution(), equalTo(10.01));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.01));
        assertThat(imageCube.getSResolutionMax(), equalTo(10.01));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertNull(imageCube.getRestFrequency());
        assertThat(imageCube.getTExptime(), equalTo(0.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersSResolution2() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.s_resolution.2"));

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getSResolution(), equalTo(9.123));
        assertThat(imageCube.getSResolutionMin(), equalTo(8.456));
        assertThat(imageCube.getSResolutionMax(), equalTo(9.123));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertNull(imageCube.getRestFrequency());
        assertThat(imageCube.getTExptime(), equalTo(0.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersTMinMax1() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.t_min_max.1"));

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getSResolution(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.0));
        assertThat(imageCube.getTMin(), equalTo(23.456));
        assertThat(imageCube.getTMax(), equalTo(111.11));
        assertThat(imageCube.getTExptime(), equalTo(0.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersTMinMax2() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.t_min_max.2"));

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getSResolution(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.0));
        assertThat(imageCube.getTMin(), equalTo(8.456));
        assertThat(imageCube.getTMax(), equalTo(9.123));
        assertThat(imageCube.getTExptime(), equalTo(0.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersIntegrationTime1() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.integration_time.1"));

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getSResolution(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.0));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertNull(imageCube.getRestFrequency());
        assertThat(imageCube.getTExptime(), equalTo(1230.0));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersIntegrationTime2() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.integration_time.2"));

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getSResolution(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.0));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.0));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertNull(imageCube.getRestFrequency());
        assertThat(imageCube.getTExptime(), equalTo(1.0001));
    }

    @Test
    public void testPopulateFitsObjectFitsHeadersAll() throws Exception
    {
        assembler = createFitsImageAssembler(defaultImageGeometryCommandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        // note this file contains new line delimiters this is not normal, but does not interfere with this test
        // as we just want to see we get back exactly what is in the file.
        File result = new File(HEADER_EXPECTED);
        String header = FileUtils.readFileToString(result, "utf-8");

        assertEquals("Image cube header does not match what was expected.", header, imageCube.getHeader());

        assertThat(imageCube.getObjectName(), is(equalTo("bar")));
        assertThat(imageCube.getSResolution(), equalTo(10.01));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.01));
        assertThat(imageCube.getSResolutionMax(), equalTo(10.01));
        assertThat(imageCube.getTMin(), equalTo(23.456));
        assertThat(imageCube.getTMax(), equalTo(111.11));
        assertThat(imageCube.getTExptime(), equalTo(1.0001));
        assertThat(imageCube.getRestFrequency(), equalTo(1.420405745510E+09));  	
    }

    @Test
    public void testPopulateFitsObjectImageGeometryEmpty() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.empty");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCells1() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.cells.1");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), equalTo(4 * 2.7777777777780e-03 * 4 * 2.7777777777780e-03));
        assertThat(imageCube.getNoOfPixels(), equalTo(16L));
        assertThat(imageCube.getCellSize(), equalTo(2.7777777777780e-03 * 2.7777777777780e-03));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCells2() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.cells.2");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), equalTo((5l * 101.1) * (7l * 11.01)));
        assertThat(imageCube.getNoOfPixels(), equalTo(5l * 7l));
        assertThat(imageCube.getCellSize(), equalTo(101.1 * 11.01));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryFreq1() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.freq.1");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(equalTo(1L)));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), equalTo(AstroConversion.frequencyToWavelength(1.2700000005000e+09)));
        assertThat(imageCube.getEmMax(), equalTo(AstroConversion.frequencyToWavelength(1.2699999995000e+09)));
        assertThat(imageCube.getEmResolution(), equalTo(1.8587170691475308E-10));
        assertThat(imageCube.getChannelWidth(), equalTo(1.0000000000000e+00));
        assertThat(imageCube.getEmResPower(), equalTo(1.2700000000000e+09 / 1.0000000000000e+00));
        assertThat(imageCube.getCentreFrequency(), equalTo(1.2700000000000e+09));
        assertThat(imageCube.getNoOfChannels(), equalTo(1));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryFreq2() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.freq.2");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(equalTo(100L)));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), equalTo(AstroConversion.frequencyToWavelength(1500.0)));
        assertThat(imageCube.getEmMax(), equalTo(AstroConversion.frequencyToWavelength(500.0)));
        assertThat(imageCube.getEmResolution(), equalTo(3.9972327733333327e3));
        assertThat(imageCube.getChannelWidth(), equalTo(10.0));
        assertThat(imageCube.getEmResPower(), equalTo(1000.0 / 10.0));
        assertThat(imageCube.getCentreFrequency(), equalTo(1000.0));
        assertThat(imageCube.getNoOfChannels(), equalTo(100));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokesMinInvalid() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "bad", "image_geometry.stokes.min.invalid");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage("STOKES axis has invalid min value: 5.1000000000000e-01");
        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokesMinUnknown() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "bad", "image_geometry.stokes.min.unknown");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage(
                "STOKES axis has invalid min value: 5.5000000000000e+00 " + "(does not map to any valid value)");
        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokesPixelSizeInvalid() throws Exception
    {
        String commandAndArgsElString = getImageGeometryCommandAndArgsElStringForTest("image", "bad",
                "image_geometry.stokes.pixelSize.invalid");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage("STOKES axis has invalid pixelSize: 1.1000000000000e+00");
        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokesMaxInvalid() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "bad", "image_geometry.stokes.max.invalid");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage("STOKES axis has invalid min (5.0000000000000e-01), pixelSize "
                + "(2.0000000000000e+00), and numPixels (3) values");
        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokesMissing() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.stokes.missing");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //
        assertThat(imageCube.getSRegion().getCoordinates()[0].x, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[0].y, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[1].x, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[1].y, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[2].x, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[2].y, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[3].x, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[3].y, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[0], equalTo(imageCube.getSRegion().getCoordinates()[4]));
        assertThat(imageCube.getSRegion().getCoordinates().length, is(5));
        assertThat(imageCube.getSRegion().getClass(), is(Polygon.class));

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), equalTo("//"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokes1() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.stokes.1");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(equalTo(1L)));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //
        assertThat(imageCube.getSRegion().getCoordinates()[0].x, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[0].y, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[1].x, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[1].y, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[2].x, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[2].y, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[3].x, equalTo(0.0));
        assertThat(imageCube.getSRegion().getCoordinates()[3].y, equalTo(1.0));
        assertThat(imageCube.getSRegion().getCoordinates()[0], equalTo(imageCube.getSRegion().getCoordinates()[4]));
        assertThat(imageCube.getSRegion().getCoordinates().length, is(5));
        assertThat(imageCube.getSRegion().getClass(), is(Polygon.class));

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), equalTo("/I/"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokes2() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.stokes.2");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(equalTo(2L)));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), equalTo("/Q/V/"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryStokes3() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.stokes.3");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(equalTo(4L)));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), equalTo("/I/Q/U/V/"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryDimensions() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.stokes.3");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        File file = new File("src/test/resources/image/good/image_geometry.stokes.3.dimensions.txt");
        String dimensions = FileUtils.readFileToString(file);

        assertThat(imageCube.getDimensions(), equalTo(dimensions));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCornersTooFew() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "bad", "image_geometry.corners.too.few");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage("Expected 4 corners but got 3");
        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCornersTooMany() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "bad", "image_geometry.corners.too.many");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        exception.expect(FitsImportException.class);
        exception.expectMessage("Expected 4 corners but got 5");
        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCorners1() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.corners.1");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(1.9462668233330e+02), Math.toRadians(-4.9424414571951e+01), //
                                Math.toRadians(1.9460961021246e+02), Math.toRadians(-4.9425394540871e+01), //
                                Math.toRadians(1.9460799427710e+02), Math.toRadians(-4.9414296596342e+01), //
                                Math.toRadians(1.9462506256805e+02), Math.toRadians(-4.9413316864524e+01)))); //
        assertThat(imageCube.getSRegion().getCoordinates()[0].x, closeTo(1.9462668233330e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0].y, closeTo(-4.9424414571951e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].x, closeTo(1.9460961021246e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].y, closeTo(-4.9425394540871e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].x, closeTo(1.9460799427710e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].y, closeTo(-4.9414296596342e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].x, closeTo(1.9462506256805e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].y, closeTo(-4.9413316864524e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0], equalTo(imageCube.getSRegion().getCoordinates()[4]));
        assertThat(imageCube.getSRegion().getCoordinates().length, is(5));
        assertThat(imageCube.getSRegion().getClass(), is(Polygon.class));

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCorners2() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.corners.2");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(-5.0), Math.toRadians(-5.0), //
                                Math.toRadians(7.0), Math.toRadians(-5.0), //
                                Math.toRadians(7.0), Math.toRadians(12.0), //
                                Math.toRadians(-5.0), Math.toRadians(12.0)))); //
        assertThat(imageCube.getSRegion().getCoordinates()[0].x, equalTo(-5.0));
        assertThat(imageCube.getSRegion().getCoordinates()[0].y, equalTo(-5.0));
        assertThat(imageCube.getSRegion().getCoordinates()[1].x, equalTo(7.0));
        assertThat(imageCube.getSRegion().getCoordinates()[1].y, equalTo(-5.0));
        assertThat(imageCube.getSRegion().getCoordinates()[2].x, equalTo(7.0));
        assertThat(imageCube.getSRegion().getCoordinates()[2].y, equalTo(12.0));
        assertThat(imageCube.getSRegion().getCoordinates()[3].x, equalTo(-5.0));
        assertThat(imageCube.getSRegion().getCoordinates()[3].y, equalTo(12.0));
        assertThat(imageCube.getSRegion().getCoordinates()[0], equalTo(imageCube.getSRegion().getCoordinates()[4]));
        assertThat(imageCube.getSRegion().getCoordinates().length, is(5));
        assertThat(imageCube.getSRegion().getClass(), is(Polygon.class));

        assertThat(imageCube.getRaDeg(), is(0.5));
        assertThat(imageCube.getDecDeg(), is(0.5));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCentre1() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.centre.1");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), equalTo(1.9461733726797e+02));
        assertThat(imageCube.getDecDeg(), equalTo(-4.9419355920530e+01));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectImageGeometryCentre2() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("image", "good", "image_geometry.centre.2");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "headers.all"));

        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getCellSize(), is(nullValue()));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(0.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(0.0), //
                                Math.toRadians(1.0), Math.toRadians(1.0), //
                                Math.toRadians(0.0), Math.toRadians(1.0)))); //

        assertThat(imageCube.getRaDeg(), is(180.0));
        assertThat(imageCube.getDecDeg(), is(-90.0));

        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmResolution(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));

        assertThat(imageCube.getStokesParameters(), is(equalTo("//")));
    }

    @Test
    public void testPopulateFitsObjectForVASTSampleSet() throws Exception
    {
        String commandAndArgsElString = getImageGeometryCommandAndArgsElStringForTest("image", "good", "VAST.SIN.4x4");

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(imageCube, getTestFitsFile("image", "good", "VAST.SIN.4x4"));

        assertThat(imageCube.getObjectName(), is(equalTo("")));
        assertThat(imageCube.getSResolution(), equalTo(0.01243718786725));
        assertThat(imageCube.getSResolutionMin(), equalTo(0.009785784768003));
        assertThat(imageCube.getSResolutionMax(), equalTo(0.01243718786725));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertThat(imageCube.getTExptime(), equalTo(0.0));

        assertThat(imageCube.getSFov(), is(equalTo(1.2345679012347653E-4)));
        assertThat(imageCube.getNoOfPixels(), is(equalTo(16L)));
        assertThat(imageCube.getCellSize(), is(equalTo(7.716049382717283E-6)));

        assertThat(imageCube.getSRegionPoly().getValue(),
                equalTo( //
                        String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                                Math.toRadians(1.9462668233330e+02), Math.toRadians(-4.9424414571951e+01), //
                                Math.toRadians(1.9460961021246e+02), Math.toRadians(-4.9425394540871e+01), //
                                Math.toRadians(1.9460799427710e+02), Math.toRadians(-4.9414296596342e+01), //
                                Math.toRadians(1.9462506256805e+02), Math.toRadians(-4.9413316864524e+01)))); //
        assertThat(imageCube.getSRegion().getCoordinates()[0].x, closeTo(1.9462668233330e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0].y, closeTo(-4.9424414571951e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].x, closeTo(1.9460961021246e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].y, closeTo(-4.9425394540871e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].x, closeTo(1.9460799427710e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].y, closeTo(-4.9414296596342e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].x, closeTo(1.9462506256805e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].y, closeTo(-4.9413316864524e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0], equalTo(imageCube.getSRegion().getCoordinates()[4]));
        assertThat(imageCube.getSRegion().getCoordinates().length, is(5));
        assertThat(imageCube.getSRegion().getClass(), is(Polygon.class));

        assertThat(imageCube.getRaDeg(), is(1.9461733726797e+02));
        assertThat(imageCube.getDecDeg(), is(-4.9419355920530e+01));

        assertThat(imageCube.getEmMin(), is(equalTo(0.23605705345037123)));
        assertThat(imageCube.getEmMax(), is(equalTo(0.23605705363624294)));
        assertThat(imageCube.getEmResolution(), is(equalTo(1.8587170691475308E-10)));
        assertThat(imageCube.getChannelWidth(), is(equalTo(1.0)));
        assertThat(imageCube.getEmResPower(), is(equalTo(1.27E9)));
        assertThat(imageCube.getCentreFrequency(), is(equalTo(1.27E9)));
        assertThat(imageCube.getNoOfChannels(), is(equalTo(1)));

        assertThat(imageCube.getStokesParameters(), is(equalTo("/I/")));
    }

    @Test
    public void testPopulateSpectrum() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("spectra", "good", "beta.image2.001.sir");

        Spectrum spectrum = new Spectrum();
        spectrum.setProject(new Project("AS031"));

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(spectrum, getTestFitsFile("spectra", "good", "beta.image2.001.sir"));

        assertThat(spectrum.getObjectName(), is(equalTo("")));
        assertNull(spectrum.getTMin());
        assertNull(spectrum.getTMax());
        assertThat(spectrum.getTExptime(), equalTo(42000.0));

        assertThat(spectrum.getRaDeg(), is(344.15127636462));
        assertThat(spectrum.getDecDeg(), is(-36.250662898927));

        assertThat(spectrum.getEmMin(), nullValue());
        assertThat(spectrum.getEmMax(), nullValue());
        assertThat(spectrum.getEmResolution(), is(nullValue()));
        assertThat(spectrum.getChannelWidth(), nullValue());
        assertThat(spectrum.getCentreFrequency(), nullValue());
        assertThat(spectrum.getNoOfChannels(), nullValue());
        PGspoly temp = spectrum.getSRegionPoly();
        String test = temp.getValue();

        String answer = String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                Math.toRadians(3.4415385005428993e+02), Math.toRadians(-3.625275411271e+01), //
                Math.toRadians(3.4414868312951e+02), Math.toRadians(-3.6252738285371e+01), //
                Math.toRadians(3.4414870281174e+02), Math.toRadians(-3.6248571629554e+01), //
                Math.toRadians(3.44153869461e+02), Math.toRadians(-3.6248587456052e+01));
        assertThat(test, equalTo(answer)); //
        assertThat(spectrum.getSRegion().getCoordinates()[0].x, closeTo(3.4415385005428993e+02, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[0].y, closeTo(-3.625275411271e+01, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[1].x, closeTo(3.4414868312951e+02, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[1].y, closeTo(-3.6252738285371e+01, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[2].x, closeTo(3.4414870281174e+02, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[2].y, closeTo(-3.6248571629554e+01, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[3].x, closeTo(3.44153869461e+02, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[3].y, closeTo(-3.6248587456052e+01, 10e-10));
        assertThat(spectrum.getSRegion().getCoordinates()[0], equalTo(spectrum.getSRegion().getCoordinates()[4]));
        assertThat(spectrum.getSRegion().getCoordinates().length, is(5));
        assertThat(spectrum.getSRegion().getClass(), is(Polygon.class));

        assertThat(spectrum.getStokesParameters(), is(equalTo("/I/")));
    }

    @Test
    public void testPopulateMomentMap() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("momentmap", "good", "mom0_1");

        MomentMap momentMap = new MomentMap();
        momentMap.setProject(new Project("AS031"));

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(momentMap, getTestFitsFile("momentmap", "good", "mom0_1"));

        assertThat(momentMap.getObjectName(), is(equalTo("")));
        assertNull(momentMap.getTMin());
        assertNull(momentMap.getTMax());
        assertThat(momentMap.getTExptime(), equalTo(0.0));

        assertThat(momentMap.getRaDeg(), is(344.15354462644));
        assertThat(momentMap.getDecDeg(), is(-36.249945278860));

        assertThat(momentMap.getEmMin(), is(0.19133313747811379));
        assertThat(momentMap.getEmMax(), is(0.23664192421840471));
        assertThat(momentMap.getEmResolution(), is(equalTo(0.04530878674029093)));
        assertThat(momentMap.getChannelWidth(), is(3e8));
        assertThat(momentMap.getCentreFrequency(), is(1.4168611404770e+09));
        assertThat(momentMap.getNoOfChannels(), is(1));
        PGspoly temp = momentMap.getSRegionPoly();
        String test = temp.getValue();

        String answer = String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                Math.toRadians(3.4423091564739e+02), Math.toRadians(-3.6285572760070e+01), //
                Math.toRadians(3.4407584316182e+02), Math.toRadians(-3.6285100677546e+01), //
                Math.toRadians(3.4407624360392e+02), Math.toRadians(-3.6214267743621e+01), //
                Math.toRadians(3.4423117558664e+02), Math.toRadians(-3.6214739400192e+01));
        assertThat(test, equalTo(answer)); //
        assertThat(momentMap.getSRegion().getCoordinates()[0].x, closeTo(3.4423091564739e+02, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[0].y, closeTo(-3.6285572760070e+01, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[1].x, closeTo(3.4407584316182e+02, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[1].y, closeTo(-3.6285100677546e+01, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[2].x, closeTo(3.4407624360392e+02, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[2].y, closeTo(-3.6214267743621e+01, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[3].x, closeTo(3.4423117558664e+02, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[3].y, closeTo(-3.6214739400192e+01, 10e-10));
        assertThat(momentMap.getSRegion().getCoordinates()[0], equalTo(momentMap.getSRegion().getCoordinates()[4]));
        assertThat(momentMap.getSRegion().getCoordinates().length, is(5));
        assertThat(momentMap.getSRegion().getClass(), is(Polygon.class));

        assertThat(momentMap.getStokesParameters(), is(equalTo("/I/")));
    }
    
    @Test
    public void testPopulateCubelet() throws Exception
    {
        String commandAndArgsElString =
                getImageGeometryCommandAndArgsElStringForTest("cubelet", "good", "cube_01");

        Cubelet cubelet = new Cubelet();
        cubelet.setProject(new Project("AS031"));

        assembler = createFitsImageAssembler(commandAndArgsElString);

        assembler.populateFitsObject(cubelet, getTestFitsFile("cubelet", "good", "cube_01"));

        assertThat(cubelet.getObjectName(), is(equalTo("")));
        assertNull(cubelet.getTMin());
        assertNull(cubelet.getTMax());
        assertThat(cubelet.getTExptime(), equalTo(0.0));

        assertThat(cubelet.getRaDeg(), is(344.15354462644));
        assertThat(cubelet.getDecDeg(), is(-36.249945278860));

        assertThat(cubelet.getEmMin(), is(0.19133313747811379));
        assertThat(cubelet.getEmMax(), is(0.23664192421840471));
        assertThat(cubelet.getEmResolution(), is(equalTo(0.04530878674029093)));
        assertThat(cubelet.getChannelWidth(), is(3e8));
        assertThat(cubelet.getCentreFrequency(), is(1.4168611404770e+09));
        assertThat(cubelet.getNoOfChannels(), is(1));
        PGspoly temp = cubelet.getSRegionPoly();
        String test = temp.getValue();

        String answer = String.format("{(%s , %s),(%s , %s),(%s , %s),(%s , %s)}", //
                Math.toRadians(3.4423091564739e+02), Math.toRadians(-3.6285572760070e+01), //
                Math.toRadians(3.4407584316182e+02), Math.toRadians(-3.6285100677546e+01), //
                Math.toRadians(3.4407624360392e+02), Math.toRadians(-3.6214267743621e+01), //
                Math.toRadians(3.4423117558664e+02), Math.toRadians(-3.6214739400192e+01));
        assertThat(test, equalTo(answer)); //
        assertThat(cubelet.getSRegion().getCoordinates()[0].x, closeTo(3.4423091564739e+02, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[0].y, closeTo(-3.6285572760070e+01, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[1].x, closeTo(3.4407584316182e+02, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[1].y, closeTo(-3.6285100677546e+01, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[2].x, closeTo(3.4407624360392e+02, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[2].y, closeTo(-3.6214267743621e+01, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[3].x, closeTo(3.4423117558664e+02, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[3].y, closeTo(-3.6214739400192e+01, 10e-10));
        assertThat(cubelet.getSRegion().getCoordinates()[0], equalTo(cubelet.getSRegion().getCoordinates()[4]));
        assertThat(cubelet.getSRegion().getCoordinates().length, is(5));
        assertThat(cubelet.getSRegion().getClass(), is(Polygon.class));

        assertThat(cubelet.getStokesParameters(), is(equalTo("/I/")));
    }

    private FitsImageAssembler createFitsImageAssembler(String imageGeometryCommandAndArgsElString)
    {
        return new FitsImageAssembler(imageGeometryCommandAndArgsElString, new FitsFileParser(),
                new JavaProcessJobFactory());
    }

    private File getTestFitsFile(String fitsType, String dataKind, String testName)
    {
        return new File("src/test/resources/" + fitsType + "/" + dataKind + "/" + testName + ".fits");
    }

    private String getImageGeometryCommandAndArgsElStringForTest(String fitsType, String dataKind, String testName)
    {
        return TestUtils.getCommandAndArgsElStringForFileContentsOutput(
                "src/test/resources/" + fitsType + "/" + dataKind + "/" + testName + ".json");

    }
}
