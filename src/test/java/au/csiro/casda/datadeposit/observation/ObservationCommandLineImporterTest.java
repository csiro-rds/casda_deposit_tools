package au.csiro.casda.datadeposit.observation;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;

import org.apache.logging.log4j.Level;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import au.csiro.casda.datadeposit.ArgumentsDrivenCommandLineTool;
import au.csiro.casda.datadeposit.observation.ObservationParser.MalformedFileException;
import au.csiro.casda.datadeposit.observation.ObservationParser.RepositoryException;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.logging.CasdaDataDepositEvents;
import de.bechte.junit.runners.context.HierarchicalContextRunner;

/**
 * Test that correct error messages are logged when parsing the metadata file.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@EnableJpaRepositories(basePackageClasses = { ObservationRepository.class })
@RunWith(HierarchicalContextRunner.class)
@ActiveProfiles("local")
public class ObservationCommandLineImporterTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Autowired
    private DynamicInstantiationAwareBeanPostProcessor beanPostProcessor;

    @Autowired
    private ConfigurableApplicationContext context;

    @Mock
    private ObservationParser observationParser;

    private ObservationCommandLineImporter importer;

    @Before
    public void setUp() throws Exception
    {
        testAppender = Log4JTestAppender.createAppender();

        // Manual equivalent to @RunWith(SpringJUnit4ClassRunner.class)
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);

        context.getBeanFactory().addBeanPostProcessor(beanPostProcessor);

        observationParser = mock(ObservationParser.class);
        importer = new ObservationCommandLineImporter(observationParser);
    }

    @Test
    public void testParserThrownFileNotFoundExceptionLoggedAsE003() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new FileNotFoundException();
        doThrow(exception).when(observationParser).parseFile(anyInt(), anyString());

        runAndCheckConditionsIgnoringExit(() -> {
            importer.run("-sbid", "12345", "-infile", "foo");
        }, () -> {
            testAppender.verifyLogMessage(Level.ERROR, matchesPattern("\\[E003\\].*observation 12345 .*"), exception);
        });
    }

    @Test
    public void testParserThrownMalformedFileExceptionLoggedAsE003() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new MalformedFileException("Oops!");
        doThrow(exception).when(observationParser).parseFile(anyInt(), anyString());

        runAndCheckConditionsIgnoringExit(() -> {
            importer.run("-sbid", "12345", "-infile", "foo");
        }, () -> {
            testAppender.verifyLogMessage(Level.ERROR, 
                    matchesPattern("\\[E003\\].*observation 12345 .*"), exception);
        });
    }

    @Test
    public void testParserThrownRepositoryExceptionLoggedAsE003() throws Exception
    {
        exit.expectSystemExitWithStatus(1);
        Exception exception = new RepositoryException("Oops!");
        doThrow(exception).when(observationParser).parseFile(anyInt(), anyString());

        runAndCheckConditionsIgnoringExit(() -> {
            importer.run("-sbid", "12345", "-infile", "foo");
        }, () -> {
            testAppender.verifyLogMessage(Level.ERROR, matchesPattern("\\[E003\\].*observation 12345 .*"), exception);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessLoggedAsE012() throws Exception
    {
        exit.expectSystemExitWithStatus(0);

        int sbid = 12345;
        String infile = "src/test/resources/observation/good/metadata-v2-good01.xml";

        Observation observation = new Observation(sbid);
        Mockito.doReturn(observation).when(observationParser).parseFile(12345, infile);

        runAndCheckConditionsIgnoringExit(() -> {
            importer.run("-sbid", Integer.toString(sbid), "-infile", infile);
        }, () -> {
            String logMessage = CasdaDataDepositEvents.E012.messageBuilder().add(Integer.toString(sbid)).toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: RTC\\].*"), matchesPattern(".*\\[destination: CASDA_DB\\].*"),
                    matchesPattern(".*\\[volumeKB: \\d+\\].*"),
                    matchesPattern(".*\\[fileId: observations-" + sbid + "\\].*")), sameInstance((Throwable) null));
        });
    }

    @Override
    protected ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter()
    {
        return new ObservationCommandLineImporter(observationParser);
    }

}
