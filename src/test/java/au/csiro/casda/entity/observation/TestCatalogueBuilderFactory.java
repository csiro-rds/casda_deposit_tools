package au.csiro.casda.entity.observation;

import java.lang.reflect.Proxy;

import org.apache.commons.lang.math.RandomUtils;
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
 * A factory for TestObjectBuilders that can be used to configure and build instances of Catalogue for use in test
 * cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class TestCatalogueBuilderFactory
{
    /**
     * TestObjectBuilder extension providing builder-style methods for setting values on a constructed Catalogue.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static interface TestCatalogueBuilder extends TestObjectBuilder<Catalogue>
    {
        public TestCatalogueBuilder setCatalogueType(CatalogueType catalogueType);

        public TestCatalogueBuilder setProject(Project project);

        public TestCatalogueBuilder setParent(ParentDepositableArtefact observation);

        public TestCatalogueBuilder setImageCube(ImageCube imageCube);

        public TestCatalogueBuilder setFormat(String format);

        public TestCatalogueBuilder setFilename(String filename);
    }

    public static final TestCatalogueBuilder createBuilder()
    {
        return (TestCatalogueBuilder) Proxy.newProxyInstance(TestCatalogueBuilder.class.getClassLoader(),
                new Class[] { TestCatalogueBuilder.class }, new CatalogueBuilderProxyHandler());
    }

    private static final class CatalogueBuilderProxyHandler extends TestObjectBuilderProxyHandler<Catalogue>
    {
        private CatalogueBuilderProxyHandler()
        {
            setDefault("catalogueType", CatalogueType.values()[RandomUtils.nextInt(CatalogueType.values().length - 1)]);
            setDefault("project", TestProjectBuilderFactory.createBuilder());
            setDefault("parent", TestObservationBuilderFactory.createBuilder());
            setDefault("format", RandomStringUtils.randomAlphabetic(10));
            setDefault("filename", RandomStringUtils.randomAlphabetic(30));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void populateFields(Catalogue catalogue)
        {
            super.populateFields(catalogue);
            if (catalogue.getParent() != null)
            {
                if (catalogue.getParent() instanceof Observation
                        && !((Observation) catalogue.getParent()).getCatalogues().contains(catalogue))
                {
                    ((Observation) catalogue.getParent()).addCatalogue(catalogue);
                }
                if (catalogue.getParent() instanceof Level7Collection
                        && !((Level7Collection) catalogue.getParent()).getCatalogues().contains(catalogue))
                {
                    ((Level7Collection) catalogue.getParent()).addCatalogue(catalogue);
                }
            }

            if (catalogue.getProject() != null && !catalogue.getProject().getCatalogues().contains(catalogue))
            {
                catalogue.getProject().addCatalogue(catalogue);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Catalogue createObject()
        {
            return new Catalogue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<Catalogue>> getBuilderClass()
        {
            return TestCatalogueBuilder.class;
        }
    }
}
