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
import au.csiro.casda.entity.observation.SpectralLineEmission;
/**
 * Functional test for Spectral Line Emission catalogue data import.
 * <p>
 * Copyright 2015, CSIRO Australia All rights reserved.
 */
public class SpectralLineEmissionCatalogueImportFunctionalTest extends FunctionalTestBase
{
    private CatalogueCommandLineImporter catalogueCommandLineImporter;

    public SpectralLineEmissionCatalogueImportFunctionalTest() throws Exception
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
    public void testSpectralLineEmissionCatalogueDatafileLoadedAndPersistedToDatabase()
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";

        importObservation(sbid, depositDir + "observation.xml");

        String catalogueDatafile = "spectralLineEmission.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "Spectral-line-emission", "-parent-id", sbid,
                    "-catalogue-filename", catalogueDatafile, "-infile", depositDir + catalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Spectral line emission catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(1, imageCubeRepository.count());
        assertEquals(7, catalogueRepository.count());
        Observation observation = observationRepository.findAll().iterator().next();
        assertEquals(1, observation.getCataloguesOfType(CatalogueType.SPECTRAL_LINE_EMISSION).size());
        Catalogue catalogue = observation.getCataloguesOfType(CatalogueType.SPECTRAL_LINE_EMISSION).get(0);
        ImageCube imageCube = observation.getImageCubes().get(0);

        assertNull(catalogue.getFreqRef());

        assertSame(imageCube, catalogue.getImageCube());

        assertEquals(1, spectralLineEmissionRepository.countByCatalogue(catalogue));

        SpectralLineEmission spectralLineEmission =
                spectralLineEmissionRepository.findByCatalogue(catalogue, new PageRequest(0, 1)).iterator().next();

        // TODO check values
        // IDENTIFIERS
        assertEquals(catalogue.getProject().getId(), spectralLineEmission.getProjectId());
        assertEquals("SB10001_verificationCube_1", spectralLineEmission.getObjectId());
        assertEquals("J060910-273517", spectralLineEmission.getObjectName());

        // POSITION RELATED
        assertEquals("06:09:10.0", spectralLineEmission.getRaHmsW());
        assertEquals("-27:35:17", spectralLineEmission.getDecDmsW());
        assertEquals(92.291535, spectralLineEmission.getRaDegW(), 1e-6);
        assertEquals(0.10, spectralLineEmission.getRaDegWErr(), 1e-2);
        assertEquals(-27.588162, spectralLineEmission.getDecDegW(), 1e-6);
        assertEquals(0.40, spectralLineEmission.getDecDegWErr(), 1e-2);
        assertEquals(15.123456, spectralLineEmission.getRaDegUw(), 1e-6);
        assertEquals(0.07, spectralLineEmission.getRaDegUwErr(), 1e-2);
        assertEquals(6.098765, spectralLineEmission.getDecDegUw(), 1e-6);
        assertEquals(0.19, spectralLineEmission.getDecDegUwErr(), 1e-2);
        assertEquals(8.546932, spectralLineEmission.getGlongW(), 1e-6);
        assertEquals(0.97, spectralLineEmission.getGlongWErr(), 1e-2);
        assertEquals(55.223377, spectralLineEmission.getGlatW(), 1e-6);
        assertEquals(9.99, spectralLineEmission.getGlatWErr(), 1e-2);
        assertEquals(44.379125, spectralLineEmission.getGlongUw(), 1e-6);
        assertEquals(1.21, spectralLineEmission.getGlongUwErr(), 1e-2);
        assertEquals(13.824679, spectralLineEmission.getGlatUw(), 1e-6);
        assertEquals(1.55, spectralLineEmission.getGlatUwErr(), 1e-2);

        // SHAPE-RELATED
        assertEquals(781.21, spectralLineEmission.getMajAxis(), 1e-2);
        assertEquals(751.70, spectralLineEmission.getMinAxis(), 1e-2);
        assertEquals(148.18, spectralLineEmission.getPosAng(), 1e-2);
        assertEquals(23.56, spectralLineEmission.getMajAxisFit(), 1e-2);
        assertEquals(66.45, spectralLineEmission.getMajAxisFitErr(), 1e-2);
        assertEquals(3.46, spectralLineEmission.getMinAxisFit(), 1e-2);
        assertEquals(7.39, spectralLineEmission.getMinAxisFitErr(), 1e-2);
        assertEquals(8.66, spectralLineEmission.getPosAngFit(), 1e-2);
        assertEquals(2.44, spectralLineEmission.getPosAngFitErr(), 1e-2);
        assertEquals(5, spectralLineEmission.getSizeX(), 1);
        assertEquals(6, spectralLineEmission.getSizeY(), 1);
        assertEquals(5, spectralLineEmission.getSizeZ(), 1);
        assertEquals(68, spectralLineEmission.getNVox().intValue());
        assertEquals(1.536, spectralLineEmission.getAsymmetry2d(), 1e-3);
        assertEquals(2.631, spectralLineEmission.getAsymmetry2dErr(), 1e-3);
        assertEquals(1.001, spectralLineEmission.getAsymmetry3d(), 1e-3);
        assertEquals(8.661, spectralLineEmission.getAsymmetry3dErr(), 1e-3);

        // SPECTRAL LOCATION (SIMPLE)
        assertEquals(45.928123, spectralLineEmission.getFreqUw(), 1e-6);
        assertEquals(6.938446, spectralLineEmission.getFreqUwErr(), 1e-6);
        assertEquals(115.649557, spectralLineEmission.getFreqW(), 1e-6);
        assertEquals(999.999999, spectralLineEmission.getFreqWErr(), 1e-6);
        assertEquals(1.000001, spectralLineEmission.getFreqPeak(), 1e-6);
        assertEquals(277.447, spectralLineEmission.getVelUw(), 1e-3);
        assertEquals(46.987, spectralLineEmission.getVelUwErr(), 1e-3);
        assertEquals(65.997, spectralLineEmission.getVelW(), 1e-3);
        assertEquals(34.664, spectralLineEmission.getVelWErr(), 1e-3);
        assertEquals(46.551, spectralLineEmission.getVelPeak(), 1e-3);

        // FLUX-RELATED (Simple)
        assertEquals(423.501, spectralLineEmission.getIntegFlux(), 1e-3);
        assertEquals(8.161, spectralLineEmission.getIntegFluxErr(), 1e-3);
        assertEquals(10574.080, spectralLineEmission.getFluxVoxelMax(), 1e-3);
        assertEquals(19.678, spectralLineEmission.getFluxVoxelMin(), 1e-3);
        assertEquals(21.943, spectralLineEmission.getFluxVoxelMean(), 1e-3);
        assertEquals(55.236, spectralLineEmission.getFluxVoxelStddev(), 1e-3);
        assertEquals(22.667, spectralLineEmission.getFluxVoxelRms(), 1e-3);
        assertEquals(993.945, spectralLineEmission.getRmsImagecube(), 01e-3);

        // SPECTRAL WIDTHS
        assertEquals(8.459, spectralLineEmission.getW50Freq(), 1e-4);
        assertEquals(89.6798, spectralLineEmission.getW50FreqErr(), 1e-4);
        assertEquals(53.4761, spectralLineEmission.getCw50Freq(), 1e-4);
        assertEquals(82.1394, spectralLineEmission.getCw50FreqErr(), 1e-4);
        assertEquals(23.6572, spectralLineEmission.getW20Freq(), 1e-4);
        assertEquals(26.3359, spectralLineEmission.getW20FreqErr(), 1e-4);
        assertEquals(97.4651, spectralLineEmission.getCw20Freq(), 1e-4);
        assertEquals(100.2357, spectralLineEmission.getCw20FreqErr(), 1e-4);
        assertEquals(66.9577, spectralLineEmission.getW50Vel(), 1e-4);
        assertEquals(158.9813, spectralLineEmission.getW50VelErr(), 1e-4);
        assertEquals(456.2346, spectralLineEmission.getCw50Vel(), 1e-4);
        assertEquals(761.4697, spectralLineEmission.getCw50VelErr(), 1e-4);
        assertEquals(100.4332, spectralLineEmission.getW20Vel(), 1e-4);
        assertEquals(89.4798, spectralLineEmission.getW20VelErr(), 1e-4);
        assertEquals(156.3476, spectralLineEmission.getCw20Vel(), 1e-4);
        assertEquals(99.4567, spectralLineEmission.getCw20VelErr(), 1e-4);

        // SPECTRAL LOCATION (COMPLEX)
        assertEquals(102.497497, spectralLineEmission.getFreqW50ClipUw(), 1e-6);
        assertEquals(615.987005, spectralLineEmission.getFreqW50ClipUwErr(), 1e-6);
        assertEquals(17.171989, spectralLineEmission.getFreqCw50ClipUw(), 1e-6);
        assertEquals(53.353131, spectralLineEmission.getFreqCw50ClipUwErr(), 1e-6);
        assertEquals(12.457775, spectralLineEmission.getFreqW20ClipUw(), 1e-6);
        assertEquals(45.659067, spectralLineEmission.getFreqW20ClipUwErr(), 1e-6);
        assertEquals(77.829538, spectralLineEmission.getFreqCw20ClipUw(), 1e-6);
        assertEquals(55.172829, spectralLineEmission.getFreqCw20ClipUwErr(), 1e-6);
        assertEquals(34.794, spectralLineEmission.getVelW50ClipUw(), 1e-3);
        assertEquals(95.316, spectralLineEmission.getVelW50ClipUwErr(), 1e-3);
        assertEquals(1.917, spectralLineEmission.getVelCw50ClipUw(), 1e-3);
        assertEquals(64.756, spectralLineEmission.getVelCw50ClipUwErr(), 1e-3);
        assertEquals(0.557, spectralLineEmission.getVelW20ClipUw(), 1e-3);
        assertEquals(36.467, spectralLineEmission.getVelW20ClipUwErr(), 1e-3);
        assertEquals(1.111, spectralLineEmission.getVelCw20ClipUw(), 1e-3);
        assertEquals(2.222, spectralLineEmission.getVelCw20ClipUwErr(), 1e-3);
        assertEquals(3.333, spectralLineEmission.getFreqW50ClipW(), 1e-6);
        assertEquals(4.444444, spectralLineEmission.getFreqW50ClipWErr(), 1e-6);
        assertEquals(5.555555, spectralLineEmission.getFreqCw50ClipW(), 1e-6);
        assertEquals(6.666666, spectralLineEmission.getFreqCw50ClipWErr(), 1e-6);
        assertEquals(7.777777, spectralLineEmission.getFreqW20ClipW(), 1e-6);
        assertEquals(8.888888, spectralLineEmission.getFreqW20ClipWErr(), 1e-6);
        assertEquals(9.999999, spectralLineEmission.getFreqCw20ClipW(), 1e-6);
        assertEquals(6.548443, spectralLineEmission.getFreqCw20ClipWErr(), 1e-6);
        assertEquals(346.657, spectralLineEmission.getVelW50ClipW(), 1e-3);
        assertEquals(67.915, spectralLineEmission.getVelW50ClipWErr(), 1e-3);
        assertEquals(9.648, spectralLineEmission.getVelCw50ClipW(), 1e-3);
        assertEquals(95.123, spectralLineEmission.getVelCw50ClipWErr(), 1e-3);
        assertEquals(66.987, spectralLineEmission.getVelW20ClipW(), 1e-3);
        assertEquals(18.763, spectralLineEmission.getVelW20ClipWErr(), 1e-3);
        assertEquals(999.111, spectralLineEmission.getVelCw20ClipW(), 1e-3);
        assertEquals(664.557, spectralLineEmission.getVelCw20ClipWErr(), 1e-3);

        // FLUX-RELATED (complex)
        assertEquals(8.652, spectralLineEmission.getIntegFluxW50Clip(), 1e-3);
        assertEquals(7.854, spectralLineEmission.getIntegFluxW50ClipErr(), 1e-3);
        assertEquals(1.258, spectralLineEmission.getIntegFluxCw50Clip(), 1e-3);
        assertEquals(3.698, spectralLineEmission.getIntegFluxCw50ClipErr(), 1e-3);
        assertEquals(7.532, spectralLineEmission.getIntegFluxW20Clip(), 1e-3);
        assertEquals(9.512, spectralLineEmission.getIntegFluxW20ClipErr(), 1e-3);
        assertEquals(66.159, spectralLineEmission.getIntegFluxCw20Clip(), 1e-3);
        assertEquals(84.357, spectralLineEmission.getIntegFluxCw20ClipErr(), 1e-3);

        // BUSY-FUNCTION PARAMETERS
        assertEquals(16.951, spectralLineEmission.getBfA(), 1e-3);
        assertEquals(12.753, spectralLineEmission.getBfAErr(), 1e-3);
        assertEquals(36.363636, spectralLineEmission.getBfW(), 1e-6);
        assertEquals(25.252525, spectralLineEmission.getBfWErr(), 1e-6);
        assertEquals(1.369, spectralLineEmission.getBfB1(), 1e-3);
        assertEquals(3.258, spectralLineEmission.getBfB1Err(), 1e-3);
        assertEquals(8.741, spectralLineEmission.getBfB2(), 1e-3);
        assertEquals(7.852, spectralLineEmission.getBfB2Err(), 1e-3);
        assertEquals(222.664664, spectralLineEmission.getBfXe(), 1e-6);
        assertEquals(111.555555, spectralLineEmission.getBfXeErr(), 1e-6);
        assertEquals(717.436436, spectralLineEmission.getBfXp(), 1e-6);
        assertEquals(604.553553, spectralLineEmission.getBfXpErr(), 1e-6);
        assertEquals(4.963, spectralLineEmission.getBfC(), 1e-3);
        assertEquals(9.789, spectralLineEmission.getBfCErr(), 1e-3);
        assertEquals(8.123, spectralLineEmission.getBfN(), 1e-3);
        assertEquals(7.456, spectralLineEmission.getBfNErr(), 1e-3);

        // FLAGS
        assertEquals(1, spectralLineEmission.getFlagS1().intValue());
        assertEquals(0, spectralLineEmission.getFlagS2().intValue());
        assertEquals(2, spectralLineEmission.getFlagS3().intValue());

        // Other
        assertEquals(QualityLevel.NOT_VALIDATED, spectralLineEmission.getQualityLevel());
        assertEquals(2.59224908, catalogue.getEmMin(), 1e-8);
        assertEquals(2.59224908, catalogue.getEmMax(), 1e-8);
        
    }

    @Test
    @Transactional
    public void testValidationMode() throws Exception
    {
        String sbid = "12345";
        String depositDir = "src/test/resources/functional_test/";
        importObservation(sbid, depositDir + "observation.xml");

        String catalogueDatafile = "spectralLineEmission.xml";
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "spectral-line-emission", "-parent-id", sbid,
                    "-catalogue-filename", catalogueDatafile, "-infile", depositDir + catalogueDatafile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Spectral line emission catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        long count = spectralLineEmissionRepository.count();
        catalogueCommandLineImporter = context.getBeanFactory().createBean(CatalogueCommandLineImporter.class);
        try
        {
            catalogueCommandLineImporter.run("-catalogue-type", "Spectral-line-emission", "-parent-id", sbid,
                    "-catalogue-filename", catalogueDatafile, "-infile", depositDir + catalogueDatafile,
                    "-validate-only");
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            assertEquals("Spectral line emission catalogue data import failed - please check log for details", 0,
                    e.getStatus().intValue());
        }

        assertEquals(count, spectralLineEmissionRepository.count());
        assertThat(out.toString(CharEncoding.UTF_8).split(System.lineSeparator()),
                arrayContaining("Catalogue record for filename '" + catalogueDatafile + "' on Observation '" + sbid
                        + "' already has catalogue entries"));
    }
}
