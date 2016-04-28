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
import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.datadeposit.fits.FitsCommandLineArgumentsParser.CommandLineArguments;
import au.csiro.casda.datadeposit.fits.service.FitsImageService;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.FitsImportException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.ImageCubeNotFoundException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.ProjectCodeMismatchException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.RepositoryException;
import au.csiro.casda.datadeposit.observation.validation.NonNull;
import au.csiro.casda.entity.observation.ImageCube;
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

        try
        {
            String inFile = commandLineArgumentsParser.getArgs().getInfile();
            String imageCubeFilename = commandLineArgumentsParser.getArgs().getImageCubeFilename();
            int sbid = commandLineArgumentsParser.getArgs().getSbid();

            if (imageCubeFilename == null)
            {
                imageCubeFilename = inFile;
            }

            ImageCube imageCube = processFitsFile(inFile, Integer.valueOf(sbid), imageCubeFilename);
            projectCode = imageCube.getProject().getOpalCode();
        }
        catch (FileNotFoundException | FitsImageService.FitsImportException e)
        {
            /*
             * if the file cannot be opened (eg: because it doesn't exist) or if the file cannot be processed as a
             * FITS file
             */
            logger.error(
                    CasdaDataDepositEvents.E066.messageBuilder().add(commandLineArgumentsParser.getArgs().getInfile())
                            .add(String.join(" ", args)).toString(), e);
            System.exit(1);
        }
        catch (FitsImageService.ProjectCodeMismatchException e)
        {
            logger.error(
                    CasdaDataDepositEvents.E108.messageBuilder().add(commandLineArgumentsParser.getArgs().getInfile())
                            .add(String.join(" ", args)).toString(), e);
            System.exit(1);
        }
        catch (ImageCubeNotFoundException | RepositoryException e)
        {
            /*
             * if an application-specific error occurred (usually db-related)
             */
            logger.error(
                    CasdaDataDepositEvents.E067.messageBuilder().add(commandLineArgumentsParser.getArgs().getInfile())
                            .add(String.join(" ", args)).toString(), e);
            System.exit(1);
        }

        Instant endTime = Instant.now();

        long filesizeInBytes = FileUtils.sizeOf(new File(this.getCommandLineArgumentsParser().getArgs().getInfile()));
        String fileId = getFileId();

        DataDepositMessageBuilder messageBuilder = CasdaDataDepositEvents.E065.messageBuilder() //
                .add(commandLineArgumentsParser.getArgs().getInfile()) //
                .add(commandLineArgumentsParser.getArgs().getSbid()) //
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
        ImageCube imageCube = new ImageCube();
        imageCube.setParent(new Observation(commandLineArgumentsParser.getArgs().getSbid()));
        imageCube.setFilename(commandLineArgumentsParser.getArgs().getImageCubeFilename());
        return imageCube.getFileId();
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
     * @return the fits file headers
     * @throws ImageCubeNotFoundException
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
    public ImageCube processFitsFile(@NonNull String inFile, Integer sbid, String imageCubeFilename)
            throws ImageCubeNotFoundException, RepositoryException, ProjectCodeMismatchException,
            FileNotFoundException, FitsImportException
    {
        File fitsFile = new File(inFile);

        if (!fitsFile.exists())
        {
            throw new FileNotFoundException(fitsFile.getName() + " not found");
        }

        // process the headers (ie find the corresponding image cube, store the data, save it).
        return fitsImageService.updateImageCubeWithFitsMetadata(sbid, imageCubeFilename, fitsFile);
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
