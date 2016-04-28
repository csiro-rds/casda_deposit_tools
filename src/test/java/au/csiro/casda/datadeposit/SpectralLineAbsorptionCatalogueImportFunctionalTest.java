package au.csiro.casda.datadeposit;

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


import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import au.csiro.casda.entity.observation.QualityLevel;
import au.csiro.casda.entity.observation.SpectralLineAbsorption;
/**
 * Functional test for Spectral Line absorption catalogue data import.
 * <p>
 * Copyright 2015, CSIRO Australia All rights reserved.
 */
public class SpectralLineAbsorptionCatalogueImportFunctionalTest extends FunctionalTestBase
{
	private CatalogueCommandLineImporter catalogueCommandLineImporter;
	
	public SpectralLineAbsorptionCatalogueImportFunctionalTest() throws Exception 
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
    public void testSpectralLineAbsorptionCatalogueDatafileLoadedAndPersistedToDatabase()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";

        importObservation(sbid, depositDir + "observation.xml");

        String catalogueDatafile = "spectralLineAbsorption.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "Spectral-line-absorption", "-parent-id", sbid,
                    "-catalogue-filename", catalogueDatafile, "-infile",
                    depositDir + catalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Spectral line absorption catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(1, imageCubeRepository.count());
        assertEquals(5, catalogueRepository.count());
        Observation observation = observationRepository.findAll().iterator().next();
        assertEquals(1, observation.getCataloguesOfType(CatalogueType.SPECTRAL_LINE_ABSORPTION).size());
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.SPECTRAL_LINE_ABSORPTION).get(0);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertNull(catalogue.getFreqRef());

        assertSame(imageCube, catalogue.getImageCube());

        assertEquals(1, spectralLineAbsorptionRepository.countByCatalogue(catalogue));

        SpectralLineAbsorption spectralLineAbsorption = spectralLineAbsorptionRepository
                .findByCatalogue(catalogue, new PageRequest(0, 1)).iterator().next();

        //IDENTIFIERS
        assertEquals(catalogue.getProject().getId(), spectralLineAbsorption.getProjectId());
        assertEquals("absCatTest.fits", spectralLineAbsorption.getImageId());
        assertEquals("2014-04-07T01:30:51", spectralLineAbsorption.getDateTimeUt());   
        assertEquals("SB10001_absCatTest_0a", spectralLineAbsorption.getContComponentId());
        assertEquals(9780.916, spectralLineAbsorption.getContFlux(), 1e-3);
        assertEquals("SB10001_absCatTest_0a_0", spectralLineAbsorption.getObjectId());
        assertEquals("J183340-210340", spectralLineAbsorption.getObjectName());
        
        //POSITIONS(P)
        assertEquals("18:33:40.1", spectralLineAbsorption.getRaHmsCont());
        assertEquals("-21:03:40", spectralLineAbsorption.getDecDmsCont());
        assertEquals(278.416931, spectralLineAbsorption.getRaDegCont(), 1e-6);
        assertEquals(-21.061312, spectralLineAbsorption.getDecDegCont(), 1e-6);
        assertEquals(21.56, spectralLineAbsorption.getRaDegContErr(), 1e-2);
        assertEquals(13.19, spectralLineAbsorption.getDecDegContErr(), 1e-2);
        
        //FREQUENCIES/VELOCITIES (V)
        assertEquals(756.7, spectralLineAbsorption.getFreqUw(), 1e-1);
        assertEquals(9.5, spectralLineAbsorption.getFreqUwErr(), 1e-1);
        assertEquals(754.7, spectralLineAbsorption.getFreqW(), 1e-1);
        assertEquals(5.3, spectralLineAbsorption.getFreqWErr(), 1e-1);        
        assertEquals(0.9, spectralLineAbsorption.getZHiUw(), 1e-1);
        assertEquals(4.4, spectralLineAbsorption.getZHiUwErr(), 1e-1);
        assertEquals(0.7, spectralLineAbsorption.getZHiW(), 1e-1);
        assertEquals(5.5, spectralLineAbsorption.getZHiWErr(), 1e-1);
        assertEquals(0.3, spectralLineAbsorption.getZHiPeak(), 1e-1);
        assertEquals(21.8, spectralLineAbsorption.getZHiPeakErr(), 1e-1);
        assertEquals(7.7, spectralLineAbsorption.getW50(), 1e-1);
        assertEquals(3.5, spectralLineAbsorption.getW50Err(), 1e-1);
        assertEquals(6.1, spectralLineAbsorption.getW20(), 1e-1);
        assertEquals(9.1, spectralLineAbsorption.getW20Err(), 1e-1);
        
        //FLUXES/OPTICAL DEPTHS (S)
        assertEquals(75.163, spectralLineAbsorption.getRmsImagecube(), 01e-3);
        assertEquals(6.903, spectralLineAbsorption.getOptDepthPeak(), 1e-3);
        assertEquals(1.123, spectralLineAbsorption.getOptDepthPeakErr(), 1e-3);   
        assertEquals(7.001, spectralLineAbsorption.getOptDepthInt(), 1e-3);
        assertEquals(8.765, spectralLineAbsorption.getOptDepthIntErr(), 1e-3);
        
        //FLAGS
        assertEquals(1, spectralLineAbsorption.getFlagS1().intValue());
        assertEquals(0, spectralLineAbsorption.getFlagS2().intValue());
        assertEquals(2, spectralLineAbsorption.getFlagS3().intValue());   
        
        //Others
        assertEquals(QualityLevel.NOT_VALIDATED, spectralLineAbsorption.getQualityLevel());
        assertEquals(0.39723393, catalogue.getEmMin(), 1e-8);
        assertEquals(0.39723393, catalogue.getEmMax(), 1e-8);
    }

    @Test
    @Transactional
    public void testValidationMode() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String catalogueDatafile = "spectralLineAbsorption.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "spectral-line-absorption", "-parent-id", sbid,
                    "-catalogue-filename", catalogueDatafile, "-infile",
                    depositDir + catalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Spectral line absorption catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        long count = spectralLineAbsorptionRepository.count();
        catalogueCommandLineImporter = context.getBeanFactory().createBean(CatalogueCommandLineImporter.class);
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "Spectral-line-absorption", "-parent-id", sbid,
                    "-catalogue-filename", catalogueDatafile, "-infile",
                    depositDir + catalogueDatafile, "-validate-only");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Spectral line absorption catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(count, spectralLineAbsorptionRepository.count());
        assertThat(out.toString(CharEncoding.UTF_8).split(System.lineSeparator()),
                arrayContaining("Catalogue record for filename '" + catalogueDatafile + "' on Observation '"
                        + sbid + "' already has catalogue entries"));
    }
}
