package au.csiro.casda.datadeposit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.apache.commons.lang3.CharEncoding;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import au.csiro.casda.TestAppConfig;
import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.MeasurementSetRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.PolarisationComponentRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ScanRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.TestContinuumComponentRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.TestContinuumIslandRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.TestSpectralLineAbsorptionRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.TestSpectralLineEmissionRepository;
import au.csiro.casda.datadeposit.observation.service.RepositoryTestHelper;

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
 * Base class for functional tests.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@ContextConfiguration(classes = { TestAppConfig.class })
@ActiveProfiles("local")
public class FunctionalTestBase
{    
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @PersistenceUnit
    protected EntityManagerFactory emf;

    @Autowired
    protected ConfigurableApplicationContext context;

    @Autowired
    protected RepositoryTestHelper repositoryTestHelper;
    
    protected ProjectRepository projectRepository;

    protected ObservationRepository observationRepository;
    
    protected Level7CollectionRepository level7CollectionRepository;

    protected CatalogueRepository catalogueRepository;

    protected TestContinuumComponentRepository continuumComponentRepository;

    protected TestContinuumIslandRepository continuumIslandRepository;

    protected PolarisationComponentRepository polarisationComponentRepository;

    protected ImageCubeRepository imageCubeRepository;

    protected EvaluationFileRepository evaluationFileRepository;

    protected MeasurementSetRepository measurementSetRepository;
    
    protected TestSpectralLineAbsorptionRepository spectralLineAbsorptionRepository;

    protected TestSpectralLineEmissionRepository spectralLineEmissionRepository;
    
    protected ScanRepository scanRepository;

    protected TestObservationCommandLineImporter observationCommandLineImporter;

    protected EntityManager entityManager;

    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private PrintStream systemOut;
    
    private SimpleJdbcRepository simpleJdbcRepository;

    public FunctionalTestBase() throws Exception
    {
        // Standard Spring test config in the absence of a SpringJUnitTestRunner
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @Before
    public void setUp() throws Exception
    {
        systemOut = System.out;
        System.setOut(new PrintStream(out, true, CharEncoding.UTF_8));
        entityManager = emf.createEntityManager();

        // Allow for System.exit's from the command line tools
        exit.expectSystemExit();

        observationCommandLineImporter = context.getBeanFactory().createBean(TestObservationCommandLineImporter.class);
        simpleJdbcRepository = mock(SimpleJdbcRepository.class);
        when(simpleJdbcRepository.getImageTypes()).thenReturn(getValidImageTypes());
        
        RepositoryFactorySupport rfs = new JpaRepositoryFactory(entityManager);
        observationRepository = rfs.getRepository(ObservationRepository.class);
        level7CollectionRepository = rfs.getRepository(Level7CollectionRepository.class);
        projectRepository = rfs.getRepository(ProjectRepository.class);
        catalogueRepository = rfs.getRepository(CatalogueRepository.class);
        continuumComponentRepository = rfs.getRepository(TestContinuumComponentRepository.class);
        continuumIslandRepository = rfs.getRepository(TestContinuumIslandRepository.class);
        polarisationComponentRepository = rfs.getRepository(PolarisationComponentRepository.class);
        imageCubeRepository = rfs.getRepository(ImageCubeRepository.class);
        evaluationFileRepository = rfs.getRepository(EvaluationFileRepository.class);
        measurementSetRepository = rfs.getRepository(MeasurementSetRepository.class);
        spectralLineAbsorptionRepository = rfs.getRepository(TestSpectralLineAbsorptionRepository.class);
        spectralLineEmissionRepository = rfs.getRepository(TestSpectralLineEmissionRepository.class);
        scanRepository = rfs.getRepository(ScanRepository.class);

        // Wipe the database
        
        repositoryTestHelper.deleteAll();
    }
    
    private List<String> getValidImageTypes()
    {
        List<String> types = new ArrayList<String>();
        types.add("cont_restored_T0");
        types.add("cont_cleanmodel_T1");
        types.add("cont_psfnat_T1");
        types.add("cont_sensitivity_4d");

        return types;
    }

    @After
    public void tearDown()
    {
        entityManager.close(); // Need to explicitly close the entity manager
        System.out.close();
        System.setOut(systemOut);
    }

    /**
     * Used by subclasses to import an observation (typically needed because other imports rely on the observation
     * existing).
     * 
     * @param sbid
     *            the SBID of the observation
     * @param observationMetadataFile
     *            the resource-relative path to the observation metadata file
     */
    protected void importObservation(String sbid, String observationMetadataFile)
    {
        try
        {
            observationCommandLineImporter.getObservationParser().setSimpleJdbcRepository(simpleJdbcRepository);
            observationCommandLineImporter.run("-sbid", sbid, "-infile", observationMetadataFile);
            failTestCase();
        }
        catch (CheckExitCalled e)
        {
            checkForNormalExit(e);
        }
        assertEquals(1, observationRepository.count());
    }
    
    protected void failTestCase()
    {
        /*
         * We have to throw a RuntimeException rather than use Assert.fail, otherwise the test case won't actually fail
         * due to the use of ExpectedSystemExit.
         */
        throw new RuntimeException("Should never get here!");
    }

    protected void checkForNormalExit(CheckExitCalled e)
    {
        assertEquals("Import failed - please check log for details", 0, e.getStatus().intValue());
    }
}
