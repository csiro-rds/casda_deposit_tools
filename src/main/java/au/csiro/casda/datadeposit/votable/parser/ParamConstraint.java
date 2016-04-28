package au.csiro.casda.datadeposit.votable.parser;

import net.ivoa.vo.Field;
import net.ivoa.vo.Param;

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
 * Extension of {@link FieldConstraint} for constraining Params.
 * <p>
 * Copyright 2014, CSIRO Australia
 * All rights reserved.
 */
public class ParamConstraint extends FieldConstraint
{
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFieldOfDescribedType(Field field)
    {
        return field instanceof Param;
    }

}
