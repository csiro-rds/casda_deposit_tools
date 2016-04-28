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
 * A factory for TestObjectBuilders that can be used to configure and build instances of ImageCube for use in test
 * cases.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class TestImageCubeBuilderFactory
{
    /**
     * TestObjectBuilder extension providing builder-style methods for setting values on a constructed ImageCube.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static interface ImageCubeBuilder extends TestObjectBuilder<ImageCube>
    {
        public ImageCubeBuilder setProject(Project project);

        public ImageCubeBuilder setParent(ParentDepositableArtefact observation);

        public ImageCubeBuilder setFormat(String format);

        public ImageCubeBuilder setFilename(String filename);
        
        public ImageCubeBuilder setType(String filename);
    }

    public static final ImageCubeBuilder createBuilder()
    {
        return (ImageCubeBuilder) Proxy.newProxyInstance(ImageCubeBuilder.class.getClassLoader(),
                new Class[] { ImageCubeBuilder.class }, new ImageCubeBuilderProxyHandler());
    }

    private static final class ImageCubeBuilderProxyHandler extends TestObjectBuilderProxyHandler<ImageCube>
    {
        private ImageCubeBuilderProxyHandler()
        {
            setDefault("project", TestProjectBuilderFactory.createBuilder());
            setDefault("parent", TestObservationBuilderFactory.createBuilder());
            setDefault("format", RandomStringUtils.randomAlphabetic(10));
            setDefault("filename", RandomStringUtils.randomAlphabetic(30));
            setDefault("type", "cont_restored_T0");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void populateFields(ImageCube imageCube)
        {
            super.populateFields(imageCube);
            if (imageCube.getParent() != null && !imageCube.getParent().getImageCubes().contains(imageCube))
            {
                imageCube.getParent().addImageCube(imageCube);
            }
            if (imageCube.getProject() != null && !imageCube.getProject().getImageCubes().contains(imageCube))
            {
                imageCube.getProject().addImageCube(imageCube);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ImageCube createObject()
        {
            return new ImageCube();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Class<? extends TestObjectBuilder<ImageCube>> getBuilderClass()
        {
            return ImageCubeBuilder.class;
        }
    }
}
