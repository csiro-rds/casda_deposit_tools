package au.csiro.casda.datadeposit;

/*
 * #%L
 * CSIRO Data Access Portal
 * %%
 * Copyright (C) 2010 - 2017 Commonwealth Scientific and Industrial Research Organisation (CSIRO) ABN 41 687 119 230.
 * %%
 * Licensed under the CSIRO Open Source License Agreement (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file.
 * #L%
 */

/**
 * An enumeration of the possible parent objects for artefacts.
 * <p>
 * Copyright 2017, CSIRO Australia. All rights reserved.
 */
public enum ParentType
{
    /** Parent is a collection of derived catalogues or images/spectra/moment maps. */
    LEVEL_7_COLLECTION (CommonCommandLineArguments.LEVEL7_PARENT_TYPE),
    
    /** Parent is a level5/6 observation. */
    OBSERVATION(CommonCommandLineArguments.OBSERVATION_PARENT_TYPE);
    
    private String key;

    private ParentType(String key)
    {
        this.key = key;
    }
    
    
    /**
     * Find a parent type matching the string key. 
     * @param typeKey The key to be searched for.
     * @return The parent type, or null if none are found.
     */
    public static ParentType findParentType(String typeKey)
    {
        for (ParentType parentType : ParentType.values())
        {
            if (parentType.key.equalsIgnoreCase(typeKey))
            {
                return parentType;
            }
        }
        
        return null;
    }
}
