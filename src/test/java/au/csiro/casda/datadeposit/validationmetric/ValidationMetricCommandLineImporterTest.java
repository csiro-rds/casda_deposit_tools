package au.csiro.casda.datadeposit.validationmetric;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
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

import com.beust.jcommander.ParameterException;

import au.csiro.DynamicInstantiationAwareBeanPostProcessor;
import au.csiro.Log4JTestAppender;
import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.DatabaseException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.MalformedFileException;
import au.csiro.casda.datadeposit.catalogue.CatalogueParser.ValidationModeSignal;
import au.csiro.casda.datadeposit.catalogue.continuum.ContinuumComponentCatalogueParser;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.entity.observation.EvaluationFile;
import au.csiro.logging.CasdaDataDepositEvents;
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
public class ValidationMetricCommandLineImporterTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    @Autowired
    private DynamicInstantiationAwareBeanPostProcessor beanPostProcessor;

    @Autowired
    private ConfigurableApplicationContext context;

    private ValidationMetricCommandLineImporter importer;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();
    
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
            importer = context.getBeanFactory().createBean(ValidationMetricCommandLineImporter.class);
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

        public class ParentIdArgumentMissing
        {
            private String infile = RandomStringUtils.randomAlphabetic(20);
            private String[] args = new String[] { "-filename", infile, "-infile", infile };

            @Test
            public void isLoggedAsE186()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E186.messageBuilder().add("NOT-SPECIFIED")
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

        public class FilenameArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String infile = RandomStringUtils.randomAlphabetic(20);

            @Test
            public void isLoggedAsE186()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-parent-id", parentId.toString(),"-infile", infile);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E186
                                    .messageBuilder()
                                    .add(parentId.toString())
                                    .add("-parent-id " + parentId.toString() + " -infile " + infile).toString(),
                            ParameterException.class, "The following option is required: -filename");
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
            private String[] args = new String[] { "-parent-id", parentId.toString(), "-filename", infile };

            @Test
            public void isLoggedAsE186()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E186.messageBuilder().add(parentId.toString())
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
            public void isLoggedAsE186()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-foo", "bar", "-ping");
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E186.messageBuilder().add("NOT-SPECIFIED").add("-foo bar -ping")
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
    public void evaluationFileIsAskedToLoadFile() throws FileNotFoundException, MalformedFileException, DatabaseException,
            ValidationModeSignal
    {
        exit.expectSystemExit();

        ValidationMetricParser parser = mock(ValidationMetricParser.class);
        doReturn(mock(EvaluationFile.class)).when(parser).parseFile(Mockito.anyInt(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any());
        beanPostProcessor.overrideBean(ValidationMetricParser.class, parser);
        importer = createCommmandLineImporterWithParser(parser);

        String fileName = "validationMetric.valid.xml";
        Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
        runAndCheckConditionsIgnoringExit(
                () -> {
                    importer.run("-parent-id", parentId.toString(),
                            "-filename", fileName, "-infile", "src/test/resources/validationmetric/good/" + fileName);
                },
                () -> {
                    try
                    {
                        verify(parser).parseFile(parentId, fileName, "src/test/resources/validationmetric/good/" 
                        		+ fileName, CatalogueParser.Mode.NORMAL);
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
        private ValidationMetricParser parser;
        private Exception exception;
        private String[] args;

        @Before
        public void setUp() throws Exception
        {
            exit.expectSystemExit();

            parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            fileName = RandomStringUtils.randomAlphabetic(20);
            parser = mock(ValidationMetricParser.class);
            beanPostProcessor.overrideBean(ContinuumComponentCatalogueParser.class, parser);
            importer = createCommmandLineImporterWithParser(parser);
        }

        public class FileNotFound
        {
            @Before
            public void setUp() throws Exception
            {
                exception = new FileNotFoundException(fileName + " not found");
                doThrow(exception).when(parser).parseFile(anyInt(), anyString(), anyString(), any());
                args =
                        new String[] {"-parent-id", parentId.toString(), "-filename", fileName, "-infile", fileName };
            }

            @Test
            public void isLoggedAsE187()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E187.messageBuilder().add(fileName)
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
                args = new String[] {"-parent-id", parentId.toString(), "-filename", fileName, "-infile", fileName };
            }

            @Test
            public void isLoggedAsE189()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E189.messageBuilder().add(fileName)
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
                args = new String[] { "-parent-id", parentId.toString(), "-filename", fileName, "-infile", fileName };
            }

            @Test
            public void isLoggedAsE188()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E188.messageBuilder().add(fileName)
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
        private ValidationMetricParser parser;
        private String[] args;

        @Before
        public void setUp() throws Exception
        {
            exit.expectSystemExit();

            parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            fileName = "validationMetric.valid.xml";
            parser = mock(ValidationMetricParser.class);
            EvaluationFile evaluationFile = mock(EvaluationFile.class);
            doReturn(evaluationFile).when(parser).parseFile(Mockito.anyInt(), Mockito.anyString(), Mockito.anyString(),
                    Mockito.any());
            doReturn("observations-" + parentId + "-evaluation_files-" + fileName).when(evaluationFile).getFileId();
            beanPostProcessor.overrideBean(ContinuumComponentCatalogueParser.class, parser);
            importer = createCommmandLineImporterWithParser(parser);
            args = new String[] {"-parent-id", parentId.toString(), "-filename", fileName, 
                    		"-infile", "src/test/resources/validationmetric/good/" + fileName };
        }

        @SuppressWarnings("unchecked")
        @Test
        public void isLoggedAsE190()
        {
            runAndCheckConditionsIgnoringExit(
                    () -> {
                        importer.run(args);
                    },
                    () -> {
                        String logMessage =
                                CasdaDataDepositEvents.E190.messageBuilder()
                                        .add("src/test/resources/validationmetric/good/" + fileName).toString();
     
                        testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                                matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                                matchesPattern(".*\\[source: RTC\\].*"),
                                matchesPattern(".*\\[destination: CASDA_DB\\].*"),
                                matchesPattern(".*\\[volumeKB: \\d+\\].*"), matchesPattern(".*\\[fileId: observations-"
                                        + parentId + "-evaluation_files-" + fileName + "\\].*")),
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
        private ValidationMetricParser parser;
        private String[] args;
        private CatalogueParser.ValidationModeSignal validationSignal;

        @Before
        public void setUp() throws Exception
        {
            exit.expectSystemExit();

            parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            fileName = RandomStringUtils.randomAlphabetic(20);
            parser = mock(ValidationMetricParser.class);
            beanPostProcessor.overrideBean(ContinuumComponentCatalogueParser.class, parser);
            importer = createCommmandLineImporterWithParser(parser);
            args = new String[] { "-parent-id", parentId.toString(), "-filename", fileName, "-infile", 
            		fileName, "-validate-only" };
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
    
	@Override
	protected ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter()
	{
		return new ValidationMetricCommandLineImporter(new ValidationMetricParser());
	}

	private ValidationMetricCommandLineImporter createCommmandLineImporterWithParser(ValidationMetricParser parser)
	{
		return new ValidationMetricCommandLineImporter(parser);
	}
}
