package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import au.csiro.casda.entity.observation.Project;

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
 * JPA Repository declaration. Copyright 2014, CSIRO Australia All rights reserved.
 */
@Repository
public interface ProjectRepository extends CrudRepository<Project, Long>
{
    /**
     * Gets a Project by Opal Code
     * 
     * @param opalCode
     *            the opalCode to look for
     * @return a Project (or null if no matching Project exists)
     */
    Project findByOpalCode(String opalCode);

    /**
     * Gets a Project by shortName. There should be a maximum of one.
     * 
     * @param shortName
     *            the shortName to look for
     * @return a Project (or null if no matching Project exists)
     */
    Project findByShortName(String shortName);

}
