package au.csiro.casda.datadeposit.observation.jpa.repository;

import org.springframework.data.repository.CrudRepository;
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
import au.csiro.casda.entity.observation.SpectralLineEmission;

/**
 * JPA Repository declaration.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public interface SpectralLineEmissionRepository
        extends CrudRepository<SpectralLineEmission, Long>, AbstractCatalogueEntryRepository<SpectralLineEmission>
{

}
