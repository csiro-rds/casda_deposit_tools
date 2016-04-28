package au.csiro.casda.datadeposit.observation.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import au.csiro.casda.datadeposit.observation.jaxb.Catalogue;
import au.csiro.casda.datadeposit.observation.jaxb.Catalogues;
import au.csiro.casda.datadeposit.observation.jaxb.Dataset;
import au.csiro.casda.datadeposit.observation.jaxb.Evaluation;
import au.csiro.casda.datadeposit.observation.jaxb.Evaluations;
import au.csiro.casda.datadeposit.observation.jaxb.Identity;
import au.csiro.casda.datadeposit.observation.jaxb.Image;
import au.csiro.casda.datadeposit.observation.jaxb.Images;
import au.csiro.casda.datadeposit.observation.jaxb.MeasurementSet;
import au.csiro.casda.datadeposit.observation.jaxb.MeasurementSets;

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
 * Extension of JAXB-generated Dataset to make it easier to access elements.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class XmlObservation extends Dataset
{

    /**
     * @return the Identity element
     */
    public Identity getIdentity()
    {
        List<Identity> elements = getElementsOfType(Identity.class);
        if (elements.size() > 1)
        {
            throw new RuntimeException("Expected valid observation <dataset> to have only one <identity> element. "
                    + "(Perhaps the schema definition has changedd?)");
        }
        return elements.get(0);
    }

    /**
     * @return the Observation element
     */
    public au.csiro.casda.datadeposit.observation.jaxb.Observation getObservation()
    {
        List<au.csiro.casda.datadeposit.observation.jaxb.Observation> elements =
                getElementsOfType(au.csiro.casda.datadeposit.observation.jaxb.Observation.class);
        if (elements.size() > 1)
        {
            throw new RuntimeException("Expected valid observation <dataset> to have only one <observation> element. "
                    + "(Perhaps the schema definition has changedd?)");
        }
        return elements.get(0);
    }

    /**
     * @return the Image elements
     */
    public List<Image> getImages()
    {
        List<Images> elements = getElementsOfType(Images.class);
        if (elements.size() > 1)
        {
            throw new RuntimeException("Unexpected number of <images> elements in XML.");
        }
        if (CollectionUtils.isEmpty(elements) || CollectionUtils.isEmpty(elements.get(0).getImage()))
        {
            return new ArrayList<Image>(0);
        }
        else
        {
            return elements.get(0).getImage();
        }
    }

    /**
     * @return the Image elements
     */
    public List<Catalogue> getCatalogues()
    {
        List<Catalogues> elements = getElementsOfType(Catalogues.class);
        if (elements.size() > 1)
        {
            throw new RuntimeException("Unexpected number of <catalogues> elements in XML.");
        }
        if (CollectionUtils.isEmpty(elements) || CollectionUtils.isEmpty(elements.get(0).getCatalogue()))
        {
            return new ArrayList<Catalogue>(0);
        }
        else
        {
            return elements.get(0).getCatalogue();
        }
    }

    /**
     * @return the Image elements
     */
    public List<MeasurementSet> getMeasurementSets()
    {
        List<MeasurementSets> elements = getElementsOfType(MeasurementSets.class);
        if (elements.size() > 1)
        {
            throw new RuntimeException("Unexpected number of <measurement_sets> elements in XML.");
        }
        if (CollectionUtils.isEmpty(elements) || CollectionUtils.isEmpty(elements.get(0).getMeasurementSet()))
        {
            return new ArrayList<MeasurementSet>(0);
        }
        else
        {
            return elements.get(0).getMeasurementSet();
        }
    }

    /**
     * @return the Image elements
     */
    public List<Evaluation> getEvaluationFiles()
    {
        List<Evaluations> elements = getElementsOfType(Evaluations.class);
        if (elements.size() > 1)
        {
            throw new RuntimeException("Unexpected number of <evaluations> elements in XML.");
        }
        if (CollectionUtils.isEmpty(elements) || CollectionUtils.isEmpty(elements.get(0).getEvaluation()))
        {
            return new ArrayList<Evaluation>(0);
        }
        else
        {
            return elements.get(0).getEvaluation();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getElementsOfType(Class<T> type)
    {
        return (List<T>) this.getIdentityOrObservationOrImages().stream()
                .filter((e) -> type.isAssignableFrom(e.getClass())).collect(Collectors.toList());
    }
}
