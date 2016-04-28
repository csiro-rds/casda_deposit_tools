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

/**
 * A factory for TestObjectBuilders that can be used to configure and build instances of Project for use in test cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class TestProjectBuilderFactory
{
    /**
     * TestObjectBuilder extension providing builder-style methods for setting values on a constructed Project.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static interface ProjectBuilder extends TestObjectBuilder<Project>
    {
        public ProjectBuilder setOpalCode(String opalCode);

        public ProjectBuilder setShortName(String shortName);

    }

    public static final ProjectBuilder createBuilder()
    {
        return (ProjectBuilder) Proxy.newProxyInstance(ProjectBuilder.class.getClassLoader(),
                new Class[] { ProjectBuilder.class }, new ProjectBuilderProxyHandler());
    }

    private static final class ProjectBuilderProxyHandler extends TestObjectBuilderProxyHandler<Project>
    {
        private ProjectBuilderProxyHandler()
        {
            String opalCode = RandomStringUtils.randomAlphabetic(2) + RandomStringUtils.randomNumeric(3);
            setDefault("opalCode", opalCode);
            setDefault("shortName", opalCode);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Project createObject()
        {
            return new Project();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<Project>> getBuilderClass()
        {
            return ProjectBuilder.class;
        }
    }
}
