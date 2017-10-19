package au.csiro.casda.datadeposit.votable.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.ivoa.vo.Field;
import net.ivoa.vo.Param;
import net.ivoa.vo.Table;
import net.ivoa.vo.Td;
import net.ivoa.vo.Tr;
import net.ivoa.vo.VoTable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

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
 * Partial (abstract) implementation of {@link VoTableElementVisitor} that accumulates visited elements into a set of
 * Params, a list of Fields and a sequence of rows of Field values. The accumulated values are provided to the
 * corresponding template methods processParams, processFields, and processRow (which is called once for each row in the
 * visited table).
 * <p>
 * The attributes of the Params and Fields found are validated against Field and Param 'constraints' which specify
 * minimum requirements on the Params and Fields. The constraints are specified by subclasses through the
 * {@link #getParamConstraints} and {@link #getFieldConstraints} template methods. This class also provides convenience
 * methods for loading such constraints from YAML files.
 * <p>
 * The actual values of the Params and the Fields (as contained in the corresponding table cells) are validated
 * according to the self-describing attributes contained in the Param/Field and any 'maximum' values in the constraints.
 * <p>
 * Provision of param and field constraints is optional - Params and Fields without corresponding constraints will have
 * no checks performed against them for their attributes but the param and field values will still be validated.
 * <p>
 * Validation errors are accumulated as visit progresses into params, fields, rows and cells error collections. Various
 * methods exist to add errors to these collections or query if such errors exist. When subclasses implement 'process'
 * methods that rely on fields, params, rows, and cells being present and correct then they should check corresponding
 * error collections before trying to work with those values.
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public abstract class AbstractVoTableElementVisitor implements VoTableElementVisitor
{
    /**
     * Exception thrown when the visited VOTABLE does not meet the expected format.
     * <p>
     * This is an unchecked exception to keep the visitor interface clean.
     * <p>
     * Copyright 2014, CSIRO Australia. All rights reserved.
     */
    public static final class MalformedVoTableException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        private State state;

        private String visitDescription;

        /**
         * Constructs a MalformedVoTableException for the given visitor, action, element, and message
         * 
         * @param visitor
         *            the visitor where the exception occurred
         * @param visitorAction
         *            the action when the exception occurred
         * @param visitedElement
         *            the element being visited when the exception occurred
         * @param message
         *            a String
         */
        public MalformedVoTableException(AbstractVoTableElementVisitor visitor, VisitorAction visitorAction,
                Object visitedElement, String message)
        {
            super(message);
            this.state = visitor.state;
            this.visitDescription = visitorAction.getDescription(visitor, visitedElement);
        }

        /**
         * Constructs a MalformedVoTableException for the given visitor, action, element, and cause
         * 
         * @param visitor
         *            the visitor where the exception occured
         * @param visitorAction
         *            the action when the exception occured
         * @param visitedElement
         *            the element being visited when the exception occurred
         * @param cause
         *            another Throwable
         */
        public MalformedVoTableException(AbstractVoTableElementVisitor visitor, VisitorAction visitorAction,
                Object visitedElement, Throwable cause)
        {
            super(cause);
            this.state = visitor.state;
            this.visitDescription = visitorAction.getDescription(visitor, visitedElement);
        }

        /**
         * @return the state the visitor was in when the exception occurred
         */
        public State getState()
        {
            return this.state;
        }

        /**
         * @return the visitDescription
         */
        public String getVisitDescription()
        {
            return this.visitDescription;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMessage()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Error");
            sb.append(" in ");
            sb.append(this.getVisitDescription());
            if (super.getCause() != null)
            {
                sb.append(" : ");
                sb.append(getCause().getMessage());
            }
            else if (super.getMessage() != null)
            {
                sb.append(" : ");
                sb.append(super.getMessage());
            }
            return sb.toString();
        }

    }

    /**
     * An enumeration describing the the possible visit actions.
     * <p>
     * (Used for exception handling.)
     * <p>
     * Copyright 2014, CSIRO Australia All rights reserved.
     */
    public enum VisitorAction
    {
        /**
         * VISIT_TABLE visit action
         */
        VISIT_TABLE
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription(AbstractVoTableElementVisitor visitor, Object visitedElement)
            {
                return "TABLE";
            }
        },
        /**
         * VISIT_PARAM visit action
         */
        VISIT_PARAM
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription(AbstractVoTableElementVisitor visitor, Object visitedElement)
            {
                return "PARAM '" + ((Param) visitedElement).getName() + "'";
            }
        },
        /**
         * VISIT_FIELD visit action
         */
        VISIT_FIELD
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription(AbstractVoTableElementVisitor visitor, Object visitedElement)
            {
                return "FIELD '" + ((Field) visitedElement).getName() + "'";
            }
        },
        /**
         * VISIT_ROW visit action
         */
        VISIT_ROW
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription(AbstractVoTableElementVisitor visitor, Object visitedElement)
            {
                return String.format("%s TR", ordinalString(visitor.numRowsVisited));
            }
        },
        /**
         * VISIT_CELL visit action
         */
        VISIT_CELL
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription(AbstractVoTableElementVisitor visitor, Object visitedElement)
            {
                String fieldInfo =
                        visitor.numCellsInCurrentRowVisited <= visitor.fields.size() ? " (FIELD '"
                                + visitor.fields.get(visitor.numCellsInCurrentRowVisited - 1).getName() + "')" : "";
                return String.format("%s TD%s of %s TR", ordinalString(visitor.numCellsInCurrentRowVisited), fieldInfo,
                        ordinalString(visitor.numRowsVisited));
            }
        },
        /**
         * STOP_VISIT visit action
         */
        STOP_VISIT
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String getDescription(AbstractVoTableElementVisitor visitor, Object visitedElement)
            {
                return "VOTABLE";
            }
        };

        /**
         * @param visitor
         *            the visitor
         * @param visitedElement
         *            the element being visited
         * @return a description of this visit action on the given visitor
         */
        public abstract String getDescription(AbstractVoTableElementVisitor visitor, Object visitedElement);

        // http://stackoverflow.com/questions/6810336
        @SuppressWarnings("checkstyle:magicnumber")
        private static String ordinalString(int i)
        {
            String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
            switch (i % 100)
            {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + sufixes[i % 10];

            }
        }
    }

    /**
     * Represents the state of an AbstractVoTableElementVisitor.
     * <p>
     * Copyright 2014, CSIRO Australia All rights reserved.
     */
    public static enum State
    {
        /**
         * The state of an AbstractVoTableElementVisitor before it has started visiting a table
         */
        INITIAL
        {
            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Table table)
            {
                visitor.state = State.VISITING_TABLE;
            }
        },

        /**
         * The state of an AbstractVoTableElementVisitor while it is visiting a TABLE in the VOTABLE
         */
        VISITING_TABLE
        {
            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Table table)
            {
                visitor.recordVoTableError(new MalformedVoTableException(visitor, VisitorAction.VISIT_TABLE, null,
                        "Multiple TABLEs not supported"));
                visitor.state = State.VISITING_TABLE;
            }

            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Param param)
            {
                visitor.state = State.VISITING_FIELDS_AND_PARAMS;
                visitor.accumulateParam(param);
            }

            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Field field)
            {
                visitor.state = VISITING_FIELDS_AND_PARAMS;
                visitor.accumulateField(field);
            }

            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Tr tableRow)
            {
                visitor.paramsFinished();
                visitor.fieldsFinished();
                visitor.state = State.VISITING_ROW;
                visitor.accumulateRow();
            }

            /** {@inheritDoc} */

            @Override
            public void handleStop(AbstractVoTableElementVisitor visitor)
            {
                visitor.state = State.STOPPED;
            }
        },

        /**
         * The state of an AbstractVoTableElementVisitor while it is visiting the fields and params of the table
         */
        VISITING_FIELDS_AND_PARAMS
        {
            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Table table)
            {
                visitor.paramsFinished();
                visitor.fieldsFinished();
                visitor.recordVoTableError(new MalformedVoTableException(visitor, VisitorAction.VISIT_TABLE, null,
                        "Multiple TABLEs not supported"));
                visitor.state = State.VISITING_TABLE;
            }

            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Param param)
            {
                visitor.accumulateParam(param);
                visitor.state = VISITING_FIELDS_AND_PARAMS;
            }

            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Field field)
            {
                visitor.accumulateField(field);
                visitor.state = VISITING_FIELDS_AND_PARAMS;
            }

            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Tr tableRow)
            {
                visitor.paramsFinished();
                visitor.fieldsFinished();
                visitor.state = State.VISITING_ROW;
                visitor.accumulateRow();
            }

            /** {@inheritDoc} */
            @Override
            public void handleStop(AbstractVoTableElementVisitor visitor)
            {
                visitor.paramsFinished();
                visitor.fieldsFinished();
                visitor.state = State.STOPPED;
            }
        },

        /**
         * The state of an AbstractVoTableElementVisitor while it is visiting a row of the data of the table
         */
        VISITING_ROW
        {
            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Table table)
            {
                visitor.rowFinished();
                visitor.state = State.VISITING_TABLE;
                visitor.recordVoTableError(new MalformedVoTableException(visitor, VisitorAction.VISIT_TABLE, null,
                        "Multiple TABLEs not supported"));
            }

            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Tr tableRow)
            {
                visitor.rowFinished();
                visitor.state = State.VISITING_ROW;
                visitor.accumulateRow();
            }

            /** {@inheritDoc} */

            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Td tableCell)
            {
                visitor.accumulateCell(tableCell);
                visitor.state = State.VISITING_CELL;
            }

            /** {@inheritDoc} */
            @Override
            public void handleStop(AbstractVoTableElementVisitor visitor)
            {
                visitor.rowFinished();
                visitor.state = State.STOPPED;
            }
        },

        /**
         * The state of an AbstractVoTableElementVisitor while it is visiting a cell in a row of the data of the table
         */
        VISITING_CELL
        {
            /** {@inheritDoc} */
            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Table table)
            {
                visitor.rowFinished();
                visitor.state = State.VISITING_TABLE;
                visitor.recordVoTableError(new MalformedVoTableException(visitor, VisitorAction.VISIT_TABLE, null,
                        "Multiple TABLEs not supported"));
            }

            /** {@inheritDoc} */

            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Tr tableRow)
            {
                visitor.rowFinished();
                visitor.state = State.VISITING_ROW;
                visitor.accumulateRow();
            }

            /** {@inheritDoc} */

            @Override
            public void handleVisit(AbstractVoTableElementVisitor visitor, Td tableCell)
            {
                visitor.state = State.VISITING_CELL;
                visitor.accumulateCell(tableCell);
            }

            /** {@inheritDoc} */
            @Override
            public void handleStop(AbstractVoTableElementVisitor visitor)
            {
                visitor.rowFinished();
                visitor.state = State.STOPPED;
            }
        },

        /**
         * The state of an AbstractVoTableElementVisitor when it has finished visitng the table
         */
        STOPPED;

        /**
         * Handle a visit to a Table
         * 
         * @param visitor
         *            an AbstractVoTableElementVisitor
         * @param table
         *            a Table
         */
        public void handleVisit(AbstractVoTableElementVisitor visitor, Table table)
        {
            throw new RuntimeException("Cannot visit(Table) in state: " + this);
        }

        /**
         * Handle a visit to a Table
         * 
         * @param visitor
         *            an AbstractVoTableElementVisitor
         * @param param
         *            a Param
         */
        public void handleVisit(AbstractVoTableElementVisitor visitor, Param param)
        {
            throw new RuntimeException("Cannot visit(Param) in state: " + this);
        }

        /**
         * Handle a visit to a Field
         * 
         * @param visitor
         *            an AbstractVoTableElementVisitor
         * @param field
         *            a Field
         */
        public void handleVisit(AbstractVoTableElementVisitor visitor, Field field)
        {
            throw new RuntimeException("Cannot visit(Field) in state: " + this);
        }

        /**
         * Handle a visit to a Tr
         * 
         * @param visitor
         *            an AbstractVoTableElementVisitor
         * @param tableRow
         *            a Tr
         */
        public void handleVisit(AbstractVoTableElementVisitor visitor, Tr tableRow)
        {
            throw new RuntimeException("Cannot visit(Tr) in state: " + this);
        }

        /**
         * Handle a visit to a Td
         * 
         * @param visitor
         *            an AbstractVoTableElementVisitor
         * @param tableCell
         *            a Td
         */
        public void handleVisit(AbstractVoTableElementVisitor visitor, Td tableCell)
        {
            throw new RuntimeException("Cannot visit(Td) in state: " + this);
        }

        /**
         * Handle stop visiting a VOTABLE
         * 
         * @param visitor
         *            an AbstractVoTableElementVisitor
         */
        public void handleStop(AbstractVoTableElementVisitor visitor)
        {
            throw new RuntimeException("Cannot stop() in state: " + this);
        }

    }

    /**
     * Loads the fields and param constraints from a yml file
     * 
     * @param constraintsYmlPath
     *            path to yml file
     * @param paramConstraints
     *            params map
     * @param fieldConstraints
     *            fields map
     */
    protected static void loadConstraintsFile(String constraintsYmlPath, List<ParamConstraint> paramConstraints,
            List<FieldConstraint> fieldConstraints)
    {
        try
        {
            Resource resource = new ClassPathResource(constraintsYmlPath);
            InputStream inputStream = resource.getInputStream();
            Yaml yaml = new Yaml(new Constructor(), new Representer(), new DumperOptions(), new Resolver()
            {
                @Override
                protected void addImplicitResolvers()
                {
                    // Do nothing because we don't want any implicit conversion
                }
            });
            @SuppressWarnings("unchecked")
            Map<String, List<FieldConstraint>> data = (Map<String, List<FieldConstraint>>) yaml.load(inputStream);
            if (data == null)
            {
                throw new Exception("No constraints");
            }
            if (data.containsKey("params") && (data.get("params") instanceof List<?>))
            {
                for (FieldConstraint fieldDescription : data.get("params"))
                {
                    paramConstraints.add((ParamConstraint) fieldDescription);
                }
            }
            if (data.containsKey("fields") && (data.get("fields") instanceof List<?>))
            {
                for (FieldConstraint fieldDescription : data.get("fields"))
                {
                    fieldConstraints.add(fieldDescription);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error reading " + constraintsYmlPath, e);
        }
    }

    private boolean failFast;
    private State state;
    private Map<String, VisitableVoTableParam> params;
    private List<VisitableVoTableField> fields;
    private Map<VisitableVoTableField, Td> currentRow;
    private int numRowsVisited;
    private int numCellsInCurrentRowVisited;
    private List<MalformedVoTableException> voTableErrors;
    private Map<String, List<MalformedVoTableException>> fieldErrors;
    private Map<String, List<MalformedVoTableException>> paramErrors;
    private Map<Integer, List<MalformedVoTableException>> rowErrors;
    private Map<Integer, Map<String, List<MalformedVoTableException>>> cellErrors;

    /**
     * Constructor
     */
    public AbstractVoTableElementVisitor()
    {
        this(true);
    }

    /**
     * Constructor
     * 
     * @param failFast
     *            whether the visitor should fail (throw a MalformedFileException) on the first error or if it should
     *            accumulate the errors (which will be available via getErrors once the visit has stopped).
     */
    public AbstractVoTableElementVisitor(boolean failFast)
    {
        this.failFast = failFast;
        this.state = State.INITIAL;
        this.voTableErrors = new ArrayList<>();
        this.fieldErrors = new HashMap<>();
        this.paramErrors = new HashMap<>();
        this.rowErrors = new HashMap<>();
        this.cellErrors = new HashMap<>();
    }

    public boolean isFailFast()
    {
        return failFast;
    }

    public void setFailFast(boolean failFast)
    {
        this.failFast = failFast;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Requests the visitor to visit the root {@link VoTable}.
     * 
     * @param table
     *            the element to visit.
     */
    @Override
    public void visit(VoTable table)
    {
        // Nothing to do at this stage
    }

    /** {@inheritDoc} */
    @Override
    public void visit(Table table)
    {
        this.numRowsVisited = 0;
        this.params = new HashMap<>();
        this.fields = new ArrayList<>();
        this.state.handleVisit(this, table);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Requests the visitor to visit a {@link Param} of the {@link VoTable}. All params visited are checked against the
     * constraints provided by {@link #getParamConstraints()} to ensure that the param's attributes are the same as
     * those expected and that the value conforms to the attributes as well. Once all the params have been visited,
     * subclasses will be notified via the {@link #processParams(Map)} method.
     * 
     * @param param
     *            the element to visit.
     */
    @Override
    public void visit(Param param)
    {
        this.state.handleVisit(this, param);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Requests the visitor to visit a {@link Field} of the {@link VoTable}. All fields visited are checked against the
     * constraints provided by {@link #getFieldConstraints()} to ensure that the field's attributes are the same as
     * those expected. Once all the fields have been visited, subclasses will be notified via the
     * {@link #processFields(Map)} method.
     * 
     * @param field
     *            the element to visit.
     */
    @Override
    public void visit(Field field)
    {
        this.state.handleVisit(this, field);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Requests the visitor to visit a {@link Tr} of the {@link VoTable}.
     * <p>
     * Initialises internal data structures required to handle cell processing. Once all the cells in a row have been
     * visited, subclasses will be notified via the {@link #processRow(Map)} method.
     * 
     * @param row
     *            the element to visit.
     */
    @Override
    public void visit(Tr row)
    {
        this.state.handleVisit(this, row);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Requests the visitor to visit a {@link Td} of a {@link Tr} of the {@link VoTable}.
     * <p>
     * Field values are validated according to the ordinal {@link FieldConstraint} associated with the cell. Once all
     * the cells in a row have been visited, subclasses will be notified via the {@link #processRow(Map)} method.
     * 
     * @param cell
     *            the element to visit.
     */
    @Override
    public void visit(Td cell)
    {
        this.state.handleVisit(this, cell);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notifies the visitor that there are no more elements to visit.
     */
    @Override
    public void stop()
    {
        this.state.handleStop(this);
    }

    /**
     * @return an ordered set of exceptions that represent the errors recorded during the visit.
     */
    public List<Throwable> getErrors()
    {
        ArrayList<Throwable> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(this.voTableErrors))
        {
            result.addAll(this.voTableErrors);
        }
        for (List<MalformedVoTableException> exceptions : this.paramErrors.values())
        {
            result.addAll(exceptions);
        }
        for (List<MalformedVoTableException> exceptions : this.fieldErrors.values())
        {
            result.addAll(exceptions);
        }
        Set<Integer> rowIndexesSet = new HashSet<>();
        rowIndexesSet.addAll(this.rowErrors.keySet());
        rowIndexesSet.addAll(this.cellErrors.keySet());
        Integer[] rowIndexes = new ArrayList<>(rowIndexesSet).toArray(new Integer[0]);
        Arrays.sort(rowIndexes);
        for (Integer rowIndex : rowIndexes)
        {
            if (this.rowErrors.containsKey(rowIndex))
            {
                result.addAll(this.rowErrors.get(rowIndex));
            }
            else
            {
                for (List<MalformedVoTableException> exceptions : this.cellErrors.get(rowIndex).values())
                {
                    result.addAll(exceptions);
                }
            }
        }
        return result;
    }

    /**
     * Template method that provides list of VOTABLE param constraints.
     * 
     * @return a map of {@link ParamConstraint} keyed by name
     */
    protected abstract List<ParamConstraint> getParamConstraints();

    /**
     * Template method that provides a list of VOTABLE field constraints.
     * 
     * @return a map of {@link FieldConstraint} keyed by name
     */
    protected abstract List<FieldConstraint> getFieldConstraints();

    /**
     * Template method that is called once all the VOTABLE's params have been visited. Subclasses can use this method to
     * process the params.
     * 
     * @param params
     *            a map of VOTABLE param values (Strings), keyed by their param (as described by a
     *            {@link ParamConstraint})
     */
    protected abstract void processParams(Collection<VisitableVoTableParam> params);

    /**
     * Template method that is called once all the VOTABLE's fields have been visited. Subclasses can use this method to
     * process the fields (though they will typically be more interest in the rows).
     * 
     * @param fields
     *            a list of the VOTABLE's fields (in the order they are defined)
     */
    protected abstract void processFields(List<VisitableVoTableField> fields);

    /**
     * Template method that is called once a VOTABLE's row has been visited. Subclasses can use this method to process
     * the cells in a row. If the order of the cells in important then subclasses can use the fields supplied by
     * {@link #processFields(List)} to record the order before accessing the elements of the row.
     * 
     * @param row
     *            the rows values (Strings) keyed by their field (as described by a {@link FieldConstraint})
     */
    protected abstract void processRow(Map<VisitableVoTableField, String> row);

    /**
     * Records that there was an error with the TABLE as a whole. Only the first occurrence of errors with the same
     * message will be recorded.
     * 
     * @param exception
     *            an error
     */
    protected void recordVoTableError(MalformedVoTableException exception)
    {
        if (this.failFast)
        {
            throw exception;
        }
        if (this.voTableErrors.stream().noneMatch((e) -> e.getMessage().equals(exception.getMessage())))
        {
            this.voTableErrors.add(exception);
        }
    }

    /**
     * Records that there was an error associated with the given Param
     * 
     * @param param
     *            a Param
     * @param exception
     *            the error
     */
    protected void recordParamError(Param param, MalformedVoTableException exception)
    {
        recordParamError(param.getName(), exception);
    }

    /**
     * Records that there was an error associated with a param identified by the given name
     * 
     * @param paramName
     *            the name of the Param
     * @param exception
     *            the error
     */
    protected void recordParamError(String paramName, MalformedVoTableException exception)
    {
        recordParamError(FieldKey.NAME, paramName, exception);
    }

    /**
     * Records that there was an error associated with a param identified by the given key and keyValue
     * 
     * @param key
     *            the key used to identify the param (either 'name' or 'ucd')
     * @param keyValue
     *            the value of the key (ie: the name or ucd value)
     * @param exception
     *            the error
     */
    protected void recordParamError(FieldKey key, String keyValue, MalformedVoTableException exception)
    {
        if (this.failFast)
        {
            throw exception;
        }
        String errorKey;
        if (key == null)
        {
            errorKey = "UNSPECIFIED_FIELD_KEY:" + keyValue;
        }
        else
        {
            errorKey = getErrorKeyForFieldKeyAndValue(key, keyValue);
        }
        if (!paramErrors.containsKey(errorKey))
        {
            paramErrors.put(errorKey, new ArrayList<>());
        }
        paramErrors.get(errorKey).add(exception);
    }

    /**
     * Records that there was an error associated with the given Field
     * 
     * @param field
     *            a Field
     * @param exception
     *            the error
     */
    protected void recordFieldError(Field field, MalformedVoTableException exception)
    {
        recordFieldError(field.getName(), exception);
    }

    /**
     * Records that there was an error associated with a field identified by the given name
     * 
     * @param fieldName
     *            the name of the Field
     * @param exception
     *            the error
     */
    protected void recordFieldError(String fieldName, MalformedVoTableException exception)
    {
        recordFieldError(FieldKey.NAME, fieldName, exception);
    }

    /**
     * Records that there was an error associated with a field identified by the given key and keyValue
     * 
     * @param key
     *            the key used to identify the field (either 'name' or 'ucd')
     * @param keyValue
     *            the value of the key (ie: the name or ucd value)
     * @param exception
     *            the error
     */
    protected void recordFieldError(FieldKey key, String keyValue, MalformedVoTableException exception)
    {
        if (this.failFast)
        {
            throw exception;
        }
        String errorKey;
        if (key == null)
        {
            errorKey = "UNSPECIFIED_FIELD_KEY:" + keyValue;
        }
        else
        {
            errorKey = getErrorKeyForFieldKeyAndValue(key, keyValue);
        }
        if (!fieldErrors.containsKey(errorKey))
        {
            fieldErrors.put(errorKey, new ArrayList<>());
        }
        fieldErrors.get(errorKey).add(exception);
    }

    /**
     * Records that there was an error with a TABLE row (TR)
     * 
     * @param rowIndex
     *            a zero-based index of a TABLE row (TR)
     * @param exception
     *            the error
     */
    protected void recordRowError(Integer rowIndex, MalformedVoTableException exception)
    {
        if (this.failFast)
        {
            throw exception;
        }
        if (!rowErrors.containsKey(rowIndex))
        {
            rowErrors.put(rowIndex, new ArrayList<>());
        }
        rowErrors.get(rowIndex).add(exception);
    }

    /**
     * Records that there was an error with the cell identified by the given Field in a TABLE row (TR)
     * 
     * @param rowIndex
     *            a zero-based index of a TABLE row (TR)
     * @param field
     *            a Field
     * @param exception
     *            the error
     */
    protected void recordCellError(Integer rowIndex, Field field, MalformedVoTableException exception)
    {
        if (this.failFast)
        {
            throw exception;
        }
        if (!cellErrors.containsKey(rowIndex))
        {
            cellErrors.put(rowIndex, new HashMap<>());
        }
        if (!cellErrors.get(rowIndex).containsKey("name:" + field.getName()))
        {
            cellErrors.get(rowIndex).put("name:" + field.getName(), new ArrayList<>());
        }
        cellErrors.get(rowIndex).get("name:" + field.getName()).add(exception);
    }

    /**
     * @param param
     *            a Param
     * @return whether a Param has any errors
     */
    protected boolean hasErrorsForParam(VisitableVoTableParam param)
    {
        return hasErrorsForParam(param.getName());
    }

    /**
     * @param paramName
     *            a Param's name
     * @return whether a param with the given name has any errors
     */
    protected boolean hasErrorsForParam(String paramName)
    {
        return hasErrorsForParam(FieldKey.NAME, paramName);
    }

    /**
     * @param key
     *            the key used to identify the param (either 'name' or 'ucd')
     * @param keyValue
     *            the value of the key (ie: the name or ucd value)
     * @return whether a param identified by the given key and keyValue has any errors
     */
    protected boolean hasErrorsForParam(FieldKey key, String keyValue)
    {
        return this.paramErrors.containsKey(getErrorKeyForFieldKeyAndValue(key, keyValue))
                && CollectionUtils.isNotEmpty(this.paramErrors.get(getErrorKeyForFieldKeyAndValue(key, keyValue)));
    }

    /**
     * @param field
     *            a Field
     * @return whether a Field has any errors
     */
    protected boolean hasErrorsForField(VisitableVoTableField field)
    {
        return hasErrorsForField(field.getName());
    }

    /**
     * @param fieldName
     *            a Field's name
     * @return whether a field with the given name has any errors
     */
    protected boolean hasErrorsForField(String fieldName)
    {
        return hasErrorsForField(FieldKey.NAME, fieldName);
    }

    /**
     * @param key
     *            the key used to identify the field (either 'name' or 'ucd')
     * @param keyValue
     *            the value of the key (ie: the name or ucd value)
     * @return whether a field identified by the given key and keyValue has any errors
     */
    protected boolean hasErrorsForField(FieldKey key, String keyValue)
    {
        return this.fieldErrors.containsKey(getErrorKeyForFieldKeyAndValue(key, keyValue))
                && CollectionUtils.isNotEmpty(this.fieldErrors.get(getErrorKeyForFieldKeyAndValue(key, keyValue)));
    }

    /**
     * @return whether the current row has errors
     */
    protected boolean hasErrorsForCurrentRow()
    {
        return this.hasErrorsForRow(this.numRowsVisited - 1);
    }

    /**
     * @param rowIndex
     *            the index of a row (zero-based)
     * @return whether the row with the given rowIndex has errors
     */
    protected boolean hasErrorsForRow(Integer rowIndex)
    {
        return this.rowErrors.containsKey(rowIndex);
    }

    /**
     * @param field
     *            a Field
     * @return whether the cell for the given field in the current row has any errors
     */
    protected boolean hasErrorsForCurrentRowCell(Field field)
    {
        return this.hasErrorsForCell(this.numRowsVisited - 1, field);
    }

    /**
     * @param rowIndex
     *            the index (zero-based) of a TABLE row (TR)
     * @param field
     *            a Field
     * @return whether the cell for the given field in the given row has any errors
     */
    protected boolean hasErrorsForCell(Integer rowIndex, Field field)
    {
        String fielName = field.getName();
        return this.cellErrors.containsKey(rowIndex)
                && this.cellErrors.get(rowIndex).containsKey("name" + ":" + fielName)
                && CollectionUtils.isNotEmpty(this.cellErrors.get(rowIndex).get("name" + ":" + fielName));
    }

    /**
     * Returns the first VisitableVoTableParam in a collection of VisitableVoTableParam with the given name
     * 
     * @param params
     *            a Collection of VisitableVoTableParams
     * @param paramName
     *            the VisitableVoTableParam name to match
     * @return a VisitableVoTableParam
     */
    protected Optional<VisitableVoTableParam> getParamWithName(Collection<VisitableVoTableParam> params,
            String paramName)
    {
        Optional<VisitableVoTableParam> param =
                params.stream().filter((p) -> p.getName().equals(paramName)).findFirst();
        return param;
    }

    /**
     * Returns the first VisitableVoTableField in a collection of VisitableVoTableField with the given name
     * 
     * @param fields
     *            a Collection of VisitableVoTableFields
     * @param fieldName
     *            the VisitableVoTableField name to match
     * @return a VisitableVoTableField
     */
    protected Optional<VisitableVoTableField> getFieldWithName(Collection<VisitableVoTableField> fields,
            String fieldName)
    {
        Optional<VisitableVoTableField> param =
                fields.stream().filter((p) -> p.getName().equals(fieldName)).findFirst();
        return param;
    }

    /**
     * @param param
     *            a Param
     * @return the ParamConstraint for the given Param (if one exists)
     */
    protected ParamConstraint getConstraintsForParam(Param param)
    {
        ParamConstraint result = new ParamConstraint();
        for (ParamConstraint paramConstraint : getParamConstraints())
        {
            if (paramConstraint.isApplicableToInstance(param))
            {
                result.mergeInOtherConstraintForField(paramConstraint, "PARAM", param.getName());
            }
        }
        return result;
    }

    /**
     * @param field
     *            a Field
     * @return the FieldConstraint for the given Field (if one exists)
     */
    protected FieldConstraint getConstraintsForField(Field field)
    {
        FieldConstraint result = new FieldConstraint();
        for (FieldConstraint fieldConstraint : getFieldConstraints())
        {
            if (fieldConstraint.isApplicableToInstance(field))
            {
                result.mergeInOtherConstraintForField(fieldConstraint, "FIELD", field.getName());
            }
        }
        return result;
    }

    private void accumulateParam(Param param)
    {
        if (StringUtils.isBlank(param.getName()))
        {
            this.recordVoTableError(new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                    "Table has one or more PARAMs with a blank 'name' attribute"));
            return;
        }
        else if (this.params.containsKey(param.getName()))
        {
            recordParamError(param, new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                    "Table contains more than one PARAM named '" + param.getName() + "'"));
            return;
        }
        this.params.put(param.getName(), (VisitableVoTableParam) param);
        ParamConstraint constraint = getConstraintsForParam(param);
        try
        {
            ((VisitableVoTableParam) param).validate(constraint);
        }
        catch (FieldFormatException | FieldValidationException e)
        {
            recordParamError(param, new MalformedVoTableException(this, VisitorAction.VISIT_PARAM, param, e));
        }
    }

    private void accumulateField(Field field)
    {
        if (StringUtils.isBlank(field.getName()))
        {
            this.recordVoTableError(new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                    "Table has one or more FIELDs with a blank 'name' attribute"));
        }
        else if ("id".equalsIgnoreCase(field.getName()))
        {
            recordFieldError(field, new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                    "Table contains a FIELD named '" + field.getName() + "'"));
        }
        else if (this.fields.stream().anyMatch((f) -> f.getName().equals(field.getName())))
        {
            recordFieldError(field, new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                    "Table contains more than one FIELD named '" + field.getName() + "'"));
            return;
        }
        this.fields.add((VisitableVoTableField) field);
        FieldConstraint constraint = getConstraintsForField(field);
        try
        {
            ((VisitableVoTableField) field).validate(constraint);
        }
        catch (FieldFormatException e)
        {
            recordFieldError(field, new MalformedVoTableException(this, VisitorAction.VISIT_FIELD, field, e));
        }
    }

    private void accumulateRow()
    {
        this.numRowsVisited += 1;
        this.numCellsInCurrentRowVisited = 0;
        this.currentRow = new HashMap<>();
    }

    private void accumulateCell(Td cell)
    {
        this.numCellsInCurrentRowVisited += 1;
        if (this.currentRow.size() >= this.fields.size())
        {
            recordRowError(this.numRowsVisited - 1, new MalformedVoTableException(this, VisitorAction.VISIT_ROW, null,
                    "Additional TD"));
            return;
        }
        VisitableVoTableField field = this.fields.get(this.currentRow.size());
        this.currentRow.put(field, cell);
        try
        {
            field.validateCell(cell, getConstraintsForField(field));
        }
        catch (FieldValidationException e)
        {
            recordCellError(this.numRowsVisited - 1, field, new MalformedVoTableException(this,
                    VisitorAction.VISIT_CELL, null, e));
        }
    }

    private void rowFinished()
    {
        if (this.currentRow.size() < this.fields.size())
        {
            recordRowError(this.numRowsVisited - 1, new MalformedVoTableException(this, VisitorAction.VISIT_ROW, null,
                    String.format("Missing TD")));
            return;
        }
        Map<VisitableVoTableField, String> rowToProcess = new HashMap<>(this.fields.size());
        for (VisitableVoTableField field : this.currentRow.keySet())
        {
            rowToProcess.put(field, field.getConvertedCellValue(this.currentRow.get(field)));
        }
        if (hasErrorsForCurrentRow()
                || rowToProcess.keySet().stream()
                        .anyMatch((f) -> hasErrorsForField(f) || hasErrorsForCurrentRowCell(f)))
        {
            return; // Don't try and do anything with the current row
        }
        processRow(rowToProcess);
    }

    private void paramsFinished()
    {
        List<ParamConstraint> missingParameters = new ArrayList<>();
        this.getParamConstraints().stream().filter((p) -> p.isMatchingFieldRequired())
                .forEach((p) -> missingParameters.add(p));
        for (Param param : this.params.values())
        {
            for (ParamConstraint constraint : this.getParamConstraints())
            {
                if (constraint.isApplicableToInstance(param))
                {
                    missingParameters.remove(constraint);
                }
            }
        }
        for (ParamConstraint missingParameter : missingParameters)
        {
            recordParamError(
                    getPrimaryRequiredFieldKeyForFieldConstraint(missingParameter),
                    getPrimaryRequiredFieldKeyForFieldConstraint(missingParameter).getValueForObject(missingParameter),
                    new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null, String.format(
                            "Missing PARAM matching %s", missingParameter.getSimpleFieldDescription())));
        }
        this.processParams(this.params.values());
    }

    private void fieldsFinished()
    {
        List<FieldConstraint> missingFields = new ArrayList<>();
        this.getFieldConstraints().stream().filter((f) -> f.isMatchingFieldRequired())
                .forEach((f) -> missingFields.add(f));
        for (Field field : this.fields)
        {
            for (FieldConstraint constraint : this.getFieldConstraints())
            {
                if (constraint.isApplicableToInstance(field))
                {
                    missingFields.remove(constraint);
                }
            }
        }
        for (FieldConstraint missingField : missingFields)
        {
            if (!"true".equals(missingField.getConstraintForFieldKey(FieldKey.OPTIONAL)))
            {
                recordFieldError(getPrimaryRequiredFieldKeyForFieldConstraint(missingField),
                        getPrimaryRequiredFieldKeyForFieldConstraint(missingField).getValueForObject(missingField),
                        new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                                String.format("Missing FIELD matching %s", missingField.getSimpleFieldDescription())));
            }
        }
        this.processFields(this.fields);
    }

    private String getErrorKeyForFieldKeyAndValue(FieldKey key, String keyValue)
    {
        if (key == null)
        {
            return "UNSPECIFIED_FIELD_KEY:" + keyValue;
        }
        else
        {
            return key.toString() + ":" + keyValue;
        }
    }

    /**
     * Used internally to get a FieldKey to use when storing missing field errors. Where the constraint has a name set
     * we will always want to use that because errors in non-missing fields will also use the name as the key, allowing
     * us to detect their absence when processing the fields/params. If there is no name then the field must be
     * identified solely by UCD. Such errors will typically be ignored during fields/params processing but could be
     * checked for explicitly using {@link #hasErrorsForParam(FieldKey, String)} or
     * {@link #hasErrorsForField(FieldKey, String)}.
     * 
     * @param fieldConstraint
     *            a FieldConstraint
     */
    private FieldKey getPrimaryRequiredFieldKeyForFieldConstraint(FieldConstraint fieldConstraint)
    {
        if (StringUtils.isNotBlank(FieldKey.NAME.getValueForObject(fieldConstraint)))
        {
            return FieldKey.NAME;
        }
        else if (StringUtils.isNotBlank(FieldKey.UCD.getValueForObject(fieldConstraint)))
        {
            return FieldKey.UCD;
        }
        else
        {
            throw new IllegalStateException("Could not determine primary required FieldKey for FieldConstraint: "
                    + fieldConstraint);
        }
    }
}
