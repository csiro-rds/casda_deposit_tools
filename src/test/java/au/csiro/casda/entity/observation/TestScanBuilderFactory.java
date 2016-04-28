package au.csiro.casda.entity.observation;

import java.lang.reflect.Proxy;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

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
 * A factory for TestObjectBuilders that can be used to configure and build instances of Scan for use in test cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class TestScanBuilderFactory
{
    /**
     * TestObjectBuilder extension providing builder-style methods for setting values on a constructed Scan.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static interface ScanBuilder extends TestObjectBuilder<Scan>
    {
        public ScanBuilder setMeasurementSet(MeasurementSet measurementSet);

        public ScanBuilder setScanId(Integer scanId);

        public ScanBuilder setCoordSystem(String coordSystem);

        public ScanBuilder setFieldName(String fieldName);

        public ScanBuilder setPolarisations(String polarisations);

        public ScanBuilder setNumChannels(Integer numChannels);

        public ScanBuilder setChannelWidth(Double channelWidth);

        public ScanBuilder setScanStart(DateTime scanStart);

        public ScanBuilder setScanEnd(DateTime scanEnd);

        public ScanBuilder setFieldCentreX(Double fieldCentreX);

        public ScanBuilder setFieldCentreY(Double fieldCentreY);

        public ScanBuilder setCentreFrequency(Double centreFrequency);

    }

    public static final ScanBuilder createBuilder()
    {
        return (ScanBuilder) Proxy.newProxyInstance(ScanBuilder.class.getClassLoader(),
                new Class[] { ScanBuilder.class }, new ScanBuilderProxyHandler());
    }

    private static final class ScanBuilderProxyHandler extends TestObjectBuilderProxyHandler<Scan>
    {
        private ScanBuilderProxyHandler()
        {
            setDefault("measurementSet", TestMeasurementSetBuilderFactory.createBuilder().noScans());
            DateTime scanEnd = DateTime.now().minusHours(RandomUtils.nextInt(12) + 1);
            setDefault("scanEnd", scanEnd);
            setDefault("scanStart", scanEnd.minusHours(RandomUtils.nextInt(12) + 1));
            setDefault("fieldCentreX", RandomUtils.nextDouble() % (2 * Math.PI));
            setDefault("fieldCentreY", RandomUtils.nextDouble() % (2 * Math.PI));
            setDefault("coordSystem", RandomStringUtils.randomAlphabetic(5));
            setDefault("polarisations", "[XX,YY]");
            setDefault("numChannels", RandomUtils.nextInt(99) + 1);
            setDefault("centreFrequency", RandomUtils.nextDouble());
            setDefault("channelWidth", RandomUtils.nextDouble());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void populateFields(Scan scan)
        {
            super.populateFields(scan);
            if (scan.getMeasurementSet() != null && !scan.getMeasurementSet().getScans().contains(scan))
            {
                scan.getMeasurementSet().addScan(scan);
            }
            if (!didBuild("scanId"))
            {
                if (scan.getMeasurementSet() != null)
                {
                    scan.setScanId(scan.getMeasurementSet().getScans().indexOf(scan));
                }
                else
                {
                    scan.setScanId(0);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Scan createObject()
        {
            return new Scan();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<Scan>> getBuilderClass()
        {
            return ScanBuilder.class;
        }
    }
}
