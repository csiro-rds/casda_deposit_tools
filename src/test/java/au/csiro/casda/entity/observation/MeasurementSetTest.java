package au.csiro.casda.entity.observation;

import static au.csiro.casda.entity.observation.ConstraintViolationExceptionMatcher.constraintViolation;
import static au.csiro.casda.entity.observation.TestAssertionMatcher.assertions;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

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
 * MeasurementSet test
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class MeasurementSetTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private MeasurementSetRepository measurementSetRepository;

    private ProjectRepository projectRepository;

    private ObservationRepository observationRepository;

    private ScanRepository scanRepository;

    public MeasurementSetTest() throws Exception
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
        measurementSetRepository = rfs.getRepository(MeasurementSetRepository.class);
        scanRepository = rfs.getRepository(ScanRepository.class);
    }

    @Test
    public void testBuilder()
    {
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().build();
        assertThat(measurementSet, is(notNullValue()));
        assertThat(measurementSet.getParent(), is(notNullValue()));
        assertThat(measurementSet.getParent().getMeasurementSets(), containsInAnyOrder(measurementSet));
        assertThat(measurementSet.getProject(), is(notNullValue()));
        assertThat(measurementSet.getProject().getMeasurementSets(), containsInAnyOrder(measurementSet));
        assertThat(measurementSet.getScans().size(), is(1));
        assertThat(measurementSet.getScans().get(0).getMeasurementSet(), is(measurementSet));
        // All other default property values are tested in this and other entity test cases.
    }

    @Test
    public void filenameIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(MeasurementSet.class, "filename", "may not be null"),
                assertions((e) -> assertPersistedMeasurementSetCount(0L))));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().setFilename(null).build();
        assertThat(measurementSetRepository.count(), is(0L));
        observationRepository.save(measurementSet.getParent());
    }

    @Test
    public void filenameIsRequiredNotEmpty()
    {
        exception
                .expect(allOf(
                        constraintViolation(MeasurementSet.class, "filename", "size must be between 1 and "
                                + Integer.MAX_VALUE), assertions((e) -> assertPersistedMeasurementSetCount(0L))));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().setFilename("").build();
        assertThat(measurementSetRepository.count(), is(0L));
        observationRepository.save(measurementSet.getParent());
    }

    @Test
    public void formatIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(MeasurementSet.class, "format", "may not be null"),
                assertions((e) -> assertPersistedMeasurementSetCount(0L))));
        exception.expect(new ConstraintViolationExceptionMatcher(MeasurementSet.class, "format", "may not be null"));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().setFormat(null).build();
        assertThat(measurementSetRepository.count(), is(0L));
        observationRepository.save(measurementSet.getParent());
    }

    @Test
    public void formatIsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(MeasurementSet.class, "format", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedMeasurementSetCount(0L))));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().setFormat("").build();
        assertThat(measurementSetRepository.count(), is(0L));
        observationRepository.save(measurementSet.getParent());
    }

    @Test
    public void projectIsRequired()
    {
        exception.expect(allOf(constraintViolation(MeasurementSet.class, "project", "may not be null"),
                assertions((e) -> assertPersistedMeasurementSetCount(0L))));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().setProject(null).build();
        assertThat(measurementSetRepository.count(), is(0L));
        observationRepository.save(measurementSet.getParent());
    }

    @Test
    public void observationIsRequired()
    {
        exception.expect(allOf(constraintViolation(MeasurementSet.class, "observation", "may not be null"),
                assertions((e) -> assertPersistedMeasurementSetCount(0L))));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().build();
        Observation observation = measurementSet.getParent();
        measurementSet.setParent(null);
        assertThat(measurementSetRepository.count(), is(0L));
        observationRepository.save(observation);
    }

    @Test
    public void atLeastOneScanIsRequired()
    {
        exception.expect(allOf(
                constraintViolation(MeasurementSet.class, "scans", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedMeasurementSetCount(0L))));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().noScans().build();
        assertThat(measurementSetRepository.count(), is(0L));
        observationRepository.save(measurementSet.getParent());
    }

    @Test
    public void testPersistence()
    {
        assertThat(measurementSetRepository.count(), is(0L));
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().build();

        observationRepository.save(measurementSet.getParent());

        assertThat(measurementSetRepository.count(), is(1L));
        MeasurementSet savedMeasurementSet = measurementSetRepository.findAll().iterator().next();
        assertThat(savedMeasurementSet, notNullValue());
    }

    @Test
    public void testCreationCascadesToProject()
    {
        assertThat(measurementSetRepository.count(), is(0L));
        assertThat(projectRepository.count(), is(0L));

        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().build();

        observationRepository.save(measurementSet.getParent());

        assertThat(measurementSetRepository.count(), is(1L));
        assertThat(projectRepository.count(), is(1L));

        MeasurementSet savedMeasurementSet = measurementSetRepository.findAll().iterator().next();
        Project savedProject = projectRepository.findAll().iterator().next();
        assertThat(savedMeasurementSet.getProject(), is(savedProject));
        entityManager.refresh(savedProject);
        assertThat(savedProject.getMeasurementSets().size(), is(1));
        assertThat(savedProject.getMeasurementSets().iterator().next(), is(savedMeasurementSet));
    }

    @Test
    public void testCreationCascadesToScans()
    {
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().build();

        assertThat(measurementSetRepository.count(), is(0L));
        assertThat(scanRepository.count(), is(0L));

        observationRepository.save(measurementSet.getParent());

        assertThat(measurementSetRepository.count(), is(1L));
        assertThat(scanRepository.count(), is(1L));
    }

    @Test
    public void testDeletion()
    {
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().build();

        observationRepository.save(measurementSet.getParent());

        assertThat(projectRepository.count(), is(1L));
        assertThat(observationRepository.count(), is(1L));
        assertThat(measurementSetRepository.count(), is(1L));
        assertThat(scanRepository.count(), is(1L));

        measurementSet.getParent().removeMeasurementSet(measurementSet);
        measurementSetRepository.delete(measurementSet);

        assertThat(scanRepository.count(), is(0L)); // Cascade to Scan
        assertThat(measurementSetRepository.count(), is(0L));
        assertThat(observationRepository.count(), is(1L)); // No cascade to Observation
        assertThat(projectRepository.count(), is(1L)); // No cascade to Project
    }

    @Test
    public void testPersistedScansAreOrderedByScanId()
    {
        MeasurementSet measurementSet = TestMeasurementSetBuilderFactory.createBuilder().noScans().build();
        Map<String, Scan> scans = new HashMap<>();
        scans.put("scan2FieldName", TestScanBuilderFactory.createBuilder().setScanId(1).setFieldName("scan2FieldName")
                .setMeasurementSet(measurementSet).build());
        scans.put("scan1FieldName", TestScanBuilderFactory.createBuilder().setScanId(0).setFieldName("scan1FieldName")
                .setMeasurementSet(measurementSet).build());
        scans.put("scan3FieldName", TestScanBuilderFactory.createBuilder().setScanId(2).setFieldName("scan3FieldName")
                .setMeasurementSet(measurementSet).build());

        /*
         * Ordered by insert order
         */
        assertThat(measurementSet.getScans(),
                contains(scans.get("scan2FieldName"), scans.get("scan1FieldName"), scans.get("scan3FieldName")));

        observationRepository.save(measurementSet.getParent());

        commit();

        measurementSet = measurementSetRepository.findAll().iterator().next();
        for (Scan scan : measurementSet.getScans())
        {
            scans.put(scan.getFieldName(), scan);
        }

        /*
         * Ordered by scan id
         */
        assertThat(measurementSet.getScans(),
                contains(scans.get("scan1FieldName"), scans.get("scan2FieldName"), scans.get("scan3FieldName")));
    }

    private void assertPersistedMeasurementSetCount(long count)
    {
        /*
         * This has to be independent of the measurementSetRepository in the test case for the following reasons: a) all
         * the repositories in the test case use the same EntityManager b) the EntityManager may not have had changes
         * flushed c) the EntityManager may be in a bad state due to something like a ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getMeasurementSetCount(), is(count));
    }
}
