package au.csiro.casda.entity.observation;

import static au.csiro.casda.entity.observation.ConstraintViolationExceptionMatcher.constraintViolation;
import static au.csiro.casda.entity.observation.TestAssertionMatcher.assertions;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.test.context.ContextConfiguration;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
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
 * ImageCube test
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
public class ImageCubeTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ObservationRepository observationRepository;

    private ProjectRepository projectRepository;

    private ImageCubeRepository imageCubeRepository;

    public ImageCubeTest() throws Exception
    {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeRepositories(RepositoryFactorySupport rfs)
    {
        observationRepository = rfs.getRepository(ObservationRepository.class);
        projectRepository = rfs.getRepository(ProjectRepository.class);
        imageCubeRepository = rfs.getRepository(ImageCubeRepository.class);
    }

    @Test
    public void testBuilder()
    {
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().build();
        assertThat(imageCube, is(notNullValue()));
        assertThat(imageCube.getParent(), is(notNullValue()));
        assertThat(imageCube.getParent().getImageCubes(), containsInAnyOrder(imageCube));
        assertThat(imageCube.getProject(), is(notNullValue()));
        assertThat(imageCube.getProject().getImageCubes(), containsInAnyOrder(imageCube));
        // All other default property values are tested in this and other entity test cases.
    }

    @Test
    public void filenameIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(ImageCube.class, "filename", "may not be null"),
                assertions((e) -> assertPersistedImageCubeCount(0L))));
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().setFilename(null).build();
        assertThat(imageCubeRepository.count(), is(0L));
        observationRepository.save(imageCube.getParent());
    }

    @Test
    public void filenameIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(ImageCube.class, "filename", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedImageCubeCount(0L))));
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().setFilename("").build();
        assertThat(imageCubeRepository.count(), is(0L));
        observationRepository.save(imageCube.getParent());
    }

    @Test
    public void formatIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(ImageCube.class, "format", "may not be null"),
                assertions((e) -> assertPersistedImageCubeCount(0L))));
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().setFormat(null).build();
        assertThat(imageCubeRepository.count(), is(0L));
        observationRepository.save(imageCube.getParent());
    }

    @Test
    public void formatIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(ImageCube.class, "format", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedImageCubeCount(0L))));
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().setFormat("").build();
        assertThat(imageCubeRepository.count(), is(0L));
        observationRepository.save(imageCube.getParent());
    }

    @Test
    public void projectIsRequired()
    {
        exception.expect(allOf(constraintViolation(ImageCube.class, "project", "may not be null"),
                assertions((e) -> assertPersistedImageCubeCount(0L))));
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().setProject(null).build();
        assertThat(imageCubeRepository.count(), is(0L));
        observationRepository.save(imageCube.getParent());
    }

    @Test
    public void observationIsRequired()
    {
        exception.expect(allOf(constraintViolation(ImageCube.class, "observation", "may not be null"),
                assertions((e) -> assertPersistedImageCubeCount(0L))));
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().build();
        Observation observation = imageCube.getParent();
        imageCube.setParent(null);
        assertThat(imageCubeRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testPersistence()
    {
        assertThat(imageCubeRepository.count(), is(0L));
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().build();

        observationRepository.save(imageCube.getParent());

        assertThat(imageCubeRepository.count(), is(1L));
        ImageCube savedImageCube = imageCubeRepository.findAll().iterator().next();
        assertThat(savedImageCube, notNullValue());
    }

    @Test
    public void testCreationCascadesToProject()
    {
        assertThat(imageCubeRepository.count(), is(0L));
        assertThat(projectRepository.count(), is(0L));

        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().build();

        observationRepository.save(imageCube.getParent());

        assertThat(imageCubeRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));

        ImageCube savedImageCube = imageCubeRepository.findAll().iterator().next();
        Project savedProject = projectRepository.findAll().iterator().next();
        assertThat(savedImageCube.getProject(), is(savedProject));
        entityManager.refresh(savedProject);
        assertThat(savedProject.getImageCubes().size(), is(1));
        assertThat(savedProject.getImageCubes().iterator().next(), is(savedImageCube));
    }

    @Test
    public void testDeletion()
    {
        ImageCube imageCube = TestImageCubeBuilderFactory.createBuilder().build();

        observationRepository.save(imageCube.getParent());
        assertThat(projectRepository.count(), is(1L));
        assertThat(observationRepository.count(), is(1L));
        assertThat(imageCubeRepository.count(), is(1L));

        imageCube.getParent().removeImageCube(imageCube);
        imageCubeRepository.delete(imageCube);

        assertThat(imageCubeRepository.count(), is(0L));
        assertThat(observationRepository.count(), is(1L)); // No cascade to Observation
        assertThat(projectRepository.count(), is(1L)); // No cascade to Observation
    }

    private void assertPersistedImageCubeCount(long count)
    {
        /*
         * This has to be independent of the imageCubeRepository in the test case for the following reasons: a) all the
         * repositories in the test case use the same EntityManager b) the EntityManager may not have had changes
         * flushed c) the EntityManager may be in a bad state due to something like a ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getImageCubeCount(), is(count));
    }
}
