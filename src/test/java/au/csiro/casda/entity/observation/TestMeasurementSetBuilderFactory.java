package au.csiro.casda.entity.observation;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.lang3.RandomStringUtils;

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
 * A factory for TestObjectBuilders that can be used to configure and build instances of MeasurementSet for use in test
 * cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class TestMeasurementSetBuilderFactory
{
    /**
     * TestObjectBuilder extension providing builder-style methods for setting values on a constructed MeasurementSet.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static interface MeasurementSetBuilder extends TestObjectBuilder<MeasurementSet>
    {
        public MeasurementSetBuilder setProject(Project project);

        public MeasurementSetBuilder setParent(ParentDepositableArtefact observation);

        public MeasurementSetBuilder setFormat(String format);

        public MeasurementSetBuilder setFilename(String filename);

        public MeasurementSetBuilder noScans();
    }

    public static final MeasurementSetBuilder createBuilder()
    {
        return (MeasurementSetBuilder) Proxy.newProxyInstance(MeasurementSetBuilder.class.getClassLoader(),
                new Class[] { MeasurementSetBuilder.class }, new MeasurementSetBuilderProxyHandler());
    }

    private static final class MeasurementSetBuilderProxyHandler extends TestObjectBuilderProxyHandler<MeasurementSet>
    {
        private boolean noScans;

        private MeasurementSetBuilderProxyHandler()
        {
            this.noScans = false;
            setDefault("project", TestProjectBuilderFactory.createBuilder());
            setDefault("parent", TestObservationBuilderFactory.createBuilder());
            setDefault("format", RandomStringUtils.randomAlphabetic(10));
            setDefault("filename", RandomStringUtils.randomAlphabetic(30));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if (method.getName().equals("noScans"))
            {
                this.noScans = true;
                return proxy;
            }
            else
            {
                return super.invoke(proxy, method, args);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void populateFields(MeasurementSet measurementSet)
        {
            super.populateFields(measurementSet);
            if (measurementSet.getParent() != null
                    && !measurementSet.getParent().getMeasurementSets().contains(measurementSet))
            {
                measurementSet.getParent().addMeasurementSet(measurementSet);
            }
            if (measurementSet.getProject() != null
                    && !measurementSet.getProject().getMeasurementSets().contains(measurementSet))
            {
                measurementSet.getProject().addMeasurementSet(measurementSet);
            }
            if (measurementSet.getScans().isEmpty() && !this.noScans)
            {
                TestScanBuilderFactory.createBuilder().setMeasurementSet(measurementSet).build();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected MeasurementSet createObject()
        {
            return new MeasurementSet();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<MeasurementSet>> getBuilderClass()
        {
            return MeasurementSetBuilder.class;
        }
    }
}
