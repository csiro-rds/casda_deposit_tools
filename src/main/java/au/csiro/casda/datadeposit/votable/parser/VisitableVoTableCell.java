package au.csiro.casda.datadeposit.votable.parser;

import net.ivoa.vo.Td;

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
 * Extension of JAXB-generated {@link Td} that implements @{link VisitableVoTableElement}.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class VisitableVoTableCell extends Td implements VisitableVoTableElement
{

    /**
     * {@inheritDoc}
     * <p>
     * Implementation of {@link VisitableVoTableElement#accept}.
     * <p>
     * See {@link VisitableVoTable} for the order in which elements are visited.
     * <p>
     * 
     * @param visitor
     *            the visitor to accept.
     */
    @Override
    public void accept(VoTableElementVisitor visitor)
    {
        visitor.visit(this);
    }

}
