package au.csiro.casda.datadeposit.level7;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import au.csiro.casda.Utils;
import au.csiro.casda.datadeposit.DepositState.Type;
import au.csiro.casda.datadeposit.DepositStateImpl;
import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.copy.CopyDataException;
import au.csiro.casda.datadeposit.exception.CreateChecksumException;
import au.csiro.casda.datadeposit.observation.ObservationParser;
import au.csiro.casda.datadeposit.service.InlineScriptException;
import au.csiro.casda.datadeposit.service.InlineScriptService;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.Project;
import au.csiro.casda.entity.observation.Spectrum;
import au.csiro.casda.entity.observation.Thumbnail;
import au.csiro.casda.exception.CommandLineServiceException;
import au.csiro.casda.jobmanager.JobManager.Job;
import au.csiro.casda.jobmanager.JobManager.JobMonitor;
import au.csiro.casda.jobmanager.ProcessJobBuilder.ProcessJobFactory;
import au.csiro.casda.jobmanager.SimpleToolProcessJobBuilder;

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
 * Implementation class to copy level 7 image/spectra data to a protected area and create objects for each file of
 * interest (fits files and thumbnails).
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@Component
public class Level7CollectionDataCopierImpl implements Level7CollectionDataCopier, JobMonitor
{
    private static final String MOMENT_MAP_FOLDER_NAME = "moment_map";

    private static final String SPECTRUM_FOLDER_NAME = "spectrum";

    private static final String IMAGE_CUBE_FOLDER_NAME = "image_cube";

    private static final String CUBELET_FOLDER_NAME = "cubelet";
    
    private static final Logger logger = LoggerFactory.getLogger(Level7CollectionDataCopierImpl.class);

    private Level7CollectionRepository level7CollectionRepository;

    private ProcessJobFactory processJobFactory;

    private String[] level7StageCommand;

    private boolean failed;

    private String failureCause;

    private String calculateChecksumScript;

    private InlineScriptService inlineScriptService;

    /**
     * Constructs a Level7CollectionDataCopierImpl that does the work of copying the files to staging and registering
     * the files.
     * 
     * @param level7CollectionRepository
     *            a Level7CollectionRepository
     * @param processJobFactory
     *            a ProcessJobFactory for running external commands
     * @param level7StageCommand
     *            The command to be used to copy the level 7 data form the user's folder
     * @param calculateChecksumScript
     *            The command to calculate a checksum.
     * @param inlineScriptService
     *            the service to run simple scripts such as checksums
     */
    @Autowired
    public Level7CollectionDataCopierImpl(Level7CollectionRepository level7CollectionRepository,
            ProcessJobFactory processJobFactory, @Value("${level7.copy.command}") String level7StageCommand,
            @Value("${calculate.checksum.script}") String calculateChecksumScript,
            InlineScriptService inlineScriptService)
    {
        super();
        this.level7CollectionRepository = level7CollectionRepository;
        this.processJobFactory = processJobFactory;
        this.level7StageCommand = Utils.elStringToArray(level7StageCommand);
        this.calculateChecksumScript = calculateChecksumScript;
        this.inlineScriptService = inlineScriptService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackOn = { Exception.class })
    public Level7Collection copyData(long dataCollectionId, String collectionFolder)
            throws CopyDataException, CreateChecksumException
    {
        // Read database record
        Level7Collection level7Collection = level7CollectionRepository.findByDapCollectionId(dataCollectionId);
        if (level7Collection == null)
        {
            throw new IllegalArgumentException(
                    "expected dataCollectionId to match an existing level7_collection record");
        }

        // Copy each file tree
        createStagingFolders(level7Collection, collectionFolder);
        copyFileTree(level7Collection.getImageCubePath(), collectionFolder, IMAGE_CUBE_FOLDER_NAME, dataCollectionId);
        copyFileTree(level7Collection.getSpectrumPath(), collectionFolder, SPECTRUM_FOLDER_NAME, dataCollectionId);
        copyFileTree(level7Collection.getMomentMapPath(), collectionFolder, MOMENT_MAP_FOLDER_NAME, dataCollectionId);
        copyFileTree(level7Collection.getCubeletPath(), collectionFolder, CUBELET_FOLDER_NAME, dataCollectionId);

        // Create objects for each file of interest
        createImageCubesForFiles(level7Collection, collectionFolder, IMAGE_CUBE_FOLDER_NAME);
        int numEncaps = createSpectraForFiles(level7Collection, collectionFolder, SPECTRUM_FOLDER_NAME, 0);
        createMomentMapsForFiles(level7Collection, collectionFolder, MOMENT_MAP_FOLDER_NAME, numEncaps);
        createCubeletsForFiles(level7Collection, collectionFolder, CUBELET_FOLDER_NAME, numEncaps);

        return level7CollectionRepository.save(level7Collection);
    }

    private void createStagingFolders(Level7Collection collection, String collectionFolder)
    {
        Path collectionPath = Paths.get(collectionFolder);
        File dcFolder = collectionPath.toFile();
        dcFolder.mkdir();
        if (collection.getImageCubePath() != null)
        {
            new File(dcFolder, IMAGE_CUBE_FOLDER_NAME).mkdir();
        }
        if (collection.getSpectrumPath() != null)
        {
            new File(dcFolder, SPECTRUM_FOLDER_NAME).mkdir();
        }
        if (collection.getMomentMapPath() != null)
        {
            new File(dcFolder, MOMENT_MAP_FOLDER_NAME).mkdir();
        }
        if (collection.getCubeletPath() != null)
        {
            new File(dcFolder, CUBELET_FOLDER_NAME).mkdir();
        }
    }

    private void copyFileTree(String sourcePath, String collectionFolder, String subFolder, long dataCollectionId)
            throws CopyDataException
    {
        if (StringUtils.isEmpty(sourcePath))
        {
            // Ignore folders which are not required
            return;
        }

        Path targetPath = Paths.get(collectionFolder, subFolder);

        logger.info(String.format("Copying %s to %s", sourcePath, targetPath));

        SimpleToolProcessJobBuilder stageProcessBuilder;
        stageProcessBuilder = new SimpleToolProcessJobBuilder(processJobFactory, this.level7StageCommand);
        stageProcessBuilder.setProcessParameter("source_folder", sourcePath);
        stageProcessBuilder.setProcessParameter("target_folder", targetPath.toString());
        Job stageJob = stageProcessBuilder.createJob("copy-data-" + dataCollectionId, "copyData");
        stageJob.run(this);
        if (this.failed)
        {
            throw new CopyDataException(dataCollectionId, sourcePath, targetPath.toString(),
                    stageProcessBuilder.getJobFailureMessage(this.failureCause));
        }
    }

    /**
     * Create a set of image cubes objects to match the files in the image cube folder.
     * 
     * @param level7Collection
     *            the target Level7Collection
     * @param collectionFolder
     *            The base folder for the collection
     * @param imageCubeFolderName
     *            The name of the folder holding the image cubes
     * @throws CreateChecksumException
     *             If checksums cannot be created for files.
     */
    void createImageCubesForFiles(Level7Collection level7Collection, String collectionFolder,
            String imageCubeFolderName) throws CreateChecksumException
    {
        Path dataPath = Paths.get(collectionFolder, imageCubeFolderName);
        Project project = level7Collection.getProject();

        List<String[]> fileList = getFileList(dataPath, new ArrayList<>());
        for (String[] dataFile : fileList)
        {
            // Create a new Image Cube for each data file and optionally a thumbnail
            ImageCube imageCube = new ImageCube(project);
            imageCube.setFilename(dataFile[0]);
            imageCube.setFormat("fits");
            imageCube.setType("Unknown");
            imageCube.setFilesize(ObservationParser.calculateFileSizeKb(new File(collectionFolder, dataFile[0])));
            imageCube.setDepositState(new DepositStateImpl(Type.PROCESSING, imageCube));
            createChecksumIfMissing(collectionFolder, dataFile[0]);

            if (StringUtils.isNotEmpty(dataFile[1]))
            {
                Thumbnail thumbnail = createThumbnail(collectionFolder, dataFile[1]);
                imageCube.setLargeThumbnail(thumbnail);
                imageCube.setSmallThumbnail(thumbnail);
                createChecksumIfMissing(collectionFolder, dataFile[1]);
            }

            level7Collection.addImageCube(imageCube);
        }

    }


    /**
     * Create a set of spectrum objects to match the files in the spectrum folder.
     * 
     * @param level7Collection
     *            the target Level7Collection
     * @param collectionFolder
     *            The base folder for the collection
     * @param spectrumFolderName
     *            The name of the folder holding the spectra
     * @param numExistingEncaps The number of encapsulations that already exist for this collection. 
     * @return The total number of encapsulations including those already present. 
     * @throws CreateChecksumException
     *             If checksums cannot be created for files.
     */
    int createSpectraForFiles(Level7Collection level7Collection, String collectionFolder,
            String spectrumFolderName, int numExistingEncaps) throws CreateChecksumException
    {
        Path dataPath = Paths.get(collectionFolder, spectrumFolderName);
        Project project = level7Collection.getProject();

        Map<String, EncapsulationFile> encapsulationMap = new HashMap<>();
        List<String> thumbnails = new ArrayList<>();
        List<String[]> fileList = getFileList(dataPath, thumbnails);
        for (String[] dataFile : fileList)
        {
            // Create a new Spectrum for each data file and optionally a thumbnail
            Spectrum spectrum = new Spectrum(project);
            spectrum.setFilename(dataFile[0]);
            spectrum.setFormat("fits");
            spectrum.setType("Unknown");
            spectrum.setFilesize(ObservationParser.calculateFileSizeKb(new File(collectionFolder, dataFile[0])));
            spectrum.setDepositState(new DepositStateImpl(Type.PROCESSING, spectrum));
            spectrum.setEncapsulationFile(
                    getEncapsulationFile(dataFile[0], encapsulationMap, level7Collection, "spectra", numExistingEncaps));
            createChecksumIfMissing(collectionFolder, dataFile[0]);

            if (StringUtils.isNotEmpty(dataFile[1]))
            {
                Thumbnail thumbnail = createThumbnail(collectionFolder, dataFile[1]);
                thumbnail.setEncapsulationFile(getEncapsulationFile(dataFile[1], encapsulationMap, level7Collection,
                        "thumb", numExistingEncaps));
                spectrum.setThumbnail(thumbnail);
                createChecksumIfMissing(collectionFolder, dataFile[1]);
                thumbnails.remove(dataFile[1]);
            }

            level7Collection.addSpectra(spectrum);
        }
        
        deleteUnusedThumbnails(collectionFolder, thumbnails);
        
        return numExistingEncaps + encapsulationMap.size();
    }

    /**
     * Create a set of moment map objects to match the files in the moment map folder.
     * 
     * @param level7Collection
     *            the target Level7Collection
     * @param collectionFolder
     *            The base folder for the collection
     * @param momentMapFolderName
     *            The name of the folder holding the spectra
     * @param numExistingEncaps The number of encapsulations that already exist for this collection. 
     * @return The total number of encapsulations including those already present. 
     * @throws CreateChecksumException
     *             If checksums cannot be created for files.
     */
    int createMomentMapsForFiles(Level7Collection level7Collection, String collectionFolder,
            String momentMapFolderName, int numExistingEncaps) throws CreateChecksumException
    {
        Path dataPath = Paths.get(collectionFolder, momentMapFolderName);
        Project project = level7Collection.getProject();

        Map<String, EncapsulationFile> encapsulationMap = new HashMap<>();
        List<String> thumbnails = new ArrayList<>();
        List<String[]> fileList = getFileList(dataPath, thumbnails);
        for (String[] dataFile : fileList)
        {
            // Create a new Moment Map for each data file and optionally a thumbnail
            MomentMap momentMap = new MomentMap(project);
            momentMap.setFilename(dataFile[0]);
            momentMap.setFormat("fits");
            momentMap.setType("Unknown");
            momentMap.setFilesize(ObservationParser.calculateFileSizeKb(new File(collectionFolder, dataFile[0])));
            momentMap.setDepositState(new DepositStateImpl(Type.PROCESSING, momentMap));
            momentMap.setEncapsulationFile(
                    getEncapsulationFile(dataFile[0], encapsulationMap, level7Collection, "mom", numExistingEncaps));
            createChecksumIfMissing(collectionFolder, dataFile[0]);

            if (StringUtils.isNotEmpty(dataFile[1]))
            {
                Thumbnail thumbnail = createThumbnail(collectionFolder, dataFile[1]);
                thumbnail.setEncapsulationFile(getEncapsulationFile(dataFile[1], encapsulationMap, level7Collection,
                        "thumb", numExistingEncaps));
                momentMap.setThumbnail(thumbnail);
                createChecksumIfMissing(collectionFolder, dataFile[1]);
                thumbnails.remove(dataFile[1]);
            }

            level7Collection.addMomentMap(momentMap);
        }
        
        deleteUnusedThumbnails(collectionFolder, thumbnails);

        return numExistingEncaps + encapsulationMap.size();
    }
    
    /**
     * Create a set of cubelet objects to match the files in the cubelet folder.
     * 
     * @param level7Collection
     *            the target Level7Collection
     * @param collectionFolder
     *            The base folder for the collection
     * @param cubeletFolderName
     *            The name of the folder holding the cubelet
     * @param numExistingEncaps The number of encapsulations that already exist for this collection. 
     * @return The total number of encapsulations including those already present. 
     * @throws CreateChecksumException
     *             If checksums cannot be created for files.
     */
    int createCubeletsForFiles(Level7Collection level7Collection, String collectionFolder,
            String cubeletFolderName, int numExistingEncaps) throws CreateChecksumException
    {
        Path dataPath = Paths.get(collectionFolder, cubeletFolderName);
        Project project = level7Collection.getProject();

        Map<String, EncapsulationFile> encapsulationMap = new HashMap<>();
        List<String> thumbnails = new ArrayList<>();
        List<String[]> fileList = getFileList(dataPath, thumbnails);
        for (String[] dataFile : fileList)
        {
            // Create a new Cubelet for each data file and optionally a thumbnail
        	Cubelet cubelet = new Cubelet(project);
        	cubelet.setFilename(dataFile[0]);
            cubelet.setFormat("fits");
            cubelet.setType("Unknown");
            cubelet.setFilesize(ObservationParser.calculateFileSizeKb(new File(collectionFolder, dataFile[0])));
            cubelet.setDepositState(new DepositStateImpl(Type.PROCESSING, cubelet));
            cubelet.setEncapsulationFile(
                    getEncapsulationFile(dataFile[0], encapsulationMap, level7Collection, "cube", numExistingEncaps));
            createChecksumIfMissing(collectionFolder, dataFile[0]);

            if (StringUtils.isNotEmpty(dataFile[1]))
            {
                Thumbnail thumbnail = createThumbnail(collectionFolder, dataFile[1]);
                thumbnail.setEncapsulationFile(getEncapsulationFile(dataFile[1], encapsulationMap, level7Collection,
                        "thumb", numExistingEncaps));
                cubelet.setThumbnail(thumbnail);
                createChecksumIfMissing(collectionFolder, dataFile[1]);
                thumbnails.remove(dataFile[1]);
            }

            level7Collection.addCubelet(cubelet);
        }
        
        deleteUnusedThumbnails(collectionFolder, thumbnails);

        return numExistingEncaps + encapsulationMap.size();
    }

    private Thumbnail createThumbnail(String collectionFolder, String filename)
    {
        Thumbnail thumbnail = new Thumbnail();
        thumbnail.setFilename(filename);
        thumbnail.setFormat(FilenameUtils.getExtension(filename));
        thumbnail.setFilesize(ObservationParser.calculateFileSizeKb(new File(collectionFolder, filename)));
        return thumbnail;
    }

    private void deleteUnusedThumbnails(String collectionFolder, List<String> unusedThumbnails)
    {
        for (String entry : unusedThumbnails)
        {
            File thumb = new File(collectionFolder, entry);
            thumb.delete();
        }
    }

    private List<String[]> getFileList(Path dataDirPath, List<String> thumbnails)
    {
        File dataDir = dataDirPath.toFile();
        if (!dataDir.exists())
        {
            return new ArrayList<>();
        }
        Path collectionPath = dataDirPath.getParent();

        IOFileFilter ignoreHiddenFilter = FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter("."));
        Collection<File> files = FileUtils.listFiles(dataDir, ignoreHiddenFilter, ignoreHiddenFilter);
        List<String> paths = new ArrayList<>();
        for (File file : files)
        {
            Path path = file.toPath();
            paths.add(collectionPath.relativize(path).toString());
        }
        Collections.sort(paths);

        final List<String> extensions = Arrays.asList(new String[] { "png", "jpg", "jpeg" });
        List<String[]> depositables = new ArrayList<>();
        for (File file : files)
        {
            String relPathNoExtension;
            Path path = file.toPath();
            String relativePath = collectionPath.relativize(path).toString();
            if (file.getName().endsWith(".fits"))
            {
                relPathNoExtension = relativePath.substring(0, relativePath.length() - (".fits".length()));
            }
            else if (file.getName().endsWith(".fits.gz"))
            {
                relPathNoExtension = relativePath.substring(0, relativePath.length() - (".fits.gz".length()));
            }
            else
            {
                String fileExt = FilenameUtils.getExtension(file.getName());
                if (extensions.contains(fileExt))
                {
                    thumbnails.add(relativePath);
                }
                continue;
            }

            String thumbnailPath = null;

            for (String imgSuffix : extensions)
            {
                String thumbnailName = relPathNoExtension + "." + imgSuffix;
                if (paths.contains(thumbnailName))
                {
                    thumbnailPath = thumbnailName;
                    break;
                }
            }
            depositables.add(new String[] { relativePath, thumbnailPath });
        }
        return depositables;
    }

    private void createChecksumIfMissing(String collectionFolder, String filename) throws CreateChecksumException
    {
        Path filePath = Paths.get(collectionFolder, filename);
        Path checksumPath = Paths.get(collectionFolder, filename + ".checksum");
        File checksumFile = checksumPath.toFile();

        if (checksumFile.exists())
        {
            // Already exists so no further work needed
            return;
        }

        try
        {
            String response = inlineScriptService.callScriptInline(calculateChecksumScript, filePath.toString());
            if (StringUtils.isNotBlank(response))
            {
                FileUtils.writeStringToFile(checksumFile, response, CharEncoding.UTF_8);
            }
            else
            {
                throw new CreateChecksumException("Script generated an empty checksum response for file: " + filePath);
            }
        }
        catch (IOException | InlineScriptException e)
        {
            throw new CreateChecksumException(e);
        }
    }

    /**
     * Create an EncapsulationFile to contain the supplied file, or return an existing one that is suitable.
     *  
     * @param filename The name of the file to be encapsulated
     * @param encapsulationMap The map of existing encapsulations - any new encapsulation will be added to it. 
     * @param level7Collection The collection the files are part of.
     * @param encapsType The type of encapsulation needed e.g. spectra, thumb; used in the encapsulation name. 
     * @param maxEncapsIndex The highest index of existing encapsulaitons for the collection.
     * @return A suitable EncapsulationFile
     */
    EncapsulationFile getEncapsulationFile(String filename, Map<String, EncapsulationFile> encapsulationMap,
            Level7Collection level7Collection, String encapsType, int maxEncapsIndex)
    {
        String folder = new File(filename).getParent();
        String extension = FilenameUtils.getExtension(filename);
        String key = encapsType + "|" + folder + "|" + extension;

        EncapsulationFile encapsulationFile = encapsulationMap.get(key);
        if (encapsulationFile == null)
        {
            String fileNum = String.valueOf(encapsulationMap.size() + 1 + maxEncapsIndex);
            encapsulationFile = new EncapsulationFile();
            encapsulationFile.setFilename("encaps-" + encapsType + "-" + fileNum + ".tar");
            encapsulationFile.setFilePattern(folder + "/*." + extension);
            encapsulationFile.setFormat("tar");

            level7Collection.addEncapsulationFile(encapsulationFile);
            encapsulationMap.put(key, encapsulationFile);
        }

        return encapsulationFile;
    }

    @Override
    public void jobCreationFailed(Job job, Throwable t)
    {
        this.jobFailed(job, t);
    }

    @Override
    public void jobCreationFailed(Job job, String cause)
    {
        this.jobFailed(job, cause);
    }

    @Override
    public void jobCreated(Job job)
    {
    }

    @Override
    public void jobStarted(Job job)
    {
    }

    @Override
    public void jobInterrupted(Job job, InterruptedException e)
    {
        this.jobFailed(job, e);
    }

    @Override
    public void jobSucceeded(Job job, String output)
    {
        this.failed = false;
    }

    @Override
    public void jobFailed(Job job, Throwable t)
    {
        String output = t instanceof CommandLineServiceException ? ((CommandLineServiceException) t).getMessage()
                : ExceptionUtils.getStackTrace(t);
        this.jobFailed(job, output);
    }

    @Override
    public void jobFailed(Job job, String cause)
    {
        this.failed = true;
        this.failureCause = cause;
    }
}
