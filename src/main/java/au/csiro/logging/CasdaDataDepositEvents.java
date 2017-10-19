package au.csiro.logging;

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


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import au.csiro.casda.datadeposit.DataDepositMessageBuilder;
import au.csiro.casda.logging.CasdaEvent;
import au.csiro.casda.logging.CasdaLogMessageBuilderFactory;

/**
 * The known events for the CASDA Data Deposit. For more information see
 * https://wiki.csiro.au/display/CASDA/APPENDIX+F:+Events+and+Notifications
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public enum CasdaDataDepositEvents implements CasdaEvent
{
    /**
     * deposit.stage.failed
     */
    E001,

    /**
     * Observation - Unable to read metadata file
     */
    E003,

    /**
     * deposit.copy.checksum.failed
     */
    E006,

    /**
     * deposit.copy.checksum.missing
     */
    E007,

    /**
     * notifier.warning.file_exists
     */
    E008,

    /**
     * notifier.failure.create_file
     */
    E010,

    /**
     * Observation - successful deposit
     */
    E012,

    /**
     * deposit.artefact.missing
     */
    E015,

    /**
     * Malformed Request
     */
    E033,

    /**
     * Unable to connect to database
     */
    E034,

    /**
     * deposit.register.success
     */
    E043,

    /**
     * deposit.stage.start
     */
    E047,

    /**
     * deposit.step.artifact.progressed
     */
    E050,

    /**
     * deposit.step.failed.controlled
     */
    E052,

    /**
     * notifier.success
     */
    E054,

    /**
     * catalogue.ingest.failure.parameters
     */
    E055,

    /**
     * catalogue.ingest.failure.file.missing
     */
    E056,

    /**
     * catalogue.ingest.failure.file.malformed
     */
    E057,

    /**
     * catalogue.ingest.failure.database
     */
    E058,

    /**
     * catalogue.ingest.success
     */
    E059,

    /**
     * fits.extract.success
     */
    E065,

    /**
     * fits.extract.parse.exception
     */
    E066,

    /**
     * fits.extract.datatabase.exception
     */
    E067,

    /**
     * notifier.failure.parameters
     */
    E069,

    /**
     * deposit.failure.parameters
     */
    E070,

    /**
     * Deposit - Status change
     */
    E075,

    /**
     * /** Deposit - Observation complete
     */
    E077,

    /**
     * deposit.register.failed
     */
    E080,

    /**
     * deposit.stage.failure.parameters
     */
    E081,

    /**
     * deposit.stage.ngas.failure
     */
    E088,

    /**
     * deposit.stage.start
     */
    E090,

    /**
     * deposit.register.success
     */
    E091,

    /**
     * deposit.copy.failure.parameters
     */
    E092,

    /**
     * fits.project.code.mismatch
     */
    E108,

    /**
     * Staging Level 7 deposit artefact
     */
    E128,

    /**
     * Failed to store Level 7 file-based data
     */
    E129,

    /**
     * Failed to deposit Level 7 data (Checksum)
     */
    E130,

    /**
     * Failed to stage Level 7 deposit artefact
     */
    E131,

    /**
     * Successful staging of Level 7 deposit artefact
     */
    E132,

    /**
     * Unable to write ERROR file
     */
    E151,

    /**
     * Successfully created encapsulation.
     */
    E154,

    /**
     * Unable to create encapsulation
     */
    E155,
    
    /**
     * validation.metric.ingest.failure.parameters
     */
    E186,

    /**
     * validation.metric.ingest.failure.file.missing
     */
    E187,

    /**
     * validation.metric.ingest.failure.file.malformed
     */
    E188,

    /**
     * validation.metric.ingest.failure.database
     */
    E189,

    /**
     * validation.metric.ingest.success
     */
    E190;

    private static Properties eventProperties = new Properties();

    static
    {
        InputStream propertiesStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("event-data_deposit.properties");
        try
        {
            eventProperties.load(propertiesStream);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not load event properties");
        }

    }

    private CasdaDataDepositEvents()
    {
    }

    /**
     * Gets the property name for this event's title.
     * 
     * @return <EVENT_CODE>.title
     */
    private String getTitlePropertyName()
    {
        return this.getCode() + ".title";
    }

    /**
     * Gets the property name for this event's description.
     * 
     * @return <EVENT_CODE>.description
     */
    private String getDescriptionPropertyName()
    {
        return this.getCode() + ".description";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get the format string. For known events, this is the Standard Content, see:
     * https://wiki.csiro.au/display/CASDA/APPENDIX+F:+Events+and+Notifications
     * 
     * @return standard java format string representing the standard content for this event see
     *         (http://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html)
     */
    @Override
    public String getFormatString()
    {
        return eventProperties.getProperty(this.getDescriptionPropertyName());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get the type of event. For known events, this is the Event Title, see:
     * https://wiki.csiro.au/display/CASDA/APPENDIX+F:+Events+and+Notifications
     * 
     * @return event title
     */
    @Override
    public String getType()
    {
        return eventProperties.getProperty(this.getTitlePropertyName());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get the Event Code, see: https://wiki.csiro.au/display/CASDA/APPENDIX+F:+Events+and+Notifications
     * 
     * @return event code, eg E001
     */
    @Override
    public String getCode()
    {
        return this.name();
    }

    /**
     * @return a message builder that can be used to build a message of this type
     */
    public DataDepositMessageBuilder messageBuilder()
    {
        return new DataDepositMessageBuilder(CasdaLogMessageBuilderFactory.getCasdaMessageBuilder(this));
    }
}
