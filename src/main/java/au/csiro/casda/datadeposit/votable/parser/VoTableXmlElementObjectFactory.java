package au.csiro.casda.datadeposit.votable.parser;

import javax.xml.bind.annotation.XmlRegistry;

import net.ivoa.vo.Field;
import net.ivoa.vo.ObjectFactory;
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
 * Extension of JAXB-generated {@link ObjectFactory} that ensures specific
 * subclasses of the XML element classes are returned by the JAXB parser.
 * <p>
 * Copyright 2014, CSIRO Australia
 * All rights reserved.
 */
@XmlRegistry
public class VoTableXmlElementObjectFactory extends ObjectFactory
{
    /**
     * Returns a {@link VisitableVoTable} rather than a simple {@link VoTable}.
     * @return a VoTable
     */
    public VoTable createVoTable() 
    {
        return new VisitableVoTable();
    }
    
    /**
     * Returns a {@link VisitableVoTableTable} rather than a simple {@link Table}.
     * @return a Table
     */
    public Table createTable()
    {
        return new VisitableVoTableTable();
    }

    /**
     * Returns a {@link VisitableVoTableParam} rather than a simple {@link VoTable}.
     * @return a Param
     */
    public Param createParam() 
    {
        return new VisitableVoTableParam();
    }

    /**
     * Returns a {@link VisitableVoTableField} rather than a simple {@link VoTable}.
     * @return a Field
     */
    public Field createField() 
    {
        return new VisitableVoTableField();
    }

    /**
     * Returns a {@link VisitableVoTableRow} rather than a simple {@link VoTable}.
     * @return a Tr
     */
    public Tr createTr() 
    {
        return new VisitableVoTableRow();
    }

    /**
     * Returns a {@link VisitableVoTableCell} rather than a simple {@link VoTable}.
     * @return a Td
     */
    public Td createTd() 
    {
        return new VisitableVoTableCell();
    }
}
