package au.csiro.casda.datadeposit.observation.parser;

import javax.xml.bind.annotation.XmlRegistry;

import au.csiro.casda.datadeposit.observation.jaxb.Dataset;
import au.csiro.casda.datadeposit.observation.jaxb.ObjectFactory;

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
 * Extension of JAXB-generated {@link ObjectFactory} that ensures specific subclasses of the XML element classes are
 * returned by the JAXB parser.
 * <p>
 * Copyright 2014, CSIRO Australia All rights reserved.
 */
@XmlRegistry
public class DatasetXmlElementObjectFactory extends ObjectFactory
{
    /**
     * Returns an Observation Dataset instance.
     * 
     * @return an Observation
     */
    public Dataset createDataset()
    {
        return new XmlObservation();
    }
}
