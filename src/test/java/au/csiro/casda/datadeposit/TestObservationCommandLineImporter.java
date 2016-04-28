package au.csiro.casda.datadeposit;

import org.springframework.beans.factory.annotation.Autowired;

import au.csiro.casda.datadeposit.observation.ObservationCommandLineImporter;
import au.csiro.casda.datadeposit.observation.ObservationParser;
/**
 * Simple extended class for tests which include parsing an observation, created so a mock SimpleJdbcRepository
 * could be set in the parser, for validation of an image type.
 * <p>
 * Copyright 2016, CSIRO Australia. All rights reserved.
 */
public class TestObservationCommandLineImporter extends ObservationCommandLineImporter
{
    private ObservationParser observationParser;
    
    @Autowired
    public TestObservationCommandLineImporter(ObservationParser observationParser)
    {
        super(observationParser);
        this.observationParser = observationParser;
    }

    protected ObservationParser getObservationParser()
    {
        return observationParser;
    }
}
