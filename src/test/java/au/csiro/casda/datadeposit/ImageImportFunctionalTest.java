package au.csiro.casda.datadeposit;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;

import au.csiro.casda.datadeposit.fits.FitsCommandLineImporter;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.PGspoly;
import au.csiro.casda.entity.observation.Spectrum;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

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
 * Functional test for image cube data import.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class ImageImportFunctionalTest extends FunctionalTestBase
{
    private FitsCommandLineImporter fitsCommandLineImporter;

    public ImageImportFunctionalTest() throws Exception
    {
        super();
    }

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        observationCommandLineImporter = context.getBeanFactory().createBean(TestObservationCommandLineImporter.class);
        fitsCommandLineImporter = context.getBeanFactory().createBean(FitsCommandLineImporter.class);
    }

    private Object deserializeBytes(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bytesIn);
        Object obj = ois.readObject();
        bytesIn.close();
        ois.close();
        return obj;
    }

    /**
     * H2 stores the pgspoly as a Hex encoded, serialised object. This method will deserialise the string to a PGspoly.
     * 
     * @param value
     *            the H2 hex encoded value of the serialised pgspoly object
     * @return
     */
    public PGspoly deserializePGspolyForH2(String value) throws Exception
    {
        // the value is a hex string, so decode it
        char[] hexSerialised = value.toCharArray();
        byte[] plainSerialised = Hex.decodeHex(hexSerialised);
        // deserialise the decoded byte array
        PGspoly pgspolyDeserialised = (PGspoly) deserializeBytes(plainSerialised);
        return pgspolyDeserialised;
    }

    @Test
    public void testFitsImport() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "metadata-v2-vast.xml");

        Observation observation = observationRepository.findAll().iterator().next();
        assertThat(observation.getImageCubes().size(), is(1));
        ImageCube imageCube = observation.getImageCubes().iterator().next();

        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getRaDeg(), is(nullValue()));
        assertThat(imageCube.getDecDeg(), is(nullValue()));
        assertThat(imageCube.getStokesParameters(), is(nullValue()));
        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));
        assertThat(imageCube.getSResolution(), is(nullValue()));
        assertThat(imageCube.getSResolutionMin(), is(nullValue()));
        assertThat(imageCube.getSResolutionMax(), is(nullValue()));
        assertThat(imageCube.getTMin(), is(nullValue()));
        assertThat(imageCube.getTMax(), is(nullValue()));
        assertThat(imageCube.getSResolutionMax(), is(nullValue()));
        assertThat(imageCube.getTExptime(), is(nullValue()));
        assertThat(imageCube.getSRegionPoly(), is(nullValue()));
        assertThat(imageCube.getSFov(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getCellSize(), is(nullValue()));
        assertThat(imageCube.getFilesize(), is(9L));
        assertThat(imageCube.getObjectName(), is(nullValue()));

        String imageFilePath = "validFile.fits";
        try
        {
            fitsCommandLineImporter.run("-parent-id", sbid, "-infile", depositDir + imageFilePath, "-fitsFilename",
                    imageFilePath, "-fits-type", "image-cube", "-parent-type", "observation");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Fits import failed - please check log for details", 0, e.getStatus().intValue());
        }
        entityManager.refresh(imageCube);

        assertThat(imageCube.getObjectName(), is(""));
        assertThat(imageCube.getBType(), is("Intensity"));
        assertThat(imageCube.getBUnit(), is("JY/BEAM"));
        assertThat(imageCube.getRaDeg(), is(194.61733726797));
        assertThat(imageCube.getDecDeg(), is(-49.419355920530));
        assertThat(imageCube.getStokesParameters(), is("/I/"));
        assertEquals(0.2360570534D, imageCube.getEmMin(), 1e-10);
        assertEquals(0.2360570536D, imageCube.getEmMax(), 1e-10);
        assertThat(imageCube.getChannelWidth(), is(1.0D));
        assertThat(imageCube.getEmResPower(), is(1.27E9D));
        assertThat(imageCube.getCentreFrequency(), is(1.27E9D));
        assertThat(imageCube.getNoOfChannels(), is(1));
        assertThat(imageCube.getSResolution(), is(0.01243718786725D));
        assertThat(imageCube.getSResolutionMin(), is(0.009785784768003D));
        assertThat(imageCube.getSResolutionMax(), is(0.01243718786725D));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertThat(imageCube.getTExptime(), is(0.0));

        // H2 writes a PGspoly object as a string that represents a serialised object, hex encoded
        // so when we read it out, we need to decode and deserialise the object for testing
        // this doesn't happen locally because we can create the mapping from an spoly to a string, and we use a dialect
        // that takes advantage of that
        // this doesn't happen on the servers because pgsphere is installed.
        PGspoly spoly = deserializePGspolyForH2(imageCube.getSRegionPoly().getValue());
        assertThat(spoly.getValue(), equalTo("{(3.3968764189491645 , -0.8626187651512087),"
                + "(3.396578454230209 , -0.86263586883543),(3.3965502508376773 , -0.8624421731631952),"
                + "(3.3968481487124427 , -0.8624250736171847)}"));
        assertThat(imageCube.getSRegion().getCoordinates()[0].x, closeTo(1.9462668233330e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0].y, closeTo(-4.9424414571951e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].x, closeTo(1.9460961021246e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].y,
                closeTo(-4.9425394540871e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].x, closeTo(1.9460799427710e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].y,
                closeTo(-4.9414296596342e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].x, closeTo(1.9462506256805e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].y,
                closeTo(-4.9413316864524e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0], equalTo(imageCube.getSRegion().getCoordinates()[4]));
        assertThat(imageCube.getSRegion().getCoordinates().length, is(5));
        assertThat(imageCube.getSRegion().getClass(), is(Polygon.class));
        
        assertThat(imageCube.getSFov(), is(1.2345679012347653E-4D));
        assertThat(imageCube.getNoOfPixels(), is(16L));
        assertThat(imageCube.getCellSize(), is(7.716049382717283E-6));
        assertThat(imageCube.getFilesize(), is(9L));
    }


    @Test
    public void testSpectrumImport() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        String imageFilePath = "beta.image2.001.sir.fits";
        importObservation(sbid, depositDir + "metadata-v2-as031.xml");

        Observation observation = observationRepository.findAll().iterator().next();
        assertThat(observation.getImageCubes().size(), is(1));
        ImageCube imageCube = observation.getImageCubes().iterator().next();
        assertThat(imageCube.getSpectra().size(), is(1));
        Spectrum spectrum = imageCube.getSpectra().get(0);
        assertThat(spectrum.getFilename(), is(imageFilePath));
        

        try
        {
            fitsCommandLineImporter.run("-parent-id", sbid, "-infile", depositDir, "-fitsFilename",
                    imageCube.getFilename(), "-fits-type", "spectrum", "-parent-type", "observation");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Fits import failed - please check log for details", 0, e.getStatus().intValue());
        }
        entityManager.refresh(imageCube);

        assertThat(imageCube.getSpectra().size(), is(1));
        spectrum = imageCube.getSpectra().get(0);
        assertThat(spectrum.getObjectName(), is(""));
        assertThat(spectrum.getRaDeg(), is(344.15127636462));
        assertThat(spectrum.getDecDeg(), is(-36.250662898927));
        assertThat(spectrum.getStokesParameters(), is("/I/"));
        
        // As our sample spectrum is in VOPT we don't have frequency details. 
        // This should be replaced with a more representative spectrum later on. 
        assertNull(spectrum.getEmMin());
        assertNull(spectrum.getEmMax());
        assertNull(spectrum.getChannelWidth());
        assertNull(spectrum.getCentreFrequency());
        assertNull(spectrum.getNoOfChannels());
        
        assertNull(spectrum.getTMin());
        assertNull(spectrum.getTMax());
        assertThat(spectrum.getTExptime(), is(42000.0));

        Coordinate[] sregionCoords = spectrum.getSRegion().getCoordinates();
        assertThat(sregionCoords[0].x, closeTo(3.4415385005429e+02, 10e-10));
        assertThat(sregionCoords[0].y, closeTo(-3.6252754112710e+01, 10e-10));
        assertThat(sregionCoords[1].x, closeTo(3.4414868312951e+02, 10e-10));
        assertThat(sregionCoords[1].y, closeTo(-3.6252738285371e+01, 10e-10));
        assertThat(sregionCoords[2].x, closeTo(3.4414870281174e+02, 10e-10));
        assertThat(sregionCoords[2].y, closeTo(-3.6248571629554e+01, 10e-10));
        assertThat(sregionCoords[3].x, closeTo(3.4415386946100e+02, 10e-10));
        assertThat(sregionCoords[3].y, closeTo(-3.6248587456052e+01, 10e-10));
        assertThat(sregionCoords[0], equalTo(sregionCoords[4]));
        assertThat(sregionCoords.length, is(5));
        assertThat(spectrum.getSRegion().getClass(), is(Polygon.class));
        assertEquals("encaps-spectra-1.tar", spectrum.getEncapsulationFile().getFilename());
        
        // H2 writes a PGspoly object as a string that represents a serialised object, hex encoded
        // so when we read it out, we need to decode and deserialise the object for testing
        // this doesn't happen locally because we can create the mapping from an spoly to a string, and we use a dialect
        // that takes advantage of that
        // this doesn't happen on the servers because pgsphere is installed.
        String expectedSPoly = buildExpectedSPoly(sregionCoords);
        PGspoly spoly = deserializePGspolyForH2(spectrum.getSRegionPoly().getValue());
        assertThat(spoly.getValue(), equalTo(expectedSPoly));
        
        assertThat(spectrum.getFilesize(), is(12L));
    }


    @Test
    public void testMomentMapImport() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        String imageFilePath = "mom0_1.fits";
        importObservation(sbid, depositDir + "metadata-v2-as031.xml");

        Observation observation = observationRepository.findAll().iterator().next();
        assertThat(observation.getImageCubes().size(), is(1));
        ImageCube imageCube = observation.getImageCubes().iterator().next();
        assertThat(imageCube.getMomentMaps().size(), is(1));
        MomentMap momentMap = imageCube.getMomentMaps().get(0);
        assertThat(momentMap.getFilename(), is(imageFilePath));
        assertThat(momentMap.getType(), is("spectral_restored_mom0"));
        

        try
        {
            fitsCommandLineImporter.run("-parent-id", sbid, "-infile", depositDir, "-fitsFilename",
                    imageCube.getFilename(), "-fits-type", "moment-map", "-parent-type", "observation");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Fits import failed - please check log for details", 0, e.getStatus().intValue());
        }
        entityManager.refresh(imageCube);

        assertThat(imageCube.getMomentMaps().size(), is(1));
        momentMap = imageCube.getMomentMaps().get(0);
        assertThat(momentMap.getFilename(), is(imageFilePath));
        assertThat(momentMap.getObjectName(), is(""));
        assertThat(momentMap.getRaDeg(), is(344.15354462644));
        assertThat(momentMap.getDecDeg(), is(-36.249945278860));
        assertThat(momentMap.getStokesParameters(), is("/I/"));
        
        // As our sample momentMap is in VOPT we don't have frequency details. 
        // This should be replaced with a more representative momentMap later on. 
        assertEquals(0.19133313747811379, momentMap.getEmMin(), 1e-10);
        assertEquals(0.23664192421840471, momentMap.getEmMax(), 1e-10);
        assertThat(momentMap.getChannelWidth(), is(3.0E8));
        assertThat(momentMap.getCentreFrequency(), is(1.416861140477E9));
        assertThat(momentMap.getNoOfChannels(), is(1));

        assertNull(momentMap.getTMin());
        assertNull(momentMap.getTMax());
        assertThat(momentMap.getTExptime(), is(0.0));

        Coordinate[] sregionCoords = momentMap.getSRegion().getCoordinates();
        assertThat(sregionCoords[0].x, closeTo(3.4423091564739e+02, 10e-10));
        assertThat(sregionCoords[0].y, closeTo(-3.6285572760070e+01, 10e-10));
        assertThat(sregionCoords[1].x, closeTo(3.4407584316182e+02, 10e-10));
        assertThat(sregionCoords[1].y, closeTo(-3.6285100677546e+01, 10e-10));
        assertThat(sregionCoords[2].x, closeTo(3.4407624360392e+02, 10e-10));
        assertThat(sregionCoords[2].y, closeTo(-3.6214267743621e+01, 10e-10));
        assertThat(sregionCoords[3].x, closeTo(3.4423117558664e+02, 10e-10));
        assertThat(sregionCoords[3].y, closeTo(-3.6214739400192e+01, 10e-10));
        assertThat(sregionCoords[0], equalTo(sregionCoords[4]));
        assertThat(sregionCoords.length, is(5));
        assertThat(momentMap.getSRegion().getClass(), is(Polygon.class));
        assertEquals("encaps-mom-3.tar", momentMap.getEncapsulationFile().getFilename());
        
        // H2 writes a PGspoly object as a string that represents a serialised object, hex encoded
        // so when we read it out, we need to decode and deserialise the object for testing
        // this doesn't happen locally because we can create the mapping from an spoly to a string, and we use a dialect
        // that takes advantage of that
        // this doesn't happen on the servers because pgsphere is installed.
        String expectedSPoly = buildExpectedSPoly(sregionCoords);
        PGspoly spoly = deserializePGspolyForH2(momentMap.getSRegionPoly().getValue());
        assertThat(spoly.getValue(), equalTo(expectedSPoly));
        
        assertThat(momentMap.getFilesize(), is(12L));
    }
    
    @Test
    public void testCubletImport() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        String imageFilePath = "cube_01.fits";
        importObservation(sbid, depositDir + "metadata-v2-as031.xml");

        Observation observation = observationRepository.findAll().iterator().next();
        assertThat(observation.getImageCubes().size(), is(1));
        ImageCube imageCube = observation.getImageCubes().iterator().next();
        assertThat(imageCube.getCubelets().size(), is(1));
        Cubelet cubelet = imageCube.getCubelets().get(0);
        assertThat(cubelet.getFilename(), is(imageFilePath));
        assertThat(cubelet.getType(), is("spectral_restored_mom0"));
        

        try
        {
            fitsCommandLineImporter.run("-parent-id", sbid, "-infile", depositDir, "-fitsFilename",
                    imageCube.getFilename(), "-fits-type", "cubelet", "-parent-type", "observation");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Fits import failed - please check log for details", 0, e.getStatus().intValue());
        }
        entityManager.refresh(imageCube);

        assertThat(imageCube.getCubelets().size(), is(1));
        cubelet = imageCube.getCubelets().get(0);
        assertThat(cubelet.getFilename(), is(imageFilePath));
        assertThat(cubelet.getObjectName(), is(""));
        assertThat(cubelet.getRaDeg(), is(344.15354462644));
        assertThat(cubelet.getDecDeg(), is(-36.249945278860));
        assertThat(cubelet.getStokesParameters(), is("/I/"));
        
        // As our sample cubelet is in VOPT we don't have frequency details. 
        // This should be replaced with a more representative cubelet later on. 
        assertEquals(0.19133313747811379, cubelet.getEmMin(), 1e-10);
        assertEquals(0.23664192421840471, cubelet.getEmMax(), 1e-10);
        assertThat(cubelet.getChannelWidth(), is(3.0E8));
        assertThat(cubelet.getCentreFrequency(), is(1.416861140477E9));
        assertThat(cubelet.getNoOfChannels(), is(1));

        assertNull(cubelet.getTMin());
        assertNull(cubelet.getTMax());
        assertThat(cubelet.getTExptime(), is(0.0));

        Coordinate[] sregionCoords = cubelet.getSRegion().getCoordinates();
        assertThat(sregionCoords[0].x, closeTo(3.4423091564739e+02, 10e-10));
        assertThat(sregionCoords[0].y, closeTo(-3.6285572760070e+01, 10e-10));
        assertThat(sregionCoords[1].x, closeTo(3.4407584316182e+02, 10e-10));
        assertThat(sregionCoords[1].y, closeTo(-3.6285100677546e+01, 10e-10));
        assertThat(sregionCoords[2].x, closeTo(3.4407624360392e+02, 10e-10));
        assertThat(sregionCoords[2].y, closeTo(-3.6214267743621e+01, 10e-10));
        assertThat(sregionCoords[3].x, closeTo(3.4423117558664e+02, 10e-10));
        assertThat(sregionCoords[3].y, closeTo(-3.6214739400192e+01, 10e-10));
        assertThat(sregionCoords[0], equalTo(sregionCoords[4]));
        assertThat(sregionCoords.length, is(5));
        assertThat(cubelet.getSRegion().getClass(), is(Polygon.class));
        assertEquals("encaps-cube-5.tar", cubelet.getEncapsulationFile().getFilename());
        
        // H2 writes a PGspoly object as a string that represents a serialised object, hex encoded
        // so when we read it out, we need to decode and deserialise the object for testing
        // this doesn't happen locally because we can create the mapping from an spoly to a string, and we use a dialect
        // that takes advantage of that
        // this doesn't happen on the servers because pgsphere is installed.
        String expectedSPoly = buildExpectedSPoly(sregionCoords);
        PGspoly spoly = deserializePGspolyForH2(cubelet.getSRegionPoly().getValue());
        assertThat(spoly.getValue(), equalTo(expectedSPoly));
        
        assertThat(cubelet.getFilesize(), is(12L));
    }

    private String buildExpectedSPoly(Coordinate[] sregionCoords)
    {
        StringBuilder expectedSPoly = new StringBuilder("{");
        for (int i = 0; i < sregionCoords.length-1; i++)
        {
            expectedSPoly.append("(");
            expectedSPoly.append(Math.toRadians(sregionCoords[i].x));
            expectedSPoly.append(" , ");
            expectedSPoly.append(Math.toRadians(sregionCoords[i].y));
            expectedSPoly.append(")");
            if (i < sregionCoords.length-2)
            {
                expectedSPoly.append(",");
            }
        }
        expectedSPoly.append("}");
        return expectedSPoly.toString();
    }

    @Test
    public void testFitsImportSfovBug() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation-sfov-bug.xml");

        assertThat(observationRepository.count(), is(1L));
        Observation observation = observationRepository.findAll().iterator().next();
        assertThat(observation.getImageCubes().size(), is(1));
        ImageCube imageCube = observation.getImageCubes().iterator().next();

        String imageFilePath = "validFile-sfov-bug.fits";
        try
        {
            fitsCommandLineImporter.run("-parent-id", sbid, "-infile", depositDir + imageFilePath, "-fitsFilename",
                    imageFilePath, "-fits-type", "image-cube", "-parent-type", "observation");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Fits import failed - please check log for details", 0, e.getStatus().intValue());
        }
        entityManager.refresh(imageCube);

        assertThat(imageCube.getObjectName(), is("SB659_1"));
        assertThat(imageCube.getRaDeg(), is(345.31046769332));
        assertThat(imageCube.getDecDeg(), is(-59.881700799090));
        assertThat(imageCube.getStokesParameters(), is("/I/"));
        assertThat(imageCube.getEmMin(), is(0.299792458D));
        assertThat(imageCube.getEmMax(), is(0.42827494D));
        assertThat(imageCube.getChannelWidth(), is(3.0E08D));
        assertThat(imageCube.getEmResPower(), is(2.8333333333333335D));
        assertThat(imageCube.getCentreFrequency(), is(8.5E8D));
        assertThat(imageCube.getNoOfChannels(), is(1));
        assertThat(imageCube.getSResolution(), is(0.02208974626329D));
        assertThat(imageCube.getSResolutionMin(), is(0.01628723780314D));
        assertThat(imageCube.getSResolutionMax(), is(0.02208974626329D));
        assertNull(imageCube.getTMin());
        assertNull(imageCube.getTMax());
        assertThat(imageCube.getTExptime(), is(0.0));
        assertNull(imageCube.getBUnit());
        assertNull(imageCube.getBType());

        // H2 writes a PGspoly object as a string that represents a serialised object, hex encoded
        // so when we read it out, we need to decode and deserialise the object for testing
        // this doesn't happen locally because we can create the mapping from an spoly to a string, and we use a dialect
        // that takes advantage of that
        // this doesn't happen on the servers because pgsphere is installed.
        PGspoly spoly = deserializePGspolyForH2(imageCube.getSRegionPoly().getValue());
        assertEquals(spoly.getValue(), "{(6.147839530639984 , -1.0938217104098265),"
                + "(5.913065954293952 , -1.0980947089189068),(5.925393935984972 , -0.9910730560794179),"
                + "(6.120544871151792 , -0.9876696795480718)}");
        assertThat(imageCube.getSRegion().getCoordinates()[0].x, closeTo(3.5224525822936e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0].y, closeTo(-6.2671367546264e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].x, closeTo(3.3879372316354e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[1].y,
                closeTo(-6.2916192326700e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].x, closeTo(3.3950006448435e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[2].y,
                closeTo(-5.6784303302483e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].x, closeTo(3.5068138943744e+02, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[3].y,
                closeTo(-5.6589304191143e+01, 10e-10));
        assertThat(imageCube.getSRegion().getCoordinates()[0], equalTo(imageCube.getSRegion().getCoordinates()[4]));
        assertThat(imageCube.getSRegion().getCoordinates().length, is(5));
        assertThat(imageCube.getSRegion().getClass(), is(Polygon.class));

        assertThat(imageCube.getSFov(), is(37.884014645418155D));
        assertThat(imageCube.getNoOfPixels(), is(3409562L));
        assertThat(imageCube.getCellSize(), is(1.1111108888889E-5D));
        assertThat(imageCube.getFilesize(), is(26646L));
    }
}
