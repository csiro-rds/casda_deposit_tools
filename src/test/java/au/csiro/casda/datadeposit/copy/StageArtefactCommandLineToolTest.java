package au.csiro.casda.datadeposit.copy;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import au.csiro.DynamicInstantiationAwareBeanPostProcessor;
import au.csiro.Log4JTestAppender;
import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.logging.CasdaDataDepositEvents;

import com.beust.jcommander.ParameterException;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

/**
 * StageArtefactCommandLineTool unit tests
 * <p>
 * Please note that this class uses a HierarchicalContextRunner to partition the test cases into a hierarchy. See the
 * README.md in src/test/java
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@RunWith(HierarchicalContextRunner.class)
@ActiveProfiles("local")
public class StageArtefactCommandLineToolTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    @Autowired
    private DynamicInstantiationAwareBeanPostProcessor beanPostProcessor;

    @Autowired
    private ConfigurableApplicationContext context;

    private StageArtefactCommandLineTool tool;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final StandardOutputStreamLog out = new StandardOutputStreamLog();

    @Rule
    public final StandardErrorStreamLog err = new StandardErrorStreamLog();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
            tool = context.getBeanFactory().createBean(StageArtefactCommandLineTool.class);
        }

        public class Usage
        {
            @Test
            public void isSupported()
            {
                StringBuilder builder = new StringBuilder();
                tool.writeUsage(builder);

                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run("-help");
                }, () -> {
                    assertThat(out.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

            @Test
            public void isNotLogged()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run("-help");
                }, () -> {
                    testAppender.verifyNoMessages();
                });
            }

            @Test
            public void exitsWithSuccess()
            {
                exit.expectSystemExitWithStatus(0);
                tool.run("-help");
            }

        }

        public class InfileArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String[] args = new String[] { "-parent-id", parentId.toString(), "-parent-type", "observation",
                    "-staging_volume", "volume1", "-file_id", "observations-123-image_cubes-foo.xml" };

            @Test
            public void isLoggedAsE081()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E081.messageBuilder().add(StringUtils.join(args, " ")).toString(),
                            ParameterException.class, "The following option is required: -infile");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                tool.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class ParentIdArgumentMissing
        {
            private String[] args = new String[] { "-infile", "foo.xml", "-parent-type", "observation",
                    "-staging_volume", "volume1", "-file_id", "observations-123-image_cubes-foo.xml" };

            @Test
            public void isLoggedAsE081()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E081.messageBuilder().add(StringUtils.join(args, " ")).toString(),
                            ParameterException.class, "The following option is required: -parent-id");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                tool.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class ParentTypeArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String[] args = new String[] { "-infile", "foo.xml", "-parent-id", parentId.toString(),
                    "-staging_volume", "volume1", "-file_id", "observations-123-image_cubes-foo.xml" };

            @Test
            public void isLoggedAsE081()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E081.messageBuilder().add(StringUtils.join(args, " ")).toString(),
                            ParameterException.class, "The following option is required: -parent-type");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                tool.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class StagingVolumeArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String[] args = new String[] { "-infile", "foo.xml", "-parent-id", parentId.toString(),
                    "-parent-type", "observation", "-file_id", "observations-123-image_cubes-foo.xml" };

            @Test
            public void isLoggedAsE081()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E081.messageBuilder().add(StringUtils.join(args, " ")).toString(),
                            ParameterException.class, "The following option is required: -staging_volume");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                tool.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class FileIdArgumentMissing
        {
            private Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            private String[] args = new String[] { "-infile", "foo.xml", "-parent-id", parentId.toString(),
                    "-parent-type", "observation", "-staging_volume", "volume1" };

            @Test
            public void isLoggedAsE081()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E081.messageBuilder().add(StringUtils.join(args, " ")).toString(),
                            ParameterException.class, "The following option is required: -file_id");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                tool.run(args);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class UnknownArguments
        {
            @Test
            public void isLoggedAsE081()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run("-foo", "bar", "-ping");
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E081.messageBuilder().add("-foo bar -ping").toString(),
                            ParameterException.class, "Unknown option: -foo");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                tool.run("-foo", "bar", "-ping");
            }

            @Test
            public void printsUsageToStdErr()
            {
                StringBuilder builder = new StringBuilder();
                tool.writeUsage(builder);

                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run("-foo", "bar", "-ping");
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

    }

    public class ObservationDeposit
    {
        private String parentType = "observation";

        @Test
        public void stagerIsAskedToStageFile() throws IOException
        {
            exit.expectSystemExit();

            NgasStager stager = mock(NgasStager.class);
            tool = new StageArtefactCommandLineTool(stager);

            String filename = "foo.xml";
            tempFolder.create();
            File file = tempFolder.newFile(filename);
            FileUtils.touch(file);
            String infile = file.getCanonicalPath();
            Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            String stagingVolume = "volume1";
            String fileId = "observations-123-image_cubes-foo.xml";
            String[] args =
                    new String[] { "-infile", infile, "-parent-id", parentId.toString(), "-parent-type", parentType,
                            "-staging_volume", stagingVolume, "-file_id", fileId };

            runAndCheckConditionsIgnoringExit(() -> {
                tool.run(args);
            }, () -> {
                try
                {
                    verify(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }
                catch (Exception e)
                {
                    fail(e.toString());
                }
            });
        }

        public class ThrownStagerException
        {
            private Integer parentId;
            private String infile;
            private String stagingVolume;
            private String fileId;
            private NgasStager stager;
            private Exception exception;
            private String[] args;

            @Before
            public void setUp() throws Exception
            {
                exit.expectSystemExit();

                parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
                infile = RandomStringUtils.randomAlphabetic(20);
                stagingVolume = RandomStringUtils.randomAlphabetic(20);
                fileId = RandomStringUtils.randomAlphabetic(20);
                args =
                        new String[] { "-infile", infile, "-parent-id", parentId.toString(), "-parent-type",
                                parentType, "-staging_volume", stagingVolume, "-file_id", fileId };

                stager = mock(NgasStager.class);
                tool = new StageArtefactCommandLineTool(stager);
            }

            public class NoSuchFile
            {
                @Before
                public void setUp() throws Exception
                {
                    exception = new NoSuchFileException(infile);
                    doThrow(exception).when(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }

                @Test
                public void isLoggedAsE015()
                {
                    runAndCheckConditionsIgnoringExit(
                            () -> {
                                tool.run(args);
                            },
                            () -> {
                                testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E047.messageBuilder()
                                        .add(infile).add(fileId).add(parentId).toString());
                                testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E015.messageBuilder()
                                        .add(infile).add(parentId).toString(), exception);
                            });
                }

                @Test
                public void exitsWithErrorCode()
                {
                    exit.expectSystemExitWithStatus(1);
                    tool.run(args);
                }
            }

            public class Stager
            {
                @Before
                public void setUp() throws Exception
                {
                    exception = new StagingException(parentId, infile, stagingVolume, fileId, "whoops");
                    doThrow(exception).when(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }

                @Test
                public void isLoggedAsE001()
                {
                    runAndCheckConditionsIgnoringExit(
                            () -> {
                                tool.run(args);
                            },
                            () -> {
                                testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E047.messageBuilder()
                                        .add(infile).add(fileId).add(parentId).toString());
                                testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E001.messageBuilder()
                                        .add(infile).add(parentId).add("register").toString(), exception);
                            });
                }

                @Test
                public void exitsWithErrorCode()
                {
                    exit.expectSystemExitWithStatus(1);
                    tool.run(args);
                }
            }

            public class ChecksumMissing
            {
                @Before
                public void setUp() throws Exception
                {
                    exception = new ChecksumMissingException(parentId, infile, Paths.get(infile));
                    doThrow(exception).when(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }

                @Test
                public void isLoggedAsE006()
                {
                    runAndCheckConditionsIgnoringExit(
                            () -> {
                                tool.run(args);
                            },
                            () -> {
                                testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E047.messageBuilder()
                                        .add(infile).add(fileId).add(parentId).toString());
                                testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E007.messageBuilder()
                                        .add(infile).add(parentId).toString(), exception);
                            });
                }

                @Test
                public void exitsWithErrorCode()
                {
                    exit.expectSystemExitWithStatus(1);
                    tool.run(args);
                }
            }
        }

        public class Success
        {

            private Integer parentId;
            private String infile;
            private String stagingVolume;
            private String fileId;
            private NgasStager stager;
            private String[] args;

            @Before
            public void setUp() throws Exception
            {
                exit.expectSystemExit();

                parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
                String filename = RandomStringUtils.randomAlphabetic(20);
                tempFolder.create();
                File file = tempFolder.newFile(filename);
                FileUtils.touch(file);
                infile = file.getCanonicalPath();
                stagingVolume = RandomStringUtils.randomAlphabetic(20);
                fileId = RandomStringUtils.randomAlphabetic(20);
                args =
                        new String[] { "-infile", infile, "-parent-id", parentId.toString(), "-parent-type",
                                parentType, "-staging_volume", stagingVolume, "-file_id", fileId };

                stager = mock(NgasStager.class);
                tool = new StageArtefactCommandLineTool(stager);
            }

            @SuppressWarnings("unchecked")
            @Test
            public void isLoggedAsE091()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                },
                        () -> {
                            testAppender.verifyLogMessage(Level.INFO,
                                    CasdaDataDepositEvents.E047.messageBuilder().add(infile).add(fileId).add(parentId)
                                            .toString());
                            String logMessage =
                                    CasdaDataDepositEvents.E091.messageBuilder().add(infile).add(fileId).add(parentId)
                                            .toString();
                            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                                    matchesPattern(".*\\[source: RTC\\].*"),
                                    matchesPattern(".*\\[destination: NGAS\\].*"),
                                    matchesPattern(".*\\[volumeKB: \\d+\\].*"), matchesPattern(".*\\[fileId: " + fileId
                                            + "\\].*")), sameInstance((Throwable) null));
                        });
            }

            @Test
            public void exitsWithNoErrorCode()
            {
                exit.expectSystemExitWithStatus(0);
                tool.run(args);
            }
        }
    }

    public class Level7Collection
    {
        private String parentType = "level7";

        @Test
        public void stagerIsAskedToStageFile() throws IOException
        {
            exit.expectSystemExit();

            NgasStager stager = mock(NgasStager.class);
            tool = new StageArtefactCommandLineTool(stager);

            String filename = "foo.xml";
            tempFolder.create();
            File file = tempFolder.newFile(filename);
            FileUtils.touch(file);
            String infile = file.getCanonicalPath();
            Integer parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
            String stagingVolume = "volume1";
            String fileId = "observations-123-image_cubes-foo.xml";
            String[] args =
                    new String[] { "-infile", infile, "-parent-id", parentId.toString(), "-parent-type", parentType,
                            "-staging_volume", stagingVolume, "-file_id", fileId };

            runAndCheckConditionsIgnoringExit(() -> {
                tool.run(args);
            }, () -> {
                try
                {
                    verify(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }
                catch (Exception e)
                {
                    fail(e.toString());
                }
            });
        }

        public class ThrownStagerException
        {
            private Integer parentId;
            private String infile;
            private String stagingVolume;
            private String fileId;
            private NgasStager stager;
            private Exception exception;
            private String[] args;

            @Before
            public void setUp() throws Exception
            {
                exit.expectSystemExit();

                parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
                infile = RandomStringUtils.randomAlphabetic(20);
                stagingVolume = RandomStringUtils.randomAlphabetic(20);
                fileId = RandomStringUtils.randomAlphabetic(20);
                args =
                        new String[] { "-infile", infile, "-parent-id", parentId.toString(), "-parent-type",
                                parentType, "-staging_volume", stagingVolume, "-file_id", fileId };

                stager = mock(NgasStager.class);
                tool = new StageArtefactCommandLineTool(stager);
            }

            public class NoSuchFile
            {
                @Before
                public void setUp() throws Exception
                {
                    exception = new NoSuchFileException(infile);
                    doThrow(exception).when(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }

                @Test
                public void isLoggedAsE129()
                {
                    runAndCheckConditionsIgnoringExit(
                            () -> {
                                tool.run(args);
                            },
                            () -> {
                                testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E128.messageBuilder()
                                        .add(infile).add(fileId).add(parentId).toString());
                                testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E129.messageBuilder()
                                        .add(infile).add(parentId).toString(), exception);
                            });
                }

                @Test
                public void exitsWithErrorCode()
                {
                    exit.expectSystemExitWithStatus(1);
                    tool.run(args);
                }
            }

            public class Stager
            {
                @Before
                public void setUp() throws Exception
                {
                    exception = new StagingException(parentId, infile, stagingVolume, fileId, "whoops");
                    doThrow(exception).when(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }

                @Test
                public void isLoggedAsE131()
                {
                    runAndCheckConditionsIgnoringExit(
                            () -> {
                                tool.run(args);
                            },
                            () -> {
                                testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E128.messageBuilder()
                                        .add(infile).add(fileId).add(parentId).toString());
                                testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E131.messageBuilder()
                                        .add(infile).add(parentId).add("register").toString(), exception);
                            });
                }

                @Test
                public void exitsWithErrorCode()
                {
                    exit.expectSystemExitWithStatus(1);
                    tool.run(args);
                }
            }

            public class ChecksumMissing
            {
                @Before
                public void setUp() throws Exception
                {
                    exception = new ChecksumMissingException(parentId, infile, Paths.get(infile));
                    doThrow(exception).when(stager).stageArtefactToNgas(parentId, infile, stagingVolume, fileId);
                }

                @Test
                public void isLoggedAsE130()
                {
                    runAndCheckConditionsIgnoringExit(
                            () -> {
                                tool.run(args);
                            },
                            () -> {
                                testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E128.messageBuilder()
                                        .add(infile).add(fileId).add(parentId).toString());
                                testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E130.messageBuilder()
                                        .add(infile).add(parentId).toString(), exception);
                            });
                }

                @Test
                public void exitsWithErrorCode()
                {
                    exit.expectSystemExitWithStatus(1);
                    tool.run(args);
                }
            }
        }

        public class Success
        {

            private Integer parentId;
            private String infile;
            private String stagingVolume;
            private String fileId;
            private NgasStager stager;
            private String[] args;

            @Before
            public void setUp() throws Exception
            {
                exit.expectSystemExit();

                parentId = Integer.parseInt(RandomStringUtils.randomNumeric(6));
                String filename = RandomStringUtils.randomAlphabetic(20);
                tempFolder.create();
                File file = tempFolder.newFile(filename);
                FileUtils.touch(file);
                infile = file.getCanonicalPath();
                stagingVolume = RandomStringUtils.randomAlphabetic(20);
                fileId = RandomStringUtils.randomAlphabetic(20);
                args =
                        new String[] { "-infile", infile, "-parent-id", parentId.toString(), "-parent-type",
                                parentType, "-staging_volume", stagingVolume, "-file_id", fileId };

                stager = mock(NgasStager.class);
                tool = new StageArtefactCommandLineTool(stager);
            }

            @SuppressWarnings("unchecked")
            @Test
            public void isLoggedAsE132()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    tool.run(args);
                },
                        () -> {
                            testAppender.verifyLogMessage(Level.INFO,
                                    CasdaDataDepositEvents.E128.messageBuilder().add(infile).add(fileId).add(parentId)
                                            .toString());
                            String logMessage =
                                    CasdaDataDepositEvents.E132.messageBuilder().add(infile).add(fileId).add(parentId)
                                            .toString();
                            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                                    matchesPattern(".*\\[source: RTC\\].*"),
                                    matchesPattern(".*\\[destination: NGAS\\].*"),
                                    matchesPattern(".*\\[volumeKB: \\d+\\].*"), matchesPattern(".*\\[fileId: " + fileId
                                            + "\\].*")), sameInstance((Throwable) null));
                        });
            }

            @Test
            public void exitsWithNoErrorCode()
            {
                exit.expectSystemExitWithStatus(0);
                tool.run(args);
            }
        }
    }

    /**
     * Implementation of {@link AbstractArgumentsDrivenCommandLineToolTest#createCommmandLineImporter()}
     */
    @Override
    public StageArtefactCommandLineTool createCommmandLineImporter()
    {
        return new StageArtefactCommandLineTool(mock(NgasStager.class));
    }

}