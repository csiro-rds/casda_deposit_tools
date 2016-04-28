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
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
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
 * EvaluationFile test
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
public class EvaluationFileTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ObservationRepository observationRepository;

    private EvaluationFileRepository evaluationFileRepository;

    public EvaluationFileTest() throws Exception
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
        evaluationFileRepository = rfs.getRepository(EvaluationFileRepository.class);
    }

    @Test
    public void testBuilder()
    {
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().build();
        assertThat(evaluationFile, is(notNullValue()));
        assertThat(evaluationFile.getParent(), is(notNullValue()));
        assertThat(evaluationFile.getParent().getEvaluationFiles(), containsInAnyOrder(evaluationFile));
        // All other default property values are tested in this and other entity test cases.
    }

    @Test
    public void filenameIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(EvaluationFile.class, "filename", "may not be null"),
                assertions((e) -> assertPersistedEvalationFileCount(0L))));
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().setFilename(null).build();
        assertThat(evaluationFileRepository.count(), is(0L));
        observationRepository.save(evaluationFile.getParent());
    }

    @Test
    public void filenameIsRequiredNotEmpty()
    {
        exception
                .expect(allOf(
                        constraintViolation(EvaluationFile.class, "filename", "size must be between 1 and "
                                + Integer.MAX_VALUE), assertions((e) -> assertPersistedEvalationFileCount(0L))));
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().setFilename("").build();
        assertThat(evaluationFileRepository.count(), is(0L));
        observationRepository.save(evaluationFile.getParent());
    }

    @Test
    public void formatIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(EvaluationFile.class, "format", "may not be null"),
                assertions((e) -> assertPersistedEvalationFileCount(0L))));
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().setFormat(null).build();
        assertThat(evaluationFileRepository.count(), is(0L));
        observationRepository.save(evaluationFile.getParent());
    }

    @Test
    public void formatIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(EvaluationFile.class, "format", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedEvalationFileCount(0L))));
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().setFormat("").build();
        assertThat(evaluationFileRepository.count(), is(0L));
        observationRepository.save(evaluationFile.getParent());
    }

    @Test
    public void observationIsRequired()
    {
        exception.expect(allOf(constraintViolation(EvaluationFile.class, "observation", "may not be null"),
                assertions((e) -> assertPersistedEvalationFileCount(0L))));
        exception
                .expect(new ConstraintViolationExceptionMatcher(EvaluationFile.class, "observation", "may not be null"));
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().build();
        Observation observation = evaluationFile.getParent();
        evaluationFile.setObservation(null);
        assertThat(evaluationFileRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void testPersistence()
    {
        assertThat(evaluationFileRepository.count(), is(0L));
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().build();

        observationRepository.save(evaluationFile.getParent());

        assertThat(evaluationFileRepository.count(), is(1L));
        EvaluationFile savedEvaluationFile = evaluationFileRepository.findAll().iterator().next();
        assertThat(savedEvaluationFile, notNullValue());
    }

    @Test
    public void testDeletion()
    {
        EvaluationFile evaluationFile = TestEvaluationFileBuilderFactory.createBuilder().build();

        observationRepository.save(evaluationFile.getParent());
        assertThat(observationRepository.count(), is(1L));
        assertThat(evaluationFileRepository.count(), is(1L));

        evaluationFile.getParent().removeEvaluationFile(evaluationFile);
        evaluationFileRepository.delete(evaluationFile);

        assertThat(evaluationFileRepository.count(), is(0L));
        assertThat(observationRepository.count(), is(1L)); // No cascasde to Observation
    }

    private void assertPersistedEvalationFileCount(long count)
    {
        /*
         * This has to be independent of the catalogueRepository in the test case for the following reasons: a) all the
         * repositories in the test case use the same EntityManager b) the EntityManager may not have had changes
         * flushed c) the EntityManager may be in a bad state due to something like a ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getEvaluationFileCount(), is(count));
    }
}
