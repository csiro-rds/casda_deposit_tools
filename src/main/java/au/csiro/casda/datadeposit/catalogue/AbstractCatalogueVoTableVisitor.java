package au.csiro.casda.datadeposit.catalogue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableParam;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.ImageCube;
import au.csiro.casda.entity.observation.Observation;
import reactor.util.CollectionUtils;

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
 * Abstract base class for VoTableVisitors that work with a Catalogue.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public abstract class AbstractCatalogueVoTableVisitor extends AbstractVoTableElementVisitor
{
    private Catalogue catalogue;

    /**
     * Sets the Catalogue that the Continuum data belongs to.
     * 
     * @param catalogue
     *            a Catalogue
     */
    public void setCatalogue(Catalogue catalogue)
    {
        if (catalogue == null)
        {
            throw new IllegalArgumentException("expected catalogue != null");
        }
        long catalogueEntriesCount = this.getCatalogueEntriesCount(catalogue);
        if (catalogueEntriesCount > 0)
        {
            throw new IllegalArgumentException("expected catalogue " + catalogue + " to have no entries but had "
                    + catalogueEntriesCount);
        }
        this.catalogue = catalogue;
    }

    /**
     * @return the Catalogue that the continuum data belongs to.
     */
    public Catalogue getCatalogue()
    {
        return catalogue;
    }

    /**
     * Returns the number of entries in the given catalogue.
     * 
     * @param catalogue
     *            a Catalogue
     * @return a number
     */
    public abstract long getCatalogueEntriesCount(Catalogue catalogue);

    /**
     * Do basic verification of the required params. This method checks that the reference image is present.
     * 
     * @param params
     *            a Map of the Params in the VOTABLE, keyed by their name
     */
    protected final void processParams(Collection<VisitableVoTableParam> params)
    {
        Observation observation = (Observation) getCatalogue().getParent();
        Optional<VisitableVoTableParam> param = getParamWithName(params, "imageFile");
        if (param.isPresent() && !hasErrorsForParam(param.get()))
        {
            ((Consumer<VisitableVoTableParam>) (p) -> {
                String imageFileName = p.getConvertedValue();
                Object[] results =
                        observation.getImageCubes().stream().filter((i) -> i.getFilename().equals(imageFileName))
                                .toArray();
                switch (results.length)
                {
                case 0:
                    recordParamError(
                            "imageFile",
                            new MalformedVoTableException(this, VisitorAction.VISIT_PARAM, p, String.format(
                                    "value '%s' does not match any image in the observation", imageFileName)));
                    break;
                case 1:
                    ImageCube imageCube = (ImageCube) results[0];
                    getCatalogue().setImageCube(imageCube);
                    break;
                default:
                    throw new RuntimeException(String.format(
                            "Found more than one ImageCube with the same filename '%s'", imageFileName));
                }
            }).accept(param.get());
        }
        processSpecificParams(params);
    }

    /**
     * Child classes may override this method to process any param values specific to their catalogue type.
     * 
     * @param params
     *            a Map of the Params in the VOTABLE, keyed by their name
     */
    protected void processSpecificParams(Collection<VisitableVoTableParam> params)
    {
    }

	/**
     * Format list of Integer sbids into a String
     * @param sbids
     * 			The list of sbids
     * @return String format of sbids
     */
    protected String formatOtherSbids(List<Integer> sbids)
    {    	
    	if(CollectionUtils.isEmpty(sbids))
    	{
    		return "";
    	}
    	return sbids.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

}
