package au.csiro.casda.entity.observation;

import static au.csiro.casda.entity.observation.ConstraintViolationExceptionMatcher.constraintViolation;
import static au.csiro.casda.entity.observation.TestAssertionMatcher.assertions;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.test.context.ContextConfiguration;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
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
 * Catalogue test
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
public class CatalogueTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ProjectRepository projectRepository;

    private ObservationRepository observationRepository;

    private CatalogueRepository catalogueRepository;

    public CatalogueTest() throws Exception
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
        catalogueRepository = rfs.getRepository(CatalogueRepository.class);
    }

    @Test
    public void testBuilder()
    {
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().build();
        assertThat(catalogue, is(notNullValue()));
        assertThat(catalogue.getParent(), is(notNullValue()));
        assertThat(((Observation) catalogue.getParent()).getCatalogues(), containsInAnyOrder(catalogue));
        assertThat(catalogue.getImageCube(), is(nullValue()));
        assertThat(catalogue.getProject(), is(notNullValue()));
        assertThat(catalogue.getProject().getCatalogues(), containsInAnyOrder(catalogue));
        // All other default property values are tested in this and other entity test cases.
    }

    @Test
    public void catalogueTypeIsRequired()
    {
        exception.expect(allOf(constraintViolation(Catalogue.class, "catalogueType", "may not be null"),
                assertions((e) -> assertPersistedCatalogueCount(0L))));
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().setCatalogueType(null).build();
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save((Observation)catalogue.getParent());
    }

    @Test
    public void filenameIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Catalogue.class, "filename", "may not be null"),
                assertions((e) -> assertPersistedCatalogueCount(0L))));
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().setFilename(null).build();
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save((Observation)catalogue.getParent());
    }

    @Test
    public void filenameIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(Catalogue.class, "filename", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedCatalogueCount(0L))));
        exception.expect(new ConstraintViolationExceptionMatcher(Catalogue.class, "filename",
                "size must be between 1 and " + Integer.MAX_VALUE));
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().setFilename("").build();
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save((Observation)catalogue.getParent());
    }

    @Test
    public void formatIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Catalogue.class, "format", "may not be null"),
                assertions((e) -> assertPersistedCatalogueCount(0L))));
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().setFormat(null).build();
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save((Observation)catalogue.getParent());
    }

    @Test
    public void formatIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(Catalogue.class, "format", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedCatalogueCount(0L))));
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().setFormat("").build();
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save((Observation)catalogue.getParent());
    }

    @Test
    public void projectIsRequired()
    {
        exception.expect(allOf(constraintViolation(Catalogue.class, "project", "may not be null"),
                assertions((e) -> assertPersistedCatalogueCount(0L))));
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().setProject(null).build();
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save((Observation)catalogue.getParent());
    }

    @Test
    public void observationIsOptional()
    {
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().build();
        Observation observation = (Observation)catalogue.getParent();
        catalogue.setParent(null);
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void imageCubeIsOptional()
    {
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().setImageCube(null).build();
        assertThat(catalogueRepository.count(), is(0L));
        observationRepository.save((Observation)catalogue.getParent());
        assertThat(catalogueRepository.count(), is(1L));
    }

    @Test
    public void testPersistence()
    {
        assertThat(catalogueRepository.count(), is(0L));
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().build();

        observationRepository.save((Observation)catalogue.getParent());

        assertThat(catalogueRepository.count(), is(1L));
        Catalogue savedCatalogue = catalogueRepository.findAll().iterator().next();
        assertThat(savedCatalogue, notNullValue());
    }

    @Test
    public void testCreationCascadesToProject()
    {
        assertThat(catalogueRepository.count(), is(0L));
        assertThat(projectRepository.count(), is(0L));

        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().build();
        observationRepository.save((Observation)catalogue.getParent());

        assertThat(catalogueRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));

        Catalogue savedCatalogue = catalogueRepository.findAll().iterator().next();
        Project savedProject = projectRepository.findAll().iterator().next();
        assertThat(savedCatalogue.getProject(), is(savedProject));
        entityManager.refresh(savedProject);
        assertThat(savedProject.getCatalogues().size(), is(1));
        assertThat(savedProject.getCatalogues().iterator().next(), is(savedCatalogue));
    }

    @Test
    public void testDeletionDoesNotCascadeToProject()
    {
        Catalogue catalogue = TestCatalogueBuilderFactory.createBuilder().build();

        observationRepository.save((Observation) catalogue.getParent());

        assertThat(projectRepository.count(), is(1L));
        assertThat(observationRepository.count(), is(1L));
        assertThat(catalogueRepository.count(), is(1L));

        ((Observation) catalogue.getParent()).removeCatalogue(catalogue);
        catalogueRepository.delete(catalogue);

        assertThat(catalogueRepository.count(), is(0L));
        assertThat(observationRepository.count(), is(1L)); // No cascade to Observation
        assertThat(projectRepository.count(), is(1L)); // No cascade to Project
    }

    private void assertPersistedCatalogueCount(long count)
    {
        /*
         * This has to be independent of the catalogueRepository in the test case for the following reasons: a) all the
         * repositories in the test case use the same EntityManager b) the EntityManager may not have had changes
         * flushed c) the EntityManager may be in a bad state due to something like a ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getCatalogueCount(), is(count));
    }

}
