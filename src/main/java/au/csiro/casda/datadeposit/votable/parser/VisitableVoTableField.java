package au.csiro.casda.datadeposit.votable.parser;

import net.ivoa.vo.Field;
import net.ivoa.vo.Td;

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
 * Extension of JAXB-generated {@link Field} that implements @{link VisitableVoTableElement}.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class VisitableVoTableField extends Field implements VisitableVoTableElement
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
     * Validate the attributes of this Field, ensuring that the attributes are constrained by the field's datype and the
     * field constraint data (if present).
     * 
     * @param constraint
     *            an optional FieldConstraint
     * @throws FieldFormatException
     *             if the Field is not valid
     */
    public void validate(FieldConstraint constraint) throws FieldFormatException
    {
        FieldDatatype.getFieldDatatypeForField(this).validateFieldAttributes(this, constraint);
    }

    /**
     * Validates the given cell's value to make sure that conforms with the appropriate attributes defined on this
     * object and the (optional) constraint. eg: if the datatype is a char and an arraysize is given then the value is
     * checked to be no larger than the arraysize (or maxarraysize if no arraysize is defined or is purely variable).
     * 
     * @param cell
     *            the cell whose value is to be validated
     * @param constraint
     *            an optional FieldConstraint
     * @throws FieldValidationException
     *             if cell's value is not valid
     */
    public void validateCell(Td cell, FieldConstraint constraint) throws FieldValidationException
    {
        String value = cell.getValue().trim();
        if (value.isEmpty())
        {
            return;
        }
        try
        {
            FieldDatatype.getFieldDatatypeForField(this).validateFieldValue(this, constraint, value);
        }
        catch (FieldFormatException e)
        {
            throw new RuntimeException("Unexpected error occurred trying to retrieve FieldDatatype for Field - "
                    + "FieldDatatype would have been successfully resolved earlier.", e);
        }
    }

    /**
     * @param constraint
     *            an optional FieldConstraint
     * @return whether this field is valid see {@link #validate(FieldConstraint)}
     */
    public boolean isValid(FieldConstraint constraint)
    {
        try
        {
            this.validate(constraint);
            return true;
        }
        catch (FieldFormatException ex)
        {
            return false;
        }
    }

    /**
     * @param cell
     *            the Td cell to validate
     * @param constraint
     *            an optional FieldConstraint
     * @return whether the given cell has a value that is valid for this field (which must also be valid) see
     *         {@link #validate(FieldConstraint)} and {@link #validateCell(Td, FieldConstraint)}
     */
    public boolean isCellValid(Td cell, FieldConstraint constraint)
    {
        try
        {
            this.validate(constraint);
            this.validateCell(cell, constraint);
            return true;
        }
        catch (FieldFormatException | FieldValidationException e)
        {
            return false;
        }
    }

    /**
     * Gets the cell's value converted to a String value that is suitable for conversion to the right datatype
     * 
     * @param cell
     *            a Td cell
     * @return a String
     */
    public String getConvertedCellValue(Td cell)
    {
        try
        {
            String value = FieldDatatype.getFieldDatatypeForField(this).convertFieldValue(cell.getValue());
            return StringUtils.isBlank(value) ? null : value;
        }
        catch (FieldFormatException e)
        {
            throw new RuntimeException("Unexpected error occurred trying to retrieve FieldDatatype for Field - "
                    + "FieldDatatype would have been successfully resolved earlier.", e);
        }
    }

    /**
     * Returns a String representing the 'width' of the this field if it, or the optional FieldConstraint, has one and
     * if such a concept makes sense for the field's datatype (and null otherwise)
     * 
     * @param constraint
     *            an optional FieldConstraint
     * @return a String
     */
    public String getWidthForDatatype(FieldConstraint constraint)
    {
        try
        {
            return FieldDatatype.getFieldDatatypeForField(this).getWidthForField(this, constraint);
        }
        catch (FieldFormatException e)
        {
            throw new RuntimeException("Unexpected error occurred trying to retrieve FieldDatatype for Field - "
                    + "FieldDatatype would have been successfully resolved earlier.", e);
        }
    }
}
