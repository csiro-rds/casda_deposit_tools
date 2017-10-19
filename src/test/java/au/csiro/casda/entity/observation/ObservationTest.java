package au.csiro.casda.entity.observation;

import static au.csiro.casda.entity.observation.ConstraintViolationExceptionMatcher.constraintViolation;
import static au.csiro.casda.entity.observation.TestAssertionMatcher.assertions;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.test.context.ContextConfiguration;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.MeasurementSetRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ScanRepository;
import au.csiro.casda.entity.AbstractPersistenceTest;

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
 * Observation test
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
public class ObservationTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ProjectRepository projectRepository;

    private ObservationRepository observationRepository;

    private ImageCubeRepository imageCubeRepository;

    private CatalogueRepository catalogueRepository;

    private MeasurementSetRepository measurementSetRepository;

    private ScanRepository scanRepository;

    private EvaluationFileRepository evaluationFileRepository;

    public ObservationTest() throws Exception
    {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeRepositories(RepositoryFactorySupport rfs)
    {
        projectRepository = rfs.getRepository(ProjectRepository.class);
        observationRepository = rfs.getRepository(ObservationRepository.class);
        imageCubeRepository = rfs.getRepository(ImageCubeRepository.class);
        catalogueRepository = rfs.getRepository(CatalogueRepository.class);
        measurementSetRepository = rfs.getRepository(MeasurementSetRepository.class);
        scanRepository = rfs.getRepository(ScanRepository.class);
        evaluationFileRepository = rfs.getRepository(EvaluationFileRepository.class);
    }

    @Test
    public void testBuilder()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().build();
        assertThat(observation.getCatalogues(), is(empty()));
        assertThat(observation.getImageCubes(), is(empty()));
        assertThat(observation.getMeasurementSets(), is(empty()));
        assertThat(observation.getProjects(), is(empty()));
        // All other default property values are tested in this and other entity test cases.
    }

    @Test
    public void testSbidIsRequired()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "sbid", "may not be null"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setSbid(null).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testSbidIsGreaterThanZero()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "sbid", "must be greater than or equal to 1"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setSbid(0).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testTelescopeIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "telescope", "may not be null"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setTelescope(null).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testTelescopeIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(Observation.class, "telescope", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setTelescope("").build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testObsProgramIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "obsProgram", "may not be null"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setObsProgram(null).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testObsProgramIsRequiredNotEmpty()
    {
        exception
                .expect(allOf(
                        constraintViolation(Observation.class, "obsProgram", "size must be between 1 and "
                                + Integer.MAX_VALUE), assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setObsProgram("").build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testObsStartIsRequired()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "obsStart", "may not be null"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setObsStart(null).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testObsStartMjdIsRequired()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "obsStartMjd", "may not be null"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setObsStartMjd(null).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testObsEndIsRequired()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "obsEnd", "may not be null"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setObsEnd(null).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testObsEndMjdIsRequired()
    {
        exception.expect(allOf(constraintViolation(Observation.class, "obsEndMjd", "may not be null"),
                assertions((e) -> assertPersistedObservationCount(0L))));
        Observation observation = TestObservationBuilderFactory.createBuilder().setObsEndMjd(null).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testPersistenceWithNoDepositableArtefacts()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().setSbid(12345).build();
        assertThat(observationRepository.count(), is(0L));
        observationRepository.save(observation);
        assertThat(observationRepository.count(), is(1L));
        Observation savedObservation = observationRepository.findAll().iterator().next();
        assertThat(savedObservation, notNullValue());
    }

    @Test
    public void testCascadeCreationOfImageCubes()
    {
        assertThat(imageCubeRepository.count(), is(0L));

        Integer sbid = 12345;
        String imageCube1Filename = "imageCube1";
        String imageCube2Filename = "imageCube2";
        String imageCube3Filename = "imageCube3";

        Observation observation = TestObservationBuilderFactory.createBuilder().setSbid(sbid).build();
        Project project1 = TestProjectBuilderFactory.createBuilder().build();
        Project project2 = TestProjectBuilderFactory.createBuilder().build();

        ImageCube imageCube1 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observation).setFilename(imageCube1Filename)
                        .setProject(project1).build();

        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(imageCubeRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));

        observation = observationRepository.findAll().iterator().next();
        project1 = projectRepository.findAll().iterator().next();

        ImageCube imageCube2 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observation).setFilename(imageCube2Filename)
                        .setProject(project1).build();

        /*
         * Trying to save this at the observation level causes a problem with Hibernate not being able to work out that
         * the image cube is actually valid. This is probably because of there are two objects that have the image cube
         * as a child: Observation and Project. Indeed, not assigning the image cube to the Project's imageCubes
         * collection makes this work using observationRepository.
         */
        imageCubeRepository.save(imageCube2);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(imageCubeRepository.count(), is(2L));
        assertThat(projectRepository.count(), is(1L));

        observation = observationRepository.findAll().iterator().next();

        ImageCube imageCube3 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observation).setFilename(imageCube3Filename)
                        .setProject(project2).build();

        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(imageCubeRepository.count(), is(3L));
        assertThat(projectRepository.count(), is(2L));

        /*
         * We do this check last otherwise the act of getting the imageCubes from the observation before we try and add
         * a new one masks a subtle bug in the implementation of getImageCubes.
         */
        observation = observationRepository.findAll().iterator().next();
        imageCube1 = imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCube1Filename);
        imageCube2 = imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCube2Filename);
        imageCube3 = imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCube3Filename);

        assertThat(observation.getImageCubes(), containsInAnyOrder(imageCube1, imageCube2, imageCube3));
        assertThat(
                observation.getImageCubes().stream()
                        .allMatch((i) -> i.getParent() == observationRepository.findAll().iterator().next()), is(true));
    }

    @Test
    public void testCascadeDeletionOfImageCubes()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().build();
        observation.addImageCube(TestImageCubeBuilderFactory.createBuilder().setParent(observation).build());
        observation.addImageCube(TestImageCubeBuilderFactory.createBuilder().setParent(observation).build());
        observation.addImageCube(TestImageCubeBuilderFactory.createBuilder().setParent(observation).build());
        observationRepository.save(observation);

        assertThat(observationRepository.count(), is(1L));
        assertThat(imageCubeRepository.count(), is(3L));

        observationRepository.delete(observation);
        assertThat(observationRepository.count(), is(0L));
        assertThat(imageCubeRepository.count(), is(0L));
    }

    @Test
    public void testCascadeCreationOfCatalogues()
    {
        assertThat(catalogueRepository.count(), is(0L));

        Integer sbid = 12345;
        String catalogue1Filename = "catalogue1";
        String catalogue2Filename = "catalogue2";
        String catalogue3Filename = "catalogue3";

        Observation observation = TestObservationBuilderFactory.createBuilder().setSbid(sbid).build();
        Project project1 = TestProjectBuilderFactory.createBuilder().build();
        Project project2 = TestProjectBuilderFactory.createBuilder().build();

        Catalogue catalogue1 =
                TestCatalogueBuilderFactory.createBuilder().setParent(observation).setFilename(catalogue1Filename)
                        .setProject(project1).build();

        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(catalogueRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));

        observation = observationRepository.findAll().iterator().next();
        project1 = projectRepository.findAll().iterator().next();

        Catalogue catalogue2 =
                TestCatalogueBuilderFactory.createBuilder().setParent(observation).setFilename(catalogue2Filename)
                        .setProject(project1).build();

        /*
         * Trying to save this at the observation level causes a problem with Hibernate not being able to work out that
         * the catalogue is actually valid. This is probably because of there are two objects that have the catalogue as
         * a child: Observation and Project. Indeed, not assigning the catalogue to the Project's catalogues collection
         * makes this work using observationRepository.
         */
        catalogueRepository.save(catalogue2);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(catalogueRepository.count(), is(2L));
        assertThat(projectRepository.count(), is(1L));

        observation = observationRepository.findAll().iterator().next();

        Catalogue catalogue3 =
                TestCatalogueBuilderFactory.createBuilder().setParent(observation).setFilename(catalogue3Filename)
                        .setProject(project2).build();

        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(catalogueRepository.count(), is(3L));
        assertThat(projectRepository.count(), is(2L));

        /*
         * We do this check last otherwise the act of getting the catalogues from the observation before we try and add
         * a new one masks a subtle bug in the implementation of getCatalogues.
         */
        observation = observationRepository.findAll().iterator().next();
        catalogue1 = catalogueRepository.findByObservationSbidAndFilename(sbid, catalogue1Filename);
        catalogue2 = catalogueRepository.findByObservationSbidAndFilename(sbid, catalogue2Filename);
        catalogue3 = catalogueRepository.findByObservationSbidAndFilename(sbid, catalogue3Filename);

        assertThat(observation.getCatalogues(), containsInAnyOrder(catalogue1, catalogue2, catalogue3));
        assertThat(
                observation.getCatalogues().stream()
                        .allMatch((i) -> i.getParent() == observationRepository.findAll().iterator().next()), is(true));
    }

    @Test
    public void testCascadeDeletionOfCatalogues()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().build();
        observation.addCatalogue(TestCatalogueBuilderFactory.createBuilder().setParent(observation)
                .setImageCube(TestImageCubeBuilderFactory.createBuilder().setParent(observation).build()).build());
        observation.addCatalogue(TestCatalogueBuilderFactory.createBuilder().setParent(observation)
                .setImageCube(TestImageCubeBuilderFactory.createBuilder().setParent(observation).build()).build());
        observation.addCatalogue(TestCatalogueBuilderFactory.createBuilder().setParent(observation)
                .setImageCube(TestImageCubeBuilderFactory.createBuilder().setParent(observation).build()).build());
        observationRepository.save(observation);

        assertThat(observationRepository.count(), is(1L));
        assertThat(catalogueRepository.count(), is(3L));

        observationRepository.delete(observation);
        assertThat(observationRepository.count(), is(0L));
        assertThat(catalogueRepository.count(), is(0L));
    }

    @Test
    public void testCascadeCreationOfMeasurementSets()
    {
        assertThat(measurementSetRepository.count(), is(0L));
        assertThat(scanRepository.count(), is(0L));

        Integer sbid = 12345;
        String measurementSet1Filename = "measurementSet1";
        String measurementSet2Filename = "measurementSet2";
        String measurementSet3Filename = "measurementSet3";

        Observation observation = TestObservationBuilderFactory.createBuilder().setSbid(sbid).build();
        Project project1 = TestProjectBuilderFactory.createBuilder().build();
        Project project2 = TestProjectBuilderFactory.createBuilder().build();

        MeasurementSet measurementSet1 =
                TestMeasurementSetBuilderFactory.createBuilder().setParent(observation)
                        .setFilename(measurementSet1Filename).setProject(project1).build();

        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(measurementSetRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));
        assertThat(scanRepository.count(), is(1L));

        observation = observationRepository.findAll().iterator().next();
        project1 = projectRepository.findAll().iterator().next();

        MeasurementSet measurementSet2 =
                TestMeasurementSetBuilderFactory.createBuilder().setParent(observation)
                        .setFilename(measurementSet2Filename).setProject(project1).build();

        /*
         * Trying to save this at the observation level causes a problem with Hibernate not being able to work out that
         * the measurement set is actually valid. This is probably because of there are two objects that have the
         * measurement set as a child: Observation and Project. Indeed, not assigning the measurementSet to the
         * Project's measurementSets collection makes this work using observationRepository.
         */
        measurementSetRepository.save(measurementSet2);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(measurementSetRepository.count(), is(2L));
        assertThat(scanRepository.count(), is(2L));
        assertThat(projectRepository.count(), is(1L));

        observation = observationRepository.findAll().iterator().next();

        MeasurementSet measurementSet3 =
                TestMeasurementSetBuilderFactory.createBuilder().setParent(observation)
                        .setFilename(measurementSet3Filename).setProject(project2).build();

        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(measurementSetRepository.count(), is(3L));
        assertThat(scanRepository.count(), is(3L));
        assertThat(projectRepository.count(), is(2L));

        /*
         * We do this check last otherwise the act of getting the measurementSets from the observation before we try and
         * add a new one masks a subtle bug in the implementation of getMeasurementSets.
         */
        observation = observationRepository.findAll().iterator().next();
        measurementSet1 = measurementSetRepository.findByObservationSbidAndFilename(sbid, measurementSet1Filename);
        measurementSet2 = measurementSetRepository.findByObservationSbidAndFilename(sbid, measurementSet2Filename);
        measurementSet3 = measurementSetRepository.findByObservationSbidAndFilename(sbid, measurementSet3Filename);

        assertThat(observation.getMeasurementSets(),
                containsInAnyOrder(measurementSet1, measurementSet2, measurementSet3));
        assertThat(
                observation.getMeasurementSets().stream()
                        .allMatch((i) -> i.getParent() == observationRepository.findAll().iterator().next()), is(true));
    }

    @Test
    public void testCascadeDeletionOfMeasurementSets()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().build();
        observation.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder().setParent(observation).build());
        observation.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder().setParent(observation).build());
        observation.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder().setParent(observation).build());
        observationRepository.save(observation);

        assertThat(observationRepository.count(), is(1L));
        assertThat(measurementSetRepository.count(), is(3L));

        observationRepository.delete(observation);
        assertThat(observationRepository.count(), is(0L));
        assertThat(measurementSetRepository.count(), is(0L));
    }

    @Test
    public void testCascadeCreationOfEvaluationFiles()
    {
        assertThat(evaluationFileRepository.count(), is(0L));

        Integer sbid = 12345;
        String evaluationFile1Filename = "evaluationFile1";
        String evaluationFile2Filename = "evaluationFile2";
        String evaluationFile3Filename = "evaluationFile3";

        Observation observation = TestObservationBuilderFactory.createBuilder().setSbid(sbid).build();

        EvaluationFile evaluationFile1 =
                TestEvaluationFileBuilderFactory.createBuilder().setParent(observation)
                        .setFilename(evaluationFile1Filename).build();
        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(evaluationFileRepository.count(), is(1L));

        observation = observationRepository.findAll().iterator().next();

        EvaluationFile evaluationFile2 =
                TestEvaluationFileBuilderFactory.createBuilder().setParent(observation)
                        .setFilename(evaluationFile2Filename).build();
        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(evaluationFileRepository.count(), is(2L));

        observation = observationRepository.findAll().iterator().next();

        EvaluationFile evaluationFile3 =
                TestEvaluationFileBuilderFactory.createBuilder().setParent(observation)
                        .setFilename(evaluationFile3Filename).build();
        observationRepository.save(observation);

        commit();

        assertThat(observationRepository.count(), is(1L));
        assertThat(evaluationFileRepository.count(), is(3L));

        /*
         * We do this check last otherwise the act of getting the evaluationFiles from the observation before we try and
         * add a new one masks a subtle bug in the implementation of getEvaluationFiles.
         */
        observation = observationRepository.findAll().iterator().next();
        evaluationFile1 = evaluationFileRepository.findByObservationSbidAndFilename(sbid, evaluationFile1Filename);
        evaluationFile2 = evaluationFileRepository.findByObservationSbidAndFilename(sbid, evaluationFile2Filename);
        evaluationFile3 = evaluationFileRepository.findByObservationSbidAndFilename(sbid, evaluationFile3Filename);

        assertThat(observation.getEvaluationFiles(),
                containsInAnyOrder(evaluationFile1, evaluationFile2, evaluationFile3));
        assertThat(
                observation.getEvaluationFiles().stream()
                        .allMatch((i) -> i.getParent() == observationRepository.findAll().iterator().next()), is(true));
    }

    @Test
    public void testCascadeDeletionOfEvaluationFiles()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().build();
        observation.addEvaluationFile(TestEvaluationFileBuilderFactory.createBuilder().setParent(observation).build());
        observation.addEvaluationFile(TestEvaluationFileBuilderFactory.createBuilder().setParent(observation).build());
        observation.addEvaluationFile(TestEvaluationFileBuilderFactory.createBuilder().setParent(observation).build());
        observationRepository.save(observation);

        assertThat(observationRepository.count(), is(1L));
        assertThat(evaluationFileRepository.count(), is(3L));

        observationRepository.delete(observation);
        assertThat(observationRepository.count(), is(0L));
        assertThat(evaluationFileRepository.count(), is(0L));
    }

    @Test
    public void testGetProjectsForNoDepositableArtefacts()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().build();
        observationRepository.save(observation);

        Observation savedObservation = observationRepository.findAll().iterator().next();
        assertThat(savedObservation.getProjects(), empty());
    }

    @Test
    public void testGetProjects()
    {
        Observation observation = TestObservationBuilderFactory.createBuilder().build();
        Project projectForImageCube1 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("projectForImageCube1").build();
        Project projectForImageCube2 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("projectForImageCube2").build();
        Project sharedProjectForImageCube3AndCatalogue3 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("sharedProjectForImageCube3AndCatalogue3")
                        .build();
        Project sharedProjectForImageCube3AndMeasurementSet3 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("sharedProjectForImageCube3AndMeasurementSet3")
                        .build();
        Project projectForCatalogue1 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("projectForCatalogue1").build();
        Project projectForCatalogue2 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("projectForCatalogue2").build();
        Project sharedProjectForCatalogue4AndMeasurementSet3 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("sharedProjectForCatalogue4AndMeasurementSet3")
                        .build();
        Project projectForMeasurementSet1 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("projectForMeasurementSet1").build();
        Project projectForMeasurementSet2 =
                TestProjectBuilderFactory.createBuilder().setOpalCode("projectForMeasurementSet2").build();
        Project sharedProjectForImageCube4Catalogue5AndMeasurementSet4 =
                TestProjectBuilderFactory.createBuilder()
                        .setOpalCode("sharedProjectForImageCube4Catalogue5AndMeasurementSet4").build();

        ImageCube imageCube1 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observation).setProject(projectForImageCube1)
                        .build();
        observation.addImageCube(imageCube1);

        ImageCube imageCube2 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observation).setProject(projectForImageCube2)
                        .build();
        observation.addImageCube(imageCube2);

        ImageCube imageCube3 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observation)
                        .setProject(sharedProjectForImageCube3AndCatalogue3).build();
        observation.addImageCube(imageCube3);

        ImageCube imageCube4 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observation)
                        .setProject(sharedProjectForImageCube4Catalogue5AndMeasurementSet4).build();
        observation.addImageCube(imageCube4);

        Catalogue catalogue1 =
                TestCatalogueBuilderFactory.createBuilder().setProject(projectForCatalogue1).setParent(observation)
                        .build();
        observation.addCatalogue(catalogue1);

        Catalogue catalogue2 =
                TestCatalogueBuilderFactory.createBuilder().setProject(projectForCatalogue2).setParent(observation)
                        .build();
        observation.addCatalogue(catalogue2);

        Catalogue catalogue3 =
                TestCatalogueBuilderFactory.createBuilder().setProject(sharedProjectForImageCube3AndCatalogue3)
                        .setParent(observation).build();
        observation.addCatalogue(catalogue3);

        Catalogue catalogue4 =
                TestCatalogueBuilderFactory.createBuilder().setProject(sharedProjectForCatalogue4AndMeasurementSet3)
                        .setParent(observation).build();
        observation.addCatalogue(catalogue4);

        Catalogue catalogue5 =
                TestCatalogueBuilderFactory.createBuilder()
                        .setProject(sharedProjectForImageCube4Catalogue5AndMeasurementSet4).setParent(observation)
                        .build();
        observation.addCatalogue(catalogue5);

        MeasurementSet measurementSet1 =
                TestMeasurementSetBuilderFactory.createBuilder().setParent(observation)
                        .setProject(projectForMeasurementSet1).build();
        observation.addMeasurementSet(measurementSet1);

        MeasurementSet measurementSet2 =
                TestMeasurementSetBuilderFactory.createBuilder().setParent(observation)
                        .setProject(projectForMeasurementSet2).build();
        measurementSet2.setProject(projectForMeasurementSet2);
        observation.addMeasurementSet(measurementSet2);

        MeasurementSet measurementSet3 =
                TestMeasurementSetBuilderFactory.createBuilder().setParent(observation)
                        .setProject(sharedProjectForImageCube3AndMeasurementSet3).build();
        observation.addMeasurementSet(measurementSet3);

        MeasurementSet measurementSet4 =
                TestMeasurementSetBuilderFactory.createBuilder().setParent(observation)
                        .setProject(sharedProjectForImageCube4Catalogue5AndMeasurementSet4).build();
        observation.addMeasurementSet(measurementSet4);

        observationRepository.save(observation);

        commit();

        assertThat(projectRepository.count(), is(10L));

        observation = observationRepository.findAll().iterator().next();
        projectForImageCube1 = projectRepository.findByOpalCode("projectForImageCube1");
        projectForImageCube2 = projectRepository.findByOpalCode("projectForImageCube2");
        sharedProjectForImageCube3AndCatalogue3 =
                projectRepository.findByOpalCode("sharedProjectForImageCube3AndCatalogue3");
        sharedProjectForImageCube3AndMeasurementSet3 =
                projectRepository.findByOpalCode("sharedProjectForImageCube3AndMeasurementSet3");
        projectForCatalogue1 = projectRepository.findByOpalCode("projectForCatalogue1");
        projectForCatalogue2 = projectRepository.findByOpalCode("projectForCatalogue2");
        sharedProjectForCatalogue4AndMeasurementSet3 =
                projectRepository.findByOpalCode("sharedProjectForCatalogue4AndMeasurementSet3");
        projectForMeasurementSet1 = projectRepository.findByOpalCode("projectForMeasurementSet1");
        projectForMeasurementSet2 = projectRepository.findByOpalCode("projectForMeasurementSet2");
        sharedProjectForImageCube4Catalogue5AndMeasurementSet4 =
                projectRepository.findByOpalCode("sharedProjectForImageCube4Catalogue5AndMeasurementSet4");

        Project[] projects =
                new Project[] { projectForImageCube1, projectForImageCube2, sharedProjectForImageCube3AndCatalogue3,
                        sharedProjectForImageCube3AndMeasurementSet3, projectForCatalogue1, projectForCatalogue2,
                        sharedProjectForCatalogue4AndMeasurementSet3, projectForMeasurementSet1,
                        projectForMeasurementSet2, sharedProjectForImageCube4Catalogue5AndMeasurementSet4 };
        assertThat(observation.getProjects().size(), is(projects.length));
        assertThat(observation.getProjects(), containsInAnyOrder(projects));
    }

    private void assertPersistedObservationCount(long count)
    {
        /*
         * This has to be independent of the observationRepository in the test case for the following reasons: a) all
         * the repositories in the test case use the same EntityManager b) the EntityManager may not have had changes
         * flushed c) the EntityManager may be in a bad state due to something like a ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getObservationCount(), is(count));
    }
}
