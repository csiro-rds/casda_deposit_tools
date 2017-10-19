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
import au.csiro.casda.entity.sourcedetect.ContinuumComponent;

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
 * Functional test for continuum component catalogue data import.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class ContinuumComponentCatalogueImportFunctionalTest extends FunctionalTestBase
{
    private CatalogueCommandLineImporter catalogueCommandLineImporter;

    public ContinuumComponentCatalogueImportFunctionalTest() throws Exception
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
    public void testContinuumComponentCatalogueDatafileLoadedAndPersistedToDatabase()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String continuumCatalogueDatafile = "selavy-results.components.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        Observation observation = observationRepository.findAll().iterator().next();
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.CONTINUUM_COMPONENT).get(1);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertEquals(8.64e+08, catalogue.getFreqRef(), 1.0);

        assertSame(imageCube, catalogue.getImageCube());

        assertEquals(1924, continuumComponentRepository.countByCatalogue(catalogue));

        ContinuumComponent continuumCatalogue = continuumComponentRepository
                .findByCatalogueOrderByIdAsc(catalogue, new PageRequest(0, 1)).iterator().next();

        assertEquals(12345, continuumCatalogue.getSbid().intValue());
        assertEquals(catalogue.getProject().getId(), continuumCatalogue.getProjectId());
        assertEquals("_Images/SB_609_617_639_643_659_no617b6_withBeam_Freq_Stokes.fits_1",
                continuumCatalogue.getIslandId());
        assertEquals("_Images/SB_609_617_639_643_659_no617b6_withBeam_Freq_Stokes.fits_1a",
                continuumCatalogue.getComponentId());
        assertEquals("J222645-623529", continuumCatalogue.getComponentName());
        assertEquals("22:26:45.4", continuumCatalogue.getRaHmsCont());
        assertEquals("-62:35:29", continuumCatalogue.getDecDmsCont());
        assertEquals(336.689324, continuumCatalogue.getRaDegCont(), 1e-6);
        assertEquals(-62.591524, continuumCatalogue.getDecDegCont(), 1e-6);
        assertEquals(21.56, continuumCatalogue.getRaErr(), 1e-2);
        assertEquals(5.79, continuumCatalogue.getDecErr(), 1e-2);  
        assertEquals(864.000, continuumCatalogue.getFreq(), 1e-1);
        assertEquals(1312.663, continuumCatalogue.getFluxPeak(), 1e-3);
        assertEquals(8.456, continuumCatalogue.getFluxPeakErr(), 1e-3);
        assertEquals(1440.637, continuumCatalogue.getFluxInt(), 1e-3);
        assertEquals(1.963, continuumCatalogue.getFluxIntErr(), 1e-3);
        assertEquals(80.09, continuumCatalogue.getMajAxis(), 1e-2);
        assertEquals(48.60, continuumCatalogue.getMinAxis(), 1e-2);
        assertEquals(156.33, continuumCatalogue.getPosAng(), 1e-2);
        assertEquals(6.21, continuumCatalogue.getMajAxisErr(), 1e-2);
        assertEquals(11.45, continuumCatalogue.getMinAxisErr(), 1e-2);
        assertEquals(33.64, continuumCatalogue.getPosAngErr(), 1e-2);
        assertEquals(21.08, continuumCatalogue.getMajAxisDeconv(), 1e-2);
        assertEquals(15.45, continuumCatalogue.getMinAxisDeconv(), 1e-2);
        assertEquals(-43.78, continuumCatalogue.getPosAngDeconv(), 1e-2);
        assertEquals(null, continuumCatalogue.getMajAxisDeconvErr());
        assertEquals(null, continuumCatalogue.getMinAxisDeconvErr());
        assertEquals(null, continuumCatalogue.getPosAngDeconvErr());
        assertEquals(1194.4159, continuumCatalogue.getChiSquaredFit(), 1e-4);
        assertEquals(2590.406, continuumCatalogue.getRmsFitGauss(), 1e-3);
        assertEquals(7.77, continuumCatalogue.getSpectralIndex(), 1e-2);
        assertEquals(4.44, continuumCatalogue.getSpectralCurvature(), 1e-2);
        assertEquals(null, continuumCatalogue.getSpectralIndexErr());
        assertEquals(null, continuumCatalogue.getSpectralCurvatureErr());
        assertEquals(1.477, continuumCatalogue.getRmsImage(), 1e-3);
        assertEquals(1l, new Short(continuumCatalogue.getHasSiblings()).longValue());
        assertEquals(2l, new Short(continuumCatalogue.getFitIsEstimate()).longValue());
        assertEquals(3l, new Short(continuumCatalogue.getFlagC3()).longValue());
        assertEquals(4l, new Short(continuumCatalogue.getFlagC4()).longValue());
        assertEquals(null, continuumCatalogue.getComment());
        assertEquals(0.248997058, catalogue.getEmMin(), 1e-8);
        assertEquals(0.346982012, catalogue.getEmMax(), 1e-8);
    }

    @Test
    @Transactional
    public void testReloadingTheSameCatalogueFileShouldFail()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String continuumCatalogueDatafile = "selavy-results.components.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        long count = continuumComponentRepository.count();

        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import should not have succeeded - please check log for details", 1,
                    e.getStatus().intValue());
        }

        assertEquals(count, continuumComponentRepository.count());
    }

    @Test
    @Transactional
    public void testNewContinuumComponentCatalogueDatafileLoadedAndPersistedToDatabase()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String continuumCatalogueDatafile = "selavy-results.components-new.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        Observation observation = observationRepository.findAll().iterator().next();
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.CONTINUUM_COMPONENT).get(0);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertEquals(8.64e+08, catalogue.getFreqRef(), 1.0);

        assertSame(imageCube, catalogue.getImageCube());

        assertEquals(2, continuumComponentRepository.countByCatalogue(catalogue));

        ContinuumComponent continuumCatalogue = continuumComponentRepository
                .findByCatalogueOrderByIdAsc(catalogue, new PageRequest(0, 1)).iterator().next();

        assertEquals(12345, continuumCatalogue.getSbid().intValue());
        assertEquals(catalogue.getProject().getId(), continuumCatalogue.getProjectId());
        assertEquals("_Images/SB_609_617_639_643_659_no617b6_withBeam_Freq_Stokes.fits_1",
                continuumCatalogue.getIslandId());
        assertEquals("_Images/SB_609_617_639_643_659_no617b6_withBeam_Freq_Stokes.fits_1a",
                continuumCatalogue.getComponentId());
        assertEquals("J222645-623529", continuumCatalogue.getComponentName());
        assertEquals("22:26:45.4", continuumCatalogue.getRaHmsCont());
        assertEquals("-62:35:29", continuumCatalogue.getDecDmsCont());
        assertEquals(336.689324, continuumCatalogue.getRaDegCont(), 1e-6);
        assertEquals(-62.591524, continuumCatalogue.getDecDegCont(), 1e-6);
        assertEquals(21.56, continuumCatalogue.getRaErr(), 1e-2);
        assertEquals(5.79, continuumCatalogue.getDecErr(), 1e-2);  
        assertEquals(864.000, continuumCatalogue.getFreq(), 1e-1);
        assertEquals(1312.663, continuumCatalogue.getFluxPeak(), 1e-3);
        assertEquals(8.456, continuumCatalogue.getFluxPeakErr(), 1e-3);
        assertEquals(1440.637, continuumCatalogue.getFluxInt(), 1e-3);
        assertEquals(1.963, continuumCatalogue.getFluxIntErr(), 1e-3);
        assertEquals(80.09, continuumCatalogue.getMajAxis(), 1e-2);
        assertEquals(48.60, continuumCatalogue.getMinAxis(), 1e-2);
        assertEquals(156.33, continuumCatalogue.getPosAng(), 1e-2);
        assertEquals(6.21, continuumCatalogue.getMajAxisErr(), 1e-2);
        assertEquals(11.45, continuumCatalogue.getMinAxisErr(), 1e-2);
        assertEquals(33.64, continuumCatalogue.getPosAngErr(), 1e-2);
        assertEquals(21.08, continuumCatalogue.getMajAxisDeconv(), 1e-2);
        assertEquals(15.45, continuumCatalogue.getMinAxisDeconv(), 1e-2);
        assertEquals(-43.78, continuumCatalogue.getPosAngDeconv(), 1e-2);
        assertEquals(0.11, continuumCatalogue.getMajAxisDeconvErr(), 1e-2);
        assertEquals(0.22, continuumCatalogue.getMinAxisDeconvErr(), 1e-2);
        assertEquals(0.33, continuumCatalogue.getPosAngDeconvErr(), 1e-2);
        assertEquals(1194.4159, continuumCatalogue.getChiSquaredFit(), 1e-4);
        assertEquals(2590.406, continuumCatalogue.getRmsFitGauss(), 1e-3);
        assertEquals(7.77, continuumCatalogue.getSpectralIndex(), 1e-2);
        assertEquals(4.44, continuumCatalogue.getSpectralCurvature(), 1e-2);
        assertEquals(1.44, continuumCatalogue.getSpectralIndexErr(), 1e-2);
        assertEquals(2.55, continuumCatalogue.getSpectralCurvatureErr(), 1e-2);
        assertEquals(1.477, continuumCatalogue.getRmsImage(), 1e-3);
        assertEquals(1l, new Short(continuumCatalogue.getHasSiblings()).longValue());
        assertEquals(2l, new Short(continuumCatalogue.getFitIsEstimate()).longValue());
        assertEquals(3l, new Short(continuumCatalogue.getFlagC3()).longValue());
        assertEquals(4l, new Short(continuumCatalogue.getFlagC4()).longValue());
        assertEquals(null, continuumCatalogue.getComment());
        assertEquals(0.248997058, catalogue.getEmMin(), 1e-8);
        assertEquals(0.346982012, catalogue.getEmMax(), 1e-8);
    }

    @Test
    @Transactional
    public void testValidationMode() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String continuumCatalogueDatafile = "selavy-results.components.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        long count = continuumComponentRepository.count();
        catalogueCommandLineImporter = context.getBeanFactory().createBean(CatalogueCommandLineImporter.class);
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile, "-validate-only");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(count, continuumComponentRepository.count());
        assertThat(out.toString(CharEncoding.UTF_8).split(System.lineSeparator()),
                arrayContaining("Catalogue record for filename '" + continuumCatalogueDatafile + "' on Observation '"
                        + sbid + "' already has catalogue entries"));
    }

    /**
     * Raised as BUG CASDA-2509
     */
    @Test
    @Transactional
    public void testContinuumDataFileWithBadMidTableCellIsNotPersisted()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observationForComponentCatalogue.bad.mid.table.cell.xml");

        String continuumCatalogueDatafile = "componentCatalogue.bad.mid.table.cell.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import should not have succeeded - please check log for details", 1,
                    e.getStatus().intValue());
        }

        assertEquals(1, imageCubeRepository.count());
        assertEquals(1, catalogueRepository.count());
        Observation observation = observationRepository.findAll().iterator().next();
        assertEquals(1, observation.getCataloguesOfType(CatalogueType.CONTINUUM_COMPONENT).size());
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.CONTINUUM_COMPONENT).get(0);

        assertNull("Catalogue freqRef was erroneously saved", catalogue.getFreqRef());

        assertNull("Catalogue imageCube reference was erroneously saved", catalogue.getImageCube());

        assertEquals("Catalogue continuum records were erroneously saved", 0,
                continuumComponentRepository.countByCatalogue(catalogue));
    }

    @Test
    @Transactional
    public void testAtlasTable4CatalogueDatafileLoadedAndPersistedToDatabase()
    {
        String sbid = "151515";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation-atlas.xml");

        String continuumCatalogueDatafile = "atlas-components.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "continuum-component", "-parent-id", sbid,
                    "-catalogue-filename", continuumCatalogueDatafile, "-infile",
                    depositDir + continuumCatalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Continuum catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(1, imageCubeRepository.count());
        assertEquals(1, catalogueRepository.count());
        Observation observation = observationRepository.findAll().iterator().next();
        assertEquals(1, observation.getCataloguesOfType(CatalogueType.CONTINUUM_COMPONENT).size());
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.CONTINUUM_COMPONENT).get(0);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertEquals(1.408e+08, catalogue.getFreqRef(), 1.0);

        assertSame(imageCube, catalogue.getImageCube());

        assertEquals(784, continuumComponentRepository.countByCatalogue(catalogue));

        ContinuumComponent continuumCatalogue = continuumComponentRepository
                .findByCatalogueOrderByIdAsc(catalogue, new PageRequest(0, 1)).iterator().next();

        assertEquals(null, continuumCatalogue.getIslandId());
        assertEquals("C001", continuumCatalogue.getComponentId());
        assertEquals("ATCDFS_J032602.78-284709.0", continuumCatalogue.getComponentName());
        assertEquals("3:26:02.785", continuumCatalogue.getRaHmsCont());
        assertEquals("-28:47:09.0", continuumCatalogue.getDecDmsCont());
        assertEquals(51.511604, continuumCatalogue.getRaDegCont(), 1e-7);
        assertEquals(-28.785833, continuumCatalogue.getDecDegCont(), 1e-7);
        assertEquals(0.78, continuumCatalogue.getRaErr(), 1e-6);
        assertEquals(0.73, continuumCatalogue.getDecErr(), 1e-6);
        assertEquals(1408.000, continuumCatalogue.getFreq(), 1e-3);
        assertEquals(0.70, continuumCatalogue.getFluxPeak(), 1e-3);
        assertEquals(0.000, continuumCatalogue.getFluxPeakErr(), 1e-3);
        assertEquals(1.38, continuumCatalogue.getFluxInt(), 1e-3);
        assertEquals(0.000, continuumCatalogue.getFluxIntErr(), 1e-3);
        assertEquals(8.3, continuumCatalogue.getMajAxis(), 1e-2);
        assertEquals(2.6, continuumCatalogue.getMinAxis(), 1e-2);
        assertEquals(60.8, continuumCatalogue.getPosAng(), 1e-2);
        assertEquals(0.00, continuumCatalogue.getMajAxisErr(), 1e-2);
        assertEquals(0.00, continuumCatalogue.getMinAxisErr(), 1e-2);
        assertEquals(0.00, continuumCatalogue.getPosAngErr(), 1e-2);
        assertEquals(8.3, continuumCatalogue.getMajAxisDeconv(), 1e-4);
        assertEquals(2.6, continuumCatalogue.getMinAxisDeconv(), 1e-2);
        assertEquals(60.8, continuumCatalogue.getPosAngDeconv(), 1e-2);
        assertEquals(-1.0, continuumCatalogue.getChiSquaredFit(), 1e-2);
        assertEquals(-1.0, continuumCatalogue.getRmsFitGauss(), 1e-4);
        assertEquals(0.000, continuumCatalogue.getSpectralIndex(), 1e-3);
        assertEquals(0.000, continuumCatalogue.getSpectralCurvature(), 1e-3);
        assertEquals(0.079, continuumCatalogue.getRmsImage(), 1e-5);
        assertEquals(0l, new Short(continuumCatalogue.getHasSiblings()).longValue());
        assertEquals(0l, new Short(continuumCatalogue.getFitIsEstimate()).longValue());
        assertEquals(0l, new Short(continuumCatalogue.getFlagC3()).longValue());
        assertEquals(0l, new Short(continuumCatalogue.getFlagC4()).longValue());
        assertEquals(null, continuumCatalogue.getComment());
        assertEquals(151515, continuumCatalogue.getSbid().intValue());
        assertEquals(catalogue.getProject().getId(), continuumCatalogue.getProjectId());
    }
}
