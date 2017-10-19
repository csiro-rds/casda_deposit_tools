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
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import au.csiro.casda.datadeposit.DepositState.Type;
import au.csiro.casda.datadeposit.DepositStateImpl;
import au.csiro.casda.datadeposit.ParentType;
import au.csiro.casda.datadeposit.fits.assembler.FitsImageAssembler;
import au.csiro.casda.datadeposit.observation.jpa.repository.CubeletRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.MomentMapRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectrumRepository;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.FitsObject;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.Project;
import au.csiro.casda.entity.observation.Spectrum;

/**
 * Provides methods to update an ImageCube with FITS metadata.
 * <p>
 * Copyright 2016, CSIRO Australia. All rights reserved.
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
     * Exception thrown when an ImageCube or Spectrum with the given filename could not be found for the Observation 
     * with the given sbid.
     */
    public static class FitsObjectNotFoundException extends Exception
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
        public FitsObjectNotFoundException(Integer sbid, String imageCubeFilename)
        {
            super("FITS file could not be found using sbid " + sbid + " and fitsFilename " + imageCubeFilename);
        }

        /**
         * Constructor
         * 
         * @param parentId
         *            Id of the parent Observation or level 7 collection
         * @param imageCubeFilename
         *            filename of ImageCube
         * @param parentType
         *            the type of parent object for this image cube
         */
        public FitsObjectNotFoundException(Integer parentId, String imageCubeFilename, ParentType parentType)
        {
            super("FITS file could not be found using parentId " + parentId + " and fitsFilename " + imageCubeFilename
                    + " for parent type of " + parentType);
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

    private SpectrumRepository spectrumRepository;

    private MomentMapRepository momentMapRepository;

    private CubeletRepository cubeletRepository;

    /**
     * Constructor for a FitsImageServiceImpl that uses the given repository to retrieve, access, and update ImageCubes.
     * 
     * @param imageCubeRepository
     *            a ImageCubeRepository
     * @param spectrumRepository
     *            a SpectrumRepository for managing Spectrum objects in the database.
     * @param momentMapRepository
     *            a MomentMapRepository for managing MomentMap objects in the database.
     * @param cubeletRepository
     *            a cubeletRepository for managing Cubelet objects in the database.
     * @param fitsImageAssembler
     *            the FitsImageFileAssembler used to populate an image cube from a FITS file
     */
    @Autowired
    public FitsImageService(ImageCubeRepository imageCubeRepository, SpectrumRepository spectrumRepository,
            MomentMapRepository momentMapRepository, CubeletRepository cubeletRepository, 
            FitsImageAssembler fitsImageAssembler)
    {
        super();
        this.imageCubeRepository = imageCubeRepository;
        this.spectrumRepository = spectrumRepository;
        this.momentMapRepository = momentMapRepository;
        this.cubeletRepository = cubeletRepository;
        this.fitsImageAssembler = fitsImageAssembler;
    }

    /**
     * Updates an ImageCube (identified by its filename and Observation's SBID) with FITS metadata.
     * 
     * @param parentId
     *            the scheduling block id of the observation
     * @param imageCubeFilename
     *            the name of the file
     * @param imageFile
     *            the image file
     * @param parentType 
     *            the type of parent object for this image cube
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     * @throws FitsObjectNotFoundException
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
    public ImageCube updateImageCubeWithFitsMetadata(Integer parentId, String imageCubeFilename, File imageFile,
            ParentType parentType, boolean refresh) throws ProjectCodeMismatchException, RepositoryException,
            FitsObjectNotFoundException, FitsImportException, FileNotFoundException
    {
        logger.debug("FitsImageServiceImpl is about to store the metadata");

        try
        {
            ImageCube imageCube = findImageCube(parentId, imageCubeFilename, parentType);
            if (imageCube == null)
            {
                throw new FitsObjectNotFoundException(parentId, imageCubeFilename, parentType);
            }

            fitsImageAssembler.populateFitsObject(imageCube, imageFile);

            // Advance the deposit status of the image cube here to avoid concurrent mod errors 
            if (!refresh)
            {
                imageCube.setDepositState(new DepositStateImpl(Type.PROCESSED, imageCube));
            }
            
            logger.debug("FitsImageServiceImpl is about to store the FITS metadata for observation {} from file {}",
                    imageCube.getParent().getUniqueId(), imageCube.getFilename());

            return imageCubeRepository.save(imageCube);
        }
        catch (DataAccessException dataAccessException)
        {
            throw new RepositoryException(dataAccessException);
        }
    }

    private ImageCube findImageCube(Integer parentId, String imageCubeFilename, ParentType parentType)
    {
        ImageCube imageCube;
        if (parentType == ParentType.LEVEL_7_COLLECTION)
        {
            imageCube = imageCubeRepository.findByLevel7CollectionDapCollectionIdAndFilename(parentId,
                    imageCubeFilename);
        }
        else
        {
            imageCube = imageCubeRepository.findByObservationSbidAndFilename(parentId, imageCubeFilename);
        }
        return imageCube;
    }
    
    
    /**
     * Updates the spectra associated with an image cube (identified by its filename and Observation's SBID) with FITS
     * metadata.
     * 
     * @param parentId
     *            the scheduling block id of the observation
     * @param imageCubeFilename
     *            the name of the parent image cube file
     * @param parentPath
     *            the path to the root of the observation or collection folder
     * @param parentType 
     *            the type of parent object for this spectrum
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     *            
     * @throws FitsObjectNotFoundException
     *             if no matching ImageCube could be found
     * @throws RepositoryException
     *             if there was a problem updating the ImageCube
     * @throws FileNotFoundException
     *             if the imagFile does not exist
     * @throws FitsImportException
     *             if there was an error processing the FITS metadata
     * @throws ProjectCodeMismatchException
     *             if the ImageCube's OPAL code does not match the project code in the FITS metadata
     * @return the populated and saved Spectrum
     */
    @Transactional(rollbackOn = Exception.class)
    public FitsObject updateSpectraWithFitsMetadata(Integer parentId, String imageCubeFilename, File parentPath,
            ParentType parentType, boolean refresh) throws ProjectCodeMismatchException, RepositoryException,
            FitsObjectNotFoundException, FitsImportException, FileNotFoundException
    {
        try
        {
            FitsObject target;
            List<Spectrum> spectra;
            if (parentType == ParentType.LEVEL_7_COLLECTION)
            {
                // Level 7 spectra do not have parent images so we currently process them one at a time
                Spectrum spectrum = spectrumRepository.findByLevel7CollectionDapCollectionIdAndFilename(parentId,
                        imageCubeFilename);
                spectra = new ArrayList<>();
                spectra.add(spectrum);
                target = spectrum;
                // Find the base of this spectrum's filename
                int countFolders = StringUtils.countMatches(spectrum.getFilename(), '/');
                for (int i = 0; i < countFolders+1; i++)
                {
                    parentPath = parentPath.getParentFile();
                }
            }
            else
            {
                // Get image cube (i.e. the parent file)
                ImageCube imageCube = findImageCube(parentId, imageCubeFilename, parentType);
                if (imageCube == null)
                {
                    throw new FitsObjectNotFoundException(parentId, imageCubeFilename, parentType);
                }
                spectra = imageCube.getSpectra();
                target = imageCube;
            }

            // Read all spectra from that file
            for (Spectrum spectrum : spectra)
            {
                File spectrumFile = new File(parentPath, spectrum.getFilename());
                logger.info("Processing spectrum {} at {}", spectrum.getFileId(), spectrumFile);
                fitsImageAssembler.populateFitsObject(spectrum, spectrumFile);

                // Advance the deposit status of the moment map here to avoid concurrent mod errors
                if (!refresh)
                {
                    spectrum.setDepositState(new DepositStateImpl(Type.ENCAPSULATING, spectrum));
                }

                logger.debug("FitsImageServiceImpl is about to store the FITS metadata for observation {} from file {}",
                        spectrum.getParent().getUniqueId(), spectrum.getFilename());

                spectrumRepository.save(spectrum);
            }

            return target;
        }
        catch (DataAccessException dataAccessException)
        {
            throw new RepositoryException(dataAccessException);
        }
    }
    
    
    /**
     * Updates the moment maps associated with an image cube (identified by its filename and Observation's SBID) with 
     * FITS metadata.
     * 
     * @param parentId
     *            the scheduling block id of the observation
     * @param imageCubeFilename
     *            the name of the parent image cube file
     * @param parentPath
     *            the path to the root of the observation or collection folder
     * @param parentType 
     *            the type of parent object for this moment map
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     *            
     * @throws FitsObjectNotFoundException
     *             if no matching MomentMap could be found
     * @throws RepositoryException
     *             if there was a problem updating the MomentMap
     * @throws FileNotFoundException
     *             if the imageFile does not exist
     * @throws FitsImportException
     *             if there was an error processing the FITS metadata
     * @throws ProjectCodeMismatchException
     *             if the ImageCube's OPAL code does not match the project code in the FITS metadata
     * @return the populated and saved MomentMap
     */
    @Transactional(rollbackOn = Exception.class)
    public FitsObject updateMomentMapsWithFitsMetadata(Integer parentId, String imageCubeFilename, File parentPath,
            ParentType parentType, boolean refresh) throws ProjectCodeMismatchException, RepositoryException,
            FitsObjectNotFoundException, FitsImportException, FileNotFoundException
    {
        try
        {
            FitsObject target;
            List<MomentMap> momentMaps;
            if (parentType == ParentType.LEVEL_7_COLLECTION)
            {
                // Level 7 moment maps do not have parent images so we currently process them one at a time
                MomentMap momentMap = momentMapRepository.findByLevel7CollectionDapCollectionIdAndFilename(parentId,
                        imageCubeFilename);
                momentMaps = new ArrayList<>();
                momentMaps.add(momentMap);
                target = momentMap;
                // Find the base of this moment map's filename
                int countFolders = StringUtils.countMatches(momentMap.getFilename(), '/');
                for (int i = 0; i < countFolders+1; i++)
                {
                    parentPath = parentPath.getParentFile();
                }
            }
            else
            {
                // Get image cube (i.e. the parent file)
                ImageCube imageCube = findImageCube(parentId, imageCubeFilename, parentType);
                if (imageCube == null)
                {
                    throw new FitsObjectNotFoundException(parentId, imageCubeFilename, parentType);
                }
                momentMaps = imageCube.getMomentMaps();
                target = imageCube;
            }

            // Read all moment maps from that file
            for (MomentMap momentMap : momentMaps)
            {
                File momentMapFile = new File(parentPath, momentMap.getFilename());
                logger.info("Processing moment map {} at {}", momentMap.getFileId(), momentMapFile);
                fitsImageAssembler.populateFitsObject(momentMap, momentMapFile);

                // Advance the deposit status of the moment map here to avoid concurrent mod errors
                if (!refresh)
                {
                    momentMap.setDepositState(new DepositStateImpl(Type.ENCAPSULATING, momentMap));
                }

                logger.debug("FitsImageServiceImpl is about to store the FITS metadata for observation {} from file {}",
                        momentMap.getParent().getUniqueId(), momentMap.getFilename());

                momentMapRepository.save(momentMap);
            }

            return target;
        }
        catch (DataAccessException dataAccessException)
        {
            throw new RepositoryException(dataAccessException);
        }
    }
    
    /**
     * Updates the cubelets associated with an image cube (identified by its filename and Observation's SBID) with FITS
     * metadata.
     * 
     * @param parentId
     *            the scheduling block id of the observation
     * @param imageCubeFilename
     *            the name of the parent image cube file
     * @param parentPath
     *            the path to the root of the observation or collection folder
     * @param parentType
     *            the type of parent object for this Cubelet
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     *            
     * @throws FitsObjectNotFoundException
     *             if no matching Cubelet could be found
     * @throws RepositoryException
     *             if there was a problem updating the Cubelet
     * @throws FileNotFoundException
     *             if the imageFile does not exist
     * @throws FitsImportException
     *             if there was an error processing the FITS metadata
     * @throws ProjectCodeMismatchException
     *             if the ImageCube's OPAL code does not match the project code in the FITS metadata
     * @return the populated and saved Cubelet
     */
    @Transactional(rollbackOn = Exception.class)
    public FitsObject updateCubeletsWithFitsMetadata(Integer parentId, String imageCubeFilename, File parentPath,
            ParentType parentType, boolean refresh) throws ProjectCodeMismatchException, RepositoryException,
            FitsObjectNotFoundException, FitsImportException, FileNotFoundException
    {
        try
        {
            FitsObject target;
            List<Cubelet> cubelets;
            if (parentType == ParentType.LEVEL_7_COLLECTION)
            {
                // Level 7 cubelets do not have parent images so we currently process them one at a time
                Cubelet cubelet = cubeletRepository.findByLevel7CollectionDapCollectionIdAndFilename(parentId,
                        imageCubeFilename);
                cubelets = new ArrayList<>();
                cubelets.add(cubelet);
                target = cubelet;
                // Find the base of this cubelet's filename
                int countFolders = StringUtils.countMatches(cubelet.getFilename(), '/');
                for (int i = 0; i < countFolders+1; i++)
                {
                    parentPath = parentPath.getParentFile();
                }
            }
            else
            {
                // Get image cube (i.e. the parent file)
                ImageCube imageCube = findImageCube(parentId, imageCubeFilename, parentType);
                if (imageCube == null)
                {
                    throw new FitsObjectNotFoundException(parentId, imageCubeFilename, parentType);
                }
                cubelets = imageCube.getCubelets();
                target = imageCube;
            }

            // Read all cubelets from that file
            for (Cubelet cubelet : cubelets)
            {
                File cubeletFile = new File(parentPath, cubelet.getFilename());
                logger.info("Processing cubelet {} at {}", cubelet.getFileId(), cubeletFile);
                fitsImageAssembler.populateFitsObject(cubelet, cubeletFile);

                // Advance the deposit status of the moment map here to avoid concurrent mod errors
                if (!refresh)
                {
                    cubelet.setDepositState(new DepositStateImpl(Type.ENCAPSULATING, cubelet));
                }

                logger.debug("FitsImageServiceImpl is about to store the FITS metadata for observation {} from file {}",
                        cubelet.getParent().getUniqueId(), cubelet.getFilename());

                cubeletRepository.save(cubelet);
            }

            return target;
        }
        catch (DataAccessException dataAccessException)
        {
            throw new RepositoryException(dataAccessException);
        }
    }
}
