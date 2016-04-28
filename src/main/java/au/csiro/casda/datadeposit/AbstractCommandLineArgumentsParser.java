package au.csiro.casda.datadeposit;

import au.csiro.casda.logging.CasdaMessageBuilder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

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
 * An extension of a JCommander command-line arguments parser that supports the return of a JCommander-style 'arguments'
 * object.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 * 
 * @param <T>
 *            the specific type of the JCommander-style 'arguments' object
 */
public abstract class AbstractCommandLineArgumentsParser<T extends CommonCommandLineArguments> extends JCommander
{
    private T args;

    /**
     * Constructor for concrete subclasses to call.
     * 
     * @param programName
     *            the name of the command-line tool
     * @param commandLineArguments
     *            the command-line arguments (subtype of CommonCommandLineArguments)
     */
    protected AbstractCommandLineArgumentsParser(String programName, T commandLineArguments)
    {
        super();
        this.setProgramName(programName);
        this.args = commandLineArguments;
        this.addObject(args);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Extension of {@link com.beust.jcommander.JCommander#parse(java.lang.String[])} that calls the 'validate' template
     * method after parsing the args.
     * 
     * @param args
     *            a list of command-line-arguments
     */
    @Override
    public void parse(String... args)
    {
        super.parse(args);
        if (!this.getArgs().isHelpOptionSpecified())
        {
            this.validate();
        }
    }

    /**
     * Return the JCommander-style 'arguments' object
     * 
     * @return the specific type of the JCommander-style 'arguments' object
     */
    public T getArgs()
    {
        return this.args;
    }

    /**
     * Add specific argument values to a malformed parameters event builder.
     * 
     * @param builder
     *            the builder to add specific argument values to.
     */
    public abstract void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder);

    /**
     * Template method to allow subclasses to validate arguments as required.
     * <p>
     * This method is typically used to perform additional validation that we can't specify with JCommander. It can also
     * be used to work around some limitations in JCommander, eg: see how SBID is handled as in String during parsing
     * but checked to be an Integer once parsed. Subclasses should throw a ParameterException if they cannot validate an
     * argument value.
     * 
     * @throws ParameterException
     *             if an argument value is invalid
     */
    protected void validate() throws ParameterException
    {
    }

}
