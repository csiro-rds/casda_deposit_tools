package au.csiro.casda.datadeposit;

import java.net.InetAddress;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.LogEvent;

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
 * Base implementation of CommandLineRunner that initialises common logging information and provides common handling of
 * startup exceptions.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
public abstract class CommandLineTool implements CommandLineRunner
{
    // set up logging information
    static
    {
        try
        {
            MDC.put("hostname", InetAddress.getLocalHost().getHostName());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MDC.put("hostname", "unknownhost");
        }

        MDC.put("component", "DataDeposit");
        MDC.put("user", "system");
        MDC.put("instanceid", UUID.randomUUID().toString());
    }

    /**
     * Instantiates and runs a CommandLineImporter, catching any unexpected exceptions and logging them as an Exxx event
     * to the logger.
     * 
     * @param commandLineToolClass
     *            the class to instantiate
     * @param logger
     *            the logger
     * @param args
     *            args that will be passed to the instance's {@link CommandLineTool#run(String...)} method
     */
    public static void runCommandLineTool(Class<? extends CommandLineTool> commandLineToolClass, Logger logger,
            String... args)
    {
        try
        {
            SpringApplication app = new SpringApplication(commandLineToolClass);
            app.setWebEnvironment(false);
            app.setHeadless(true);
            app.setShowBanner(false);
            app.setLogStartupInfo(false);
            app.run(args);
        }
        catch (Exception ex)
        {
            logger.error(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(LogEvent.UNKNOWN_EVENT).toString(), ex);
            System.exit(1);
        }
        catch (Error er)
        {
            try
            {
                er.printStackTrace();
                System.out.println("Application exited unexpectedly.");
            }
            finally
            {
                System.exit(1);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Redefinition of CommandLineRunner to force Exceptions to be caught and turned into RuntimeExceptions.
     * 
     * @param args
     *            incoming main method arguments
     */
    @Override
    public abstract void run(String... args);

}
