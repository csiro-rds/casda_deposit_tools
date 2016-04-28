package au.csiro.casda.datadeposit.observation.parser;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

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
 * This class is for parsing RTC observation metadata xml and returning a JAXB Observation and associated JAXB pojos for
 * that data.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
@Component
public class XmlObservationParser
{
    /**
     * 
     */
    private static final String SCHEMA_FILE_RESOURCE_PATH = "schemas/observation_metadata.xsd";

    /**
     * Parses the given inputStream that is the Dataset XML from the RTC metadata xml file and returns the Dataset.
     * 
     * @param inputStreamXML
     *            the xml to parse
     * @return the inputStreamXML as a Dataset
     * @throws JAXBException
     *             if there are any exceptions when unmarshalling.
     * @throws IOException
     *             if something low-level went wrong (with the filesystem)
     * @throws SAXException
     *             if something went wrong while parsing the xml
     */
    public static XmlObservation parseDataSetXML(InputStream inputStreamXML) throws JAXBException, SAXException,
            IOException
    {
        JAXBContext jaxbContext = JAXBContext.newInstance(DatasetXmlElementObjectFactory.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(getSchema());
        unmarshaller.setEventHandler(new ValidationEventHandler()
        {
            // The default XMLValidationEventHandler is too noisy
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean handleEvent(ValidationEvent event)
            {
                return false;
            }
        });
        unmarshaller.setProperty("com.sun.xml.internal.bind.ObjectFactory", new DatasetXmlElementObjectFactory());
        return (XmlObservation) unmarshaller.unmarshal(inputStreamXML);
    }

    private static Schema getSchema()
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream schemaFileInputStream = new ClassPathResource(SCHEMA_FILE_RESOURCE_PATH).getInputStream())
        {
            return schemaFactory.newSchema(new StreamSource(schemaFileInputStream));
        }
        catch (IOException e)
        {
            throw new RuntimeException(String.format("Unexpected IOException accessing the resource '%s'",
                    SCHEMA_FILE_RESOURCE_PATH), e);
        }
        catch (SAXException e)
        {
            throw new RuntimeException(String.format("Unexpected SAXException reading the schema file resource '%s'",
                    SCHEMA_FILE_RESOURCE_PATH), e);
        }
    }
}
