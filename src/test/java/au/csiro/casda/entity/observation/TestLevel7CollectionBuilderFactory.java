package au.csiro.casda.entity.observation;

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

public abstract class TestLevel7CollectionBuilderFactory
{
    public static interface Level7CollectionBuilder extends TestObjectBuilder<Level7Collection>
    {
        public Level7CollectionBuilder setProject(Project project);
        
        public Level7CollectionBuilder setDapCollectionId(long collectionId);
    }

    public static final Level7CollectionBuilder createBuilder()
    {
        return (Level7CollectionBuilder) Proxy.newProxyInstance(Level7CollectionBuilder.class.getClassLoader(),
                new Class[] { Level7CollectionBuilder.class }, new Level7CollectionBuilderProxyHandler());
    }

    private static final class Level7CollectionBuilderProxyHandler extends TestObjectBuilderProxyHandler<Level7Collection>
    {
        private Level7CollectionBuilderProxyHandler()
        {
            setDefault("project", TestProjectBuilderFactory.createBuilder());
            setDefault("dapCollectionId", Long.parseLong(RandomStringUtils.randomNumeric(6)));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Level7Collection createObject()
        {
            return new Level7Collection();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<Level7Collection>> getBuilderClass()
        {
            return Level7CollectionBuilder.class;
        }

    }
}
