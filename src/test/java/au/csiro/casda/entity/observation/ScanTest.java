package au.csiro.casda.entity.observation;

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

import au.csiro.casda.datadeposit.observation.jpa.repository.MeasurementSetRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ScanRepository;
import au.csiro.casda.entity.AbstractPersistenceTest;

/**
 * Scan test
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class ScanTest extends AbstractPersistenceTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ObservationRepository observationRepository;

    private MeasurementSetRepository measurementSetRepository;

    private ScanRepository scanRepository;

    public ScanTest() throws Exception
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
        measurementSetRepository = rfs.getRepository(MeasurementSetRepository.class);
        scanRepository = rfs.getRepository(ScanRepository.class);
    }

    @Test
    public void testBuilder()
    {
        Scan scan = TestScanBuilderFactory.createBuilder().build();
        assertThat(scan, is(notNullValue()));
        assertThat(scan.getMeasurementSet(), is(notNullValue()));
        assertThat(scan.getMeasurementSet().getScans(), containsInAnyOrder(scan));
        // All other default property values are tested in this and other entity test cases.
    }

    @Test
    public void measurementSetIsRequired()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "measurementSet", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().build();
        MeasurementSet measurementSet = scan.getMeasurementSet();
        scan.setMeasurementSet(null);
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(measurementSet.getParent());
    }

    @Test
    public void scanIdIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "scanId", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setScanId(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void scanStartIsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "scanStart", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setScanStart(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void scanEndRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "scanEnd", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setScanEnd(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void fieldCentreXRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "fieldCentreX", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setFieldCentreX(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void fieldCentreYRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "fieldCentreY", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setFieldCentreY(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void coordSystemRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "coordSystem", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setCoordSystem(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void coordSystemRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(Scan.class, "coordSystem", "size must be between 1 and " + Integer.MAX_VALUE),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setCoordSystem("").build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void fieldNameIsOptional()
    {
        Scan scan = TestScanBuilderFactory.createBuilder().setFieldName(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
        assertThat(scanRepository.count(), is(1L));
    }

    @Test
    public void polarisationsRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "polarisations", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        exception.expect(new ConstraintViolationExceptionMatcher(Scan.class, "polarisations", "may not be null"));
        Scan scan = TestScanBuilderFactory.createBuilder().setPolarisations(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void polarisationsRequiredNotEmpty()
    {
        exception.expect(allOf(
                constraintViolation(Scan.class, "polarisations",
                        "must match \"\\[((XX|XY|YX|YY|xx|xy|yx|yy)\\s*,?\\s*){1,4}\\]\""),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setPolarisations("").build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void polarisationsRequiredMatchesPattern()
    {
        exception.expect(allOf(
                constraintViolation(Scan.class, "polarisations",
                        "must match \"\\[((XX|XY|YX|YY|xx|xy|yx|yy)\\s*,?\\s*){1,4}\\]\""),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setPolarisations("[X,]").build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void numChansRequiredNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "numChannels", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        exception.expect(new ConstraintViolationExceptionMatcher(Scan.class, "numChannels", "may not be null"));
        Scan scan = TestScanBuilderFactory.createBuilder().setNumChannels(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void numChansRequiredGreaterThanOrEqualToZero()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "numChannels", "must be greater than or equal to 0"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setNumChannels(-1).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void numCentreFrequencyNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "centreFrequency", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setCentreFrequency(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void numChannelWidthNotNull()
    {
        exception.expect(allOf(constraintViolation(Scan.class, "channelWidth", "may not be null"),
                assertions((e) -> assertPersistedScanCount(0L))));
        Scan scan = TestScanBuilderFactory.createBuilder().setChannelWidth(null).build();
        assertThat(scanRepository.count(), is(0L));
        observationRepository.save(scan.getMeasurementSet().getParent());
    }

    @Test
    public void testPersistence()
    {
        assertThat(scanRepository.count(), is(0L));
        Scan scan = TestScanBuilderFactory.createBuilder().build();

        observationRepository.save(scan.getMeasurementSet().getParent());

        assertThat(scanRepository.count(), is(1L));
        Scan savedScan = scanRepository.findAll().iterator().next();
        assertThat(savedScan, notNullValue());
    }

    @Test
    public void testDeletion()
    {
        Scan scan = TestScanBuilderFactory.createBuilder().build();

        observationRepository.save(scan.getMeasurementSet().getParent());
        assertThat(scanRepository.count(), is(1L));
        scan.getMeasurementSet().removeScan(scan); // Otherwise the object gets re-saved.

        scanRepository.delete(scan);
        assertThat(scanRepository.count(), is(0L));
    }

    @Test
    public void testDeletionDoesNotCascadeToMeasurementSet()
    {
        Scan scan = TestScanBuilderFactory.createBuilder().build();

        observationRepository.save(scan.getMeasurementSet().getParent());

        assertThat(scanRepository.count(), is(1L));
        assertThat(measurementSetRepository.count(), is(1L));

        scan.getMeasurementSet().removeScan(scan);
        scanRepository.delete(scan);

        assertThat(scanRepository.count(), is(0L));
        assertThat(measurementSetRepository.count(), is(1L));
    }

    private void assertPersistedScanCount(long count)
    {
        /*
         * This has to be independent of the scanRepository in the test case for the following reasons: a) all the
         * repositories in the test case use the same EntityManager b) the EntityManager may not have had changes
         * flushed c) the EntityManager may be in a bad state due to something like a ConstraintViolationException
         */
        assertThat(repositoryTestHelper.getScanCount(), is(count));
    }
}
