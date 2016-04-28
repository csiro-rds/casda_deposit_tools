package au.csiro.casda.datadeposit.copy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

import au.csiro.casda.datadeposit.service.NgasService;
import au.csiro.casda.datadeposit.service.NgasService.ServiceCallException;
import au.csiro.casda.datadeposit.service.NgasService.Status;

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
 * Service that can be used to register artefacts in the NGAS staging area with NGAS.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class NgasRegistrar
{
    private String ngasStagingDirectory;
    private NgasService ngasService;

    /**
     * Constructor
     * 
     * @param ngasService
     *            the NgasService instance required by parent classes
     * @param ngasStagingDirectory
     *            the directory on the NGAS server used to stage files for a REGISTER command
     */
    @Autowired
    public NgasRegistrar(NgasService ngasService, @Value("${ngas.staging.directory}") String ngasStagingDirectory)
    {
        this.ngasService = ngasService;
        this.ngasStagingDirectory = ngasStagingDirectory;
    }

    /**
     * Register an artefact in the NGAS staging area with NGAS
     * 
     * @param parentId
     *            the parentId of the observation that the artefact belongs to
     * @param infile
     *            the full file path of the artefact
     * @param stagingVolume
     *            the volume that the artefact was staged to
     * @param fileId
     *            the identify of the artefact within the archive
     * @throws NoSuchFileException
     *             if the depositable artefact's checksum file could not be found
     * @throws RegisterException
     *             if the depositable artefact could not be registered
     * @throws ChecksumVerificationException
     *             if the depositable artefacts checksum in NGAS does not match the value in the artefact's checksum
     *             file
     */
    public void registerArtefactWithNgas(Integer parentId, String infile, String stagingVolume, String fileId)
            throws NoSuchFileException, RegisterException, ChecksumVerificationException
    {

        Path filePath = Paths.get(infile);
        Path parentDir = filePath.getParent();
        String filename = filePath.getFileName().toString();

        Path artefactChecksumPath = parentDir.resolve(filename + ".checksum");
        if (!artefactChecksumPath.toFile().exists())
        {
            throw new NoSuchFileException(String.format(
                    "Checksum could not be compared for artefact with filename '%s' for observation %d because "
                            + "checksum file %s could not be found.", infile, parentId,
                    artefactChecksumPath.toString()));
        }

        try
        {
            long size = (Long) Files.getAttribute(filePath, "basic:size", java.nio.file.LinkOption.NOFOLLOW_LINKS);
            if (size == 0)
            {
                throw new RegisterException(parentId, infile, getNgasStagingPath(stagingVolume), fileId,
                        String.format("Artefact with filename '%s' is empty.", infile));
            }
        }
        catch (IOException ioe)
        {
            throw new RegisterException(parentId, infile, getNgasStagingPath(stagingVolume), fileId, ioe);
        }

        NgasService.Status status;
        try
        {
            status = ngasService.registerFile(getNgasStagingPath(stagingVolume), fileId);
        }
        catch (ServiceCallException e)
        {
            throw new RegisterException(parentId, infile, getNgasStagingPath(stagingVolume), fileId, e);
        }
        if (!status.wasSuccess())
        {
            throw new RegisterException(parentId, infile, getNgasStagingPath(stagingVolume), fileId,
                    "unknown. Status: " + status.toString());
        }

        verifyChecksums(parentId, infile, artefactChecksumPath, fileId);
    }

    private void verifyChecksums(Integer sbid, String infile, Path depositChecksumFilePath, String fileId)
            throws ChecksumVerificationFailedException, UnexpectedChecksumVerificationException
    {
        String fileChecksum = null;
        try (FileInputStream inputStream = new FileInputStream(depositChecksumFilePath.toFile()))
        {
            fileChecksum = IOUtils.toString(inputStream);
            // strips the end of line characters and leading/trailing whitespace from the file CASDA-4296
            fileChecksum = StringUtils.stripToEmpty(fileChecksum);
        }
        catch (IOException e)
        {
            throw new UnexpectedChecksumVerificationException(sbid, infile, e);
        }
        String ngasChecksum;
        try
        {
            Status status = ngasService.getStatus(fileId);
            if (!status.wasSuccess())
            {
                throw new UnexpectedChecksumVerificationException(sbid, infile, "Unexpected NGAS status: " + status);
            }
            ngasChecksum = status.getChecksum();
        }
        catch (ServiceCallException e)
        {
            throw new UnexpectedChecksumVerificationException(sbid, infile, e);
        }
        if (!ngasChecksum.equals(fileChecksum))
        {
            throw new ChecksumVerificationFailedException(sbid, infile, fileChecksum, ngasChecksum);
        }
    }

    private String getNgasStagingPath(String stagingVolume)
    {
        ST ngasStagingDirectoryTemplate = new ST(this.ngasStagingDirectory);
        ngasStagingDirectoryTemplate.add("volume", stagingVolume);
        String ngasStagingPath = ngasStagingDirectoryTemplate.render();
        return ngasStagingPath;
    }

}
