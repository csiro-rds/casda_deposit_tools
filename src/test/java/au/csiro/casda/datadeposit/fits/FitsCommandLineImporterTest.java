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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.StandardErrorStreamLog;
import org.junit.contrib.java.lang.system.StandardOutputStreamLog;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.beust.jcommander.ParameterException;

import au.csiro.Log4JTestAppender;
import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.ParentType;
import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.fits.service.FitsImageService;
import au.csiro.casda.entity.observation.Cubelet;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.casda.entity.observation.MomentMap;
import au.csiro.casda.entity.observation.Project;
import au.csiro.casda.entity.observation.Spectrum;
import au.csiro.logging.CasdaDataDepositEvents;
import nom.tam.fits.FitsException;

/**
 * FitsCommandLineImporter unit tests. Test the program invocation, arguments and logging.
 * 
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestAppConfig.class })
@ActiveProfiles("local")
public class FitsCommandLineImporterTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    private FitsCommandLineImporter importer;
    @Mock
    private Level7CollectionRepository level7CollectionRepository;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final StandardOutputStreamLog out = new StandardOutputStreamLog();

    @Rule
    public final StandardErrorStreamLog err = new StandardErrorStreamLog();

    private static final String GOOD_FITS_FILE = "validFile.fits";

    private static final String GOOD_SPECTRUM_FILE = "beta.image2.001.sir.fits";

    private static final String BAD_FITS_FILE = "invalid.fits";

    private static final String NON_EXISTENT_FITS_FILE = "this.file.does.not.exist";

    /**
     * Before each test create a semi-mock version of the importer (using spy()). This allows us to dynamically stub the
     * methods on the importer (using when()).
     * 
     * @throws FileNotFoundException
     * @throws IOException
     * @throws FitsException
     */
    @Before
    public void setUp() throws FileNotFoundException, IOException, FitsException
    {
    	MockitoAnnotations.initMocks(this);
        testAppender = Log4JTestAppender.createAppender();
        importer = spy(new FitsCommandLineImporter(level7CollectionRepository));
    }

    /**
     * After each test clear all messages from the logger.
     */
    @After
    public void tearDown()
    {
        // TestUtils.setStaticField(Notifier.class, "logger", ORIGINAL_LOGGER);
    }

    @Test
    public void testIsUsageSupported()
    {
        exit.expectSystemExitWithStatus(0);
        StringBuilder builder = new StringBuilder();
        new FitsCommandLineArgumentsParser().usage(builder);
        importer.run("-help");
        assertEquals(builder.toString().trim(), out.getLog().trim());
    }

    @Test
    public void testHelpIsNotLogged()
    {
        exit.expectSystemExitWithStatus(0);
        try
        {
            importer.run("-help");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyNoMessages();
        }
    }

    /**
     * Tests the program arguments. infile and sbid are mandatory. imageCubeFilename is not mandatory and will default
     * to the infile if not specified
     */
    @Test
    public void testMissingArguments()
    {

        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run();
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E033.messageBuilder()
                    .add("NOT-SPECIFIED").toString(), ParameterException.class,
                    "The following options are required: -infile -fits-type -parent-type -fitsFilename -parent-id");
        }
    }

    /**
     * Tests the program arguments. infile is mandatory.
     */
    @Test
    public void testMissingInfileArgument()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-parent-id", "12345", "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "image-cube",
                    "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E033.messageBuilder()
                    .add("NOT-SPECIFIED").add("-sbid 12345 -fitsFilename " + GOOD_FITS_FILE).toString(),
                    ParameterException.class, "The following option is required: -infile");
        }
    }

    /**
     * Tests the program arguments. parent-id is mandatory.
     */
    @Test
    public void testMissingParentIdArgument()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", GOOD_FITS_FILE, "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "spectrum",
                    "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(
                    Level.ERROR,
                    CasdaDataDepositEvents.E033
                            .messageBuilder()
                            .add("NOT-SPECIFIED")
                            .add("-infile " + "src/test/resources/image/good/" + GOOD_FITS_FILE
                                    + " -fitsFilename " + GOOD_FITS_FILE).toString(), ParameterException.class,
                    "The following option is required: -parent-id");
        }
    }

    /**
     * Tests the program arguments. fitsFilename is mandatory.
     */
    @Test
    public void testMissingImageCubeFilenameArgument()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                    "-fits-type", "image-cube", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(
                    Level.ERROR,
                    CasdaDataDepositEvents.E033.messageBuilder().add("NOT-SPECIFIED")
                            .add("-infile " + "src/test/resources/image/good/" + GOOD_FITS_FILE + " sbid 12345")
                            .toString(), ParameterException.class,
                    "The following option is required: -fitsFilename");
        }
    }

    /**
     * Tests the program arguments. fits-type is a restricted list. The valid values are checked by other tests.
     */
    @Test
    public void testInvalidFitsTypeArgument()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", GOOD_FITS_FILE, "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "foo", "-parent-id",
                    "13", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(
                    Level.ERROR,
                    CasdaDataDepositEvents.E033
                            .messageBuilder()
                            .add("NOT-SPECIFIED")
                            .add("-infile " + "src/test/resources/image/good/" + GOOD_FITS_FILE
                                    + " -fitsFilename " + GOOD_FITS_FILE).toString(), ParameterException.class,
                    "Parameter fits-type must be either image-cube, moment-map, spectrum or cubelet");
        }
    }

    @Test
    public void testNoArgumentsPrintsUsageToStdErr()
    {
        exit.expectSystemExitWithStatus(1);
        StringBuilder builder = new StringBuilder();
        new FitsCommandLineArgumentsParser().usage(builder);

        try
        {
            importer.run();
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            assertEquals(builder.toString().trim(), err.getLog().trim());
        }
    }

    @Test
    public void testBadArgumentsisLoggedAsE033()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-foo", "foo", "-bar", "bar", "-blah", "blah");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E033.messageBuilder()
                    .add("NOT-SPECIFIED").add("-foo foo -bar bar -blah blah").toString(), ParameterException.class,
                    "Unknown option: -foo");
        }
    }

    @Test
    public void testBadArgumentsExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-foo", "foo", "-bar", "bar", "-blah", "blah");
    }

    @Test
    public void testBadArgumentsPrintsUsageToStdErr()
    {
        exit.expectSystemExitWithStatus(1);
        StringBuilder builder = new StringBuilder();
        new FitsCommandLineArgumentsParser().usage(builder);

        try
        {
            importer.run("-foo", "foo", "-bar", "bar", "-blah", "blah");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            assertEquals(builder.toString().trim(), err.getLog().trim());
        }
    }

    @Test
    public void testFileNotFoundExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE,
                "-fits-type", "image-cube");
    }

    @Test
    public void testFileNotFoundLoggedAsE066()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-fitsFilename",
                    NON_EXISTENT_FITS_FILE, "-fits-type", "image-cube", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E066.messageBuilder().add(NON_EXISTENT_FITS_FILE).toString(),
                    FileNotFoundException.class, NON_EXISTENT_FITS_FILE);
        }
    }

    @Test
    public void testSpectrumFileNotFoundExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE,
                "-fits-type", "spectrum");
    }

    @Test
    public void testSpectrumFileNotFoundLoggedAsE066()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-fitsFilename",
                    NON_EXISTENT_FITS_FILE, "-fits-type", "spectrum", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E066.messageBuilder().add(NON_EXISTENT_FITS_FILE).toString(),
                    FileNotFoundException.class, NON_EXISTENT_FITS_FILE);
        }
    }

    @Test
    public void testMomentMapFileNotFoundExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE,
                "-fits-type", "moment-map");
    }
    
    @Test
    public void testCubeletFileNotFoundExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE,
                "-fits-type", "cubelet");
    }

    @Test
    public void testMomentMapFileNotFoundLoggedAsE066()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-fitsFilename",
                    NON_EXISTENT_FITS_FILE, "-fits-type", "moment-map", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E066.messageBuilder().add(NON_EXISTENT_FITS_FILE).toString(),
                    FileNotFoundException.class, NON_EXISTENT_FITS_FILE);
        }
    }

    @Test
    public void testCubletFileNotFoundLoggedAsE066()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-fitsFilename",
                    NON_EXISTENT_FITS_FILE, "-fits-type", "cubelet", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E066.messageBuilder().add(NON_EXISTENT_FITS_FILE).toString(),
                    FileNotFoundException.class, NON_EXISTENT_FITS_FILE);
        }
    }

    @Test
    public void testFitsProjectCodeDoesNotMatchImageCubeProjectCodeExitsWithErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Project project = mock(Project.class);
        when(project.getOpalCode()).thenReturn("AS007");
        Exception exception = new FitsImageService.ProjectCodeMismatchException(project, "AS004");
        doThrow(exception).when(importer).processImageCubeFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyBoolean());
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                "-imageCubeFilename", GOOD_FITS_FILE);
    }
    
    @Test
    public void testFitsProjectCodeDoesNotMatchExceptionForLevel7() throws Exception
    {
    	Level7Collection parent = new Level7Collection();
    	parent.setErrors(new ArrayList<String>());
    	when(level7CollectionRepository.findByDapCollectionId(any(Long.class))).thenReturn(parent);
        exit.expectSystemExitWithStatus(1);
        Project project = mock(Project.class);
        when(project.getOpalCode()).thenReturn("AS007");
        Exception exception = new FitsImageService.ProjectCodeMismatchException(project, "VAST");
        doThrow(exception).when(importer).processImageCubeFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyBoolean());
        importer.run("-fits-type", "image-cube", "-fitsFilename", GOOD_FITS_FILE, "-infile", 
        		"src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345", "-parent-type", 
        		"derived-catalogue");
    }
    
    @Test
    public void testFitsFitsImportExceptionForLevel7() throws Exception
    {
    	Level7Collection parent = new Level7Collection();
    	parent.setErrors(new ArrayList<String>());
    	when(level7CollectionRepository.findByDapCollectionId(any(Long.class))).thenReturn(parent);
        exit.expectSystemExitWithStatus(1);
        Project project = mock(Project.class);
        when(project.getOpalCode()).thenReturn("AS007");
        Exception cause = new Exception("'STEVE' is not a valid header");
        Exception exception = new FitsImageService.FitsImportException(cause);
        doThrow(exception).when(importer).processImageCubeFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyBoolean());
        importer.run("-fits-type", "image-cube", "-fitsFilename", GOOD_FITS_FILE, "-infile", 
        		"src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345", "-parent-type", 
        		"derived-catalogue");
    }


    @Test
    public void testFitsProjectCodeDoesNotMatchImageCubeProjectCodeLoggedAsE108() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Project project = mock(Project.class);
        when(project.getOpalCode()).thenReturn("AS007");
        Exception exception = new FitsImageService.ProjectCodeMismatchException(project, "AS004");
        doThrow(exception).when(importer).processImageCubeFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyBoolean());

        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                    "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "image-cube", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E108.messageBuilder().add("src/test/resources/image/good/" + GOOD_FITS_FILE)
                            .toString(), exception);
        }
    }

    @Test
    public void testMetadataStoreExceptionExitsWithErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new FitsImageService.RepositoryException("An unknown error occurred");
        doThrow(exception).when(importer).processImageCubeFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyBoolean());
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                "-fitsFilename", GOOD_FITS_FILE);
    }

    @Test
    public void testMetadataStoreExceptionLoggedAsE067() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new FitsImageService.RepositoryException("An unknown error occurred");
        doThrow(exception).when(importer).processImageCubeFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyBoolean());

        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                    "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "image-cube", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E067.messageBuilder().add("src/test/resources/image/good/" + GOOD_FITS_FILE)
                            .toString(), exception);
        }
    }

    @Test
    public void testFitsExceptionExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", "src/test/resources/image/good/" + BAD_FITS_FILE, "-parent-id", "12345",
                "-imageCubeFilename", BAD_FITS_FILE);
    }

    @Test
    public void testFitsExceptionLoggedAsE066() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new FitsImageService.FitsImportException("An unknown error occurred");
        exception.initCause(new IOException("this is an error"));
        doThrow(exception).when(importer).processImageCubeFile(anyObject(), anyObject(), anyObject(), anyObject(),
                anyBoolean());
   
        Level7Collection collection = mock(Level7Collection.class);
        List<String> errors = mock(ArrayList.class);
   
        doReturn(collection).when(level7CollectionRepository).findByDapCollectionId(anyInt());
        doReturn(errors).when(collection).getErrors();
        doReturn(null).when(level7CollectionRepository).save(any(Level7Collection.class));

        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + BAD_FITS_FILE, "-parent-id", "12345",
                    "-fitsFilename", BAD_FITS_FILE, "-fits-type", "image-cube", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E066.messageBuilder().add("src/test/resources/image/good/" + BAD_FITS_FILE)
                            .toString(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessIsLoggedAsE065() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        ImageCube imageCube = new ImageCube();
        imageCube.setProject(new Project("AS014"));
        doReturn(imageCube).when(importer).processImageCubeFile(any(String.class), any(Integer.class),
                any(String.class), any(ParentType.class), anyBoolean());
        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                    "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "image-cube", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            String logMessage =
                    CasdaDataDepositEvents.E065.messageBuilder().add("src/test/resources/image/good/" + GOOD_FITS_FILE)
                            .add(12345).add("AS014").toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: RTC\\].*"), matchesPattern(".*\\[destination: CASDA_DB\\].*"),
                    matchesPattern(".*\\[volumeKB: \\d+\\].*"),
                    matchesPattern(".*\\[fileId: observations-12345-image_cubes-" + GOOD_FITS_FILE + "\\].*")),
                    sameInstance((Throwable) null));
        }
    }

    @Test
    public void testSuccessExitsWithNoErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        ImageCube imageCube = new ImageCube();
        imageCube.setProject(new Project("AS014"));
        doReturn(imageCube).when(importer).processImageCubeFile(any(String.class), any(Integer.class),
                any(String.class), any(ParentType.class), anyBoolean());
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "image-cube", "-parent-type", "observation");
    }

    @Test
    public void testRefreshSuccessExitsWithNoErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        ImageCube imageCube = new ImageCube();
        imageCube.setProject(new Project("AS014"));
        doReturn(imageCube).when(importer).processImageCubeFile(any(String.class), any(Integer.class),
                any(String.class), any(ParentType.class), anyBoolean());
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-parent-id", "12345",
                "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "image-cube", "-parent-type", "observation", "-refresh");
    }

    @Test
    public void testSpectrumSuccessExitsWithNoErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        Spectrum spectrum = new Spectrum();
        spectrum.setProject(new Project("AS031"));
        doReturn(spectrum).when(importer).processSpectrumFiles(any(String.class), any(Integer.class), any(String.class),
                any(ParentType.class), anyBoolean());
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_SPECTRUM_FILE, "-parent-id", "12345",
                "-fitsFilename", GOOD_FITS_FILE, "-fits-type", "spectrum", "-parent-type", "observation");
    }

    @Test
    public void testMomentMapSuccessExitsWithNoErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        MomentMap momentMap = new MomentMap();
        momentMap.setProject(new Project("AS031"));
        doReturn(momentMap).when(importer).processMomentMapFiles(any(String.class), any(Integer.class),
                any(String.class), any(ParentType.class), anyBoolean());
        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_SPECTRUM_FILE, "-parent-id", "12345",
                    "-fitsFilename", GOOD_SPECTRUM_FILE, "-fits-type", "moment-map", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            String logMessage =
                    CasdaDataDepositEvents.E065.messageBuilder().add("src/test/resources/image/good/" + GOOD_SPECTRUM_FILE)
                            .add(12345).add("AS031").toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: RTC\\].*"), matchesPattern(".*\\[destination: CASDA_DB\\].*"),
                    matchesPattern(".*\\[volumeKB: \\d+\\].*"),
                    matchesPattern(".*\\[fileId: observations-12345-image_cubes-" + GOOD_SPECTRUM_FILE + "\\].*")),
                    sameInstance((Throwable) null));
        }
    }
    
    @Test
    public void testCubletSuccessExitsWithNoErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        Cubelet cubelet = new Cubelet();
        cubelet.setProject(new Project("AS031"));
        doReturn(cubelet).when(importer).processCubeletFiles(any(String.class), any(Integer.class),
                any(String.class), any(ParentType.class), anyBoolean());
        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_SPECTRUM_FILE, "-parent-id", "12345",
                    "-fitsFilename", GOOD_SPECTRUM_FILE, "-fits-type", "cubelet", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            String logMessage =
                    CasdaDataDepositEvents.E065.messageBuilder().add("src/test/resources/image/good/" + GOOD_SPECTRUM_FILE)
                            .add(12345).add("AS031").toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: RTC\\].*"), matchesPattern(".*\\[destination: CASDA_DB\\].*"),
                    matchesPattern(".*\\[volumeKB: \\d+\\].*"),
                    matchesPattern(".*\\[fileId: observations-12345-image_cubes-" + GOOD_SPECTRUM_FILE + "\\].*")),
                    sameInstance((Throwable) null));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.casda.datadeposit.AbstractCommandLineImporterTest#createCommmandLineImporter()
     */
    @Override
    protected ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter()
    {
        return new FitsCommandLineImporter(level7CollectionRepository);
    }

}
