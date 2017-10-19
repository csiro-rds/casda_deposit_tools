package au.csiro.casda.datadeposit.catalogue.continuum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor;
import au.csiro.casda.datadeposit.observation.jpa.repository.ContinuumComponentRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableField;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableParam;
import au.csiro.casda.datadeposit.votable.parser.FieldConstraint;
import au.csiro.casda.datadeposit.votable.parser.ParamConstraint;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.sourcedetect.ContinuumComponent;
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
 * Extension of {@link AbstractCatalogueVoTableVisitor} that specialises it to visit a Continuum Catalogue VOTABLE in
 * preparation for writing to a database.
 * <p>
 * TODO: Document how this writes to the database once that's implemented.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class ContinuumComponentVoTableVisitor extends AbstractCatalogueVoTableVisitor
{

    private static final String CONTINUUM_METADATA_RESOURCE_PATH = "schemas/continuum_component_metadata.yml";

    private static final List<ParamConstraint> PARAM_CONSTRAINTS = new ArrayList<>();

    private static final List<FieldConstraint> FIELD_CONSTRAINTS = new ArrayList<>();

    static
    {
        loadConstraintsFile(CONTINUUM_METADATA_RESOURCE_PATH, PARAM_CONSTRAINTS, FIELD_CONSTRAINTS);
    }

    private static final Map<String, BiConsumer<ContinuumComponent, String>> APPLIERS;
    static
    {
        APPLIERS = new HashMap<>();
        APPLIERS.put("island_id", (continuumComponent, value) -> continuumComponent.setIslandId(value));
        APPLIERS.put("component_id", (continuumComponent, value) -> continuumComponent.setComponentId(value));
        APPLIERS.put("component_name", (continuumComponent, value) -> continuumComponent.setComponentName(value));
        APPLIERS.put("ra_hms_cont", (continuumComponent, value) -> continuumComponent.setRaHmsCont(value));
        APPLIERS.put("dec_dms_cont", (continuumComponent, value) -> continuumComponent.setDecDmsCont(value));
        APPLIERS.put("ra_deg_cont",
                (continuumComponent, value) -> continuumComponent.setRaDegCont(Double.parseDouble(value)));
        APPLIERS.put("dec_deg_cont",
                (continuumComponent, value) -> continuumComponent.setDecDegCont(Double.parseDouble(value)));
        APPLIERS.put("ra_err", (continuumComponent, value) -> continuumComponent.setRaErr(Float.parseFloat(value)));
        APPLIERS.put("dec_err", (continuumComponent, value) -> continuumComponent.setDecErr(Float.parseFloat(value)));
        APPLIERS.put("freq", (continuumComponent, value) -> continuumComponent.setFreq(Float.parseFloat(value)));
        APPLIERS.put("flux_peak",
                (continuumComponent, value) -> continuumComponent.setFluxPeak(Float.parseFloat(value)));
        APPLIERS.put("flux_peak_err",
                (continuumComponent, value) -> continuumComponent.setFluxPeakErr(Float.parseFloat(value)));
        APPLIERS.put("flux_int", (continuumComponent, value) -> continuumComponent.setFluxInt(Float.parseFloat(value)));
        APPLIERS.put("flux_int_err",
                (continuumComponent, value) -> continuumComponent.setFluxIntErr(Float.parseFloat(value)));
        APPLIERS.put("maj_axis", (continuumComponent, value) -> continuumComponent.setMajAxis(Float.parseFloat(value)));
        APPLIERS.put("min_axis", (continuumComponent, value) -> continuumComponent.setMinAxis(Float.parseFloat(value)));
        APPLIERS.put("pos_ang", (continuumComponent, value) -> continuumComponent.setPosAng(Float.parseFloat(value)));
        APPLIERS.put("maj_axis_err",
                (continuumComponent, value) -> continuumComponent.setMajAxisErr(Float.parseFloat(value)));
        APPLIERS.put("min_axis_err",
                (continuumComponent, value) -> continuumComponent.setMinAxisErr(Float.parseFloat(value)));
        APPLIERS.put("pos_ang_err",
                (continuumComponent, value) -> continuumComponent.setPosAngErr(Float.parseFloat(value)));
        APPLIERS.put("maj_axis_deconv",
                (continuumComponent, value) -> continuumComponent.setMajAxisDeconv(Float.parseFloat(value)));
        APPLIERS.put("min_axis_deconv",
                (continuumComponent, value) -> continuumComponent.setMinAxisDeconv(Float.parseFloat(value)));
        APPLIERS.put("pos_ang_deconv",
                (continuumComponent, value) -> continuumComponent.setPosAngDeconv(Float.parseFloat(value)));
        APPLIERS.put("maj_axis_deconv_err",
                (continuumComponent, value) -> continuumComponent.setMajAxisDeconvErr(Float.parseFloat(value)));
        APPLIERS.put("min_axis_deconv_err",
                (continuumComponent, value) -> continuumComponent.setMinAxisDeconvErr(Float.parseFloat(value)));
        APPLIERS.put("pos_ang_deconv_err",
                (continuumComponent, value) -> continuumComponent.setPosAngDeconvErr(Float.parseFloat(value)));
        APPLIERS.put("chi_squared_fit",
                (continuumComponent, value) -> continuumComponent.setChiSquaredFit(Float.parseFloat(value)));
        APPLIERS.put("rms_fit_gauss",
                (continuumComponent, value) -> continuumComponent.setRmsFitGauss(Float.parseFloat(value)));
        APPLIERS.put("spectral_index",
                (continuumComponent, value) -> continuumComponent.setSpectralIndex(Float.parseFloat(value)));
        APPLIERS.put("spectral_curvature",
                (continuumComponent, value) -> continuumComponent.setSpectralCurvature(Float.parseFloat(value)));
        APPLIERS.put("spectral_index_err",
                (continuumComponent, value) -> continuumComponent.setSpectralIndexErr(Float.parseFloat(value)));
        APPLIERS.put("spectral_curvature_err",
                (continuumComponent, value) -> continuumComponent.setSpectralCurvatureErr(Float.parseFloat(value)));
        APPLIERS.put("rms_image",
                (continuumComponent, value) -> continuumComponent.setRmsImage(Float.parseFloat(value)));
        APPLIERS.put("has_siblings", 
                (continuumComponent, value) -> continuumComponent.setHasSiblings(Short.parseShort(value)));
        APPLIERS.put("fit_is_estimate", 
                (continuumComponent, value) -> continuumComponent.setFitIsEstimate(Short.parseShort(value)));
        // We have two aliases for the flag_c3 field for backwards compatibility
        APPLIERS.put("flag_c3", (continuumComponent, value) -> continuumComponent.setFlagC3(Short.parseShort(value)));
        APPLIERS.put("spectral_index_from_tt",
                (continuumComponent, value) -> continuumComponent.setFlagC3(Short.parseShort(value)));
        APPLIERS.put("flag_c4", (continuumComponent, value) -> continuumComponent.setFlagC4(Short.parseShort(value)));
        APPLIERS.put("comment", (continuumComponent, value) -> continuumComponent.setComment(value));
    }

    private ContinuumComponentRepository continuumComponentRepository;

    /**
     * Constructs a ContinuumComponentVoTableVisitor that uses the given ContinuumComponentRepository to perform any
     * operations associated with ContinuumComponent persistence.
     * 
     * @param continuumComponentRepository
     *            a ContinuumComponentRepository
     */
    public ContinuumComponentVoTableVisitor(ContinuumComponentRepository continuumComponentRepository)
    {
        this.continuumComponentRepository = continuumComponentRepository;
    }

    /**
     * Implementation of template method
     * {@link au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor#getParamConstraints()} that provides
     * metadata about the specific VOTABLE Param's that are acceptable for the Continuum Catalogue data.
     * 
     * @return a Map of {@link VoTableParamDescription>s keyed by the description's name
     */
    @Override
    protected List<ParamConstraint> getParamConstraints()
    {
        return PARAM_CONSTRAINTS;
    }

    /**
     * Implementation of template method
     * {@link au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor#getFieldConstraints()} that provides
     * metadata about the specific VOTABLE Field's that are acceptable for the Continuum Catalogue data.
     * 
     * @return a Map of {@link VoTableFieldDescription>s keyed by the description's name
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
     * saves a ContinuumComponent instance for the provided VoTable row.
     * 
     * @param row
     *            a Map of the Tds in a VOTABLE row, keyed by their corresponding Field
     */
    @Override
    protected void processRow(Map<VisitableVoTableField, String> row)
    {
        ContinuumComponent continuumComponent = new ContinuumComponent();
        continuumComponent.setCatalogue(getCatalogue());
        continuumComponent.setSbid(((Observation) getCatalogue().getParent()).getSbid());
        continuumComponent.setOtherSbids(formatOtherSbids(((Observation) getCatalogue().getParent()).getSbids()));
        continuumComponent.setProjectId(getCatalogue().getProject().getId());
        for (VisitableVoTableField field : row.keySet())
        {
            if (row.get(field) != null)
            {
                BiConsumer<ContinuumComponent, String> applier = APPLIERS.get(field.getName());
                if (applier != null) // Not all fields will be written to the ContinuumComponent
                {
                    applier.accept(continuumComponent, row.get(field));
                }
            }
        }
        
        double wavelen = AstroConversion.frequencyMhzToWavelength(continuumComponent.getFreq());
        Catalogue catalogue = getCatalogue();
        if (wavelen > 0 && (catalogue.getEmMin() == null || wavelen < catalogue.getEmMin()))
        {
            catalogue.setEmMin(wavelen);
        }
        if (wavelen > 0 && (catalogue.getEmMax() == null || wavelen > catalogue.getEmMax()))
        {
            catalogue.setEmMax(wavelen);
        }
        
        this.continuumComponentRepository.save(continuumComponent);
    }

    @Override
    public long getCatalogueEntriesCount(Catalogue catalogue)
    {
        return this.continuumComponentRepository.countByCatalogue(this.getCatalogue());
    }

    @Override
    protected void processSpecificParams(Collection<VisitableVoTableParam> params)
    {
        Optional<VisitableVoTableParam> param = getParamWithName(params, "Reference frequency");
        if (param.isPresent() && !hasErrorsForParam(param.get()) && param.get().getConvertedValue() != null)
        {
            float freqRef = Float.parseFloat(param.get().getConvertedValue());
            getCatalogue().setFreqRef(freqRef);
        }
    }

}
