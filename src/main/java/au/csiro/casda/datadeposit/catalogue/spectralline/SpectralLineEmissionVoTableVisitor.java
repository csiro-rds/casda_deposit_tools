package au.csiro.casda.datadeposit.catalogue.spectralline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor;
import au.csiro.casda.datadeposit.observation.jpa.repository.SpectralLineEmissionRepository;
import au.csiro.casda.datadeposit.votable.parser.FieldConstraint;
import au.csiro.casda.datadeposit.votable.parser.ParamConstraint;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableField;
import au.csiro.casda.entity.observation.Catalogue;
import au.csiro.casda.entity.observation.Observation;
import au.csiro.casda.entity.observation.SpectralLineEmission;
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
 * Extension of {@link AbstractCatalogueVoTableVisitor} that specialises it to visit a spectral line emission 
 * Catalogue VOTABLE in preparation for writing to a database.
 * 
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class SpectralLineEmissionVoTableVisitor extends AbstractCatalogueVoTableVisitor 
{
    
    private static final String SPECTRAL_LINE_ABSORPTION_CONSTRAINTS_RESOURCE_PATH 
    = "schemas/spectral_line_emission_metadata.yml";

    private static final List<ParamConstraint> PARAM_CONSTRAINTS = new ArrayList<>();

    private static final List<FieldConstraint> FIELD_CONSTRAINTS = new ArrayList<>();
    
    static
    {
        loadConstraintsFile(SPECTRAL_LINE_ABSORPTION_CONSTRAINTS_RESOURCE_PATH, PARAM_CONSTRAINTS, FIELD_CONSTRAINTS);
    }
    
    private static final Map<String, BiConsumer<SpectralLineEmission, String>> APPLIERS;
    static
    {
        APPLIERS = new HashMap<>();
        //Identifiers
        APPLIERS.put("id", (spectralLineEmission, value) -> spectralLineEmission.setId(Long.parseLong(value)));
        APPLIERS.put("object_id", (spectralLineEmission, value) -> spectralLineEmission.setObjectId(value));
        APPLIERS.put("object_name",
                (spectralLineEmission, value) -> spectralLineEmission.setObjectName(value));
        //Position Related
        APPLIERS.put("ra_hms_w",
                (spectralLineEmission, value) -> spectralLineEmission.setRaHmsW(value));
        APPLIERS.put("dec_dms_w",
                (spectralLineEmission, value) -> spectralLineEmission.setDecDmsW(value));       
        APPLIERS.put("ra_deg_w",
                (spectralLineEmission, value) -> spectralLineEmission.setRaDegW(Double.parseDouble(value)));
        APPLIERS.put("ra_deg_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setRaDegWErr(Float.parseFloat(value)));
        APPLIERS.put("dec_deg_w",
                (spectralLineEmission, value) -> spectralLineEmission.setDecDegW(Double.parseDouble(value)));
        APPLIERS.put("dec_deg_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setDecDegWErr(Float.parseFloat(value)));
        APPLIERS.put("ra_deg_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setRaDegUw(Double.parseDouble(value)));
        APPLIERS.put("ra_deg_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setRaDegUwErr(Float.parseFloat(value)));
        APPLIERS.put("dec_deg_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setDecDegUw(Double.parseDouble(value)));
        APPLIERS.put("dec_deg_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setDecDegUwErr(Float.parseFloat(value)));
        APPLIERS.put("glong_w",
                (spectralLineEmission, value) -> spectralLineEmission.setGlongW(Double.parseDouble(value)));
        APPLIERS.put("glong_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setGlongWErr(Float.parseFloat(value)));
        APPLIERS.put("glat_w",
                (spectralLineEmission, value) -> spectralLineEmission.setGlatW(Double.parseDouble(value)));
        APPLIERS.put("glat_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setGlatWErr(Float.parseFloat(value)));
        APPLIERS.put("glong_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setGlongUw(Double.parseDouble(value)));
        APPLIERS.put("glong_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setGlongUwErr(Float.parseFloat(value)));
        APPLIERS.put("glat_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setGlatUw(Double.parseDouble(value)));
        APPLIERS.put("glat_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setGlatUwErr(Float.parseFloat(value)));
        //Shape Related
        APPLIERS.put("maj_axis",
                (spectralLineEmission, value) -> spectralLineEmission.setMajAxis(Float.parseFloat(value)));
        APPLIERS.put("min_axis",
                (spectralLineEmission, value) -> spectralLineEmission.setMinAxis(Float.parseFloat(value)));
        APPLIERS.put("pos_ang",
                (spectralLineEmission, value) -> spectralLineEmission.setPosAng(Float.parseFloat(value)));
        APPLIERS.put("maj_axis_fit",
                (spectralLineEmission, value) -> spectralLineEmission.setMajAxisFit(Float.parseFloat(value)));
        APPLIERS.put("maj_axis_fit_err",
                (spectralLineEmission, value) -> spectralLineEmission.setMajAxisFitErr(Float.parseFloat(value)));
        APPLIERS.put("min_axis_fit",
                (spectralLineEmission, value) -> spectralLineEmission.setMinAxisFit(Float.parseFloat(value)));
        APPLIERS.put("min_axis_fit_err",
                (spectralLineEmission, value) -> spectralLineEmission.setMinAxisFitErr(Float.parseFloat(value)));
        APPLIERS.put("pos_ang_fit",
                (spectralLineEmission, value) -> spectralLineEmission.setPosAngFit(Float.parseFloat(value)));
        APPLIERS.put("pos_ang_fit_err",
                (spectralLineEmission, value) -> spectralLineEmission.setPosAngFitErr(Float.parseFloat(value)));
        APPLIERS.put("size_x",
                (spectralLineEmission, value) -> spectralLineEmission.setSizeX(Integer.parseInt(value)));
        APPLIERS.put("size_y",
                (spectralLineEmission, value) -> spectralLineEmission.setSizeY(Integer.parseInt(value)));
        APPLIERS.put("size_z",
                (spectralLineEmission, value) -> spectralLineEmission.setSizeZ(Integer.parseInt(value)));
        APPLIERS.put("n_vox",
                (spectralLineEmission, value) -> spectralLineEmission.setNVox(Integer.parseInt(value)));
        APPLIERS.put("asymmetry_2d",
                (spectralLineEmission, value) -> spectralLineEmission.setAsymmetry2d(Float.parseFloat(value)));
        APPLIERS.put("asymmetry_2d_err",
                (spectralLineEmission, value) -> spectralLineEmission.setAsymmetry2dErr(Float.parseFloat(value)));
        APPLIERS.put("asymmetry_3d",
                (spectralLineEmission, value) -> spectralLineEmission.setAsymmetry3d(Float.parseFloat(value)));
        APPLIERS.put("asymmetry_3d_err",
                (spectralLineEmission, value) -> spectralLineEmission.setAsymmetry3dErr(Float.parseFloat(value)));
        //SPECTRAL LOCATION (SIMPLE)
        APPLIERS.put("freq_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqUw(Double.parseDouble(value)));
        APPLIERS.put("freq_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqUwErr(Double.parseDouble(value)));
        APPLIERS.put("freq_w",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW(Double.parseDouble(value)));
        APPLIERS.put("freq_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqWErr(Double.parseDouble(value)));
        APPLIERS.put("freq_peak",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqPeak(Double.parseDouble(value)));
        APPLIERS.put("vel_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setVelUw(Float.parseFloat(value)));
        APPLIERS.put("vel_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelUwErr(Float.parseFloat(value)));
        APPLIERS.put("vel_w",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW(Float.parseFloat(value)));
        APPLIERS.put("vel_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelWErr(Float.parseFloat(value)));
        APPLIERS.put("vel_peak",
                (spectralLineEmission, value) -> spectralLineEmission.setVelPeak(Float.parseFloat(value)));
        //FLUX-RELATED (Simple)
        APPLIERS.put("integ_flux",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFlux(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_err",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxErr(Float.parseFloat(value)));
        APPLIERS.put("flux_voxel_min",
                (spectralLineEmission, value) -> spectralLineEmission.setFluxVoxelMin(Float.parseFloat(value)));
        APPLIERS.put("flux_voxel_max",
                (spectralLineEmission, value) -> spectralLineEmission.setFluxVoxelMax(Float.parseFloat(value)));
        APPLIERS.put("flux_voxel_mean",
                (spectralLineEmission, value) -> spectralLineEmission.setFluxVoxelMean(Float.parseFloat(value)));
        APPLIERS.put("flux_voxel_stddev",
                (spectralLineEmission, value) -> spectralLineEmission.setFluxVoxelStddev(Float.parseFloat(value)));
        APPLIERS.put("flux_voxel_rms",
                (spectralLineEmission, value) -> spectralLineEmission.setFluxVoxelRms(Float.parseFloat(value)));
        APPLIERS.put("rms_imagecube",
                (spectralLineEmission, value) -> spectralLineEmission.setRmsImagecube(Float.parseFloat(value)));
        //SPECTRAL WIDTHS 
        APPLIERS.put("w50_freq",
                (spectralLineEmission, value) -> spectralLineEmission.setW50Freq(Float.parseFloat(value)));
        APPLIERS.put("w50_freq_err",
                (spectralLineEmission, value) -> spectralLineEmission.setW50FreqErr(Float.parseFloat(value)));
        APPLIERS.put("cw50_freq",
                (spectralLineEmission, value) -> spectralLineEmission.setCw50Freq(Float.parseFloat(value)));
        APPLIERS.put("cw50_freq_err",
                (spectralLineEmission, value) -> spectralLineEmission.setCw50FreqErr(Float.parseFloat(value)));
        APPLIERS.put("w20_freq",
                (spectralLineEmission, value) -> spectralLineEmission.setW20Freq(Float.parseFloat(value)));
        APPLIERS.put("w20_freq_err",
                (spectralLineEmission, value) -> spectralLineEmission.setW20FreqErr(Float.parseFloat(value)));
        APPLIERS.put("cw20_freq",
                (spectralLineEmission, value) -> spectralLineEmission.setCw20Freq(Float.parseFloat(value)));
        APPLIERS.put("cw20_freq_err",
                (spectralLineEmission, value) -> spectralLineEmission.setCw20FreqErr(Float.parseFloat(value)));
        APPLIERS.put("w50_vel",
                (spectralLineEmission, value) -> spectralLineEmission.setW50Vel(Float.parseFloat(value)));
        APPLIERS.put("w50_vel_err",
                (spectralLineEmission, value) -> spectralLineEmission.setW50VelErr(Float.parseFloat(value)));
        APPLIERS.put("cw50_vel",
                (spectralLineEmission, value) -> spectralLineEmission.setCw50Vel(Float.parseFloat(value)));
        APPLIERS.put("cw50_vel_err",
                (spectralLineEmission, value) -> spectralLineEmission.setCw50VelErr(Float.parseFloat(value)));
        APPLIERS.put("w20_vel",
                (spectralLineEmission, value) -> spectralLineEmission.setW20Vel(Float.parseFloat(value)));
        APPLIERS.put("w20_vel_err",
                (spectralLineEmission, value) -> spectralLineEmission.setW20VelErr(Float.parseFloat(value)));
        APPLIERS.put("cw20_vel",
                (spectralLineEmission, value) -> spectralLineEmission.setCw20Vel(Float.parseFloat(value)));
        APPLIERS.put("cw20_vel_err",
                (spectralLineEmission, value) -> spectralLineEmission.setCw20VelErr(Float.parseFloat(value)));
        // SPECTRAL LOCATION (COMPLEX) 
        APPLIERS.put("freq_w50_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW50ClipUw(Double.parseDouble(value)));
        APPLIERS.put("freq_w50_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW50ClipUwErr(Double.parseDouble(value)));
        APPLIERS.put("freq_cw50_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw50ClipUw(Double.parseDouble(value)));
        APPLIERS.put("freq_cw50_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw50ClipUwErr(Double.parseDouble(value)));
        APPLIERS.put("freq_w20_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW20ClipUw(Double.parseDouble(value)));
        APPLIERS.put("freq_w20_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW20ClipUwErr(Double.parseDouble(value)));
        APPLIERS.put("freq_cw20_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw20ClipUw(Double.parseDouble(value)));
        APPLIERS.put("freq_cw20_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw20ClipUwErr(Double.parseDouble(value)));
        APPLIERS.put("vel_w50_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW50ClipUw(Float.parseFloat(value)));
        APPLIERS.put("vel_w50_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW50ClipUwErr(Float.parseFloat(value)));
        APPLIERS.put("vel_cw50_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw50ClipUw(Float.parseFloat(value)));
        APPLIERS.put("vel_cw50_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw50ClipUwErr(Float.parseFloat(value)));
        APPLIERS.put("vel_w20_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW20ClipUw(Float.parseFloat(value)));
        APPLIERS.put("vel_w20_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW20ClipUwErr(Float.parseFloat(value)));
        APPLIERS.put("vel_cw20_clip_uw",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw20ClipUw(Float.parseFloat(value)));
        APPLIERS.put("vel_cw20_clip_uw_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw20ClipUwErr(Float.parseFloat(value)));
        APPLIERS.put("freq_w50_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW50ClipW(Double.parseDouble(value)));
        APPLIERS.put("freq_w50_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW50ClipWErr(Double.parseDouble(value)));
        APPLIERS.put("freq_cw50_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw50ClipW(Double.parseDouble(value)));
        APPLIERS.put("freq_cw50_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw50ClipWErr(Double.parseDouble(value)));
        APPLIERS.put("freq_w20_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW20ClipW(Double.parseDouble(value)));
        APPLIERS.put("freq_w20_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqW20ClipWErr(Double.parseDouble(value)));
        APPLIERS.put("freq_cw20_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw20ClipW(Double.parseDouble(value)));
        APPLIERS.put("freq_cw20_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setFreqCw20ClipWErr(Double.parseDouble(value)));
        APPLIERS.put("vel_w50_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW50ClipW(Float.parseFloat(value)));
        APPLIERS.put("vel_w50_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW50ClipWErr(Float.parseFloat(value)));
        APPLIERS.put("vel_cw50_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw50ClipW(Float.parseFloat(value)));
        APPLIERS.put("vel_cw50_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw50ClipWErr(Float.parseFloat(value)));
        APPLIERS.put("vel_w20_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW20ClipW(Float.parseFloat(value)));
        APPLIERS.put("vel_w20_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelW20ClipWErr(Float.parseFloat(value)));
        APPLIERS.put("vel_cw20_clip_w",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw20ClipW(Float.parseFloat(value)));
        APPLIERS.put("vel_cw20_clip_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setVelCw20ClipWErr(Float.parseFloat(value)));
        //FLUX-RELATED (complex)
        APPLIERS.put("integ_flux_w50_clip",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxW50Clip(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_w50_clip_err",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxW50ClipErr(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_cw50_clip",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxCw50Clip(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_cw50_clip_err",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxCw50ClipErr(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_w20_clip",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxW20Clip(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_w20_clip_err",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxW20ClipErr(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_cw20_clip",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxCw20Clip(Float.parseFloat(value)));
        APPLIERS.put("integ_flux_cw20_clip_err",
                (spectralLineEmission, value) -> spectralLineEmission.setIntegFluxCw20ClipErr(Float.parseFloat(value)));
        //BUSY-FUNCTION PARAMETERS
        APPLIERS.put("bf_a",
                (spectralLineEmission, value) -> spectralLineEmission.setBfA(Float.parseFloat(value)));
        APPLIERS.put("bf_a_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfAErr(Float.parseFloat(value)));
        APPLIERS.put("bf_w",
                (spectralLineEmission, value) -> spectralLineEmission.setBfW(Double.parseDouble(value)));
        APPLIERS.put("bf_w_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfWErr(Double.parseDouble(value)));
        APPLIERS.put("bf_b1",
                (spectralLineEmission, value) -> spectralLineEmission.setBfB1(Float.parseFloat(value)));
        APPLIERS.put("bf_b1_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfB1Err(Float.parseFloat(value)));
        APPLIERS.put("bf_b2",
                (spectralLineEmission, value) -> spectralLineEmission.setBfB2(Float.parseFloat(value)));
        APPLIERS.put("bf_b2_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfB2Err(Float.parseFloat(value)));
        APPLIERS.put("bf_xe",
                (spectralLineEmission, value) -> spectralLineEmission.setBfXe(Double.parseDouble(value)));
        APPLIERS.put("bf_xe_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfXeErr(Double.parseDouble(value)));
        APPLIERS.put("bf_xp",
                (spectralLineEmission, value) -> spectralLineEmission.setBfXp(Double.parseDouble(value)));
        APPLIERS.put("bf_xp_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfXpErr(Double.parseDouble(value)));
        APPLIERS.put("bf_c",
                (spectralLineEmission, value) -> spectralLineEmission.setBfC(Float.parseFloat(value)));
        APPLIERS.put("bf_c_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfCErr(Float.parseFloat(value)));
        APPLIERS.put("bf_n",
                (spectralLineEmission, value) -> spectralLineEmission.setBfN(Float.parseFloat(value)));
        APPLIERS.put("bf_n_err",
                (spectralLineEmission, value) -> spectralLineEmission.setBfNErr(Float.parseFloat(value)));
        //FLAGS
        APPLIERS.put("flag_s1", (spectralLineEmission, value) 
                -> spectralLineEmission.setFlagS1(Integer.parseInt(value)));
        APPLIERS.put("flag_s2", (spectralLineEmission, value) 
                -> spectralLineEmission.setFlagS2(Integer.parseInt(value)));
        APPLIERS.put("flag_s3", (spectralLineEmission, value) 
                -> spectralLineEmission.setFlagS3(Integer.parseInt(value)));
    }
    
    private SpectralLineEmissionRepository spectralLineEmissionRepository;
    
    /**
     * Constructs a spectralLineEmissionVoTableVisitor that uses the given spectralLineEmissionRepository
     *  to perform any operations associated with spectralLineEmission persistence.
     * 
     * @param spectralLineEmissionRepository
     *            a spectralLineEmissionRepository
     */
    public SpectralLineEmissionVoTableVisitor(SpectralLineEmissionRepository spectralLineEmissionRepository)
    {
        this.spectralLineEmissionRepository = spectralLineEmissionRepository;
    }
    
    @Override
    public long getCatalogueEntriesCount(Catalogue catalogue) 
    {
        return this.spectralLineEmissionRepository.countByCatalogue(this.getCatalogue());
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
        SpectralLineEmission spectralLineEmission = new SpectralLineEmission();
        spectralLineEmission.setCatalogue(getCatalogue());
        spectralLineEmission.setSbid(((Observation) getCatalogue().getParent()).getSbid());
        spectralLineEmission.setOtherSbids(formatOtherSbids(((Observation) getCatalogue().getParent()).getSbids()));
        spectralLineEmission.setProjectId(getCatalogue().getProject().getId());
        for (VisitableVoTableField field : row.keySet())
        {
            if (row.get(field) != null)
            {
                BiConsumer<SpectralLineEmission, String> applier = APPLIERS.get(field.getName());
                if (applier != null) // Not all fields will be written to the ContinuumComponent
                {
                    applier.accept(spectralLineEmission, row.get(field));
                }
            }
        }
        
        double wavelen = AstroConversion.frequencyMhzToWavelength(spectralLineEmission.getFreqW());
        Catalogue catalogue = getCatalogue();
        if (wavelen > 0 && (catalogue.getEmMin() == null || wavelen < catalogue.getEmMin()))
        {
            catalogue.setEmMin(wavelen);
        }
        if (wavelen > 0 && (catalogue.getEmMax() == null || wavelen > catalogue.getEmMax()))
        {
            catalogue.setEmMax(wavelen);
        }
        
        this.spectralLineEmissionRepository.save(spectralLineEmission);
    }


}
