package au.csiro.casda.datadeposit;

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import javax.transaction.Transactional;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Before;
import org.junit.Test;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.springframework.data.domain.PageRequest;

import au.csiro.casda.datadeposit.catalogue.CatalogueCommandLineImporter;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.sourcedetect.PolarisationComponent;

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
 * Functional test for continuum polarisation catalogue data import.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class PolarisationComponentCatalogueImportFunctionalTest extends FunctionalTestBase
{
    private CatalogueCommandLineImporter catalogueCommandLineImporter;

    public PolarisationComponentCatalogueImportFunctionalTest() throws Exception
    {
        super();
    }

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        catalogueCommandLineImporter = context.getBeanFactory().createBean(CatalogueCommandLineImporter.class);
    }

    @Test
    @Transactional
    public void testPolarisationComponentCatalogueDatafileLoadedAndPersistedToDatabase()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String polarisationCatalogueDatafile = "sample-polarisation.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "polarisation-component", "-parent-id", sbid,
                    "-catalogue-filename", polarisationCatalogueDatafile, "-infile",
                    depositDir + polarisationCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            checkForNormalExit(e);
        }

        assertEquals(1, imageCubeRepository.count());
        assertEquals(5, catalogueRepository.count());
        Observation observation = observationRepository.findAll().iterator().next();
        assertEquals(1, observation.getCataloguesOfType(CatalogueType.POLARISATION_COMPONENT).size());
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.POLARISATION_COMPONENT).get(0);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertSame(imageCube, catalogue.getImageCube());

        assertEquals(1, polarisationComponentRepository.countByCatalogue(catalogue));

        PolarisationComponent polarisationCatalogue =
                polarisationComponentRepository.findByCatalogue(catalogue, new PageRequest(0, 1)).iterator().next();

        assertEquals("SB001_image_1a", polarisationCatalogue.getComponentId());
        assertEquals("J1234-4321", polarisationCatalogue.getComponentName());
        assertEquals(12.582438, polarisationCatalogue.getRaDegCont(), 1e-6);
        assertEquals(-43.352742, polarisationCatalogue.getDecDegCont(), 1e-6);
        assertEquals(1.01, polarisationCatalogue.getFluxIMedian(), 1e-2);
        assertEquals(0.53, polarisationCatalogue.getFluxQMedian(), 1e-2);
        assertEquals(-0.42, polarisationCatalogue.getFluxUMedian(), 1e-2);
        assertEquals(0.0010, polarisationCatalogue.getFluxVMedian(), 1e-4);
        assertEquals(0.0010, polarisationCatalogue.getRmsI(), 1e-4);
        assertEquals(0.0012, polarisationCatalogue.getRmsQ(), 1e-4);
        assertEquals(0.0013, polarisationCatalogue.getRmsU(), 1e-4);
        assertEquals(0.0012, polarisationCatalogue.getRmsV(), 1e-4);
        assertEquals(1.09, polarisationCatalogue.getCo1(), 1e-2);
        assertEquals(1.18, polarisationCatalogue.getCo2(), 1e-2);
        assertEquals(1.27, polarisationCatalogue.getCo3(), 1e-2);
        assertEquals(1.36, polarisationCatalogue.getCo4(), 1e-2);
        assertEquals(1.45, polarisationCatalogue.getCo5(), 1e-2);
        assertEquals(0.09, polarisationCatalogue.getLambdaRefSq(), 1e-2);
        assertEquals(5.51, polarisationCatalogue.getRmsfFwhm(), 1e-2);
        assertEquals(1.57, polarisationCatalogue.getPolPeak(), 1e-2);
        assertEquals(1.41, polarisationCatalogue.getPolPeakDebias(), 1e-2);
        assertEquals(0.01, polarisationCatalogue.getPolPeakErr(), 1e-2);
        assertEquals(1.55, polarisationCatalogue.getPolPeakFit(), 1e-2);
        assertEquals(1.42, polarisationCatalogue.getPolPeakFitDebias(), 1e-2);
        assertEquals(0.01, polarisationCatalogue.getPolPeakFitErr(), 1e-2);
        assertEquals(150.0, polarisationCatalogue.getPolPeakFitSnr(), 1e-2);
        assertEquals(14.06, polarisationCatalogue.getPolPeakFitSnrErr(), 1e-2);
        assertEquals(50.13, polarisationCatalogue.getFdPeak(), 1e-2);
        assertEquals(5.66, polarisationCatalogue.getFdPeakErr(), 1e-2);
        assertEquals(52.68, polarisationCatalogue.getFdPeakFit(), 1e-2);
        assertEquals(5.23, polarisationCatalogue.getFdPeakFitErr(), 1e-2);
        assertEquals(30.09, polarisationCatalogue.getPolAngRef(), 1e-2);
        assertEquals(1.07, polarisationCatalogue.getPolAngRefErr(), 1e-2);
        assertEquals(45.88, polarisationCatalogue.getPolAngZero(), 1e-2);
        assertEquals(1.53, polarisationCatalogue.getPolAngZeroErr(), 1e-2);
        assertEquals(8.91, polarisationCatalogue.getPolFrac(), 1e-2);
        assertEquals(0.14, polarisationCatalogue.getPolFracErr(), 1e-2);
        assertEquals(0.37, polarisationCatalogue.getComplex1(), 1e-2);
        assertEquals(0.82, polarisationCatalogue.getComplex2(), 1e-2);
        assertEquals(true, polarisationCatalogue.isFlagP1());
        assertEquals(false, polarisationCatalogue.isFlagP2());
        assertEquals("0.0", polarisationCatalogue.getFlagP3());
        assertEquals("1.0", polarisationCatalogue.getFlagP4());

    }

    @Test
    @Transactional
    public void testValidationMode() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String polarisationCatalogueDatafile = "sample-polarisation.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "polarisation-component", "-parent-id", sbid,
                    "-catalogue-filename", polarisationCatalogueDatafile, "-infile",
                    depositDir + polarisationCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            checkForNormalExit(e);
        }

        long count = polarisationComponentRepository.count();
        catalogueCommandLineImporter = context.getBeanFactory().createBean(CatalogueCommandLineImporter.class);
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "polarisation-component", "-parent-id", sbid,
                    "-catalogue-filename", polarisationCatalogueDatafile, "-infile",
                    depositDir + polarisationCatalogueDatafile, "-validate-only");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            checkForNormalExit(e);
        }

        assertEquals(count, polarisationComponentRepository.count());
        assertThat(out.toString(CharEncoding.UTF_8).split(System.lineSeparator()),
                arrayContaining("Catalogue record for filename '" + polarisationCatalogueDatafile + "' on Observation '"
                        + sbid + "' already has catalogue entries"));
    }

}
