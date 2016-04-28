package au.csiro.casda.datadeposit.catalogue.continuum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor;
import au.csiro.casda.datadeposit.observation.jpa.repository.PolarisationComponentRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableField;
import au.csiro.casda.datadeposit.votable.parser.FieldConstraint;
import au.csiro.casda.datadeposit.votable.parser.ParamConstraint;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.sourcedetect.PolarisationComponent;

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
 * Extension of {@link AbstractCatalogueVoTableVisitor} that specialises it to visit a Polarisation Catalogue VOTABLE in
 * preparation for writing to a database.
 * <p>
 * TODO: Document how this writes to the database once that's implemented.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class PolarisationComponentVoTableVisitor extends AbstractCatalogueVoTableVisitor
{
    private static final String POLARISATION_CONSTRAINTS_RESOURCE_PATH = "schemas/polarisation_component_metadata.yml";

    private static final List<ParamConstraint> PARAM_CONSTRAINTS = new ArrayList<>();

    private static final List<FieldConstraint> FIELD_CONSTRAINTS = new ArrayList<>();

    static
    {
        loadConstraintsFile(POLARISATION_CONSTRAINTS_RESOURCE_PATH, PARAM_CONSTRAINTS, FIELD_CONSTRAINTS);
    }
    private static final Map<String, BiConsumer<PolarisationComponent, String>> APPLIERS;
    static
    {
        APPLIERS = new HashMap<>();
        APPLIERS.put("sbid", (polarisationComponent, value) -> polarisationComponent.setSbid(Integer.parseInt(value)));
        APPLIERS.put("project_id",
                (polarisationComponent, value) -> polarisationComponent.setProjectId(Long.parseLong(value)));
        APPLIERS.put("component_id", (polarisationComponent, value) -> polarisationComponent.setComponentId(value));
        APPLIERS.put("component_name", (polarisationComponent, value) -> polarisationComponent.setComponentName(value));
        APPLIERS.put("ra_deg_cont",
                (polarisationComponent, value) -> polarisationComponent.setRaDegCont(Double.parseDouble(value)));
        APPLIERS.put("dec_deg_cont",
                (polarisationComponent, value) -> polarisationComponent.setDecDegCont(Double.parseDouble(value)));
        APPLIERS.put("flux_I_median",
                (polarisationComponent, value) -> polarisationComponent.setFluxIMedian(Double.parseDouble(value)));
        APPLIERS.put("flux_Q_median",
                (polarisationComponent, value) -> polarisationComponent.setFluxQMedian(Double.parseDouble(value)));
        APPLIERS.put("flux_U_median",
                (polarisationComponent, value) -> polarisationComponent.setFluxUMedian(Double.parseDouble(value)));
        APPLIERS.put("flux_V_median",
                (polarisationComponent, value) -> polarisationComponent.setFluxVMedian(Double.parseDouble(value)));
        APPLIERS.put("rms_I",
                (polarisationComponent, value) -> polarisationComponent.setRmsI(Double.parseDouble(value)));
        APPLIERS.put("rms_Q",
                (polarisationComponent, value) -> polarisationComponent.setRmsQ(Double.parseDouble(value)));
        APPLIERS.put("rms_U",
                (polarisationComponent, value) -> polarisationComponent.setRmsU(Double.parseDouble(value)));
        APPLIERS.put("rms_V",
                (polarisationComponent, value) -> polarisationComponent.setRmsV(Double.parseDouble(value)));
        APPLIERS.put("co_1", (polarisationComponent, value) -> polarisationComponent.setCo1(Double.parseDouble(value)));
        APPLIERS.put("co_2", (polarisationComponent, value) -> polarisationComponent.setCo2(Double.parseDouble(value)));
        APPLIERS.put("co_3", (polarisationComponent, value) -> polarisationComponent.setCo3(Double.parseDouble(value)));
        APPLIERS.put("co_4", (polarisationComponent, value) -> polarisationComponent.setCo4(Double.parseDouble(value)));
        APPLIERS.put("co_5", (polarisationComponent, value) -> polarisationComponent.setCo5(Double.parseDouble(value)));
        APPLIERS.put("lambda_ref_sq",
                (polarisationComponent, value) -> polarisationComponent.setLambdaRefSq(Double.parseDouble(value)));
        APPLIERS.put("rmsf_fwhm",
                (polarisationComponent, value) -> polarisationComponent.setRmsfFwhm(Double.parseDouble(value)));
        APPLIERS.put("pol_peak",
                (polarisationComponent, value) -> polarisationComponent.setPolPeak(Double.parseDouble(value)));
        APPLIERS.put("pol_peak_debias",
                (polarisationComponent, value) -> polarisationComponent.setPolPeakDebias(Double.parseDouble(value)));
        APPLIERS.put("pol_peak_err",
                (polarisationComponent, value) -> polarisationComponent.setPolPeakErr(Double.parseDouble(value)));
        APPLIERS.put("pol_peak_fit",
                (polarisationComponent, value) -> polarisationComponent.setPolPeakFit(Double.parseDouble(value)));
        APPLIERS.put("pol_peak_fit_debias",
                (polarisationComponent, value) -> polarisationComponent.setPolPeakFitDebias(Double.parseDouble(value)));
        APPLIERS.put("pol_peak_fit_err",
                (polarisationComponent, value) -> polarisationComponent.setPolPeakFitErr(Double.parseDouble(value)));
        APPLIERS.put("pol_peak_fit_snr",
                (polarisationComponent, value) -> polarisationComponent.setPolPeakFitSnr(Double.parseDouble(value)));
        APPLIERS.put("pol_peak_fit_snr_err",
                (polarisationComponent, value) -> polarisationComponent.setPolPeakFitSnrErr(Double.parseDouble(value)));
        APPLIERS.put("fd_peak",
                (polarisationComponent, value) -> polarisationComponent.setFdPeak(Double.parseDouble(value)));
        APPLIERS.put("fd_peak_err",
                (polarisationComponent, value) -> polarisationComponent.setFdPeakErr(Double.parseDouble(value)));
        APPLIERS.put("fd_peak_fit",
                (polarisationComponent, value) -> polarisationComponent.setFdPeakFit(Double.parseDouble(value)));
        APPLIERS.put("fd_peak_fit_err",
                (polarisationComponent, value) -> polarisationComponent.setFdPeakFitErr(Double.parseDouble(value)));
        APPLIERS.put("pol_ang_ref",
                (polarisationComponent, value) -> polarisationComponent.setPolAngRef(Double.parseDouble(value)));
        APPLIERS.put("pol_ang_ref_err",
                (polarisationComponent, value) -> polarisationComponent.setPolAngRefErr(Double.parseDouble(value)));
        APPLIERS.put("pol_ang_zero",
                (polarisationComponent, value) -> polarisationComponent.setPolAngZero(Double.parseDouble(value)));
        APPLIERS.put("pol_ang_zero_err",
                (polarisationComponent, value) -> polarisationComponent.setPolAngZeroErr(Double.parseDouble(value)));
        APPLIERS.put("pol_frac",
                (polarisationComponent, value) -> polarisationComponent.setPolFrac(Double.parseDouble(value)));
        APPLIERS.put("pol_frac_err",
                (polarisationComponent, value) -> polarisationComponent.setPolFracErr(Double.parseDouble(value)));
        APPLIERS.put("complex_1",
                (polarisationComponent, value) -> polarisationComponent.setComplex1(Double.parseDouble(value)));
        APPLIERS.put("complex_2",
                (polarisationComponent, value) -> polarisationComponent.setComplex2(Double.parseDouble(value)));
        APPLIERS.put("flag_p1", (polarisationComponent, value) -> polarisationComponent.setFlagP1("t".equals(value)));
        APPLIERS.put("flag_p2", (polarisationComponent, value) -> polarisationComponent.setFlagP2("t".equals(value)));
        APPLIERS.put("flag_p3", (polarisationComponent, value) -> polarisationComponent.setFlagP3(value));
        APPLIERS.put("flag_p4", (polarisationComponent, value) -> polarisationComponent.setFlagP4(value));
    }

    private PolarisationComponentRepository polarisationComponentRepository;

    /**
     * Constructs a PolarisationComponentVoTableVisitor that uses the given PolarisationComponentRepository to perform
     * any operations associated with PolarisationComponent persistence.
     * 
     * @param polarisationComponentRepository
     *            a polarisationComponentRepository
     */
    public PolarisationComponentVoTableVisitor(PolarisationComponentRepository polarisationComponentRepository)
    {
        this.polarisationComponentRepository = polarisationComponentRepository;
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
     * saves a PolarisationComponent instance for the provided VoTable row.
     * 
     * @param row
     *            a Map of the Tds in a VOTABLE row, keyed by their corresponding Field
     */
    @Override
    protected void processRow(Map<VisitableVoTableField, String> row)
    {
        PolarisationComponent polarisationComponent = new PolarisationComponent();
        polarisationComponent.setCatalogue(getCatalogue());
        polarisationComponent.setSbid(((Observation) getCatalogue().getParent()).getSbid());
        polarisationComponent.setOtherSbids(formatOtherSbids(((Observation) getCatalogue().getParent()).getSbids()));
        polarisationComponent.setProjectId(getCatalogue().getProject().getId());
        for (VisitableVoTableField field : row.keySet())
        {
            if (row.get(field) != null)
            {
                BiConsumer<PolarisationComponent, String> applier = APPLIERS.get(field.getName());
                if (applier != null) // Not all fields will be written to the ContinuumComponent
                {
                    applier.accept(polarisationComponent, row.get(field));
                }
            }
        }
        this.polarisationComponentRepository.save(polarisationComponent);
    }

    @Override
    public long getCatalogueEntriesCount(Catalogue catalogue)
    {
        return this.polarisationComponentRepository.countByCatalogue(this.getCatalogue());
    }
}
