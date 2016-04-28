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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;

import nom.tam.fits.FitsException;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import au.csiro.Log4JTestAppender;
import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.fits.service.FitsImageService;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Project;
import au.csiro.logging.CasdaDataDepositEvents;

import com.beust.jcommander.ParameterException;

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

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final StandardOutputStreamLog out = new StandardOutputStreamLog();

    @Rule
    public final StandardErrorStreamLog err = new StandardErrorStreamLog();

    private static final String GOOD_FITS_FILE = "validFile.fits";

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
        testAppender = Log4JTestAppender.createAppender();
        importer = spy(new FitsCommandLineImporter());
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
                    "The following options are required: -infile -imageCubeFilename -sbid");
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
            importer.run("-sbid", "12345", "-imageCubeFilename", GOOD_FITS_FILE);
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E033.messageBuilder()
                    .add("NOT-SPECIFIED").add("-sbid 12345 -imageCubeFilename " + GOOD_FITS_FILE).toString(),
                    ParameterException.class, "The following option is required: -infile");
        }
    }

    /**
     * Tests the program arguments. sbid is mandatory.
     */
    @Test
    public void testMissingSbidArgument()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", GOOD_FITS_FILE, "-imageCubeFilename", GOOD_FITS_FILE);
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
                                    + " -imageCubeFilename " + GOOD_FITS_FILE).toString(), ParameterException.class,
                    "The following option is required: -sbid");
        }
    }

    /**
     * Tests the program arguments. imageCubeFilename is NOT mandatory.
     */
    @Test
    public void testMissingImageCubeFilenameArgument()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-sbid", "12345");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(
                    Level.ERROR,
                    CasdaDataDepositEvents.E033.messageBuilder().add("NOT-SPECIFIED")
                            .add("-infile " + "src/test/resources/image/good/" + GOOD_FITS_FILE + " sbid 12345")
                            .toString(), ParameterException.class,
                    "The following option is required: -imageCubeFilename");
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
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-sbid", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE);
    }

    @Test
    public void testFileNotFoundLoggedAsE066()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", NON_EXISTENT_FITS_FILE, "-sbid", "12345", "-imageCubeFilename",
                    NON_EXISTENT_FITS_FILE);
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
        doThrow(exception).when(importer).processFitsFile(anyObject(), anyObject(), anyObject());
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-sbid", "12345",
                "-imageCubeFilename", GOOD_FITS_FILE);
    }

    @Test
    public void testFitsProjectCodeDoesNotMatchImageCubeProjectCodeLoggedAsE108() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Project project = mock(Project.class);
        when(project.getOpalCode()).thenReturn("AS007");
        Exception exception = new FitsImageService.ProjectCodeMismatchException(project, "AS004");
        doThrow(exception).when(importer).processFitsFile(anyObject(), anyObject(), anyObject());

        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-sbid", "12345",
                    "-imageCubeFilename", GOOD_FITS_FILE);
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
        doThrow(exception).when(importer).processFitsFile(anyObject(), anyObject(), anyObject());
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-sbid", "12345",
                "-imageCubeFilename", GOOD_FITS_FILE);
    }

    @Test
    public void testMetadataStoreExceptionLoggedAsE067() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new FitsImageService.RepositoryException("An unknown error occurred");
        doThrow(exception).when(importer).processFitsFile(anyObject(), anyObject(), anyObject());

        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-sbid", "12345",
                    "-imageCubeFilename", GOOD_FITS_FILE);
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
        importer.run("-infile", "src/test/resources/image/good/" + BAD_FITS_FILE, "-sbid", "12345",
                "-imageCubeFilename", BAD_FITS_FILE);
    }

    @Test
    public void testFitsExceptionLoggedAsE066() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new FitsImageService.FitsImportException("An unknown error occurred");
        doThrow(exception).when(importer).processFitsFile(anyObject(), anyObject(), anyObject());

        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + BAD_FITS_FILE, "-sbid", "12345",
                    "-imageCubeFilename", BAD_FITS_FILE);
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
        doReturn(imageCube).when(importer).processFitsFile(any(String.class), any(Integer.class), any(String.class));
        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-sbid", "12345",
                    "-imageCubeFilename", GOOD_FITS_FILE);
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
        doReturn(imageCube).when(importer).processFitsFile(any(String.class), any(Integer.class), any(String.class));
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_FITS_FILE, "-sbid", "12345",
                "-imageCubeFilename", GOOD_FITS_FILE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.casda.datadeposit.AbstractCommandLineImporterTest#createCommmandLineImporter()
     */
    @Override
    protected ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter()
    {
        return new FitsCommandLineImporter();
    }

}
