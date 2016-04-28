package au.csiro.casda.datadeposit.catalogue.continuum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor;
import au.csiro.casda.datadeposit.observation.jpa.repository.ContinuumIslandRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableField;
import au.csiro.casda.datadeposit.votable.parser.FieldConstraint;
import au.csiro.casda.datadeposit.votable.parser.ParamConstraint;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.sourcedetect.ContinuumIsland;
import au.csiro.util.AstroConversion;

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
 * Extension of {@link AbstractCatalogueVoTableVisitor} that specialises it to visit a Continuum Island Catalogue
 * VOTABLE in preparation for writing to a database.
 * <p>
 * TODO: Document how this writes to the database once that's implemented.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class ContinuumIslandVoTableVisitor extends AbstractCatalogueVoTableVisitor
{

    private static final String CONTINUUM_CONSTRAINTS_RESOURCE_PATH = "schemas/continuum_island_metadata.yml";

    private static final List<ParamConstraint> PARAM_CONSTRAINTS = new ArrayList<>();

    private static final List<FieldConstraint> FIELD_CONSTRAINTS = new ArrayList<>();

    static
    {
        loadConstraintsFile(CONTINUUM_CONSTRAINTS_RESOURCE_PATH, PARAM_CONSTRAINTS, FIELD_CONSTRAINTS);
    }

    private static final Map<String, BiConsumer<ContinuumIsland, String>> APPLIERS;
    static
    {
        APPLIERS = new HashMap<>();
        APPLIERS.put("island_id", (continuumIsland, value) -> continuumIsland.setIslandId(value));
        APPLIERS.put("island_name", (continuumIsland, value) -> continuumIsland.setIslandName(value));
        APPLIERS.put("n_components",
                (continuumIsland, value) -> continuumIsland.setNumberComponents(Integer.parseInt(value)));
        APPLIERS.put("ra_hms_cont", (continuumIsland, value) -> continuumIsland.setRaHmsCont(value));
        APPLIERS.put("dec_dms_cont", (continuumIsland, value) -> continuumIsland.setDecDmsCont(value));
        APPLIERS.put("ra_deg_cont", (continuumIsland, value) -> continuumIsland.setRaDegCont(Double.parseDouble(value)));
        APPLIERS.put("dec_deg_cont",
                (continuumIsland, value) -> continuumIsland.setDecDegCont(Double.parseDouble(value)));
        APPLIERS.put("freq", (continuumIsland, value) -> continuumIsland.setFreq(Float.parseFloat(value)));
        APPLIERS.put("maj_axis", (continuumIsland, value) -> continuumIsland.setMajAxis(Float.parseFloat(value)));
        APPLIERS.put("min_axis", (continuumIsland, value) -> continuumIsland.setMinAxis(Float.parseFloat(value)));
        APPLIERS.put("pos_ang", (continuumIsland, value) -> continuumIsland.setPosAng(Float.parseFloat(value)));
        APPLIERS.put("flux_int", (continuumIsland, value) -> continuumIsland.setFluxInt(Float.parseFloat(value)));
        APPLIERS.put("flux_peak", (continuumIsland, value) -> continuumIsland.setFluxPeak(Float.parseFloat(value)));
        APPLIERS.put("x_min", (continuumIsland, value) -> continuumIsland.setXMin(Integer.parseInt(value)));
        APPLIERS.put("x_max", (continuumIsland, value) -> continuumIsland.setXMax(Integer.parseInt(value)));
        APPLIERS.put("y_min", (continuumIsland, value) -> continuumIsland.setYMin(Integer.parseInt(value)));
        APPLIERS.put("y_max", (continuumIsland, value) -> continuumIsland.setYMax(Integer.parseInt(value)));
        APPLIERS.put("n_pix", (continuumIsland, value) -> continuumIsland.setNumPixels(Integer.parseInt(value)));
        APPLIERS.put("x_ave", (continuumIsland, value) -> continuumIsland.setXAve(Float.parseFloat(value)));
        APPLIERS.put("y_ave", (continuumIsland, value) -> continuumIsland.setYAve(Float.parseFloat(value)));
        APPLIERS.put("x_cen", (continuumIsland, value) -> continuumIsland.setXCen(Float.parseFloat(value)));
        APPLIERS.put("y_cen", (continuumIsland, value) -> continuumIsland.setYCen(Float.parseFloat(value)));
        APPLIERS.put("x_peak", (continuumIsland, value) -> continuumIsland.setXPeak(Integer.parseInt(value)));
        APPLIERS.put("y_peak", (continuumIsland, value) -> continuumIsland.setYPeak(Integer.parseInt(value)));
        APPLIERS.put("flag_i1", (continuumIsland, value) -> continuumIsland.setFlagI1(Short.parseShort(value)));
        APPLIERS.put("flag_i2", (continuumIsland, value) -> continuumIsland.setFlagI2(Short.parseShort(value)));
        APPLIERS.put("flag_i3", (continuumIsland, value) -> continuumIsland.setFlagI3(Short.parseShort(value)));
        APPLIERS.put("flag_i4", (continuumIsland, value) -> continuumIsland.setFlagI4(Short.parseShort(value)));
        APPLIERS.put("comment", (continuumIsland, value) -> continuumIsland.setComment(value));
    }

    private ContinuumIslandRepository continuumIslandRepository;

    /**
     * Constructs a ContinuumIslandVoTableVisitor that uses the given ContinuumIslandRepository to perform any
     * operations associated with ContinuumIsland persistence.
     * 
     * @param continuumIslandRepository
     *            a ContinuumIslandRepository
     */
    public ContinuumIslandVoTableVisitor(ContinuumIslandRepository continuumIslandRepository)
    {
        this.continuumIslandRepository = continuumIslandRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<ParamConstraint> getParamConstraints()
    {
        return PARAM_CONSTRAINTS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<FieldConstraint> getFieldConstraints()
    {
        return FIELD_CONSTRAINTS;
    }

    /**
     * Implementation of template method
     * {@link au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor#processFields(Map)} that doesn't
     * do much right now.
     * 
     * @param fields
     *            a list of the Fields in the VOTABLE
     */
    @Override
    protected void processFields(List<VisitableVoTableField> fields)
    {
        // We don't really care about the fields as long as they match the defined ones.
    }

    /**
     * Implementation of template method
     * {@link au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor#processRow(Map)} that creates and
     * saves a ContinuumIsland instance for the provided VoTable row.
     * 
     * @param row
     *            a Map of the Tds in a VOTABLE row, keyed by their corresponding Field
     */
    @Override
    protected void processRow(Map<VisitableVoTableField, String> row)
    {
        ContinuumIsland continuumIsland = new ContinuumIsland();
        continuumIsland.setCatalogue(getCatalogue());
        continuumIsland.setSbid(((Observation) getCatalogue().getParent()).getSbid());
        continuumIsland.setOtherSbids(formatOtherSbids(((Observation) getCatalogue().getParent()).getSbids()));
        continuumIsland.setProjectId(getCatalogue().getProject().getId());
        for (VisitableVoTableField field : row.keySet())
        {
            if (row.get(field) != null)
            {
                BiConsumer<ContinuumIsland, String> applier = APPLIERS.get(field.getName());
                if (applier != null) // Not all fields will be written to the ContinuumComponent
                {
                    applier.accept(continuumIsland, row.get(field));
                }
            }
        }
        double wavelen = AstroConversion.frequencyMhzToWavelength(continuumIsland.getFreq());
        Catalogue catalogue = getCatalogue();
        if (wavelen > 0 && (catalogue.getEmMin() == null || wavelen < catalogue.getEmMin()))
        {
            catalogue.setEmMin(wavelen);
        }
        if (wavelen > 0 && (catalogue.getEmMax() == null || wavelen > catalogue.getEmMax()))
        {
            catalogue.setEmMax(wavelen);
        }
        
        this.continuumIslandRepository.save(continuumIsland);
    }

    @Override
    public long getCatalogueEntriesCount(Catalogue catalogue)
    {
        return this.continuumIslandRepository.countByCatalogue(this.getCatalogue());
    }

}
