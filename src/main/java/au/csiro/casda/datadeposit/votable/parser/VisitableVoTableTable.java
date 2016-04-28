package au.csiro.casda.datadeposit.votable.parser;

import javax.xml.bind.JAXBElement;

import net.ivoa.vo.Data;
import net.ivoa.vo.Table;
import net.ivoa.vo.Tr;

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
 * Extension of JAXB-generated {@link Table} that implements @{link VisitableVoTableElement}.
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
public class VisitableVoTableTable extends Table implements VisitableVoTableElement
{

    /** {@inheritDoc} */
    @Override
    public void accept(VoTableElementVisitor visitor)
    {
        for (JAXBElement<?> element : this.getContent())
        {
            switch (element.getName().getLocalPart())
            {
            case "DESCRIPTION":
                // Ignore
                break;
            case "PARAM":
                ((VisitableVoTableParam) element.getValue()).accept(visitor);
                break;
            case "FIELD":
                ((VisitableVoTableField) element.getValue()).accept(visitor);
                break;
            case "DATA":
                for (Tr tr : ((Data) element.getValue()).getTABLEDATA().getTR())
                {
                    ((VisitableVoTableRow) tr).accept(visitor);
                }
                break;
            default:
                throw new RuntimeException(
                        "Can only process DESCRIPTION, PARAM, FIELD, and DATA elements of the RESOURCE element");
            }
        }
    }

}
