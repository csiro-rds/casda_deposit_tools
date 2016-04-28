package au.csiro.casda;

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


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import au.csiro.DynamicInstantiationAwareBeanPostProcessor;
import au.csiro.casda.datadeposit.catalogue.level7.Level7CollectionRepository;
import au.csiro.casda.datadeposit.observation.jpa.repository.CatalogueRepository;

/**
 * Spring Configuration for unit tests. To use the class, add the following line to the top of your test:
 * 
 * @ContextConfiguration(classes = { TestAppConfig.class })
 * 
 *                               This class ensures that all components under the au.csiro.casda.datadeposit.observation
 *                               package are autowired. It also ensures that the standard application.properties files
 *                               are read in the order defined below.
 * 
 *                               Note: If it turns out that some components should not be included then an exclusion
 *                               filter should be added to the
 * @ComponentScan annotation.
 * 
 *                Copyright 2013, CSIRO Australia All rights reserved.
 */
@EnableAutoConfiguration
@PropertySource("classpath:/application.properties")
@PropertySource("classpath:/config/application.properties")
@PropertySource("classpath:/test_config/application.properties")
@PropertySource("classpath:/test_config/application-${env:local}.properties")
@ComponentScan
@EnableJpaRepositories(basePackageClasses = { CatalogueRepository.class, Level7CollectionRepository.class })
/*
 * --------------------------------------------------------------------------------------------------------------------
 * 
 * WARNING: Do NOT declare this class as an @Configuration or it will be automatically loaded when you run the command
 * line applications through Eclipse. See comment above about how to configure test classes to pick up this class as a
 * configuration.
 * 
 * --------------------------------------------------------------------------------------------------------------------
 */
public class TestAppConfig
{
    /**
     * Required to configure the PropertySource(s) (see https://jira.spring.io/browse/SPR-8539)
     * 
     * @return a PropertySourcesPlaceholderConfigurer
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer()
    {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DynamicInstantiationAwareBeanPostProcessor dynamicInstantiationAwareBeanPostProcessor()
    {
        return new DynamicInstantiationAwareBeanPostProcessor();
    }

    /**
     * NOTE: Alternative technique to get application.properties loaded. Requires additional configuration at use-site
     * but allows more Spring Boot-like application.properties loading.
     * 
     * Custom ApplicationContextInitializer that overrides how application.properties files are loaded. In this case the
     * default set of Spring search locations has 'classpath:/test_config/' added so that properties can be overridden
     * in test cases. This class can be used by specifying it in an initializer in an
     * 
     * @ContextConfiguration, eg:
     * 
     * @ContextConfiguration( classes = { TestAppConfig.class }, initializers =
     *                        TestAppConfig.CustomApplicationContextInitializer.class )
     * 
     *                        Copyright 2014, CSIRO Australia All rights reserved.
     */
    @SuppressWarnings("unused")
    private static class CustomApplicationContextInitializer implements
            ApplicationContextInitializer<ConfigurableApplicationContext>
    {

        @Override
        public void initialize(final ConfigurableApplicationContext applicationContext)
        {
            new ConfigFileApplicationListener()
            {
                public void apply()
                {
                    this.setSearchLocations("classpath:/" + "," + "classpath:/config/" + "," + "file:./" + ","
                            + "file:./config/" + "," + "classpath:/test_config/");
                    addPropertySources(applicationContext.getEnvironment(), applicationContext);
                    addPostProcessors(applicationContext);
                }
            }.apply();
        }

    }
}
