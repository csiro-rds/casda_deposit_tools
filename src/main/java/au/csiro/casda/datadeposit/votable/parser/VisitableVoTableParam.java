package au.csiro.casda.datadeposit.votable.parser;

import net.ivoa.vo.Param;

import org.apache.commons.lang3.StringUtils;

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
 * Extension of JAXB-generated {@link Param} that implements @{link VisitableVoTableElement}.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class VisitableVoTableParam extends Param implements VisitableVoTableElement
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

    /**
     * Validate the attributes and value of this Param, ensuring that the attributes are constrained by the param's
     * datype and the param metadata (if present), and that the value conforms with the appropriate attributes defined
     * on this object and the optional metadata. eg: if the datatype is a char and an arraysize (or maxarraysize on the
     * metadata) is given then the value is checked to be no larger than the arraysize.
     * 
     * @param metadata
     *            an optional VoTableParamDescription
     * @throws FieldFormatException
     *             if any of this Param's attributes (except value) are not valid
     * @throws FieldValidationException
     *             if this Param's value is not valid
     */
    public void validate(ParamConstraint metadata) throws FieldFormatException, FieldValidationException
    {
        FieldDatatype fieldDatatype = null;
        try
        {
            fieldDatatype = FieldDatatype.valueOf(this.getDatatype().value().toUpperCase());
        }
        catch (IllegalArgumentException ex)
        {
            throw new FieldFormatException(String.format(
                    "Parser does not know how to handle the datatype '%s' on %s '%s'", this.getDatatype().value(),
                    "FIELD", this.getName()));
        }
        fieldDatatype.validateFieldAttributes(this, metadata);
        String value = this.getValue().trim();
        if (value.isEmpty())
        {
            return;
        }
        fieldDatatype.validateFieldValue(this, metadata, value);
    }

    /**
     * @param metadata
     *            an optional VoTableParamDescription
     * @return whether this param is valid see {@link #validate(ParamConstraint)}
     */
    public boolean isValid(ParamConstraint metadata)
    {
        try
        {
            this.validate(metadata);
            return true;
        }
        catch (FieldFormatException | FieldValidationException e)
        {
            return false;
        }
    }

    /**
     * @return the param's value converted to a String value that is suitable for conversion to the right datatype
     */
    public String getConvertedValue()
    {
        try
        {
            String value = FieldDatatype.getFieldDatatypeForField(this).convertFieldValue(this.getValue());
            return StringUtils.isBlank(value) ? null : value;
        }
        catch (FieldFormatException e)
        {
            throw new RuntimeException("Unexpected error occurred trying to retrieve FieldDatatype for Param - "
                    + "FieldDatatype would have been successfully resolved earlier.", e);
        }
    }
    
    /**
     * Convert to String
     * 
     * @return string representation of the object
     */
    public String toString()
    {
        return name + " : " + value;
    }
}
