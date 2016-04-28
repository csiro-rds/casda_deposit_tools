package au.csiro.casda.entity.observation;

import static au.csiro.casda.entity.observation.ConstraintViolationExceptionMatcher.constraintViolation;
import static au.csiro.casda.entity.observation.TestAssertionMatcher.assertions;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.sql.SQLException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.test.context.ContextConfiguration;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
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
 * Project test
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
public class ProjectTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ProjectRepository projectRepository;

    private ObservationRepository observationRepository;

    public ProjectTest() throws Exception
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
    }

    @Test
    public void testBuilder()
    {
        Project project = TestProjectBuilderFactory.createBuilder().build();
        assertThat(project.getCatalogues(), is(empty()));
        assertThat(project.getImageCubes(), is(empty()));
        assertThat(project.getMeasurementSets(), is(empty()));
        assertThat(project.getObservations(), is(empty()));
        // All other default property values are tested in this and other entity test cases.
    }

    @Test
    public void testOpalCodeIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Project.class, "opalCode", "may not be null"),
                assertions((e) -> assertPersistedProjectCount(0L))));
        Project project = TestProjectBuilderFactory.createBuilder().setOpalCode(null).build();
        assertThat(projectRepository.count(), is(0L));
        projectRepository.save(project);
    }

    @Test
    public void testOpalCodeIsRequiredNotEmpty()
    {
        exception.expect(allOf(constraintViolation(Project.class, "opalCode", "must match \"([A-Za-z0-9])+\""),
                assertions((e) -> assertPersistedProjectCount(0L))));
        Project project = TestProjectBuilderFactory.createBuilder().setOpalCode("").build();
        assertThat(projectRepository.count(), is(0L));
        projectRepository.save(project);
    }

    @Test
    public void testOpalCodeMustMatchPattern()
    {
        exception.expect(allOf(constraintViolation(Project.class, "opalCode", "must match \"([A-Za-z0-9])+\""),
                assertions((e) -> assertPersistedProjectCount(0L))));
        Project project = TestProjectBuilderFactory.createBuilder().setOpalCode("_1A37").build();
        assertThat(projectRepository.count(), is(0L));
        projectRepository.save(project);
    }

    @Test
    public void testShortNameIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Project.class, "shortName", "may not be null"),
                assertions((e) -> assertPersistedProjectCount(0L))));
        Project project = TestProjectBuilderFactory.createBuilder().setShortName(null).build();
        assertThat(projectRepository.count(), is(0L));
        projectRepository.save(project);
    }

    @Test
    public void testShortNameIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(Project.class, "shortName", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedProjectCount(0L))));
        Project project = TestProjectBuilderFactory.createBuilder().setShortName("").build();
        assertThat(projectRepository.count(), is(0L));
        projectRepository.save(project);
    }

    @Test
    public void testPersistence()
    {
        assertThat(observationRepository.count(), is(0L));

        Project project = TestProjectBuilderFactory.createBuilder().build();

        projectRepository.save(project);

        commit();

        assertThat(projectRepository.count(), is(1L));
        Project savedProject = projectRepository.findAll().iterator().next();
        assertThat(savedProject, notNullValue());
    }

    @Test
    public void testDeletion()
    {
        Project project = TestProjectBuilderFactory.createBuilder().build();
        projectRepository.save(project);
        assertThat(projectRepository.count(), is(1L));

        commit();

        project = projectRepository.findAll().iterator().next();
        projectRepository.delete(project);
        assertThat(projectRepository.count(), is(0L));
    }

    @Test
    public void testGetObservationsForNoDepositableArtefacts()
    {
        Project project = TestProjectBuilderFactory.createBuilder().build();
        projectRepository.save(project);
        assertThat(projectRepository.count(), is(1L));

        commit();

        project = projectRepository.findAll().iterator().next();
        assertThat(project.getObservations(), empty());
    }

    @Test
    public void testGetObservations() throws SQLException
    {
        Project project = TestProjectBuilderFactory.createBuilder().build();
        projectRepository.save(project);
        commit();
        project = projectRepository.findAll().iterator().next();

        Observation observationWithOneImageCube = TestObservationBuilderFactory.createBuilder().setSbid(100100).build();
        observationWithOneImageCube.addImageCube(TestImageCubeBuilderFactory.createBuilder()
                .setParent(observationWithOneImageCube).setProject(project).build());
        observationRepository.save(observationWithOneImageCube);

        Observation observationWithTwoImageCubes =
                TestObservationBuilderFactory.createBuilder().setSbid(100200).build();
        observationWithTwoImageCubes.addImageCube(TestImageCubeBuilderFactory.createBuilder()
                .setParent(observationWithTwoImageCubes).setProject(project).build());
        observationWithTwoImageCubes.addImageCube(TestImageCubeBuilderFactory.createBuilder()
                .setParent(observationWithTwoImageCubes).setProject(project).build());
        observationRepository.save(observationWithTwoImageCubes);

        Observation observationWithOneCatalogue = TestObservationBuilderFactory.createBuilder().setSbid(100010).build();
        observationWithOneCatalogue.addCatalogue(TestCatalogueBuilderFactory.createBuilder().setProject(project)
                .setParent(observationWithOneCatalogue).setImageCube(null).build());
        observationRepository.save(observationWithOneCatalogue);

        Observation observationWithTwoCatalogues =
                TestObservationBuilderFactory.createBuilder().setSbid(100020).build();
        observationWithTwoCatalogues.addCatalogue(TestCatalogueBuilderFactory.createBuilder().setProject(project)
                .setParent(observationWithTwoCatalogues).setImageCube(null).build());
        observationWithTwoCatalogues.addCatalogue(TestCatalogueBuilderFactory.createBuilder().setProject(project)
                .setParent(observationWithTwoCatalogues).setImageCube(null).build());
        observationRepository.save(observationWithTwoCatalogues);

        Observation observationWithOneMeasurementSet =
                TestObservationBuilderFactory.createBuilder().setSbid(100001).build();
        observationWithOneMeasurementSet.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder()
                .setParent(observationWithOneMeasurementSet).setProject(project).build());
        observationRepository.save(observationWithOneMeasurementSet);

        Observation observationWithTwoMeasurementSets =
                TestObservationBuilderFactory.createBuilder().setSbid(100002).build();
        observationWithTwoMeasurementSets.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder()
                .setParent(observationWithTwoMeasurementSets).setProject(project).build());
        observationWithTwoMeasurementSets.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder()
                .setParent(observationWithTwoMeasurementSets).setProject(project).build());
        observationRepository.save(observationWithTwoMeasurementSets);

        Observation observationWithImageCubesAndCatalogues =
                TestObservationBuilderFactory.createBuilder().setSbid(100220).build();
        ImageCube imageCube1 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observationWithImageCubesAndCatalogues)
                        .setProject(project).build();
        ImageCube imageCube2 =
                TestImageCubeBuilderFactory.createBuilder().setParent(observationWithImageCubesAndCatalogues)
                        .setProject(project).build();
        observationWithImageCubesAndCatalogues.addImageCube(imageCube1);
        observationWithImageCubesAndCatalogues.addImageCube(imageCube2);
        observationWithImageCubesAndCatalogues.addCatalogue(TestCatalogueBuilderFactory.createBuilder()
                .setProject(project).setParent(observationWithImageCubesAndCatalogues).setImageCube(imageCube1)
                .build());
        observationWithImageCubesAndCatalogues.addCatalogue(TestCatalogueBuilderFactory.createBuilder()
                .setProject(project).setParent(observationWithImageCubesAndCatalogues).setImageCube(imageCube2)
                .build());
        observationRepository.save(observationWithImageCubesAndCatalogues);

        Observation observationWithImageCubesAndMeasurementSets =
                TestObservationBuilderFactory.createBuilder().setSbid(100022).build();
        observationWithImageCubesAndMeasurementSets.addImageCube(TestImageCubeBuilderFactory.createBuilder()
                .setParent(observationWithImageCubesAndMeasurementSets).setProject(project).build());
        observationWithImageCubesAndMeasurementSets.addImageCube(TestImageCubeBuilderFactory.createBuilder()
                .setParent(observationWithImageCubesAndMeasurementSets).setProject(project).build());
        observationWithImageCubesAndMeasurementSets.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder()
                .setParent(observationWithImageCubesAndMeasurementSets).setProject(project).build());
        observationWithImageCubesAndMeasurementSets.addMeasurementSet(TestMeasurementSetBuilderFactory.createBuilder()
                .setParent(observationWithImageCubesAndMeasurementSets).setProject(project).build());
        observationRepository.save(observationWithImageCubesAndMeasurementSets);

        Observation observationWithImageCubesAndCataloguesAndMeasurementSets =
                TestObservationBuilderFactory.createBuilder().setSbid(100222).build();
        ImageCube imageCube3 =
                TestImageCubeBuilderFactory.createBuilder()
                        .setParent(observationWithImageCubesAndCataloguesAndMeasurementSets).setProject(project)
                        .build();
        ImageCube imageCube4 =
                TestImageCubeBuilderFactory.createBuilder()
                        .setParent(observationWithImageCubesAndCataloguesAndMeasurementSets).setProject(project)
                        .build();
        observationWithImageCubesAndCataloguesAndMeasurementSets.addImageCube(imageCube3);
        observationWithImageCubesAndCataloguesAndMeasurementSets.addImageCube(imageCube4);
        observationWithImageCubesAndCataloguesAndMeasurementSets.addCatalogue(TestCatalogueBuilderFactory
                .createBuilder().setProject(project)
                .setParent(observationWithImageCubesAndCataloguesAndMeasurementSets).setImageCube(imageCube3)
                .build());
        observationWithImageCubesAndCataloguesAndMeasurementSets.addCatalogue(TestCatalogueBuilderFactory
                .createBuilder().setProject(project)
                .setParent(observationWithImageCubesAndCataloguesAndMeasurementSets).setImageCube(imageCube4)
                .build());
        observationWithImageCubesAndCataloguesAndMeasurementSets.addMeasurementSet(TestMeasurementSetBuilderFactory
                .createBuilder().setParent(observationWithImageCubesAndCataloguesAndMeasurementSets)
                .setProject(project).build());
        observationWithImageCubesAndCataloguesAndMeasurementSets.addMeasurementSet(TestMeasurementSetBuilderFactory
                .createBuilder().setParent(observationWithImageCubesAndCataloguesAndMeasurementSets)
                .setProject(project).build());
        observationRepository.save(observationWithImageCubesAndCataloguesAndMeasurementSets);

        commit();

        assertThat(projectRepository.count(), is(1L));
        assertThat(observationRepository.count(), is(9L));

        project = projectRepository.findAll().iterator().next();
        observationWithOneImageCube = observationRepository.findBySbid(100100);
        observationWithTwoImageCubes = observationRepository.findBySbid(100200);
        observationWithOneCatalogue = observationRepository.findBySbid(100010);
        observationWithTwoCatalogues = observationRepository.findBySbid(100020);
        observationWithOneMeasurementSet = observationRepository.findBySbid(100001);
        observationWithTwoMeasurementSets = observationRepository.findBySbid(100002);
        observationWithImageCubesAndCatalogues = observationRepository.findBySbid(100220);
        observationWithImageCubesAndMeasurementSets = observationRepository.findBySbid(100022);
        observationWithImageCubesAndCataloguesAndMeasurementSets = observationRepository.findBySbid(100222);

        Observation[] observations =
                new Observation[] { observationWithOneImageCube, observationWithTwoImageCubes,
                        observationWithOneCatalogue, observationWithTwoCatalogues, observationWithOneMeasurementSet,
                        observationWithTwoMeasurementSets, observationWithImageCubesAndCatalogues,
                        observationWithImageCubesAndMeasurementSets,
                        observationWithImageCubesAndCataloguesAndMeasurementSets };
        assertThat(project.getObservations().size(), is(observations.length));
        assertThat(project.getObservations(), containsInAnyOrder(observations));
    }

    private void assertPersistedProjectCount(long count)
    {
        /*
         * This has to be independent of the projectRepository in the test case for the following reasons: a) all the
         * repositories in the test case use the same EntityManager b) the EntityManager may not have had changes
         * flushed c) the EntityManager may be in a bad state due to something like a ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getProjectCount(), is(count));
    }
}
