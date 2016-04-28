package au.csiro.casda.datadeposit.catalogue.continuum;

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

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Level;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.StandardErrorStreamLog;
import org.junit.contrib.java.lang.system.StandardOutputStreamLog;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import au.csiro.DynamicInstantiationAwareBeanPostProcessor;
import au.csiro.Log4JTestAppender;
import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.casda.datadeposit.catalogue.CatalogueCommandLineImporter;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.DatabaseException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.MalformedFileException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.ValidationModeSignal;
import au.csiro.casda.datadeposit.catalogue.CatalogueType;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.logging.CasdaDataDepositEvents;

import com.beust.jcommander.ParameterException;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

/**
 * CommandLineImporter unit tests
 * <p>
 * Please note that this class uses a HierarchicalContextRunner to partition the test cases into a hierarchy. See the
 * README.md in src/test/java
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@EnableJpaRepositories(basePackageClasses = { ObservationRepository.class })
@RunWith(HierarchicalContextRunner.class)
@ActiveProfiles("local")
public class CatalogueCommandLineImporterTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    @Autowired
    private DynamicInstantiationAwareBeanPostProcessor beanPostProcessor;

    @Autowired
    private ConfigurableApplicationContext context;

    private CatalogueCommandLineImporter importer;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    /*
     * TODO: look at rewriting the StandardOutputStreamLog and error one so they completely absorb the output (they use
     * TeeOutputStream to split).
     */
    @Rule
    public final StandardOutputStreamLog out = new StandardOutputStreamLog();

    @Rule
    public final StandardErrorStreamLog err = new StandardErrorStreamLog();

    @Before
    public void setUp() throws Exception
    {
        testAppender = Log4JTestAppender.createAppender();

        // Manual equivalent to @RunWith(SpringJUnit4ClassRunner.class)
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        context.getBeanFactory().addBeanPostProcessor(beanPostProcessor);
    }

    @After
    public void tearDown()
    {
        beanPostProcessor.reset();
    }

    public class CommandLineArguments
    {

        @Before
        public void setUp()
        {
            exit.expectSystemExit();
            importer = context.getBeanFactory().createBean(CatalogueCommandLineImporter.class);
        }

        public class Usage
        {
            @Test
            public void isSupported()
            {
                StringBuilder builder = new StringBuilder();
                importer.writeUsage(builder);

                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-help");
                }, () -> {
                    assertThat(out.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

            @Test
            public void isNotLogged()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-help");
                }, () -> {
                    testAppender.verifyNoMessages();
                });
            }

            @Test
            public void exitsWithSuccess()
            {
                exit.expectSystemExitWithStatus(0);
                importer.run("-help");
            }

        }

        public class CatalogueTypeArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String infile = RandomStringUtils.randomAlphabetic(20);
            private String[] args = new String[] { "-catalogue-filename", infile, "-infile", infile, "-parent-id",
                    parentId.toString() };

            @Test
            public void isLoggedAsE055()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E055.messageBuilder().add(parentId.toString())
                                    .add(StringUtils.join(args, " ")).toString(), ParameterException.class,
                            "The following option is required: -catalogue-type");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class ParentIdArgumentMissing
        {
            private String infile = RandomStringUtils.randomAlphabetic(20);
            private String[] args = new String[] { "-catalogue-type", "continuum-component", "-catalogue-filename",
                    infile, "-infile", infile };

            @Test
            public void isLoggedAsE055()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E055.messageBuilder().add("NOT-SPECIFIED")
                                    .add(StringUtils.join(args, " ")).toString(), ParameterException.class,
                            "The following option is required: -parent-id");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class CatalogueFilenameArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String infile = RandomStringUtils.randomAlphabetic(20);

            @Test
            public void isLoggedAsE055()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-catalogue-type", "continuum_component", "-parent-id", parentId.toString(),
                            "-infile", infile);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E055
                                    .messageBuilder()
                                    .add(parentId.toString())
                                    .add("-catalogue-type " + "continuum_component" + " -parent-id "
                                            + parentId.toString() + " -infile " + infile).toString(),
                            ParameterException.class, "The following option is required: -catalogue-filename");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run("-parent-id", parentId.toString(), "-infile", infile);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-parent-id", parentId.toString(), "-infile", infile);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class InfileArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String infile = RandomStringUtils.randomAlphabetic(20);
            private String[] args = new String[] { "-catalogue-type", "continuum-component", "-parent-id",
                    parentId.toString(), "-catalogue-filename", infile };

            @Test
            public void isLoggedAsE055()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E055.messageBuilder().add(parentId.toString())
                                    .add(StringUtils.join(args, " ")).toString(), ParameterException.class,
                            "The following option is required: -infile");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class UnknownArguments
        {
            @Test
            public void isLoggedAsE055()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-foo", "bar", "-ping");
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E055.messageBuilder().add("NOT-SPECIFIED").add("-foo bar -ping")
                                    .toString(), ParameterException.class, "Unknown option: -foo");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run("-foo", "bar", "-ping");
            }

            @Test
            public void printsUsageToStdErr()
            {
                StringBuilder builder = new StringBuilder();
                importer.writeUsage(builder);

                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-foo", "bar", "-ping");
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

    }

    @Test
    public void catalogueIsAskedToLoadFile() throws FileNotFoundException, MalformedFileException, DatabaseException,
            ValidationModeSignal
    {
        exit.expectSystemExit();

        CatalogueParser parser = mock(CatalogueParser.class);
        doReturn(mock(Catalogue.class)).when(parser).parseFile(Mockito.anyInt(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any());
        beanPostProcessor.overrideBean(CatalogueParser.class, parser);
        importer = createCatalogueCommandLineImporterWithParser(parser);

        String fileName = "atlas-components.xml";
        Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
        runAndCheckConditionsIgnoringExit(
                () -> {
                    importer.run("-catalogue-type", "continuum-component", "-parent-id", parentId.toString(),
                            "-catalogue-filename", fileName, "-infile", "src/test/resources/catalogue/good/" + fileName);
                },
                () -> {
                    try
                    {
                        verify(parser).parseFile(parentId, fileName, "src/test/resources/catalogue/good/" + fileName,
                                CatalogueParser.Mode.NORMAL);
                    }
                    catch (Exception e)
                    {
                        fail(e.toString());
                    }
                });
    }

    public class ThrownParserException
    {
        private Integer parentId;
        private String fileName;
        private CatalogueParser parser;
        private Exception exception;
        private String[] args;

        @Before
        public void setUp() throws Exception
        {
            exit.expectSystemExit();

            parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            fileName = RandomStringUtils.randomAlphabetic(20);
            parser = mock(CatalogueParser.class);
            beanPostProcessor.overrideBean(ContinuumComponentCatalogueParser.class, parser);
            importer = createCatalogueCommandLineImporterWithParser(parser);
        }

        public class FileNotFound
        {
            @Before
            public void setUp() throws Exception
            {
                exception = new FileNotFoundException(fileName + " not found");
                doThrow(exception).when(parser).parseFile(anyInt(), anyString(), anyString(), any());
                args =
                        new String[] { "-catalogue-type", "continuum-component", "-parent-id", parentId.toString(),
                                "-catalogue-filename", fileName, "-infile", fileName };
            }

            @Test
            public void isLoggedAsE056()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E056.messageBuilder().add("continuum component").add(fileName)
                                    .add("-infile " + fileName).toString(), exception);
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run(args);
            }
        }

        public class Database
        {
            @Before
            public void setUp() throws Exception
            {
                exception = new CatalogueParser.DatabaseException(RandomStringUtils.randomAlphabetic(100));
                doThrow(exception).when(parser).parseFile(anyInt(), anyString(), anyString(), any());
                args =
                        new String[] { "-catalogue-type", "continuum-component", "-parent-id", parentId.toString(),
                                "-catalogue-filename", fileName, "-infile", fileName };
            }

            @Test
            public void isLoggedAsE058()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E058.messageBuilder().add("continuum component").add(fileName)
                                    .add("-infile " + fileName).toString(), exception);
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run(args);
            }
        }

        public class MalformedFile
        {
            @Before
            public void setUp() throws Exception
            {
                int lengthOfRandomString = 100; // Help force a newline so Checkstyle doesn't complain
                exception =
                        new CatalogueParser.MalformedFileException(
                                RandomStringUtils.randomAlphabetic(lengthOfRandomString));
                doThrow(exception).when(parser).parseFile(anyInt(), anyString(), anyString(), any());
                args =
                        new String[] { "-catalogue-type", "continuum-component", "-parent-id", parentId.toString(),
                                "-catalogue-filename", fileName, "-infile", fileName };
            }

            @Test
            public void isLoggedAsE057()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E057.messageBuilder().add("continuum component").add(fileName)
                                    .add("-infile " + fileName).toString(), exception);
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run(args);
            }
        }

    }

    public class Success
    {

        private Integer parentId;
        private String fileName;
        private CatalogueParser parser;
        private String[] args;

        @Before
        public void setUp() throws Exception
        {
            exit.expectSystemExit();

            parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            fileName = "atlas-components.xml";
            parser = mock(CatalogueParser.class);
            Catalogue catalogue = mock(Catalogue.class);
            doReturn(catalogue).when(parser).parseFile(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(),
                    Mockito.any());
            doReturn("observations-" + parentId + "-catalogues-" + fileName).when(catalogue).getFileId();
            beanPostProcessor.overrideBean(ContinuumComponentCatalogueParser.class, parser);
            importer = createCatalogueCommandLineImporterWithParser(parser);
            args =
                    new String[] { "-catalogue-type", "continuum-component", "-parent-id", parentId.toString(),
                            "-catalogue-filename", fileName, "-infile", "src/test/resources/catalogue/good/" + fileName };
        }

        @SuppressWarnings("unchecked")
        @Test
        public void isLoggedAsE059()
        {
            runAndCheckConditionsIgnoringExit(
                    () -> {
                        importer.run(args);
                    },
                    () -> {
                        String logMessage =
                                CasdaDataDepositEvents.E059.messageBuilder().add("continuum component")
                                        .add("src/test/resources/catalogue/good/" + fileName).toString();
                        testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                                matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                                matchesPattern(".*\\[source: RTC\\].*"),
                                matchesPattern(".*\\[destination: CASDA_DB\\].*"),
                                matchesPattern(".*\\[volumeKB: \\d+\\].*"), matchesPattern(".*\\[fileId: observations-"
                                        + parentId + "-catalogues-" + fileName + "\\].*")),
                                sameInstance((Throwable) null));
                    });
        }

        @Test
        public void exitsWithNoErrorCode()
        {
            exit.expectSystemExitWithStatus(0);
            importer.run(args);
        }
    }

    public class ValidationMode
    {
        private Integer parentId;
        private String fileName;
        private CatalogueParser parser;
        private String[] args;
        private CatalogueParser.ValidationModeSignal validationSignal;

        @Before
        public void setUp() throws Exception
        {
            exit.expectSystemExit();

            parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            fileName = RandomStringUtils.randomAlphabetic(20);
            parser = mock(CatalogueParser.class);
            beanPostProcessor.overrideBean(ContinuumComponentCatalogueParser.class, parser);
            importer = createCatalogueCommandLineImporterWithParser(parser);
            args =
                    new String[] { "-catalogue-type", "continuum-component", "-parent-id", parentId.toString(),
                            "-catalogue-filename", fileName, "-infile", fileName, "-validate-only" };
        }

        public class SuccessfulParse
        {
            @Before
            public void setUp() throws Exception
            {
                validationSignal = new CatalogueParser.ValidationModeSignal();
                doThrow(validationSignal).when(parser).parseFile(anyInt(), anyString(), anyString(),
                        eq(CatalogueParser.Mode.VALIDATE_ONLY));
            }

            @Test
            public void isNotLogged()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyNoMessages();
                });
            }

            @Test
            public void exitsWithNoErrorCode()
            {
                exit.expectSystemExitWithStatus(0);
                importer.run(args);
            }
        }

        public class FailedWithException
        {
            private PrintStream systemOut;
            private final ByteArrayOutputStream out = new ByteArrayOutputStream();

            @Before
            public void setUp() throws Exception
            {
                systemOut = System.out;
                System.setOut(new PrintStream(out, true, CharEncoding.UTF_8));
            }

            @After
            public void cleanUpStreams()
            {
                System.out.close();
                System.setOut(systemOut);
            }

            public class FailedParseWithDatabaseException
            {
                @Before
                public void setUp() throws Exception
                {
                    validationSignal = new CatalogueParser.ValidationModeSignal();
                    validationSignal.addFailureMessage("Weee!");
                    doThrow(validationSignal).when(parser).parseFile(anyInt(), anyString(), anyString(),
                            eq(CatalogueParser.Mode.VALIDATE_ONLY));
                }

                @Test
                public void isNotLogged()
                {
                    runAndCheckConditionsIgnoringExit(() -> {
                        importer.run(args);
                    }, () -> {
                        testAppender.verifyNoMessages();
                    });
                }

                @Test
                public void printsToSystemOut()
                {
                    runAndCheckConditionsIgnoringExit(() -> {
                        importer.run(args);
                    }, () -> {
                        String[] lines = out.toString().split(System.lineSeparator());
                        assertThat(Arrays.asList(lines), Matchers.contains("Weee!"));
                    });
                }

                @Test
                public void exitsWithNoErrorCode()
                {
                    exit.expectSystemExitWithStatus(0);
                    importer.run(args);
                }
            }

            public class FailedParseWithMalformedFileException
            {
                @Before
                public void setUp() throws Exception
                {
                    validationSignal = new CatalogueParser.ValidationModeSignal();
                    validationSignal.addFailureMessage("Oops!");
                    doThrow(validationSignal).when(parser).parseFile(anyInt(), anyString(), anyString(),
                            eq(CatalogueParser.Mode.VALIDATE_ONLY));
                }

                @Test
                public void isNotLogged()
                {
                    runAndCheckConditionsIgnoringExit(() -> {
                        importer.run(args);
                    }, () -> {
                        testAppender.verifyNoMessages();
                    });
                }

                @Test
                public void printsToSystemOut()
                {
                    runAndCheckConditionsIgnoringExit(() -> {
                        importer.run(args);
                    }, () -> {
                        String[] lines = out.toString().split(System.lineSeparator());
                        assertThat(Arrays.asList(lines), Matchers.contains("Oops!"));
                    });
                }

                @Test
                public void exitsWithNoErrorCode()
                {
                    exit.expectSystemExitWithStatus(0);
                    importer.run(args);
                }
            }

            public class FailedParseWithFileNotFoundException
            {
                @Before
                public void setUp() throws Exception
                {
                    validationSignal = new CatalogueParser.ValidationModeSignal();
                    validationSignal.addFailureMessage("Missing!");
                    doThrow(validationSignal).when(parser).parseFile(anyInt(), anyString(), anyString(),
                            eq(CatalogueParser.Mode.VALIDATE_ONLY));
                }

                @Test
                public void isNotLogged()
                {
                    runAndCheckConditionsIgnoringExit(() -> {
                        importer.run(args);
                    }, () -> {
                        testAppender.verifyNoMessages();
                    });
                }

                @Test
                public void printsToSystemOut()
                {
                    runAndCheckConditionsIgnoringExit(() -> {
                        importer.run(args);
                    }, () -> {
                        String[] lines = out.toString().split(System.lineSeparator());
                        assertThat(Arrays.asList(lines), Matchers.contains("Missing!"));
                    });
                }

                @Test
                public void exitsWithNoErrorCode()
                {
                    exit.expectSystemExitWithStatus(0);
                    importer.run(args);
                }
            }
        }
    }

    /**
     * Implementation of {@link AbstractArgumentsDrivenCommandLineToolTest#createCommmandLineImporter()}
     */
    @Override
    public CatalogueCommandLineImporter createCommmandLineImporter()
    {
        return new CatalogueCommandLineImporter();
    }

    private CatalogueCommandLineImporter createCatalogueCommandLineImporterWithParser(CatalogueParser parser)
    {
        return new CatalogueCommandLineImporter()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            protected CatalogueParser createParserForCatalogueType(CatalogueType catalogueType)
            {
                return parser;
            }
        };
    }

}
