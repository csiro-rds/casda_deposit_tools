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
        assertEquals(7, catalogueRepository.count());
        Observation observation = observationRepository.findAll().iterator().next();
        assertEquals(1, observation.getCataloguesOfType(CatalogueType.POLARISATION_COMPONENT).size());
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.POLARISATION_COMPONENT).get(0);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertSame(imageCube, catalogue.getImageCube());

        PolarisationComponent polarisationCatalogue =
                polarisationComponentRepository.findByCatalogue(catalogue, new PageRequest(0, 1)).iterator().next();

        assertEquals("SB2338_image.i.NGC7232.cont.sb2338.NGC7232_A_T0-0A.linmos.restored_1000a",
                polarisationCatalogue.getComponentId());
        assertEquals("J215812-432858", polarisationCatalogue.getComponentName());
        assertEquals(329.552673, polarisationCatalogue.getRaDegCont(), 1e-6);
        assertEquals(-43.482868, polarisationCatalogue.getDecDegCont(), 1e-6);
        assertEquals(3.4850, polarisationCatalogue.getFluxIMedian(), 1e-2);
        assertEquals(0.040735, polarisationCatalogue.getFluxQMedian(), 1e-2);
        assertEquals(-0.01641708331590052694082260131835938, polarisationCatalogue.getFluxUMedian(), 1e-8);
        assertEquals(0.2011, polarisationCatalogue.getFluxVMedian(), 1e-4);
        assertEquals(2.402, polarisationCatalogue.getRmsI(), 1e-4);
        assertEquals(2.036, polarisationCatalogue.getRmsQ(), 1e-4);
        assertEquals(1.80754, polarisationCatalogue.getRmsU(), 1e-4);
        assertEquals(1.854, polarisationCatalogue.getRmsV(), 1e-4);
        assertEquals(4.938, polarisationCatalogue.getCo1(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getCo2(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getCo3(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getCo4(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getCo5(), 1e-2);
        assertEquals(0.0459, polarisationCatalogue.getLambdaRefSq(), 1e-3);
        assertEquals(1289.6588, polarisationCatalogue.getRmsfFwhm(), 1e-4);
        assertEquals(2.204, polarisationCatalogue.getPolPeak(), 1e-3);
        assertEquals(-1000.000, polarisationCatalogue.getPolPeakDebias(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolPeakErr(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolPeakFit(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolPeakFitDebias(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolPeakFitErr(), 1e-2);
        assertEquals(1.303, polarisationCatalogue.getPolPeakFitSnr(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolPeakFitSnrErr(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getFdPeak(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getFdPeakErr(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getFdPeakFit(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getFdPeakFitErr(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolAngRef(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolAngRefErr(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolAngZero(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolAngZeroErr(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolFrac(), 1e-2);
        assertEquals(0.000, polarisationCatalogue.getPolFracErr(), 1e-2);
        assertEquals(1.00, polarisationCatalogue.getComplex1(), 1e-2);
        assertEquals(12.515, polarisationCatalogue.getComplex2(), 1e-2);
        assertEquals(0, polarisationCatalogue.isFlagIsDetection());
        assertEquals(0, polarisationCatalogue.isFlagEdge());
        assertEquals(0, polarisationCatalogue.getFlagP3());
        assertEquals(0, polarisationCatalogue.getFlagP4());


        assertEquals(7, polarisationComponentRepository.countByCatalogue(catalogue));
        
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
