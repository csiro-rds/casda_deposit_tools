package au.csiro.casda.datadeposit.rtcnotifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Level;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.StandardErrorStreamLog;
import org.junit.contrib.java.lang.system.StandardOutputStreamLog;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;

import au.csiro.Log4JTestAppender;
import au.csiro.casda.datadeposit.AbstractArgumentsDrivenCommandLineToolTest;
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.logging.CasdaDataDepositEvents;

import com.beust.jcommander.ParameterException;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

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
 * Tests for validations in the Notifier class.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@RunWith(HierarchicalContextRunner.class)
public class NotifierTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private String observationFolderPath;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Rule
    public final StandardOutputStreamLog out = new StandardOutputStreamLog();

    @Rule
    public final StandardErrorStreamLog err = new StandardErrorStreamLog();

    private ArgumentsDrivenCommandLineTool<?> importer;

    @Before
    public void setUp() throws IOException
    {
        testAppender = Log4JTestAppender.createAppender();

        importer = createCommmandLineImporter();
        observationFolderPath = testFolder.getRoot().toPath().toString();
        testFolder.newFile(Notifier.DONE_FILE).delete();
    }

    public class CommandLineArguments
    {

        private String[] args;

        @Before
        public void setUp()
        {
            args = new String[] { "-help" };
            exit.expectSystemExit();
        }

        public class Usage
        {
            @Test
            public void isSupported()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    assertThat(out.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
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
            public void exitsWithSuccess()
            {
                exit.expectSystemExitWithStatus(0);
                importer.run(args);
            }

        }

        public class InfileArgumentMissing
        {
            private String sbid;

            @Before
            public void setUp()
            {
                sbid = createTestSbid();
            }

            @Test
            public void isLoggedAsIncorrectParametersMessageE069()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-sbid", sbid);
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E069.messageBuilder().add(sbid)
                            .add("-sbid " + sbid).toString(), ParameterException.class,
                            "The following option is required: -infile");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run("-sbid", sbid);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-sbid", sbid);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class SbidArgumentMissing
        {
            private String path;

            @Before
            public void setUp()
            {
                path = RandomStringUtils.randomAlphabetic(6);
            }

            @Test
            public void isLoggedAsIncorrectParametersMessageE069()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-infile", path);
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR,
                            CasdaDataDepositEvents.E069.messageBuilder().add("NOT-SPECIFIED").add("-infile " + path)
                                    .toString(), ParameterException.class, "The following option is required: -sbid");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run("-infile", path);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-infile", path);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class SbidArgumentIsNotAnInteger
        {
            private String path;

            @Before
            public void setUp()
            {
                path = RandomStringUtils.randomAlphabetic(6);
            }

            @Test
            public void isLoggedAsIncorrectParametersMessageE069()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-infile", path, "-sbid", "ab12c");
                }, () -> {
                    testAppender.verifyLogMessage(Level.ERROR, CasdaDataDepositEvents.E069.messageBuilder()
                            .add("ab12c").add("-infile " + path + " -sbid ab12c").toString(), ParameterException.class,
                            "Parameter sbid must be an integer");
                });
            }

            @Test
            public void exitsWithErrorCode()
            {
                exit.expectSystemExitWithStatus(1);
                importer.run("-infile", path);
            }

            @Test
            public void printsUsageToStdErr()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run("-infile", path);
                }, () -> {
                    assertThat(err.getLog().trim(), equalTo(getCommandLineImporterUsage().trim()));
                });
            }

        }

        public class UnknownArguments
        {
            private String[] args;

            @Before
            public void setUp()
            {
                args = new String[] { "-foo", "bar", "-ping" };
            }

            @Test
            public void isLoggedAsIncorrectParametersMessageE069()
            {
                runAndCheckConditionsIgnoringExit(() -> {
                    importer.run(args);
                }, () -> {
                    testAppender.verifyLogMessage(
                            Level.ERROR,
                            CasdaDataDepositEvents.E069.messageBuilder().add("NOT-SPECIFIED")
                                    .add(String.join(" ", args)).toString(), ParameterException.class,
                            "Unknown option: -foo");
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

    }

    public class DoneFileDoesNotExist
    {
        private String sbid;

        @Before
        public void setUp() throws IOException
        {
            exit.expectSystemExit();
            sbid = createTestSbid();
        }

        @Test
        public void doneFileIsCreated() throws Exception
        {
            importer.run("-infile", observationFolderPath, "-sbid", sbid);
            assertTrue("File is not created ", (new File(observationFolderPath, "DONE")).exists());
        }

        @Test
        public void isLoggedAsSuccessMessageE054() throws Exception
        {
            runAndCheckConditionsIgnoringExit(
                    () -> {
                        importer.run("-infile", observationFolderPath, "-sbid", sbid);
                    },
                    () -> {
                        testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E054.messageBuilder()
                                .add(sbid).toString());
                    });
        }

        @Test
        public void exitsWithNoErrorCode() throws Exception
        {
            exit.expectSystemExitWithStatus(0);
            importer.run("-infile", observationFolderPath, "-sbid", sbid);
        }
    }

    public class DoneFileExists
    {
        private String sbid;

        @Before
        public void setUp() throws IOException
        {
            exit.expectSystemExit();
            sbid = createTestSbid();
            testFolder.newFile(Notifier.DONE_FILE).createNewFile();
        }

        @Test
        public void doneFileRemains() throws Exception
        {
            assertTrue("File is not created ", (new File(observationFolderPath, "DONE")).exists());
            importer.run("-infile", observationFolderPath, "-sbid", sbid);
            assertTrue("File is not created ", (new File(observationFolderPath, "DONE")).exists());
        }

        @Test
        public void isLoggedAsBothProblemMessageE008andSuccessMessageE054() throws Exception
        {
            runAndCheckConditionsIgnoringExit(
                    () -> {
                        importer.run("-infile", observationFolderPath, "-sbid", sbid);
                    },
                    () -> {
                        testAppender.verifyLogMessage(Level.WARN, CasdaDataDepositEvents.E008.messageBuilder()
                                .add(sbid).toString());
                        testAppender.verifyLogMessage(Level.INFO, CasdaDataDepositEvents.E054.messageBuilder()
                                .add(sbid).toString());
                    });
        }

        @Test
        public void exitsWithNoErrorCode() throws Exception
        {
            exit.expectSystemExitWithStatus(0);
            importer.run("-infile", observationFolderPath, "-sbid", sbid);
        }

    }

    public class InvalidPathToDoneFile
    {
        private String sbid;
        private final String path = "foo*bar"; // Has to be final or it won't be captured in the inner class.

        @Before
        public void setUp()
        {
            exit.expectSystemExit();
            sbid = createTestSbid();
        }

        @Test
        public void isLoggedAsFailureMessageE010()
        {
            runAndCheckConditionsIgnoringExit(() -> {
                importer.run("-infile", path, "-sbid", sbid);
            }, () -> {
                testAppender.verifyLogMessage(Level.ERROR,
                        CasdaDataDepositEvents.E010.messageBuilder().add(sbid).add(path).toString(),
                        new ArgumentMatcher<Exception>()
                        {
                            @Override
                            public boolean matches(Object ex)
                            {
                                return
                                // Windows
                                ((ex instanceof InvalidPathException) && ((InvalidPathException) ex).getMessage()
                                        .contains(path)) ||
                                // UNIX
                                        ((ex instanceof NoSuchFileException) && ((NoSuchFileException) ex).getMessage()
                                                .contains(path));
                            }
                        });
            });
        }

        @Test
        public void exitsWithErrorCode()
        {
            exit.expectSystemExitWithStatus(1);
            importer.run("-infile", path, "-sbid", sbid);
        }
    }

    public class InfileDirectoryDoesNotExist
    {
        private String sbid;
        private final String path = "NO_SUCH_DIR"; // Has to be final or it won't be captured in the inner class.

        @Before
        public void setUp()
        {
            exit.expectSystemExit();
            sbid = createTestSbid();
        }

        @Test
        public void isLoggedAsFailureMessageE010()
        {
            runAndCheckConditionsIgnoringExit(() -> {
                importer.run("-infile", path, "-sbid", sbid);
            }, () -> {
                testAppender.verifyLogMessage(Level.ERROR,
                        CasdaDataDepositEvents.E010.messageBuilder().add(sbid).add(path).toString(),
                        new ArgumentMatcher<NoSuchFileException>()
                        {
                            @Override
                            public boolean matches(Object ex)
                            {
                                return ((NoSuchFileException) ex).getMessage().equals(path);
                            }
                        });
            });
        }

        @Test
        public void exitsWithErrorCode()
        {
            exit.expectSystemExitWithStatus(1);
            importer.run("-infile", path, "-sbid", sbid);
        }
    }

    public class InfileIsNotADirectory
    {
        private String sbid;
        private String path;

        @Before
        public void setUp() throws IOException
        {
            testFolder.newFile("foo").createNewFile();
            path = testFolder.getRoot().toPath().resolve("foo").toString();
            exit.expectSystemExit();
            sbid = createTestSbid();
        }

        @Test
        public void isLoggedAsFailureMessageE010()
        {
            runAndCheckConditionsIgnoringExit(
                    () -> {
                        importer.run("-infile", path, "-sbid", sbid);
                    },
                    () -> {
                        testAppender.verifyLogMessage(
                                Level.ERROR,
                                CasdaDataDepositEvents.E010.messageBuilder().add(sbid).add(path)
                                        .addCustomMessage("'infile' is not a directory").toString());
                    });
        }

        @Test
        public void exitsWithErrorCode()
        {
            exit.expectSystemExitWithStatus(1);
            importer.run("-infile", path, "-sbid", sbid);
        }
    }

    public class IOExceptionThrown
    {
        private String sbid;
        private Notifier notifier;
        private IOException exception;

        @Before
        public void setUp() throws Exception
        {
            exit.expectSystemExit();
            sbid = createTestSbid();
            notifier = (Notifier) spy(importer);
            exception = new IOException();
            doThrow(exception).when(notifier).createFile(any(Path.class));
        }

        @Test
        public void isLoggedAsFailureMessageE010()
        {
            runAndCheckConditionsIgnoringExit(
                    () -> {
                        notifier.run("-infile", observationFolderPath, "-sbid", sbid);
                    },
                    () -> {
                        testAppender.verifyLogMessage(Level.ERROR,
                                CasdaDataDepositEvents.E010.messageBuilder().add(sbid).add(observationFolderPath)
                                        .toString(), exception);
                    });
        }

        @Test
        public void exitsWithErrorCode()
        {
            exit.expectSystemExitWithStatus(1);
            notifier.run("-infile", observationFolderPath, "-sbid", sbid);
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
        return new Notifier();
    }

    private String createTestSbid()
    {
        return Integer.toString(Integer.parseInt(RandomStringUtils.randomNumeric(6))); // Handle leading zeros
    }

}
