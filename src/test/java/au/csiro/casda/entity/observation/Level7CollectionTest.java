package au.csiro.casda.entity.observation;

import static au.csiro.casda.entity.observation.ConstraintViolationExceptionMatcher.constraintViolation;
import static au.csiro.casda.entity.observation.TestAssertionMatcher.assertions;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.test.context.ContextConfiguration;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
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
public class Level7CollectionTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Level7CollectionRepository level7CollectionRepository;

    private ProjectRepository projectRepository;

    private CatalogueRepository catalogueRepository;

    public Level7CollectionTest() throws Exception
    {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeRepositories(RepositoryFactorySupport rfs)
    {
        level7CollectionRepository = rfs.getRepository(Level7CollectionRepository.class);
        projectRepository = rfs.getRepository(ProjectRepository.class);
        catalogueRepository = rfs.getRepository(CatalogueRepository.class);
    }

    @Test
    public void testBuilder()
    {
        Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().build();
        assertThat(level7Collection, is(notNullValue()));
        assertThat(level7Collection.getProject(), is(notNullValue()));

        Catalogue catalogue =
                TestCatalogueBuilderFactory.createBuilder().setCatalogueType(CatalogueType.LEVEL7)
                        .setParent(level7Collection).setProject(level7Collection.getProject()).build();
        assertEquals(1, level7Collection.getCatalogues().size());
        assertEquals(level7Collection.getProject(), catalogue.getProject());
    }

    @Test
    public void testLevel7CatalogueMustHaveSameProject()
    {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Catalogues in a level 7 collection must all be associated with the same project");

        Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().build();
        TestCatalogueBuilderFactory.createBuilder().setCatalogueType(CatalogueType.LEVEL7).setParent(level7Collection)
                .build();
    }

    @Test
    public void testLevel7CatalogueCanOnlyBeAddedToLevel7Collection()
    {
        try
        {
            Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().build();
            TestCatalogueBuilderFactory.createBuilder().setCatalogueType(CatalogueType.CONTINUUM_COMPONENT)
                    .setParent(level7Collection).build();
        }
        catch (Exception e)
        {
            Throwable cause = e.getCause().getCause();
            assertEquals(IllegalArgumentException.class, cause.getClass());
            assertEquals("Can only set level 7 collection for a level 7 catalogue", cause.getMessage());
        }
    }

    @Test
    public void projectIsRequired()
    {
        exception.expect(allOf(constraintViolation(Level7Collection.class, "project", "may not be null"),
                assertions((e) -> assertPersistedLevel7CollectionCount(0L))));
        Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().setProject(null).build();
        assertThat(level7CollectionRepository.count(), is(0L));
        level7CollectionRepository.save(level7Collection);
    }

    @Test
    public void testPersistence()
    {
        assertThat(level7CollectionRepository.count(), is(0L));
        Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().build();

        level7CollectionRepository.save(level7Collection);

        assertThat(level7CollectionRepository.count(), is(1L));
        Level7Collection savedLevel7Collection = level7CollectionRepository.findAll().iterator().next();
        assertThat(savedLevel7Collection, notNullValue());
    }

    @Test
    public void testCreationCascadesToCatalogue()
    {
        assertThat(level7CollectionRepository.count(), is(0L));
        assertThat(projectRepository.count(), is(0L));
        assertThat(catalogueRepository.count(), is(0L));

        Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().build();
        TestCatalogueBuilderFactory.createBuilder().setCatalogueType(CatalogueType.LEVEL7).setParent(level7Collection)
                .setProject(level7Collection.getProject()).build();

        level7CollectionRepository.save(level7Collection);

        assertThat(level7CollectionRepository.count(), is(1L));
        assertThat(catalogueRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));

        Level7Collection savedLevel7Collection = level7CollectionRepository.findAll().iterator().next();
        Project savedProject = projectRepository.findAll().iterator().next();
        Catalogue savedLevel7Catalogue = catalogueRepository.findAll().iterator().next();
        assertThat(savedLevel7Collection, notNullValue());

        assertThat(savedProject, is(savedLevel7Collection.getProject()));
        assertThat(savedProject, is(savedLevel7Catalogue.getProject()));
        assertThat(savedLevel7Collection, is(savedLevel7Catalogue.getParent()));
    }

    @Test
    public void testCreationCascadesToProject()
    {
        assertThat(level7CollectionRepository.count(), is(0L));
        assertThat(projectRepository.count(), is(0L));

        Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().build();

        level7CollectionRepository.save(level7Collection);

        assertThat(level7CollectionRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));

        Level7Collection savedLevel7Collection = level7CollectionRepository.findAll().iterator().next();
        Project savedProject = projectRepository.findAll().iterator().next();
        assertThat(savedLevel7Collection.getProject(), is(savedProject));
        entityManager.refresh(savedProject);
    }

    @Test
    public void testDeletion()
    {
        Level7Collection level7Collection = TestLevel7CollectionBuilderFactory.createBuilder().build();

        level7CollectionRepository.save(level7Collection);
        assertThat(projectRepository.count(), is(1L));
        assertThat(level7CollectionRepository.count(), is(1L));

        level7CollectionRepository.delete(level7Collection);

        assertThat(level7CollectionRepository.count(), is(0L));
        assertThat(projectRepository.count(), is(1L)); // No cascade to Project
    }

    private void assertPersistedLevel7CollectionCount(long count)
    {
        /*
         * This has to be independent of the level7CollectionRepository in the test case for the following reasons: a)
         * all the repositories in the test case use the same EntityManager b) the EntityManager may not have had
         * changes flushed c) the EntityManager may be in a bad state due to something like a
         * ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getLevel7CollectionCount(), is(count));
    }
}
