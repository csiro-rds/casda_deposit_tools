package au.csiro.casda.datadeposit.level7;

import au.csiro.casda.datadeposit.AbstractCommandLineArgumentsParser;
import au.csiro.casda.datadeposit.CommonCommandLineArguments;
import au.csiro.casda.logging.CasdaMessageBuilder;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

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
 * Helper class to support command line parameter parsing for ObservationCommandLineImporter.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class DataCopyCommandLineArgumentsParser extends
        AbstractCommandLineArgumentsParser<DataCopyCommandLineArgumentsParser.CommandLineArguments>
{

    /**
     * Describes and holds argument values
     * <p>
     * 
     * @see JCommander <p>
     *      Copyright 2014, CSIRO Australia All rights reserved.
     */
    @Parameters(commandDescription = "Copy the data for a level 7 image collection")
    public static class CommandLineArguments extends CommonCommandLineArguments
    {
        @Parameter(names = "-parent-id", description = "the collection id of the level 7 collection", required = true)
        private long parentId;
        
        @Parameter(names = "-folder", description = "the staging folder for the level 7 collection", required = true)
        private String collectionFolder;
        

        public long getParentId()
        {
            return parentId;
        }

        public String getCollectionFolder()
        {
            return collectionFolder;
        }
    }

    /**
     * Constructs a ObservationCommandLineArgumentsParser
     */
    public DataCopyCommandLineArgumentsParser()
    {
        super(DataCopyCommand.TOOL_NAME, new CommandLineArguments());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addArgumentValuesToMalformedParametersEvent(CasdaMessageBuilder<?> builder)
    {
        // Nothing to add
    }
}
