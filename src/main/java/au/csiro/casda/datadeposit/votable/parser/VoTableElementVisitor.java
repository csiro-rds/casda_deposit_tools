package au.csiro.casda.datadeposit.votable.parser;

import net.ivoa.vo.Field;
import net.ivoa.vo.Param;
import net.ivoa.vo.Table;
import net.ivoa.vo.Td;
import net.ivoa.vo.Tr;
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
 * Visitor interface for classes that wish to visit the elements of a VOTABLE.
 * <p>
 * A visitor will be passed to a 'root' {@link VisitableVoTableElement} via its
 * 'accept' method.  The root element and the various sub-elements will coordinate
 * to have the vistitor visit each of them in turn.
 * <p>
 * Copyright 2014, CSIRO Australia
 * All rights reserved.
 */
public interface VoTableElementVisitor
{

    /**
     * Requests the visitor to visit the root {@link VoTable}.
     * @param voTable
     *          the element to visit.
     */
    void visit(VoTable voTable);

    /**
     * Requests the visitor to visit a {@link Param} of the {@link VoTable}.
     * @param param
     *          the element to visit.
     */
    void visit(Param param);

    /**
     * Requests the visitor to visit a {@link Field} of the {@link VoTable}.
     * @param field
     *          the element to visit.
     */
    void visit(Field field);

    /**
     * Requests the visitor to visit a {@link Table} of the {@link VoTable}.
     * @param table
     *          the element to visit.
     */
    void visit(Table table);

    /**
     * Requests the visitor to visit a {@link Tr} of the {@link VoTable}.
     * @param tableRow
     *          the element to visit.
     */
    void visit(Tr tableRow);

    /**
     * Requests the visitor to visit a {@link Td} of a {@link Tr} of the {@link VoTable}.
     * <p>
     * Note: visitors are expected to keep track of both the table row
     * and the field associated with the cell.
     * @param tableCell
     *          the element to visit.
     */
    void visit(Td tableCell);

    /**
     * Notifies the visitor that there are no more elements to visit.
     * <p>
     * Implementations can use this method to finalise the visit.
     */
    void stop();
}
