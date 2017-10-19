package au.csiro.casda.datadeposit.encapsulation;

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
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import au.csiro.casda.Utils;
import au.csiro.casda.datadeposit.DepositStateImpl;
import au.csiro.casda.datadeposit.ParentType;
import au.csiro.casda.datadeposit.DepositState.Type;
import au.csiro.casda.datadeposit.exception.CreateChecksumException;
import au.csiro.casda.datadeposit.observation.jpa.repository.EncapsulationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.service.InlineScriptException;
import au.csiro.casda.datadeposit.service.InlineScriptService;
import au.csiro.casda.entity.observation.EncapsulationFile;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.casda.jobmanager.ProcessJob;
import au.csiro.casda.jobmanager.ProcessJobBuilder.ProcessJobFactory;
import au.csiro.casda.jobmanager.SimpleToolProcessJobBuilder;
import au.csiro.casda.jobmanager.SingleJobMonitor;

/**
 * Provides methods to encapsulate a set of small files.
 * <p>
 * Copyright 2016, CSIRO Australia. All rights reserved.
 */
@Component
public class EncapsulationService
{

    /**
     * Exception that represents a problem when creating an encapsulation.
     */
    public static class EncapsulationException extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         * 
         * @param message
         *            the exception detail
         */
        public EncapsulationException(String message)
        {
            super(message);
        }

        /**
         * Constructor
         * 
         * @param cause
         *            the cause of the Exception
         */
        public EncapsulationException(Throwable cause)
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
        public EncapsulationException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(EncapsulationService.class);

    private InlineScriptService inlineScriptService;

    private ProcessJobFactory processJobFactory;

    private String encapsulationCommandAndArgs;

    private String encapsulationVerifyCommandAndArgs;

    private String calculateChecksumScript;

    private EncapsulationFileRepository encapsulationFileRepository;
    
    private EvaluationFileRepository evaluationFileRepository;

    /**
     * Constructor for a EncapsulationService that creates encapsulations.
     * 
     * @param encapsulationCommandAndArgs
     *            An EL-string containing the command and initial arguments used to create an encapsulation. The names
     *            of the files will be appended to this command string.
     * @param encapsulationVerifyCommandAndArgs
     *            An EL-string containing the command and arguments used to verify an encapsulation.
     * @param calculateChecksumScript
     *            The command to calculate a checksum.
     * @param processJobFactory
     *            the factory to be used to create job processes.
     * @param inlineScriptService
     *            the service to run simple scripts such as checksums
     * @param encapsulationFileRepository
     *            The repository for encapsulation files.
     * @param evaluationFileRepository
     *            The repository for evaluation files.
     */
    @Autowired
    public EncapsulationService(@Value("${encapsulation.create.command.and.args}") String encapsulationCommandAndArgs,
            @Value("${encapsulation.verify.command.and.args}") String encapsulationVerifyCommandAndArgs,
            @Value("${calculate.checksum.script}") String calculateChecksumScript, ProcessJobFactory processJobFactory,
            InlineScriptService inlineScriptService, EncapsulationFileRepository encapsulationFileRepository, 
            EvaluationFileRepository evaluationFileRepository)
    {
        super();
        this.encapsulationCommandAndArgs = encapsulationCommandAndArgs;
        this.encapsulationVerifyCommandAndArgs = encapsulationVerifyCommandAndArgs;
        this.calculateChecksumScript = calculateChecksumScript;
        this.processJobFactory = processJobFactory;
        this.inlineScriptService = inlineScriptService;
        this.encapsulationFileRepository = encapsulationFileRepository;
        this.evaluationFileRepository = evaluationFileRepository;
    }

    /**
     * Verify the checksums of the files matching the pattern.
     * 
     * @param inFile
     *            The full path to the encapsulation file.
     * @param encapsulationFilename
     *            The filename of the encapsulation file
     * @param pattern
     *            The pattern of files to be encapsulated.
     * @param sbid
     * 			  The primary sbid for the observation
     * @param eval 
     * 			  boolean denoting if this encapsulation is for evaluation files		  
     * @throws EncapsulationException
     *             If the checksum does not match or cannot be checked.
     */
    public void verifyChecksums(String inFile, String encapsulationFilename, String pattern, int sbid, boolean eval) 
    		throws EncapsulationException
    {
    	Collection<File> files = getMatchingFiles(inFile, encapsulationFilename, pattern, sbid, false, eval);
        for (File file : files)
        {
            String filename = file.getPath();
            try
            {
                filename = file.getCanonicalPath();
                verifyChecksumForFile(filename);
            }
            catch (InlineScriptException | IOException e)
            {
                throw new EncapsulationException("Checksum could not be checked for " + filename, e);
            }
        }

    }

    /**
     * Build a new archive file encapsulating the file matched by the pattern.
     * 
     * @param sbid
     *            The primary scheduling block id.
     * @param encapsulationFilename
     *            The name of the file to be created
     * @param inFile
     *            The full path to the file to be created
     * @param pattern
     *            The pattern of files to be encapsulated.
     * @param eval 
     * 			  boolean denoting if this encapsulation is for evaluation files
     * @return The number of files 
     * @throws EncapsulationException
     *             If the encapsulation could not be created.
     * @throws CreateChecksumException
     *             If a checksum could not be calculated or saved for the new archive file.
     */
    @Transactional(rollbackOn = Exception.class)
    public int createEncapsulation(int sbid, String encapsulationFilename, String inFile, String pattern, boolean eval)
            throws EncapsulationException, CreateChecksumException
    {
        List<String> argList = new ArrayList<>(Utils.elStringToList(this.encapsulationCommandAndArgs));

        Collection<File> files = getMatchingFiles(inFile, encapsulationFilename, pattern, sbid, true, eval);
        for (File file : files)
        {
            argList.add(file.getPath());
            argList.add(file.getPath() + ".checksum");
        }

        String obsFolder = new File(inFile).getParent();
        SimpleToolProcessJobBuilder builder =
                new SimpleToolProcessJobBuilder(processJobFactory, argList.toArray(new String[0]));

        builder.setProcessParameter("encapsfile", inFile);
        builder.setProcessParameter("folder", obsFolder);

        ProcessJob job = builder.createJob("jobId", "type");
        SingleJobMonitor monitor = new SingleJobMonitor();
        job.run(monitor);
        if (monitor.isJobFailed())
        {
            throw new EncapsulationException(
                    String.format("Could not create encapsulation using command %s Output from command was: %s",
                            StringUtils.join(builder.getCommandAndArgs(), " "), monitor.getJobOutput()));
        }

        createChecksumFile(new File(inFile));

        return files.size();
    }

    /**
     * Updates an EncapsulationFile (identified by its filename and Observation's SBID) with the file size
     * 
     * @param parentId
     *            the scheduling block id of the observation
     * @param encapsFilename
     *            the name of the file
     * @param inFile
     *            the encapsulation file
     * @param parentType 
     *            the type of parent object for this image cube
     * @throws EncapsulationException
     *             if no matching EncapsulationFile could be found or the record could not be updated
     * @return the populated and saved EncapsulationFile
     * @throws EncapsulationException 
     */
    @Transactional(rollbackOn = Exception.class)
    public EncapsulationFile updateEncapsulationFileWithMetadata(Integer parentId, String encapsFilename, File inFile,
            ParentType parentType) throws EncapsulationException
    {
        logger.debug("FitsImageServiceImpl is about to store the metadata");

        try
        {
            EncapsulationFile encapsFile =
                    encapsulationFileRepository.findByObservationSbidAndFilename(parentId, encapsFilename);
            if (parentType == ParentType.LEVEL_7_COLLECTION)
            {
                encapsFile = encapsulationFileRepository.findByLevel7CollectionDapCollectionIdAndFilename(parentId,
                        encapsFilename);
            }
            else
            {
                encapsFile =
                        encapsulationFileRepository.findByObservationSbidAndFilename(parentId, encapsFilename);
            }
            if (encapsFile == null)
            {
                throw new EncapsulationException(
                        String.format("Unable to find encpsulation file object for scheduling block %s and name %s.",
                                parentId, encapsFilename));
            }

            // Record the file size
            long filesizeInBytes = FileUtils.sizeOf(inFile);
            long fileSizeInKBytes = (long) Math.ceil(((double) filesizeInBytes) / FileUtils.ONE_KB);
            encapsFile.setFilesize(fileSizeInKBytes);

            // Advance the deposit status of the encapsulation file here to avoid concurrent mod errors 
            encapsFile.setDepositState(new DepositStateImpl(Type.PROCESSED, encapsFile));

            return encapsulationFileRepository.save(encapsFile);
        }
        catch (DataAccessException dataAccessException)
        {
            throw new EncapsulationException(dataAccessException);
        }
    }

    private Collection<File> getMatchingFiles(String inFile, String encapsulationFilename, String pattern, 
    		int parentId, boolean relative, boolean eval)
    {
        File baseFolder = new File(inFile).getParentFile();
        String filePattern = pattern;
        if (pattern != null && pattern.contains("/"))
        {
            String subfolder = pattern.substring(0, pattern.lastIndexOf("/"));
            if (subfolder.length() > 0)
            {
                baseFolder = new File(baseFolder, subfolder);
            }
            filePattern = pattern.substring(pattern.lastIndexOf("/")+1);
        }
        
	    Collection<File> files;
        if(eval)
        {
        	files = new ArrayList<File>();
            List<EvaluationFile> evals = evaluationFileRepository
                    .findByObservationSbidAndEncapsulationFileFilename(parentId, encapsulationFilename);
        	for(EvaluationFile file : evals)
        	{
        		files.add(new File(baseFolder + "/" +file.getFilename()));
        	}
        }
        else
        {
        	files =
                    FileUtils.listFiles(baseFolder, new WildcardFileFilter(filePattern), null);
        }
        
        if (relative)
        {
            Path basePath = new File(inFile).getParentFile().toPath();
            List<File> relativeFiles = new ArrayList<>();
            for (File file : files)
            {
                Path relativePath = basePath.relativize(file.toPath());
                relativeFiles.add(relativePath.toFile());
            }
            files = relativeFiles;
        }
        return files;
    }

    /**
     * Verify that the encapsulation has been correctly created. This will check the archive checksum and that it
     * contains the right files.
     * 
     * @param sbid
     *            The primary scheduling block id.
     * @param inFile
     *            The full path to the file to be created
     * @param pattern
     *            The pattern of files that should be in the encapsulation.
     * @throws EncapsulationException
     *             If the encapsulation could not be verified.
     */
    public void verifyEncapsulation(int sbid, String inFile, String pattern)
            throws EncapsulationException
    {
        String filename = inFile;
        try
        {
            // First check the checksum
            filename = new File(inFile).getCanonicalPath();
            verifyChecksumForFile(filename);

            // Now check the contents
            List<String> argList = new ArrayList<>(Utils.elStringToList(this.encapsulationVerifyCommandAndArgs));
            SimpleToolProcessJobBuilder builder =
                    new SimpleToolProcessJobBuilder(processJobFactory, argList.toArray(new String[0]));
            builder.setProcessParameter("encapsfile", inFile);

            ProcessJob job = builder.createJob("jobId", "type");
            SingleJobMonitor monitor = new SingleJobMonitor();
            job.run(monitor);
            if (monitor.isJobFailed())
            {
                throw new EncapsulationException(
                        String.format("Encapsulation %s was not valid. Output from command was: %s",
                                inFile, monitor.getJobOutput()));
            }

        }
        catch (InlineScriptException | IOException e)
        {
            throw new EncapsulationException(String.format("Could not verify encapsulation %s", filename), e);
        }

    }

    private void verifyChecksumForFile(String encapsulationFilename)
            throws InlineScriptException, IOException, EncapsulationException
    {
        String response = StringUtils
                .trimToEmpty(inlineScriptService.callScriptInline(calculateChecksumScript, encapsulationFilename));
        String expectedChecksum = StringUtils.trimToEmpty(
                FileUtils.readFileToString(new File(encapsulationFilename + ".checksum"), CharEncoding.UTF_8));
        if (!(response.equals(expectedChecksum)))
        {
            throw new EncapsulationException(String.format("Checksum was invalid for %s. Excepted %s but got %s.",
                    encapsulationFilename, expectedChecksum, response));
        }
    }

    /**
     * Creates a checksum file for a given file. The destination will be file.checksum
     * 
     * @param file
     *            the file to calculate the checksum for
     * @throws CreateChecksumException
     *             if there is a problem creating the checksum file
     */
    protected void createChecksumFile(File file) throws CreateChecksumException
    {
        logger.debug("Creating checksum file for: {} exists: {}", file, file.exists());
        try
        {
            String response = inlineScriptService.callScriptInline(calculateChecksumScript, file.getCanonicalPath());
            if (StringUtils.isNotBlank(response))
            {
                FileUtils.writeStringToFile(new File(file.getCanonicalPath() + ".checksum"), response,
                        CharEncoding.UTF_8);
            }
            else
            {
                throw new CreateChecksumException(
                        "Script generated an empty checksum response for file: " + file.getCanonicalPath());
            }
        }
        catch (IOException | InlineScriptException e)
        {
            throw new CreateChecksumException(e);
        }
    }
}
