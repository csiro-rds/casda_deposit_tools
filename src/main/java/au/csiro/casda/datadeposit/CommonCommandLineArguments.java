package au.csiro.casda.datadeposit;

import com.beust.jcommander.Parameter;

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
 * Common command line arguments for DataDeposit command-line tools.
 * <p>
 * Copyright 2014, CSIRO Australia
 * All rights reserved.
 */
public class CommonCommandLineArguments
{
    /**
     * Level 7 type deposit
     */
    public static final String LEVEL7_PARENT_TYPE = "level7";
    
    /**
     * Observation deposit
     */
    public static final String OBSERVATION_PARENT_TYPE = "observation";
    
    @Parameter(names = "-help", help = true, hidden = true) private boolean help;

    /**
     * @return whether the help option has been specified
     */
    public boolean isHelpOptionSpecified()
    {
        return help;
    }

}
