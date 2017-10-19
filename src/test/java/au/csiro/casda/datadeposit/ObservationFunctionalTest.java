package au.csiro.casda.datadeposit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.transaction.Transactional;

import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;

import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.CatalogueType;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.MeasurementSet;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.Scan;

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
 * Functional test for observation import.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public class ObservationFunctionalTest extends FunctionalTestBase
{
    public ObservationFunctionalTest() throws Exception
    {
        super();
    }

    @Test
    @Transactional
    public void testObservationImport()
    {
        String sbid = "12345";
        String observationMetadataFile = "src/test/resources/functional_test/observation.xml";
        assertThat(observationRepository.count(), is(0L));
        assertThat(catalogueRepository.count(), is(0L));
        assertThat(continuumComponentRepository.count(), is(0L));
        assertThat(continuumIslandRepository.count(), is(0L));
        assertThat(polarisationComponentRepository.count(), is(0L));
        assertThat(imageCubeRepository.count(), is(0L));
        assertThat(evaluationFileRepository.count(), is(0L));
        assertThat(measurementSetRepository.count(), is(0L));
        assertThat(scanRepository.count(), is(0L));
        try
        {
            observationCommandLineImporter.run("-sbid", sbid, "-infile", observationMetadataFile);
            failTestCase();
        }
        catch (CheckExitCalled e) // expected
        {
            checkForNormalExit(e);
        }
        assertThat(observationRepository.count(), is(1L));
        Observation observation = observationRepository.findAll().iterator().next();
        assertThat(observation.getSbid(), is(12345));
        assertThat(observation.getObsProgram(), is(equalTo("test")));
        assertThat(observation.getObsStart().toString(ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)),
                equalTo("2013-11-19T02:30:00.000" + "Z"));
        assertThat(observation.getObsEnd().toString(ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)),
                equalTo("2013-11-19T03:30:00.000" + "Z"));
        assertThat(observation.getTelescope(), equalTo("ASKAP"));

        assertThat(catalogueRepository.count(), is(7L));
        for (Catalogue catalogue : catalogueRepository.findAll())
        {
            assertThat(observation.getCatalogues().contains(catalogue), is(true));
        }

        List<Catalogue> continuumComponentCatalogues =
                observation.getCataloguesOfType(CatalogueType.CONTINUUM_COMPONENT);
        assertThat(continuumComponentCatalogues.size(), is(2));
        Catalogue continuumComponentCatalogue = continuumComponentCatalogues.get(0);
        assertThat(continuumComponentCatalogue.getFormat(), is("votable"));
        assertThat(continuumComponentCatalogue.getFilename(), is("selavy-results.components-new.xml"));
        assertThat(continuumComponentCatalogue.getProject().getShortName(), is("AS007"));
        continuumComponentCatalogue = continuumComponentCatalogues.get(1);
        assertThat(continuumComponentCatalogue.getFormat(), is("votable"));
        assertThat(continuumComponentCatalogue.getFilename(), is("selavy-results.components.xml"));
        assertThat(continuumComponentCatalogue.getProject().getShortName(), is("AS007"));

        List<Catalogue> continuumIslandCatalogues = observation.getCataloguesOfType(CatalogueType.CONTINUUM_ISLAND);
        assertThat(continuumIslandCatalogues.size(), is(2));
        Catalogue continuumIslandCatalogue = continuumIslandCatalogues.get(0);
        assertThat(continuumIslandCatalogue.getFormat(), is("votable"));
        assertThat(continuumIslandCatalogue.getFilename(), is("selavy-results.islands-new.xml"));
        assertThat(continuumIslandCatalogue.getProject().getShortName(), is("AS007"));
        continuumIslandCatalogue = continuumIslandCatalogues.get(1);
        assertThat(continuumIslandCatalogue.getFormat(), is("votable"));
        assertThat(continuumIslandCatalogue.getFilename(), is("selavy-results.islands.xml"));
        assertThat(continuumIslandCatalogue.getProject().getShortName(), is("AS007"));

        List<Catalogue> polarisationComponentCatalogues =
                observation.getCataloguesOfType(CatalogueType.POLARISATION_COMPONENT);
        assertThat(polarisationComponentCatalogues.size(), is(1));
        Catalogue polarisationComponentCatalogue = polarisationComponentCatalogues.get(0);
        assertThat(polarisationComponentCatalogue.getFormat(), is("votable"));
        assertThat(polarisationComponentCatalogue.getFilename(), is("sample-polarisation.xml"));
        assertThat(polarisationComponentCatalogue.getProject().getShortName(), is("AS007"));

        assertEquals(0, continuumComponentRepository.count());

        assertEquals(0, continuumIslandRepository.count());

        assertEquals(0, polarisationComponentRepository.count());

        assertEquals(1, imageCubeRepository.count());
        for (ImageCube imageCube : imageCubeRepository.findAll())
        {
            assertThat(observation.getImageCubes().contains(imageCube), is(true));
        }
        ImageCube imageCube = observation.getImageCubes().get(0);
        assertThat(imageCube.getFormat(), is("fits"));
        assertThat(imageCube.getFilename(), is("validFile.fits"));
        assertThat(imageCube.getCellSize(), is(nullValue()));
        assertThat(imageCube.getCentreFrequency(), is(nullValue()));
        assertThat(imageCube.getChannelWidth(), is(nullValue()));
        assertThat(imageCube.getDecDeg(), is(nullValue()));
        assertThat(imageCube.getEmMax(), is(nullValue()));
        assertThat(imageCube.getEmMin(), is(nullValue()));
        assertThat(imageCube.getEmResPower(), is(nullValue()));
        assertThat(imageCube.getFilesize(), is(9L));
        assertThat(imageCube.getTExptime(), is(nullValue()));
        assertThat(imageCube.getNoOfChannels(), is(nullValue()));
        assertThat(imageCube.getNoOfPixels(), is(nullValue()));
        assertThat(imageCube.getProject().getShortName(), is("AS007"));
        assertThat(imageCube.getRaDeg(), is(nullValue()));
        assertThat(imageCube.getSRegionPoly(), is(nullValue()));
        assertThat(imageCube.getSResolution(), is(nullValue()));
        assertThat(imageCube.getSResolutionMax(), is(nullValue()));
        assertThat(imageCube.getSResolutionMin(), is(nullValue()));
        assertThat(imageCube.getStokesParameters(), is(nullValue()));
        assertThat(imageCube.getObjectName(), is(nullValue()));
        assertThat(imageCube.getTMax(), is(nullValue()));
        assertThat(imageCube.getTMin(), is(nullValue()));

        assertEquals(1, measurementSetRepository.count());

        for (MeasurementSet measurementSet : measurementSetRepository.findAll())
        {
            assertThat(observation.getMeasurementSets().contains(measurementSet), is(true));
        }
        MeasurementSet measurementSet = observation.getMeasurementSets().get(0);
        assertThat(measurementSet.getFilename(), is("no_file.ms.tar"));
        assertThat(measurementSet.getFormat(), is("tar"));
        assertThat(measurementSet.getProject().getShortName(), is("AS007"));
        assertEquals(0.193165773, measurementSet.getEmMin(), 0.000001);
        assertEquals(0.261682133, measurementSet.getEmMax(), 0.000001);
        assertEquals(2, measurementSet.getScans().size());
        assertEquals(1L, measurementSet.getFilesize().longValue());

        assertEquals(2, scanRepository.count());

        // select them in order so we can test the values are set correctly
        Scan firstScan = measurementSet.getScans().stream().filter(scan -> scan.getScanId() == 0).findFirst().get();
        Scan secondScan = measurementSet.getScans().stream().filter(scan -> scan.getScanId() == 1).findFirst().get();

        assertThat(firstScan.getScanId(), is(0));
        assertThat(firstScan.getScanStart().toDateTime(DateTimeZone.UTC).toString(), is("2013-11-19T02:30:00.000Z"));
        assertThat(firstScan.getScanEnd().toDateTime(DateTimeZone.UTC).toString(), is("2013-11-19T03:00:00.000Z"));
        assertThat(firstScan.getCoordSystem(), is("J2000"));
        assertThat(firstScan.getFieldCentreX(), is(-3.02));
        assertThat(firstScan.getFieldCentreY(), is(-0.785));
        assertThat(firstScan.getFieldName(), is("Fornax"));
        assertThat(firstScan.getPolarisations(), is("/XX/XY/YX/YY/"));
        assertThat(firstScan.getNumChannels(), is(16416));
        assertThat(firstScan.getCentreFrequency(), is(1400000000.0));
        assertThat(firstScan.getChannelWidth(), is(18518.0));

        assertThat(secondScan.getScanId(), is(1));
        assertThat(secondScan.getScanStart().toDateTime(DateTimeZone.UTC).toString(), is("2013-11-19T03:00:00.000Z"));
        assertThat(secondScan.getScanEnd().toDateTime(DateTimeZone.UTC).toString(), is("2013-11-19T03:30:00.000Z"));
        assertThat(secondScan.getCoordSystem(), is("J2000"));
        assertThat(secondScan.getFieldCentreX(), is(-2.12));
        assertThat(secondScan.getFieldCentreY(), is(-0.895));
        assertThat(secondScan.getFieldName(), is("Fornax-2"));
        assertThat(secondScan.getPolarisations(), is("/XX/YX/YY/"));
        assertThat(secondScan.getNumChannels(), is(16406));
        assertThat(secondScan.getCentreFrequency(), is(1300000000.0));
        assertThat(secondScan.getChannelWidth(), is(18818.0));

        for (Scan scan : scanRepository.findAll())
        {
            assertThat(measurementSet.getScans().contains(scan), is(true));
        }

        assertEquals(2, evaluationFileRepository.count());
        for (EvaluationFile evaluationFile : evaluationFileRepository.findAll())
        {
            assertThat(observation.getEvaluationFiles().contains(evaluationFile), is(true));
        }
        EvaluationFile evaluationFile = observation.getEvaluationFiles().get(0);
        assertThat(evaluationFile.getFilename(), is("no_file.pdf"));
        assertThat(evaluationFile.getFormat(), is("pdf"));
    }

}
