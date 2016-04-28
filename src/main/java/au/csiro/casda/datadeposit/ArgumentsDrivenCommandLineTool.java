package au.csiro.casda.datadeposit;

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

import org.slf4j.Logger;

import au.csiro.casda.logging.CasdaEvent;
import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;
import au.csiro.casda.logging.CasdaMessageBuilder;

import com.beust.jcommander.ParameterException;

/**
 * Extension of CommandLineTool that supports parsing of command-line-arguments using a JCommander-style 'arguments'
 * object.
 * <p>
 * 
 * @param <T>
 *            the specific type of the JCommander-style 'arguments' object
 *            <p>
 *            Copyright 2014, CSIRO Australia All rights reserved.
 */
public abstract class ArgumentsDrivenCommandLineTool<T extends CommonCommandLineArguments> extends CommandLineTool
{
    /**
     * Outputs the command-line tool's usage information to the given StringBuilder.
     * 
     * @param builder
     *            the builder to add the usage information to
     */
    public void writeUsage(StringBuilder builder)
    {
        getCommandLineArgumentsParser().usage(builder);
    }

    /**
     * Parses the command line arguments.
     * <p>
     * If the args specify a help option then that will be displayed and the tool will successfully exit (code 0).
     * <p>
     * If the args supplied do not match those required then the usage will be displayed on stderr, and an appropriate
     * 'malformed' parameters event will be logged. The system will exit with a failure exit code (ie: 1).
     * <p>
     * 
     * @param logger
     *            the logger to use to notify a malformed parameter event
     * @param args
     *            the command-line arguments
     */
    protected void parseCommandLineArguments(Logger logger, String... args)
    {
        try
        {
            getCommandLineArgumentsParser().parse(args);
        }
        catch (ParameterException e)
        {
            StringBuilder sb = new StringBuilder();
            getCommandLineArgumentsParser().usage(sb);
            System.err.println(sb);

            CasdaMessageBuilder<?> builder = CasdaLogMessageBuilderFactory
                    .getCasdaMessageBuilder(getMalformedParametersEvent());
            getCommandLineArgumentsParser().addArgumentValuesToMalformedParametersEvent(builder);
            builder.add(String.join(" ", args));

            logger.error(builder.toString(), e);
            System.exit(1);
        }

        if (getCommandLineArgumentsParser().getArgs().isHelpOptionSpecified())
        {
            // Write usage to the console
            getCommandLineArgumentsParser().usage();
            System.exit(0);
        }
    }

    /**
     * Template method used to obtain a specific command-line arguments parser for the subclass' specific command line
     * arguments.
     * 
     * @return an AbstractCommandLineArgumentsParser
     */
    protected abstract AbstractCommandLineArgumentsParser<T> getCommandLineArgumentsParser();

    /**
     * Template method used to return the specific event to be used when the command-line arguments are malformed.
     * 
     * @return an event to use when the command-line arguments are malformed
     */
    protected abstract CasdaEvent getMalformedParametersEvent();
}
