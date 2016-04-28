package au.csiro.casda.datadeposit.catalogue.spectralline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectralLineAbsorptionRepository;
import au.csiro.casda.datadeposit.votable.parser.FieldConstraint;
import au.csiro.casda.datadeposit.votable.parser.ParamConstraint;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableField;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.SpectralLineAbsorption;
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
 * Extension of {@link AbstractCatalogueVoTableVisitor} that specialises it to visit a spectral line absorption 
 * Catalogue VOTABLE in preparation for writing to a database.
 * 
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class SpectralLineAbsorptionVoTableVisitor extends AbstractCatalogueVoTableVisitor 
{
	
    private static final String SPECTRAL_LINE_ABSORPTION_CONSTRAINTS_RESOURCE_PATH 
    = "schemas/spectral_line_absorption_metadata.yml";

    private static final List<ParamConstraint> PARAM_CONSTRAINTS = new ArrayList<>();

    private static final List<FieldConstraint> FIELD_CONSTRAINTS = new ArrayList<>();
    
    static
    {
        loadConstraintsFile(SPECTRAL_LINE_ABSORPTION_CONSTRAINTS_RESOURCE_PATH, PARAM_CONSTRAINTS, FIELD_CONSTRAINTS);
    }
    
    private static final Map<String, BiConsumer<SpectralLineAbsorption, String>> APPLIERS;
    static
    {
        APPLIERS = new HashMap<>();
        APPLIERS.put("id", (spectralLineAbsorption, value) -> spectralLineAbsorption.setId(Long.parseLong(value)));
        APPLIERS.put("image_id", (spectralLineAbsorption, value) -> spectralLineAbsorption.setImageId(value));
        APPLIERS.put("date_time_ut", (spectralLineAbsorption, value) -> spectralLineAbsorption.setDateTimeUt(value));
        APPLIERS.put("cont_component_id", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setContComponentId(value));
        APPLIERS.put("cont_flux", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setContFlux(Float.parseFloat(value)));
        APPLIERS.put("object_id", (spectralLineAbsorption, value) -> spectralLineAbsorption.setObjectId(value));
        APPLIERS.put("object_name",
                (spectralLineAbsorption, value) -> spectralLineAbsorption.setObjectName(value));
        APPLIERS.put("ra_hms_cont", (spectralLineAbsorption, value) -> spectralLineAbsorption.setRaHmsCont(value));
        APPLIERS.put("dec_dms_cont", (spectralLineAbsorption, value) -> spectralLineAbsorption.setDecDmsCont(value));
        APPLIERS.put("ra_deg_cont", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setRaDegCont(Double.parseDouble(value)));
        APPLIERS.put("dec_deg_cont", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setDecDegCont(Double.parseDouble(value)));
        APPLIERS.put("ra_deg_cont_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setRaDegContErr(Float.parseFloat(value)));
        APPLIERS.put("dec_deg_cont_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setDecDegContErr(Float.parseFloat(value)));
        APPLIERS.put("freq_uw", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setFreqUw(Float.parseFloat(value)));
        APPLIERS.put("freq_uw_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setFreqUwErr(Float.parseFloat(value)));
        APPLIERS.put("freq_w", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setFreqW(Float.parseFloat(value)));
        APPLIERS.put("freq_w_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setFreqWErr(Float.parseFloat(value)));
        APPLIERS.put("z_hi_uw", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setZHiUw(Float.parseFloat(value)));
        APPLIERS.put("z_hi_uw_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setZHiUwErr(Float.parseFloat(value)));
        APPLIERS.put("z_hi_w", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setZHiW(Float.parseFloat(value)));
        APPLIERS.put("z_hi_w_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setZHiWErr(Float.parseFloat(value)));
        APPLIERS.put("z_hi_peak", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setZHiPeak(Float.parseFloat(value)));
        APPLIERS.put("z_hi_peak_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setZHiPeakErr(Float.parseFloat(value)));
        APPLIERS.put("w50", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setW50(Float.parseFloat(value)));
        APPLIERS.put("w50_err", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setW50Err(Float.parseFloat(value)));
        APPLIERS.put("w20", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setW20(Float.parseFloat(value)));
        APPLIERS.put("w20_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setW20Err(Float.parseFloat(value)));
        APPLIERS.put("rms_imagecube", (spectralLineAbsorption, value) 
												-> spectralLineAbsorption.setRmsImagecube(Float.parseFloat(value)));
        APPLIERS.put("opt_depth_peak", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setOptDepthPeak(Float.parseFloat(value)));
        APPLIERS.put("opt_depth_peak_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setOptDepthPeakErr(Float.parseFloat(value)));
        APPLIERS.put("opt_depth_int", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setOptDepthInt(Float.parseFloat(value)));
        APPLIERS.put("opt_depth_int_err", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setOptDepthIntErr(Float.parseFloat(value)));
        APPLIERS.put("flag_s1", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setFlagS1(Short.parseShort(value)));
        APPLIERS.put("flag_s2", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setFlagS2(Short.parseShort(value)));
        APPLIERS.put("flag_s3", (spectralLineAbsorption, value) 
        										-> spectralLineAbsorption.setFlagS3(Short.parseShort(value)));
    }
    
    private SpectralLineAbsorptionRepository spectralLineAbsorptionRepository;
    
    /**
     * Constructs a spectralLineAbsorptionVoTableVisitor that uses the given spectralLineAbsorptionRepository
     *  to perform any operations associated with spectralLineAbsorption persistence.
     * 
     * @param spectralLineAbsorptionRepository
     *            a spectralLineAbsorptionRepository
     */
    public SpectralLineAbsorptionVoTableVisitor(SpectralLineAbsorptionRepository spectralLineAbsorptionRepository)
    {
        this.spectralLineAbsorptionRepository = spectralLineAbsorptionRepository;
    }
    
	@Override
	public long getCatalogueEntriesCount(Catalogue catalogue) 
	{
		return this.spectralLineAbsorptionRepository.countByCatalogue(this.getCatalogue());
	}

	@Override
	protected List<ParamConstraint> getParamConstraints() 
	{
		return PARAM_CONSTRAINTS;
	}

	@Override
	protected List<FieldConstraint> getFieldConstraints() 
	{
		return FIELD_CONSTRAINTS;
	}

	@Override
	protected void processFields(List<VisitableVoTableField> fields)
	{
		// We don't really care about the fields as long as they match the defined ones.
	}

	@Override
	protected void processRow(Map<VisitableVoTableField, String> row) 
	{
		SpectralLineAbsorption spectralLineAbsorption = new SpectralLineAbsorption();
		Catalogue catalogue = getCatalogue();
        spectralLineAbsorption.setCatalogue(catalogue);
        spectralLineAbsorption.setSbid(((Observation) getCatalogue().getParent()).getSbid());
        spectralLineAbsorption.setOtherSbids(formatOtherSbids(((Observation) getCatalogue().getParent()).getSbids()));
        spectralLineAbsorption.setProjectId(getCatalogue().getProject().getId());
        for (VisitableVoTableField field : row.keySet())
        {
            if (row.get(field) != null)
            {
                BiConsumer<SpectralLineAbsorption, String> applier = APPLIERS.get(field.getName());
                if (applier != null) // Not all fields will be written to the ContinuumComponent
                {
                    applier.accept(spectralLineAbsorption, row.get(field));
                }
            }
        }
        double wavelen = AstroConversion.frequencyMhzToWavelength(spectralLineAbsorption.getFreqW());
        if (wavelen > 0 && (catalogue.getEmMin() == null || wavelen < catalogue.getEmMin()))
        {
            catalogue.setEmMin(wavelen);
        }
        if (wavelen > 0 && (catalogue.getEmMax() == null || wavelen > catalogue.getEmMax()))
        {
            catalogue.setEmMax(wavelen);
        }
        this.spectralLineAbsorptionRepository.save(spectralLineAbsorption);
	}


}
