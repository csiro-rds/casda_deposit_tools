package au.csiro.casda.entity.observation;

import java.lang.reflect.Proxy;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

import au.csiro.casda.datadeposit.observation.assembler.ObservationTime;

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

public abstract class TestObservationBuilderFactory
{
    public static interface ObservationBuilder extends TestObjectBuilder<Observation>
    {
        public ObservationBuilder setSbid(Integer sbid);

        public ObservationBuilder setObsStart(DateTime obsStart);

        public ObservationBuilder setObsStartMjd(Double mjd);

        public ObservationBuilder setObsEnd(DateTime obsEnd);

        public ObservationBuilder setObsEndMjd(Double mjd);

        public ObservationBuilder setTelescope(String telescope);

        public ObservationBuilder setObsProgram(String obsProgram);
    }

    public static final ObservationBuilder createBuilder()
    {
        return (ObservationBuilder) Proxy.newProxyInstance(ObservationBuilder.class.getClassLoader(),
                new Class[] { ObservationBuilder.class }, new ObservationBuilderProxyHandler());
    }

    private static final class ObservationBuilderProxyHandler extends TestObjectBuilderProxyHandler<Observation>
    {
        private ObservationBuilderProxyHandler()
        {
            setDefault("sbid", Integer.parseInt(RandomStringUtils.randomNumeric(6)));
            DateTime obsEnd = DateTime.now().minusDays(RandomUtils.nextInt(5) + 1);
            setDefault("obsEnd", obsEnd);
            setDefault("obsStart", obsEnd.minusDays(1));
            setDefault("telescope", RandomStringUtils.randomAlphabetic(10));
            setDefault("obsProgram", RandomStringUtils.randomAlphabetic(6));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void populateFields(Observation observation)
        {
            super.populateFields(observation);
            /*
             * Eventually the mjd setters will go away and we can ditch this configuration
             */
            if (!this.didBuild("obsStartMjd"))
            {
                if (observation.getObsStart() == null)
                {
                    observation.setObsStartMjd(0.0);
                }
                else
                {
                    observation.setObsStartMjd(ObservationTime.dateTimeToModifiedJulianDate(observation.getObsStart()));
                }
            }
            if (!this.didBuild("obsEndMjd"))
            {
                if (observation.getObsEnd() == null)
                {
                    observation.setObsEndMjd(0.0);
                }
                else
                {
                    observation.setObsEndMjd(ObservationTime.dateTimeToModifiedJulianDate(observation.getObsEnd()));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Observation createObject()
        {
            return new Observation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<Observation>> getBuilderClass()
        {
            return ObservationBuilder.class;
        }

    }
}
