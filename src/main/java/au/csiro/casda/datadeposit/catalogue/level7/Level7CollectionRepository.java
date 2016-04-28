package au.csiro.casda.datadeposit.catalogue.level7;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import au.csiro.casda.entity.observation.Level7Collection;

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
 * JPA Repository declaration.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@Repository
public interface Level7CollectionRepository extends CrudRepository<Level7Collection, Long>
{
    /**
     * Finds a level 7 collection by its collection id.
     * 
     * @param dapCollectionId
     *            the dap collection id
     * @return the matching level 7 collection record, or null.
     */
    public Level7Collection findByDapCollectionId(long dapCollectionId);
}
