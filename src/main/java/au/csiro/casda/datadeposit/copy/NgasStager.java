package au.csiro.casda.datadeposit.copy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

import au.csiro.casda.Utils;
import au.csiro.casda.jobmanager.JobManager;
import au.csiro.casda.jobmanager.JobManager.Job;
import au.csiro.casda.jobmanager.SimpleToolJobProcessBuilder;

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
 * Service that can be used to stage artefacts to the NGAS staging area (prior to registration with NGAS).
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class NgasStager implements JobManager.JobMonitor
{
    private String[] ngasStageCommand;
    private String ngasServerName;
    private String ngasStagingDirectory;
    private boolean failed;
    private String failureCause;

    /**
     * Constructor
     * 
     * @param ngasServerName
     *            the name of the NGAS server
     * @param ngasStagingDirectory
     *            the directory on the NGAS server used to stage files for a REGISTER command
     * @param ngasCopyCommand
     *            the external command (with any arguments) to run to copy an artefact into the NGAS staging area
     */
    @Autowired
    public NgasStager( @Value("${ngas.server.name}") String ngasServerName,
            @Value("${ngas.staging.directory}") String ngasStagingDirectory,
            @Value("${ngas.copy.command}") String ngasCopyCommand)
    {
        this.ngasStagingDirectory = ngasStagingDirectory;
        this.ngasStageCommand = Utils.elStringToArray(ngasCopyCommand);
        this.ngasServerName = ngasServerName;
    }

    /**
     * Copy an artefact to the NGAS staging area
     * 
     * @param parentId
     *            the sbid of the observation or level7 collection Id that the artefact belongs to
     * @param infile
     *            the full file path of the artefact
     * @param stagingVolume
     *            the volume of the staging area to copy the artefact to
     * @param fileId
     *            the identify of the artefact within the archive
     * @throws NoSuchFileException
     *             if the depositable artefact to copy could not be found
     * @throws StagingException
     *             if the depositable artefact could not be staged
     * @throws ChecksumMissingException
     *             if the depositable artefact's checksum file is missing
     */
    public void stageArtefactToNgas(Integer parentId, String infile, String stagingVolume, String fileId)
            throws NoSuchFileException, ChecksumMissingException, StagingException
    {
        Path artefactPath = Paths.get(infile);
        if (!artefactPath.toFile().exists())
        {
            throw new NoSuchFileException(String.format(
                    "Artefact with filename '%s' for observation %d not found at path: %s", infile, parentId,
                    artefactPath.toString()));
        }

        Path parentDir = artefactPath.getParent();
        Path artefactChecksumPath = parentDir.resolve(artefactPath.getFileName().toString() + ".checksum");
        if (!artefactChecksumPath.toFile().exists())
        {
            throw new ChecksumMissingException(parentId, infile, artefactChecksumPath);
        }
        
        // check if the artifact is an empty file
		try
        {
			long size = (Long) Files.getAttribute(artefactPath, "basic:size", java.nio.file.LinkOption.NOFOLLOW_LINKS);
			if (size == 0)
	        {
				throw new StagingException(parentId, infile, getNgasStagingPath(stagingVolume), fileId,
						String.format("Artefact with filename '%s' is empty.", infile));
	        }
		} 
		catch (IOException ioe) 
		{
			throw new StagingException(parentId, infile, getNgasStagingPath(stagingVolume), fileId, ioe.getMessage());
		}

        SimpleToolJobProcessBuilder stageProcessBuilder;
        stageProcessBuilder = new SimpleToolJobProcessBuilder(this.ngasStageCommand);
        stageProcessBuilder.setProcessParameter("ngas_server_name", this.ngasServerName);
        stageProcessBuilder.setProcessParameter("ngas_staging_directory", getNgasStagingPath(stagingVolume));
        stageProcessBuilder.setProcessParameter("parent-id", parentId.toString());
        stageProcessBuilder.setProcessParameter("artefact_filepath", infile);
        stageProcessBuilder.setProcessParameter("artefact_id", fileId);
        Job stageJob = stageProcessBuilder.createJob("staging-" + fileId, "staging");
        stageJob.run(this);
        if (this.failed)
        {
            throw new StagingException(parentId, infile, getNgasStagingPath(stagingVolume), fileId,
                    stageProcessBuilder.getJobFailureMessage(this.failureCause));
        }
    }

    private String getNgasStagingPath(String stagingVolume)
    {
        ST ngasStagingDirectoryTemplate = new ST(this.ngasStagingDirectory);
        ngasStagingDirectoryTemplate.add("volume", stagingVolume);
        String ngasStagingPath = ngasStagingDirectoryTemplate.render();
        return ngasStagingPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobCreationFailed(Job job, Throwable e)
    {
        this.jobFailed(job, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobCreationFailed(Job job, String cause)
    {
        this.jobFailed(job, cause);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobCreated(Job job)
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobStarted(Job job)
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobInterrupted(Job job, InterruptedException e)
    {
        this.jobFailed(job, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobSucceeded(Job job, String output)
    {
        this.failed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobFailed(Job job, Throwable e)
    {
        this.jobFailed(job, ExceptionUtils.getStackTrace(e));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void jobFailed(Job job, String cause)
    {
        this.failed = true;
        this.failureCause = cause;
    }
}
