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
 * A factory for TestObjectBuilders that can be used to configure and build instances of EvaluationFile for use in test
 * cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class TestEvaluationFileBuilderFactory
{
    /**
     * TestObjectBuilder extension providing builder-style methods for setting values on a constructed EvaluationFile.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static interface EvaluationFileBuilder extends TestObjectBuilder<EvaluationFile>
    {
        public EvaluationFileBuilder setParent(ParentDepositableArtefact observation);

        public EvaluationFileBuilder setFormat(String format);

        public EvaluationFileBuilder setFilename(String filename);
    }

    public static final EvaluationFileBuilder createBuilder()
    {
        return (EvaluationFileBuilder) Proxy.newProxyInstance(EvaluationFileBuilder.class.getClassLoader(),
                new Class[] { EvaluationFileBuilder.class }, new EvaluationFileBuilderProxyHandler());
    }

    private static final class EvaluationFileBuilderProxyHandler extends TestObjectBuilderProxyHandler<EvaluationFile>
    {
        private EvaluationFileBuilderProxyHandler()
        {
            setDefault("parent", TestObservationBuilderFactory.createBuilder());
            setDefault("format", RandomStringUtils.randomAlphabetic(10));
            setDefault("filename", RandomStringUtils.randomAlphabetic(30));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void populateFields(EvaluationFile evalutionFile)
        {
            super.populateFields(evalutionFile);
            if (evalutionFile.getParent() != null
                    && !evalutionFile.getParent().getEvaluationFiles().contains(evalutionFile))
            {
                evalutionFile.getParent().addEvaluationFile(evalutionFile);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected EvaluationFile createObject()
        {
            return new EvaluationFile();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<EvaluationFile>> getBuilderClass()
        {
            return EvaluationFileBuilder.class;
        }
    }
}
