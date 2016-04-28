package au.csiro.casda.datadeposit.votable.parser;

import java.util.List;

import net.ivoa.vo.Table;
import net.ivoa.vo.VoTable;

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
 * Extension of JAXB-generated {@link VoTable} that implements @{link VisitableVoTableElement}.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class VisitableVoTable extends VoTable implements VisitableVoTableElement
{
    /**
     * {@inheritDoc}
     * <p>
     * Implementation of {@link VisitableVoTableElement#accept}.
     * <p>
     * Elements of the VoTable will be visited as follows:
     * <ul>
     * <li>{@link net.ivoa.vo.Table}
     * <ul>
     * <li>{@link net.ivoa.vo.Param}s</li>
     * <li>{@link net.ivoa.vo.Field}s</li>
     * <li>{@link net.ivoa.vo.Tr}s
     * <ul>
     * <li>{@link net.ivoa.vo.Td}s</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * </ul>
     * This class also ensures that the VOTABLE only meets our existing capabilities for processing VOTABLEs.
     * 
     * @param visitor
     *            is the visitor
     */
    @Override
    public void accept(VoTableElementVisitor visitor)
    {
        visitor.visit(this);

        if (this.getRESOURCE().size() == 0)
        {
            throw new RuntimeException("VOTABLE is a missing a RESOURCE element");
        }
        if (this.getRESOURCE().size() > 1)
        {
            throw new RuntimeException("Cannot process more than one RESOURCE element in a VOTABLE");
        }
        List<Object> resourceElements = this.getRESOURCE().get(0).getLINKAndTABLEOrRESOURCE();
        if (resourceElements.stream().anyMatch(element -> !(element instanceof Table)) || resourceElements.size() > 1)
        {
            throw new RuntimeException("Can only process a single TABLE element under a RESOURCE");
        }
        Table table = (Table) resourceElements.get(0);
        visitor.visit(table);
        ((VisitableVoTableTable) table).accept(visitor);
        visitor.stop();
    }
}
