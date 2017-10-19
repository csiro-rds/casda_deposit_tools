package au.csiro.casda.datadeposit.level7;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.sameInstance;

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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.TemporaryFolder;
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
import au.csiro.casda.datadeposit.copy.CopyDataException;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.entity.observation.Level7Collection;
import au.csiro.logging.CasdaDataDepositEvents;
import de.bechte.junit.runners.context.HierarchicalContextRunner;

/**
 * Test that correct error messages are logged when parsing the metadata file.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@EnableJpaRepositories(basePackageClasses = { ObservationRepository.class })
@RunWith(HierarchicalContextRunner.class)
@ActiveProfiles("local")
public class DataCopyCommandTest extends AbstractArgumentsDrivenCommandLineToolTest
{
    private Log4JTestAppender testAppender;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Autowired
    private DynamicInstantiationAwareBeanPostProcessor beanPostProcessor;

    @Autowired
    private ConfigurableApplicationContext context;

    @Mock
    private Level7CollectionDataCopier level7CollectionDataCopier;

    private DataCopyCommand dataCopyCommand;

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

        level7CollectionDataCopier = mock(Level7CollectionDataCopier.class);
        dataCopyCommand = new DataCopyCommand(level7CollectionDataCopier);
    }

    @Test
    public void testParserThrowCopyDataExceptionLoggedAsE003() throws Exception
    {
        exit.expectSystemExitWithStatus(1);

        Exception exception = new CopyDataException(12345, "/scratch2/somewhere", "/ASKAP/archive/somewhere", "Oops!");
        doThrow(exception).when(level7CollectionDataCopier).copyData(anyInt(), anyString());

        runAndCheckConditionsIgnoringExit(() -> {
            dataCopyCommand.run("-parent-id", "12345", "-folder", "foo");
        }, () -> {
            testAppender.verifyLogMessage(Level.INFO, "Starting data copy");
            testAppender.verifyLogMessage(Level.ERROR, matchesPattern("\\[E003\\].*observation 12345 .*"), exception);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessLoggedAsE012() throws Exception
    {
        exit.expectSystemExitWithStatus(0);

        int collectionId = 12345;
        File collectionFolder = tempFolder.newFolder(String.valueOf(collectionId));
        String infile = collectionFolder.toString();

        Level7Collection level7Collection = new Level7Collection(collectionId);
        Mockito.doReturn(level7Collection).when(level7CollectionDataCopier).copyData(12345, infile);

        runAndCheckConditionsIgnoringExit(() -> {
            dataCopyCommand.run("-parent-id", Integer.toString(collectionId), "-folder", infile);
        }, () -> {
            testAppender.verifyLogMessage(Level.INFO, "Starting data copy");
            String logMessage = CasdaDataDepositEvents.E012.messageBuilder().add(Integer.toString(collectionId)).toString();
            testAppender.verifyLogMessage(Level.INFO, Matchers.allOf(containsString(logMessage),
                    matchesPattern(".*\\[startTime: .*\\].*"), matchesPattern(".*\\[endTime: .*\\].*"),
                    matchesPattern(".*\\[source: RTC\\].*"), matchesPattern(".*\\[destination: CASDA_DB\\].*"),
                    matchesPattern(".*\\[volumeKB: \\d+\\].*"),
                    matchesPattern(".*\\[fileId: level7-" + collectionId + "\\].*")), sameInstance((Throwable) null));
        });
    }

    @Override
    protected ArgumentsDrivenCommandLineTool<?> createCommmandLineImporter()
    {
        return new DataCopyCommand(level7CollectionDataCopier);
    }

}
