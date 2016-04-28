package au.csiro.casda.datadeposit.fits.assembler;

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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import au.csiro.casda.Utils;
import au.csiro.casda.datadeposit.fits.AskapFitsKey;
import au.csiro.casda.datadeposit.fits.FitsFileParser;
import au.csiro.casda.datadeposit.fits.StokesPolarisationMapping;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.FitsImportException;
import au.csiro.casda.datadeposit.fits.service.FitsImageService.ProjectCodeMismatchException;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.jobmanager.ProcessJob;
import au.csiro.casda.jobmanager.SimpleToolJobProcessBuilder;
import au.csiro.casda.jobmanager.SingleJobMonitor;
import au.csiro.util.AstroConversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Component used to populate an ImageCube's fields from certain header values in a FITS file.
 * <p>
 * Copyright 2013, CSIRO Australia All rights reserved.
 */
@Component
public class FitsImageAssembler
{
    private static final String STOKES_PARAMETERS_DELIMITER = "/";

    private static final Logger logger = LoggerFactory.getLogger(FitsImageAssembler.class);

    private static final int NUM_CORNERS_PER_IMAGE = 4;

    private String imageGeometryCommandAndArgs;

    private FitsFileParser fitsFileParser;

    /**
     * Constructor
     * 
     * @param imageGeometryCommandAndArgs
     *            an EL-string containing the command and arguments used to find the image geometry
     * @param fitsFileParser
     *            the FitsFileParser used to obtain non-geometric headers
     */
    @Autowired
    public FitsImageAssembler(@Value("${image.geometry.command.and.args}") String imageGeometryCommandAndArgs,
            FitsFileParser fitsFileParser)
    {
        super();
        this.imageGeometryCommandAndArgs = imageGeometryCommandAndArgs;
        this.fitsFileParser = fitsFileParser;
    }

    /**
     * Populates an Image Cube with details from a FITS file's headers.
     * 
     * @param imageCube
     *            the image cube entity that we wish to populate
     * @param fitsFile
     *            a fits File that will be parsed to populate certain fields of the imageCube
     * @throws ProjectCodeMismatchException
     *             if the fitsFile's project code header doesn't match the imageCube's project code
     * @throws FitsImportException
     *             if there was a problem processing the fitsFile
     * @throws FileNotFoundException
     *             if the fitsFile could not be found
     */
    public void populateImageCube(ImageCube imageCube, File fitsFile)
            throws FitsImportException, ProjectCodeMismatchException, FileNotFoundException
    {

        logger.debug("Populating ImageCube entity");

        Map<AskapFitsKey, Object> headerValues = getFitsHeaders(fitsFile);

        // populate the new values
        String projectName = (String) headerValues.get(AskapFitsKey.PROJECT);
        String imageCubeProjectName = imageCube.getProject().getOpalCode();
        if (!StringUtils.equals(imageCubeProjectName, projectName))
        {
            throw new ProjectCodeMismatchException(imageCube.getProject(), projectName);
        }

        populateImageCubeGeometry(imageCube, fitsFile);

        imageCube.setTargetName((String) headerValues.get(AskapFitsKey.OBJECT));

        imageCube.setSResolution((Double) headerValues.get(AskapFitsKey.BMAJ));
        imageCube.setSResolutionMin((Double) headerValues.get(AskapFitsKey.BMIN));
        imageCube.setSResolutionMax((Double) headerValues.get(AskapFitsKey.BMAJ));

        imageCube.setTMin((Double) headerValues.get(AskapFitsKey.TMIN)); // TODO check keyword
        imageCube.setTMax((Double) headerValues.get(AskapFitsKey.TMAX)); // TODO check keyword
        imageCube.setTExptime((Double) headerValues.get(AskapFitsKey.INTIME)); // TODO check keyword
    }

    private Map<AskapFitsKey, Object> getFitsHeaders(File fitsFile) throws FitsImportException, FileNotFoundException
    {
        try
        {
            // read the FITS headers
            fitsFileParser.setFitsFile(fitsFile);
            Map<AskapFitsKey, Object> headerValues = fitsFileParser.getHeaderValues();
            return headerValues;
        }
        catch (FileNotFoundException ex)
        {
            throw ex;
        }
        catch (Exception ex) // because there are other exceptions not wrapped in FitsException
        {
            throw new FitsImportException(ex);
        }
    }

    private void populateImageCubeGeometry(ImageCube imageCube, File fitsFile) throws FitsImportException
    {
        SimpleToolJobProcessBuilder builder =
                new SimpleToolJobProcessBuilder(Utils.elStringToArray(this.imageGeometryCommandAndArgs));

        builder.setProcessParameter("infile", fitsFile.toString());

        ProcessJob job = builder.createJob("jobId", "type");
        SingleJobMonitor monitor = new SingleJobMonitor();
        job.run(monitor);
        if (monitor.isJobFailed())
        {
            throw new FitsImportException(String.format(
                    "Could not determine geometry of image cube using command %s Output from command was: %s",
                    StringUtils.join(builder.getCommandAndArgs(), " "), monitor.getJobOutput()));
        }
        JsonNode geometryDetails;
        try
        {
            geometryDetails = new ObjectMapper().readTree(monitor.getJobOutput());
        }
        catch (IOException e)
        {
            throw new FitsImportException(String.format(
                    "Could not determine geometry of image cube using command %s Could not parse output from "
                            + "command (%s) into a JSON map.",
                    StringUtils.join(builder.getCommandAndArgs(), " "), monitor.getJobOutput()));
        }

        Double sFov = null;
        Long numPixels = null;
        Double cellSize = null;
        try
        {
            JsonNode axes = geometryDetails.get("axes");
            int i = 0;
            JsonNode axis = axes.get(i);
            while (axis != null)
            {
                long numPixelsInAxis = Long.parseLong(axis.get("numPixels").asText());
                if (numPixels == null)
                {
                    numPixels = numPixelsInAxis;
                }
                else
                {
                    numPixels *= numPixelsInAxis;
                }
                if ("RA".equals(axis.get("name").asText()) || "DEC".equals(axis.get("name").asText()))
                {
                    double cellSizeInAxis = Double.parseDouble(axis.get("pixelSize").asText());
                    if (cellSize == null)
                    {
                        cellSize = cellSizeInAxis;
                    }
                    else
                    {
                        cellSize *= cellSizeInAxis;
                    }
                    /*
                     * Note (for code review): this calculation is fundamentally different to the original
                     */
                    if (sFov == null)
                    {
                        sFov = numPixelsInAxis * cellSizeInAxis;
                    }
                    else
                    {
                        sFov *= (numPixelsInAxis * cellSizeInAxis);
                    }
                }
                if ("FREQ".equals(axis.get("name").asText()))
                {
                    /*
                     * min freq will be max wavelength and vice versa
                     */
                    imageCube.setEmMin(
                            AstroConversion.frequencyToWavelength(Double.parseDouble(axis.get("max").asText())));
                    imageCube.setEmMax(
                            AstroConversion.frequencyToWavelength(Double.parseDouble(axis.get("min").asText())));
                    imageCube.setChannelWidth(Double.parseDouble(axis.get("pixelSize").asText()));
                    imageCube.setEmResPower(new BigDecimal(axis.get("centre").asText())
                            .divide(new BigDecimal(axis.get("pixelSize").asText()), MathContext.DECIMAL128)
                            .doubleValue());
                    imageCube.setCentreFrequency(Double.parseDouble(axis.get("centre").asText()));
                    imageCube.setNoOfChannels(Integer.parseInt(axis.get("numPixels").asText()));
                }
                if ("STOKES".equals(axis.get("name").asText()))
                {
                    /*
                     * Polarisation information is captured in our image cubes as an extra dimension (the STOKES axis).
                     * There may be up to four stokes polarisation 'planes' in the image cube, corresponding to the
                     * Stokes parameters I, Q, U, V. In the FITS file the number of polarisation planes is indicated by
                     * the number of pixels on the Stokes axis. The stokes parameter value (I, Q, U, or V) for each
                     * plane will be determined by the min and max values on the axis and the pixel size. Due to the
                     * pixelised nature of FITS images the min and max values will correspond to pixel 0.5 and 0.5 +
                     * numPixels. The pixel size must be an integer and must be either 1, 2, or 3. Consequently the min
                     * and max values will range between 0.5 and 4.5 (and must end in 0.5).
                     * 
                     * Note: we only need the 'startPixel' value (the minimum) and the 'step' to calculate all the
                     * pixels.
                     */
                    int min;
                    int step;
                    try
                    {
                        min = new BigDecimal(axis.get("min").asText()).add(new BigDecimal("0.5")).intValueExact();
                    }
                    catch (ArithmeticException ex)
                    {
                        throw new FitsImportException("STOKES axis has invalid min value: " + axis.get("min").asText(),
                                ex);
                    }
                    try
                    {
                        step = new BigDecimal(axis.get("pixelSize").asText()).intValueExact();
                    }
                    catch (ArithmeticException ex)
                    {
                        throw new FitsImportException(
                                "STOKES axis has invalid pixelSize: " + axis.get("pixelSize").asText(), ex);
                    }
                    if (StokesPolarisationMapping.getFromFitsValue(min) == StokesPolarisationMapping.UNDEFINED)
                    {
                        throw new FitsImportException("STOKES axis has invalid min value: " + axis.get("min").asText()
                                + " (does not map to any valid value)");
                    }
                    int max = (int) (min + step * (numPixelsInAxis - 1));
                    if (StokesPolarisationMapping.getFromFitsValue(max) == StokesPolarisationMapping.UNDEFINED)
                    {
                        throw new FitsImportException("STOKES axis has invalid min (" + axis.get("min").asText()
                                + "), pixelSize (" + axis.get("pixelSize").asText() + "), and numPixels ("
                                + numPixelsInAxis + ") values");
                    }
                    List<String> stokesList = new ArrayList<>();
                    for (int pixelIndex = 0; pixelIndex < numPixelsInAxis; pixelIndex++)
                    {
                        StokesPolarisationMapping stokesValue =
                                StokesPolarisationMapping.getFromFitsValue(min + step * pixelIndex);
                        if (stokesValue == StokesPolarisationMapping.UNDEFINED)
                        {
                            /*
                             * Should never occur due to checks above
                             */
                            throw new FitsImportException("STOKES axis has invalid value for pixel " + (pixelIndex + 1)
                                    + " (" + (min + step * pixelIndex) + ")");
                        }
                        stokesList.add(stokesValue.getStokesValue());
                    }
                    imageCube.setStokesParameters(STOKES_PARAMETERS_DELIMITER
                            + StringUtils.join(stokesList, STOKES_PARAMETERS_DELIMITER) + STOKES_PARAMETERS_DELIMITER);
                }
                i += 1;
                axis = axes.get(i);
            }
            if (imageCube.getStokesParameters() == null)
            {
                imageCube.setStokesParameters(STOKES_PARAMETERS_DELIMITER + STOKES_PARAMETERS_DELIMITER);
            }
            imageCube.setSFov(sFov == null ? null : Math.abs(sFov));
            imageCube.setNoOfPixels(numPixels);
            imageCube.setCellSize(cellSize == null ? null : Math.abs(cellSize));

            geometryDetails.get("corners");

            List<Coordinate> coordinates = new ArrayList<>();
            JsonNode corners = geometryDetails.get("corners");
            i = 0;
            JsonNode corner = corners.get(i);
            while (corner != null)
            {
                coordinates.add(new Coordinate(Double.parseDouble(corner.get("RA").asText()),
                        Double.parseDouble(corner.get("DEC").asText())));
                i++;
                corner = corners.get(i);
            }
            if (coordinates.size() != NUM_CORNERS_PER_IMAGE)
            {
                throw new FitsImportException(
                        "Expected " + NUM_CORNERS_PER_IMAGE + " corners but got " + coordinates.size());
            }
            // add the first coordinate to the end, to close the polygon
            Coordinate firstCoord = coordinates.get(0);
            coordinates.add(new Coordinate(firstCoord.x, firstCoord.y));

            Geometry geometry =
                    (new GeometryFactory()).createPolygon(coordinates.toArray(new Coordinate[coordinates.size()]));
            imageCube.setSRegion(geometry);

            // Centre
            JsonNode centre = geometryDetails.get("centre");
            imageCube.setRaDeg(Double.parseDouble(centre.get("RA").asText()));
            imageCube.setDecDeg(Double.parseDouble(centre.get("DEC").asText()));
            imageCube.setDimensions(geometryDetails.toString());
        }
        catch (FitsImportException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new FitsImportException(String.format(
                    "Could not determine geometry of image cube using command %s Output from "
                            + "command (%s) was a JSON object but is missing expected elements or otherwise invalid.",
                    StringUtils.join(builder.getCommandAndArgs(), " "), monitor.getJobOutput()), ex);
        }
    }
}
