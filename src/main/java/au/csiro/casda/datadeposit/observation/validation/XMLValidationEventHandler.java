package au.csiro.casda.datadeposit.observation.validation;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Event handler for validating XML
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class XMLValidationEventHandler implements ValidationEventHandler
{
    private static final Logger logger = LoggerFactory.getLogger(XMLValidationEventHandler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handleEvent(ValidationEvent event)
    {
        StringBuilder validationEventInfo = new StringBuilder("XML Validation Event");
        validationEventInfo.append("\nSEVERITY: " + event.getSeverity());
        validationEventInfo.append("\nMESSAGE: " + event.getMessage());
        validationEventInfo.append("\nLINKED EXCEPTION: " + event.getLinkedException());
        validationEventInfo.append("\nLOCATOR");
        validationEventInfo.append("\n LINE NUMBER: " + event.getLocator().getLineNumber());
        validationEventInfo.append("\n COLUMN NUMBER: " + event.getLocator().getColumnNumber());
        validationEventInfo.append("\n OFFSET: " + event.getLocator().getOffset());
        validationEventInfo.append("\n OBJECT: " + event.getLocator().getObject());
        validationEventInfo.append("\n NODE: " + event.getLocator().getNode());
        validationEventInfo.append("\n URL: " + event.getLocator().getURL());
        logger.debug("{}", validationEventInfo);
        return false;
    }
}
