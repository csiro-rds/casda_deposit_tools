package au.csiro.casda.datadeposit.fits;

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
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import au.csiro.casda.AppConfig;
import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.ParentType;
import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.fits.FitsCommandLineArgumentsParser.CommandLineArguments;
import au.csiro.casda.datadeposit.fits.service.FitsImageService;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.FitsImportException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.FitsObjectNotFoundException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.ProjectCodeMismatchException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.RepositoryException;
import au.csiro.casda.datadeposit.observation.validation.NonNull;
import au.csiro.casda.entity.observation.FitsObject;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.logging.DataLocation;
import au.csiro.logging.CasdaDataDepositEvents;

/**
 * CommandLineImporter implements a Spring CommandLineRunner that can be used to import data from a FITS file header.
 * The data will then be added to the database.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Import(AppConfig.class)
public class FitsCommandLineImporter extends
        ArgumentsDrivenCommandLineTool<FitsCommandLineArgumentsParser.CommandLineArguments>
{
    /**
     * The name of this tool.
     */
    public static final String TOOL_NAME = "fits_import";

    private static final Logger logger = LoggerFactory.getLogger(FitsCommandLineImporter.class);

    /**
     * The parser for the arguments for this CommandLineRunner.
     */
    private FitsCommandLineArgumentsParser commandLineArgumentsParser = new FitsCommandLineArgumentsParser();

    @Autowired
    private FitsImageService fitsImageService;
    
    private Level7CollectionRepository level7CollectionRepository;
    
    /**
     * Contructor
     * 
     * @param level7CollectionRepository the level 7 colleciton repository
     */
    @Autowired
    public FitsCommandLineImporter(Level7CollectionRepository level7CollectionRepository)
    {
    	this.level7CollectionRepository = level7CollectionRepository;
    }

    /**
     * The arguments passed in to the program from the command line by the operator
     * 
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args)
    {
        runCommandLineTool(FitsCommandLineImporter.class, logger, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args)
    {

        parseCommandLineArguments(logger, args);

        Instant startTime = Instant.now();

        String projectCode = null;

        CommandLineArguments parsedArgs = commandLineArgumentsParser.getArgs();
        FitsObject fitsObject = null;
        try
        {
            String inFile = parsedArgs.getInfile();
            String fitsFilename = parsedArgs.getFitsFilename();
            int sbid = parsedArgs.getParentId();
            ParentType parentType = ParentType.findParentType(parsedArgs.getParentType());
            FitsObjectType fitsType = parsedArgs.getFitsObjectType();
            boolean refresh = parsedArgs.isRefresh();

            if (fitsFilename == null)
            {
                fitsFilename = inFile;
            }

            if (fitsType == FitsObjectType.IMAGE_CUBE)
            {
                fitsObject = processImageCubeFile(inFile, Integer.valueOf(sbid), fitsFilename, parentType, refresh);
            }
            else if (fitsType == FitsObjectType.SPECTRUM)
            {
                fitsObject = processSpectrumFiles(inFile, Integer.valueOf(sbid), fitsFilename, parentType, refresh);
            }
            else if (fitsType == FitsObjectType.MOMENT_MAP)
            {
                fitsObject = processMomentMapFiles(inFile, Integer.valueOf(sbid), fitsFilename, parentType, refresh);
            }
            else if (fitsType == FitsObjectType.CUBELET)
            {
                fitsObject = processCubeletFiles(inFile, Integer.valueOf(sbid), fitsFilename, parentType, refresh);
            }
            else
            {
                throw new IllegalStateException("Unexpected fits object type of " + fitsType);
            }
            projectCode = fitsObject.getProject().getOpalCode();
        }
        catch (FileNotFoundException | FitsImageService.FitsImportException e)
        {
            /*
             * if the file cannot be opened (eg: because it doesn't exist) or if the file cannot be processed as a
             * FITS file
             */
            logger.error(
                    CasdaDataDepositEvents.E066.messageBuilder().add(parsedArgs.getInfile())
                            .add(String.join(" ", args)).toString(), e);

            if(ParentType.LEVEL_7_COLLECTION == ParentType.findParentType(parsedArgs.getParentType()) 
        			&& e instanceof FitsImportException)
        	{
            	Level7Collection parent = level7CollectionRepository.findByDapCollectionId(parsedArgs.getParentId());
            	if (parent != null)
            	{
                	parent.getErrors().add(parsedArgs.getFitsFilename() + " : " + e.getCause().getMessage());
                	level7CollectionRepository.save(parent);
            	}
        	}
            System.exit(1);
        }
        catch (FitsImageService.ProjectCodeMismatchException e)
        {
        	if(ParentType.LEVEL_7_COLLECTION == ParentType.findParentType(parsedArgs.getParentType()))
        	{
            	Level7Collection parent = level7CollectionRepository.findByDapCollectionId(parsedArgs.getParentId());
            	parent.getErrors().add(parsedArgs.getFitsFilename() + " : " + e.getMessage());
            	level7CollectionRepository.save(parent);
        	}
            logger.error(
                    CasdaDataDepositEvents.E108.messageBuilder().add(parsedArgs.getInfile())
                            .add(String.join(" ", args)).toString(), e);
            System.exit(1);
        }
        catch (FitsObjectNotFoundException | RepositoryException e)
        {
            /*
             * if an application-specific error occurred (usually db-related)
             */
            logger.error(
                    CasdaDataDepositEvents.E067.messageBuilder().add(parsedArgs.getInfile())
                            .add(String.join(" ", args)).toString(), e);
            System.exit(1);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOf(new File(this.getCommandLineArgumentsParser().getArgs().getInfile()));
        String fileId = getFileId();

        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E065.messageBuilder() //
                .add(parsedArgs.getInfile()) //
                .add(parsedArgs.getParentId()) //
                .add(projectCode) //
                .addStartTime(startTime) //
                .addEndTime(endTime) //
                .addSource(DataLocation.RTC) //
                .addDestination(DataLocation.CASDA_DB) //
                .addVolumeBytes(filesizeInBytes) //
                .addFileId(fileId);
        logger.info(messageBuilder.toString());

        System.exit(0);
    }

    private String getFileId()
    {
        CommandLineArguments args = commandLineArgumentsParser.getArgs();

        FitsObject fitsObject = new ImageCube();

        fitsObject.setFilename(args.getFitsFilename());
        if (CommonCommandLineArguments.LEVEL7_PARENT_TYPE.equalsIgnoreCase(args.getParentType()))
        {
            fitsObject.setParent(new Level7Collection(args.getParentId()));
        }
        else
        {
            fitsObject.setParent(new Observation(args.getParentId()));
        }
        return fitsObject.getFileId();
    }

    /**
     * Processes the given file by extracting the metadata from the header and updating the database accordingly.
     * 
     * @param inFile
     *            the File to parse. May not be null.
     * @param sbid
     *            the observation's scheduling block id
     * @param imageCubeFilename
     *            the filename in the image cube table.
     * @param parentType 
     *            the type of parent object for this image cube
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     * @return the fits file headers
     * @throws FitsObjectNotFoundException
     *             if an ImageCube with the given name and observation (of the given sbid) could not be found
     * @throws RepositoryException
     *             if there was a problem accessing or updating a Repository
     * @throws FileNotFoundException
     *             if the file could not be found
     * @throws FitsImportException
     *             if the file is not a valid FITS file
     * @throws ProjectCodeMismatchException
     *             if the project code in the FITS file does not match the associated Image Cube's project code
     */
    public ImageCube processImageCubeFile(@NonNull String inFile, Integer sbid, String imageCubeFilename,
            ParentType parentType, boolean refresh) throws FitsObjectNotFoundException, RepositoryException,
            ProjectCodeMismatchException, FileNotFoundException, FitsImportException
    {
        File fitsFile = new File(inFile);

        if (!fitsFile.exists())
        {
            throw new FileNotFoundException(fitsFile.getName() + " not found");
        }

        // process the headers (ie find the corresponding image cube, store the data, save it).
        return fitsImageService.updateImageCubeWithFitsMetadata(sbid, imageCubeFilename, fitsFile, parentType, refresh);
    }

    /**
     * Processes a set of spectrum files by extracting the metadata from their headers and updating the database
     * accordingly.
     * 
     * @param inFile
     *            the File to parse. May not be null.
     * @param sbid
     *            the observation's scheduling block id
     * @param imageCubeFilename
     *            the filename in the image_cube table - the source cube of all spectra to be processed.
     * @param parentType 
     *            the type of parent object for this image cube
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     *            
     * @return the fits file headers
     * @throws FitsObjectNotFoundException
     *             if a Spectrum with the given name and observation (of the given sbid) could not be found
     * @throws RepositoryException
     *             if there was a problem accessing or updating a Repository
     * @throws FileNotFoundException
     *             if the file could not be found
     * @throws FitsImportException
     *             if the file is not a valid FITS file
     * @throws ProjectCodeMismatchException
     *             if the project code in the FITS file does not match the associated Image Cube's project code
     */
    public FitsObject processSpectrumFiles(@NonNull String inFile, Integer sbid, String imageCubeFilename,
            ParentType parentType, boolean refresh) throws FitsObjectNotFoundException, RepositoryException,
            ProjectCodeMismatchException, FileNotFoundException, FitsImportException
    {
        File parentFile = new File(inFile);

        if (!parentFile.exists())
        {
            throw new FileNotFoundException(parentFile.getName() + " not found");
        }

        // process the headers (ie find the corresponding image cube, store the data, save it).
        return fitsImageService.updateSpectraWithFitsMetadata(sbid, imageCubeFilename, parentFile, parentType, refresh);
    }

    /**
     * Processes a set of moment map files by extracting the metadata from their headers and updating the database
     * accordingly.
     * 
     * @param inFile
     *            the path to the collection or scheduling block folder. May not be null.
     * @param sbid
     *            the observation's scheduling block id
     * @param imageCubeFilename
     *            the filename in the image_cube table - the source cube of all moment maps to be processed.
     * @param parentType
     *            the type of parent object for this image cube
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     * 
     * @return the fits file headers
     * @throws FitsObjectNotFoundException
     *             if a Spectrum with the given name and observation (of the given sbid) could not be found
     * @throws RepositoryException
     *             if there was a problem accessing or updating a Repository
     * @throws FileNotFoundException
     *             if the parent folder could not be found
     * @throws FitsImportException
     *             if the file is not a valid FITS file
     * @throws ProjectCodeMismatchException
     *             if the project code in the FITS file does not match the associated Image Cube's project code
     */
    public FitsObject processMomentMapFiles(@NonNull String inFile, Integer sbid, String imageCubeFilename,
            ParentType parentType, boolean refresh) throws FitsObjectNotFoundException, RepositoryException,
            ProjectCodeMismatchException, FileNotFoundException, FitsImportException
    {
        File parentFile = new File(inFile);

        if (!parentFile.exists())
        {
            throw new FileNotFoundException(parentFile.getName() + " not found");
        }

        // process the headers (ie find the corresponding image cube, store the data, save it).
        return fitsImageService.updateMomentMapsWithFitsMetadata(sbid, imageCubeFilename, parentFile, parentType, refresh);
    }
    
    /**
     * Processes a set of cubelet files by extracting the metadata from their headers and updating the database
     * accordingly.
     * 
     * @param inFile
     *            the path to the collection or scheduling block folder. May not be null.
     * @param sbid
     *            the observation's scheduling block id
     * @param imageCubeFilename
     *            the filename in the image_cube table - the source cube of all cubelets to be processed.
     * @param parentType 
     *            the type of parent object for this image cube
     * @param refresh
     *            Indicates that this is a refresh of an already deposited FITS object
     * @return the fits file headers
     * @throws FitsObjectNotFoundException
     *             if a Spectrum with the given name and observation (of the given sbid) could not be found
     * @throws RepositoryException
     *             if there was a problem accessing or updating a Repository
     * @throws FileNotFoundException
     *             if the parent folder could not be found
     * @throws FitsImportException
     *             if the file is not a valid FITS file
     * @throws ProjectCodeMismatchException
     *             if the project code in the FITS file does not match the associated Image Cube's project code
     */
    public FitsObject processCubeletFiles(@NonNull String inFile, Integer sbid, String imageCubeFilename,
            ParentType parentType, boolean refresh) throws FitsObjectNotFoundException, RepositoryException,
            ProjectCodeMismatchException, FileNotFoundException, FitsImportException
    {
        File parentFile = new File(inFile);

        if (!parentFile.exists())
        {
            throw new FileNotFoundException(parentFile.getName() + " not found");
        }

        // process the headers (ie find the corresponding image cube, store the data, save it).
        return fitsImageService.updateCubeletsWithFitsMetadata(sbid, imageCubeFilename, parentFile, parentType, refresh);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractCommandLineArgumentsParser<CommandLineArguments> getCommandLineArgumentsParser()
    {
        return this.commandLineArgumentsParser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CasdaDataDepositEvents getMalformedParametersEvent()
    {
        return CasdaDataDepositEvents.E033;
    }
}
