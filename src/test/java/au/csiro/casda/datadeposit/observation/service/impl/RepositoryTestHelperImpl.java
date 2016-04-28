package au.csiro.casda.datadeposit.observation.service.impl;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ContinuumComponentRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ContinuumIslandRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.EvaluationFileRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ImageCubeRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.MeasurementSetRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ObservationRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.PolarisationComponentRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ProjectRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.ScanRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectralLineAbsorptionRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectralLineEmissionRepository;
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
 * Repository Helper for test cases.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Component
@Primary
@Transactional
@ActiveProfiles("local")
public class RepositoryTestHelperImpl implements RepositoryTestHelper
{

    @Autowired
    private ObservationRepository observationRepository;
    
    @Autowired
    private Level7CollectionRepository level7CollectionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CatalogueRepository catalogueRepository;

    @Autowired
    private ContinuumComponentRepository continuumComponentRepository;

    @Autowired
    private PolarisationComponentRepository polarisationComponentRepository;

    @Autowired
    private ContinuumIslandRepository continuumIslandRepository;
    
    @Autowired
    private SpectralLineAbsorptionRepository spectralLineAbsorptionRepository;
    
    @Autowired
    private SpectralLineEmissionRepository spectralLineEmissionRepository;

    @Autowired
    private ImageCubeRepository imageCubeRepository;

    @Autowired
    private MeasurementSetRepository measurementSetRepository;
    
    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private EvaluationFileRepository evaluationFileRepository;

    /** {@inheritDoc} */
    @Override
    public void deleteAll()
    {
        evaluationFileRepository.deleteAll();
        scanRepository.deleteAll();
        measurementSetRepository.deleteAll();
        continuumComponentRepository.deleteAll();
        continuumIslandRepository.deleteAll();
        spectralLineAbsorptionRepository.deleteAll();
        spectralLineEmissionRepository.deleteAll();
        polarisationComponentRepository.deleteAll();
        catalogueRepository.deleteAll();
        imageCubeRepository.deleteAll();
        level7CollectionRepository.deleteAll();
        projectRepository.deleteAll();
        observationRepository.deleteAll();
        
        
    }

    /** {@inheritDoc} */
    @Override
    public Long getProjectCount()
    {
        return projectRepository.count();
    }
    
    /** {@inheritDoc} */
    @Override
    public Long getObservationCount()
    {
        return observationRepository.count();
    }

    /** {@inheritDoc} */
    @Override
    public Long getCatalogueCount()
    {
        return catalogueRepository.count();
    }

    /** {@inheritDoc} */
    @Override
    public Long getImageCubeCount()
    {
        return imageCubeRepository.count();
    }

    /** {@inheritDoc} */
    @Override
    public Long getMeasurementSetCount()
    {
        return measurementSetRepository.count();
    }

    /** {@inheritDoc} */
    @Override
    public Long getEvaluationFileCount()
    {
        return evaluationFileRepository.count();
    }

    /** {@inheritDoc} */
    @Override
    public Long getScanCount()
    {
        return scanRepository.count();
    }
    
    /** {@inheritDoc} */
    @Override
    public Long getLevel7CollectionCount()
    {
        return level7CollectionRepository.count();
    }
    
    
}
