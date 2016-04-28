package au.csiro.casda.datadeposit.fits.service;

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

import java.io.File;
import java.io.FileNotFoundException;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import au.csiro.casda.datadeposit.fits.assembler.FitsImageAssembler;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Project;

/**
 * Provides methods to update an ImageCube with FITS metadata.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Component
public class FitsImageService
{

    /**
     * Exception thrown when the ImageCube project code does not match a FITS file's project code.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static class ProjectCodeMismatchException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         * 
         * @param expectedProject
         *            a Project
         * @param fitsProjectCode
         *            the FITS file's project code
         */
        public ProjectCodeMismatchException(Project expectedProject, String fitsProjectCode)
        {
            super(String.format("Expected FITS file to have project code '%s' but was '%s'",
                    expectedProject.getOpalCode(), fitsProjectCode));
        }
    }

    /**
     * Exception thrown when an ImageCube with the given filename could not be found for the Observation with the given
     * sbid.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static class ImageCubeNotFoundException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         * 
         * @param sbid
         *            SBID of Observation
         * @param imageCubeFilename
         *            filename of ImageCube
         */
        public ImageCubeNotFoundException(Integer sbid, String imageCubeFilename)
        {
            super("Image Cube could not be found using sbid " + sbid + " and imageCubeFilename " + imageCubeFilename);
        }
    }

    /**
     * Exception thrown when the ImageCube could not be updated.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static class RepositoryException extends Exception
    {
        /**
         * Constructor
         * 
         * @param message
         *            the cause of the Exception
         */
        public RepositoryException(String message)
        {
            super(message);
        }

        /**
         * Constructor
         * 
         * @param cause
         *            the cause of the Exception
         */
        public RepositoryException(DataAccessException cause)
        {
            super(cause);
        }

        private static final long serialVersionUID = 1L;

    }

    /**
     * Exception that represents a problem when importing a FITS file.
     * <p>
     * Copyright 2015, CSIRO Australia. All rights reserved.
     */
    public static class FitsImportException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         * 
         * @param message
         *            the exception detail
         */
        public FitsImportException(String message)
        {
            super(message);
        }

        /**
         * Constructor
         * 
         * @param cause
         *            the cause of the Exception
         */
        public FitsImportException(Throwable cause)
        {
            super(cause);
        }

        /**
         * Constructor
         * 
         * @param message
         *            the exception detail
         * @param cause
         *            the cause of the Exception
         */
        public FitsImportException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(FitsImageService.class);

    private FitsImageAssembler fitsImageAssembler;

    private ImageCubeRepository imageCubeRepository;

    /**
     * Constructor for a FitsImageServiceImpl that uses the given repository to retrieve, access, and update ImageCubes.
     * 
     * @param imageCubeRepository
     *            a ImageCubeRepository
     * @param fitsImageAssembler
     *            the FitsImageFileAssembler used to populate an image cube from a FITS file
     */
    @Autowired
    public FitsImageService(ImageCubeRepository imageCubeRepository, FitsImageAssembler fitsImageAssembler)
    {
        super();
        this.imageCubeRepository = imageCubeRepository;
        this.fitsImageAssembler = fitsImageAssembler;
    }

    /**
     * Updates an ImageCube (identified by its filename and Observation's SBID) with FITS metadata.
     * 
     * @param sbid
     *            the scheduling block id of the observation
     * @param imageCubeFilename
     *            the name of the file
     * @param imageFile
     *            the image file
     * @throws ImageCubeNotFoundException
     *             if no matching ImageCube could be found
     * @throws RepositoryException
     *             if there was a problem updating the ImageCube
     * @throws FileNotFoundException
     *             if the imagFile does not exist
     * @throws FitsImportException
     *             if there was an error processing the FITS metadata
     * @throws ProjectCodeMismatchException
     *             if the ImageCube's OPAL code does not match the project code in the FITS metadata
     * @return the populated and saved ImageCube
     */
    @Transactional(rollbackOn = Exception.class)
    public ImageCube updateImageCubeWithFitsMetadata(Integer sbid, String imageCubeFilename, File imageFile)
            throws ProjectCodeMismatchException, RepositoryException, ImageCubeNotFoundException, FitsImportException,
            FileNotFoundException
    {
        logger.debug("FitsImageServiceImpl is about to store the metadata");

        try
        {
            ImageCube imageCube = imageCubeRepository.findByObservationSbidAndFilename(sbid, imageCubeFilename);
            if (imageCube == null)
            {
                throw new ImageCubeNotFoundException(sbid, imageCubeFilename);
            }

            fitsImageAssembler.populateImageCube(imageCube, imageFile);

            logger.debug("FitsImageServiceImpl is about to store the FITS metadata for observation {} from file {}",
                    imageCube.getParent().getSbid(), imageCube.getFilename());

            return imageCubeRepository.save(imageCube);
        }
        catch (DataAccessException dataAccessException)
        {
            throw new RepositoryException(dataAccessException);
        }
    }
}
