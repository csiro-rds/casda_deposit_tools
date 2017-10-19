package au.csiro.casda;

import org.apache.commons.lang3.StringUtils;

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


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;
import au.csiro.casda.jobmanager.CommandRunnerServiceProcessJobFactory;
import au.csiro.casda.jobmanager.JavaProcessJobFactory;
import au.csiro.casda.jobmanager.SynchronousProcessJobManager;
import au.csiro.casda.jobmanager.ProcessJobBuilder.ProcessJobFactory;
import au.csiro.casda.logging.CasdaLoggingSettings;

/**
 * Spring Java Config for Command line application(s).
 * 
 * datadeposit-application.properties necessary for use with a Web App in Casda Deposit Manager since
 * application.properties exists in that project and thus the data-deposit one is never found. Its marked as optional.
 * 
 * Copyright 2013, CSIRO Australia All rights reserved.
 */
@EnableAutoConfiguration
@Configuration
@ComponentScan
@EnableJpaRepositories(basePackageClasses = { CatalogueRepository.class, SimpleJdbcRepository.class,
        Level7CollectionRepository.class })
public class AppConfig
{
    private static final String APPLICATION_NAME = "DataDeposit";

    static
    {
        CasdaLoggingSettings loggingSettings = new CasdaLoggingSettings(APPLICATION_NAME);
        loggingSettings.addGeneralLoggingSettings();
        loggingSettings.addLoggingInstanceId();
    }

    @Autowired
    private ApplicationContext context;

    /**
     * @return a JobManager for observationImport
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SynchronousProcessJobManager observationImportJobManager()
    {
        return new SynchronousProcessJobManager();
    }

    @Bean
    public RestTemplate getRestTemplate()
    {
        return new RestTemplate();
    }

    /**
     * Return the factory which will create ProcessJob instance for us to run commands.
     * 
     * @param cmdWebServiceUrl
     *            The URL of the command runner web service.
     * @param factoryName
     *            The name of the factory to use, if not using the default.
     * @return The ProcessJobFactory instance
     */
    @Bean
    public ProcessJobFactory getProcessJobFactory(@Value("${command.webservice.url:}") String cmdWebServiceUrl,
            @Value("${command.process.job.factory:}") String factoryName)
    {
        if ("CommandRunnerServiceProcessJobFactory".equalsIgnoreCase(factoryName))
        {
            if (StringUtils.isBlank(cmdWebServiceUrl))
            {
                throw new IllegalArgumentException(
                        "command.webservice.url must be configured to use CommandRunnerServiceProcessJobFactory");
            }
            return new CommandRunnerServiceProcessJobFactory(cmdWebServiceUrl, APPLICATION_NAME);
        }

        return new JavaProcessJobFactory();
    }
}
