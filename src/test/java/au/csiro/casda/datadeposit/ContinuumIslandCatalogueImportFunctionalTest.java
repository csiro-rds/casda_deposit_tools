package au.csiro.casda.datadeposit;

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
import au.csiro.casda.entity.sourcedetect.ContinuumIsland;

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
 * Functional test for continuum island catalogue data import.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class ContinuumIslandCatalogueImportFunctionalTest extends FunctionalTestBase
{
    private CatalogueCommandLineImporter catalogueCommandLineImporter;

    public ContinuumIslandCatalogueImportFunctionalTest() throws Exception
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
    public void testContinuumIslandCatalogueDatafileLoadedAndPersistedToDatabase()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";

        importObservation(sbid, depositDir + "observation.xml");

        String continuumCatalogueDatafile = "selavy-results.islands.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-island", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum island catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(1, imageCubeRepository.count());
        assertEquals(5, catalogueRepository.count());
        Observation observation = observationRepository.findAll().iterator().next();
        assertEquals(1, observation.getCataloguesOfType(CatalogueType.CONTINUUM_ISLAND).size());
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.CONTINUUM_ISLAND).get(0);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertNull(catalogue.getFreqRef());

        assertSame(imageCube, catalogue.getImageCube());

        assertEquals(1578, continuumIslandRepository.countByCatalogue(catalogue));

        ContinuumIsland continuumIsland = continuumIslandRepository
                .findByCatalogueOrderByIdAsc(catalogue, new PageRequest(0, 1)).iterator().next();
        
        assertEquals(12345, continuumIsland.getSbid().intValue());
        assertEquals(catalogue.getProject().getId(), continuumIsland.getProjectId());
        assertEquals("1", continuumIsland.getIslandId());
        assertEquals("J222633-623305", continuumIsland.getIslandName());
        assertEquals(1, continuumIsland.getNumberComponents());
        assertEquals("22:26:34", continuumIsland.getRaHmsCont());
        assertEquals("-62:33:06", continuumIsland.getDecDmsCont());
        assertEquals(336.641196, continuumIsland.getRaDegCont(), 1e-7);
        assertEquals(-62.551581, continuumIsland.getDecDegCont(), 1e-7);
        assertEquals(864.000, continuumIsland.getFreq(), 1e-4);
        assertEquals(1.24, continuumIsland.getMajAxis(), 1e-3);
        assertEquals(0.74, continuumIsland.getMinAxis(), 1e-3);
        assertEquals(157.31, continuumIsland.getPosAng(), 1e-3);
        assertEquals(1.43658, continuumIsland.getFluxInt(), 1e-6);
        assertEquals(1.2909, continuumIsland.getFluxPeak(), 1e-5);
        assertEquals(2350, continuumIsland.getXMin());
        assertEquals(2361, continuumIsland.getXMax());
        assertEquals(290, continuumIsland.getYMin());
        assertEquals(308, continuumIsland.getYMax());
        assertEquals(178, continuumIsland.getNumPixels());
        assertEquals(2355.73, continuumIsland.getXAve(), 1e-4);
        assertEquals(299.16, continuumIsland.getYAve(), 1e-4);
        assertEquals(2355.49, continuumIsland.getXCen(), 1e-4);
        assertEquals(300.12, continuumIsland.getYCen(), 1e-4);
        assertEquals(2355, continuumIsland.getXPeak());
        assertEquals(300, continuumIsland.getYPeak());
        assertEquals(0l, new Short(continuumIsland.getFlagI1()).longValue());
        assertEquals(0l, new Short(continuumIsland.getFlagI2()).longValue());
        assertEquals(0l, new Short(continuumIsland.getFlagI3()).longValue());
        assertEquals(0l, new Short(continuumIsland.getFlagI4()).longValue());
        assertEquals("--", continuumIsland.getComment());
        assertEquals(0.346982012, catalogue.getEmMin(), 1e-8);
        assertEquals(0.346982012, catalogue.getEmMax(), 1e-8);

    }

    @Test
    @Transactional
    public void testValidationMode() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String continuumCatalogueDatafile = "selavy-results.islands.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-island", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum island catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        long count = continuumIslandRepository.count();
        catalogueCommandLineImporter = context.getBeanFactory().createBean(CatalogueCommandLineImporter.class);
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-island", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile, "-validate-only");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum island catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(count, continuumIslandRepository.count());
        assertThat(out.toString(CharEncoding.UTF_8).split(System.lineSeparator()),
                arrayContaining("Catalogue record for filename '" + continuumCatalogueDatafile + "' on Observation '"
                        + sbid + "' already has catalogue entries"));
    }

}
