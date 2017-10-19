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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
import au.csiro.casda.datadeposit.fits.FitsCommandLineImporter;
import au.csiro.logging.CasdaDataDepositEvents;

/**
 * EncapsulationCommandLineImporter unit tests. Test the program invocation, arguments and logging.
 * 
 * Copyright 2016, CSIRO Australia All rights reserved.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestAppConfig.class })
@ActiveProfiles("local")
public class EncapsulationCommandLineImporterTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    private EncapsulationCommandLineImporter importer;

    @Mock
    private EncapsulationService service;
    @Mock
    private Level7CollectionRepository level7CollectionRepository;
    
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final StandardOutputStreamLog out = new StandardOutputStreamLog();

    @Rule
    public final StandardErrorStreamLog err = new StandardErrorStreamLog();

    private static final String GOOD_FITS_FILE = "validFile.fits";

    private static final String GOOD_ENCAPS_FILE = "validFile.tar";

    private static final String GOOD_SPECTRUM_FILE = "beta.image2.001.sir.fits";

    private static final String BAD_FITS_FILE = "invalid.fits";

    private static final String NON_EXISTENT_FITS_FILE = "this.file.does.not.exist";

    /**
     * Before each test create a semi-mock version of the importer (using spy()). This allows us to dynamically stub the
     * methods on the importer (using when()).
     * 
     */
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        testAppender = Log4JTestAppender.createAppender();
        importer = spy(new EncapsulationCommandLineImporter(service));
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
        new EncapsulationCommandLineArgumentsParser().usage(builder);
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
     * Tests the program arguments. infile, parent-id, parent-type & encapsFilename are mandatory. while pattern & eval
     * are optional
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
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E033.messageBuilder().add("NOT-SPECIFIED").toString(),
                    ParameterException.class,
                    "The following options are required: -infile -parent-id -encapsFilename");
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
            importer.run("-parent-id", "12345", "-encapsFilename", GOOD_ENCAPS_FILE, "-pattern", "spec_*.png",
                    "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E033.messageBuilder().add("NOT-SPECIFIED")
                            .add("-parent-id 12345 -fitsFilename " + GOOD_ENCAPS_FILE).toString(),
                    ParameterException.class, "The following option is required: -infile");
        }
    }

    /**
     * Tests the program arguments. parent-id is mandatory.
     */
    @Test
    public void testMissingParentidArgument()
    {
        exit.expectSystemExitWithStatus(1);
        try
        {
            importer.run("-infile", GOOD_ENCAPS_FILE, "-encapsFilename", GOOD_ENCAPS_FILE, "-pattern", "spec_*.fits",
                    "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E033.messageBuilder().add("NOT-SPECIFIED")
                            .add("-infile " + "src/test/resources/image/good/" + GOOD_ENCAPS_FILE + " -encapsFilename "
                                    + GOOD_FITS_FILE)
                            .toString(),
                    ParameterException.class, "The following option is required: -parent-id");
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
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_ENCAPS_FILE, "-parent-id", "12345", "-pattern",
                    "spec_*.fits", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender
                    .verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E033.messageBuilder().add("NOT-SPECIFIED")
                                    .add("-infile " + "src/test/resources/image/good/" + GOOD_ENCAPS_FILE
                                            + " sbid 12345")
                                    .toString(),
                            ParameterException.class, "The following option is required: -encapsFilename");
        }
    }

    @Test
    public void testNoArgumentsPrintsUsageToStdErr()
    {
        exit.expectSystemExitWithStatus(1);
        StringBuilder builder = new StringBuilder();
        new EncapsulationCommandLineArgumentsParser().usage(builder);

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
            testAppender.verifyLogMessage(Level.ERROR,
                    CasdaDataDepositEvents.E033.messageBuilder().add("NOT-SPECIFIED")
                            .add("-foo foo -bar bar -blah blah").toString(),
                    ParameterException.class, "Unknown option: -foo");
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
        new EncapsulationCommandLineArgumentsParser().usage(builder);

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
                "-fits-type", "image-cube", "-parent-type", "observation");
    }

    @Test
    public void testSpectrumFileNotFoundExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE,
                "-fits-type", "spectrum", "-parent-type", "observation");
    }

    @Test
    public void testMomentMapFileNotFoundExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE,
                "-fits-type", "moment-map", "-parent-type", "observation");
    }
    
    @Test
    public void testCubeletFileNotFoundExitsWithErrorCode()
    {
        exit.expectSystemExitWithStatus(1);
        importer.run("-infile", NON_EXISTENT_FITS_FILE, "-parent-id", "12345", "-imageCubeFilename", NON_EXISTENT_FITS_FILE,
                "-fits-type", "cubelet", "-parent-type", "observation");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessIsLoggedAsE065() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        when(service.createEncapsulation(any(Integer.class), any(String.class), any(String.class),
                any(String.class), any(Boolean.class))).thenReturn(5);
        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_ENCAPS_FILE, "-parent-id", "12345",
                    "-encapsFilename", GOOD_ENCAPS_FILE, "-pattern", "spec*.fits", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            String logMessage = CasdaDataDepositEvents.E154.messageBuilder().add(5).add("spec*.fits")
                    .add("src/test/resources/image/good/" + GOOD_ENCAPS_FILE).add(12345).toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: RTC\\].*"), matchesPattern(".*\\[destination: RTC\\].*"),
                    matchesPattern(".*\\[volumeKB: \\d+\\].*"),
                    matchesPattern(
                            ".*\\[fileId: observations-12345-encapsulation_files-" + GOOD_ENCAPS_FILE + "\\].*")),
                    sameInstance((Throwable) null));
        }
    }

    @Test
    public void testSuccessExitsWithNoErrorCode() throws Exception
    {
        exit.expectSystemExitWithStatus(0);
        doReturn(5).when(importer).produceEncapsulation(any(Integer.class), any(String.class), any(String.class),
                any(String.class), any(ParentType.class), any(Boolean.class));
        importer.run("-infile", "src/test/resources/image/good/" + GOOD_ENCAPS_FILE, "-parent-id", "12345",
                "-encapsFilename", GOOD_ENCAPS_FILE, "-pattern", "spec-*.fits", "-parent-type", "observation");
    }

    @Test
    public void testEncapsulationExceptionLoggedAsE155() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new EncapsulationService.EncapsulationException("An unknown error occurred");
        doThrow(exception).when(service).verifyChecksums(any(String.class), any(String.class), any(String.class), 
        		any(Integer.class), any(Boolean.class));

        try
        {
            importer.run("-infile", "src/test/resources/image/good/" + GOOD_ENCAPS_FILE, "-parent-id", "12345",
                    "-encapsFilename", GOOD_ENCAPS_FILE, "-pattern", "mom0_*.fits", "-parent-type", "observation");
            fail("Should never get here!");
        }
        catch (CheckExitCalled e)
        {
            testAppender
                    .verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E155.messageBuilder()
                                    .add("src/test/resources/image/good/" + GOOD_ENCAPS_FILE).add(12345).toString(),
                            exception);
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
