package au.csiro.casda.datadeposit.catalogue.level7;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import net.ivoa.vo.AnyTEXT;
import net.ivoa.vo.DataType;
import net.ivoa.vo.Table;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.votable.parser.AbstractVoTableElementVisitor;
import au.csiro.casda.datadeposit.votable.parser.FieldConstraint;
import au.csiro.casda.datadeposit.votable.parser.ParamConstraint;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableField;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableParam;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

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
 * Extension of {@link au.csiro.casda.datadeposit.catalogue.AbstractCatalogueVoTableVisitor} that specialises it to
 * visit a Level7 Catalogue VOTABLE and generate DDL to:
 * <ul>
 * <li>add an entry to the casda.catalogue table</li>
 * <li>create a new table for the catalogue entries</li>
 * <li>populate the new table with the catalogue entries</li>
 * <li>update the VOTAP metadata for the new catalogue</li>
 * </ul>
 * <p>
 * Copyright 2014, CSIRO Australia. All rights reserved.
 */
public class Level7VoTableVisitor extends AbstractVoTableElementVisitor
{
    private static final String LEVEL7_CONSTRAINTS_RESOURCE_PATH = "schemas/level7_metadata.yml";

    private static final List<ParamConstraint> PARAM_CONSTRAINTS = new ArrayList<>();

    private static final List<FieldConstraint> FIELD_CONSTRAINTS = new ArrayList<>();

    static
    {
        loadConstraintsFile(LEVEL7_CONSTRAINTS_RESOURCE_PATH, PARAM_CONSTRAINTS, FIELD_CONSTRAINTS);
    }

    private String catalogueName;

    private String projectCode;

    private Long level7CollectionId;

    private String filename;

    private String description;

    private SimpleJdbcRepository repository;

    private final Configuration freemarkerConfiguration;

    private List<Map<String, Object>> columns;
    
    private Collection<VisitableVoTableParam> params;

    private Date generationDate;

    private Set<String> indexedFields;

    private Set<String> principalFields;

    private int descriptionMaxLength;

    /**
     * Constructor
     * 
     * @param repository
     *            the simple jdbc repository
     * @param descriptionMaxLength
     *            description max length
     */
    public Level7VoTableVisitor(SimpleJdbcRepository repository, int descriptionMaxLength)
    {
        this(repository, new Date(), descriptionMaxLength);
    }

    /**
     * Constructor for test cases
     * 
     * @param repository
     *            the simple jdbc repository
     * @param generationDate
     *            the date this catalogue is being generated
     * @param descriptionMaxLength
     *            description max length
     */
    public Level7VoTableVisitor(SimpleJdbcRepository repository, Date generationDate, int descriptionMaxLength)
    {
        super();
        freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_22);
        freemarkerConfiguration.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "");
        this.repository = repository;
        this.generationDate = generationDate;
        this.descriptionMaxLength = descriptionMaxLength;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<ParamConstraint> getParamConstraints()
    {
        return PARAM_CONSTRAINTS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<FieldConstraint> getFieldConstraints()
    {
        return FIELD_CONSTRAINTS;
    }

    /** {@inheritDoc} */
    @Override
    public void visit(Table table)
    {
        super.visit(table);
        for (JAXBElement<?> element : table.getContent())
        {
            switch (element.getName().getLocalPart())
            {
            case "DESCRIPTION":
                this.description = convertAnyTEXTToString((AnyTEXT) element.getValue());
                if (StringUtils.isNotBlank(this.description) && this.description.length() > descriptionMaxLength)
                {
                    recordVoTableError(new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                            "Description element must be no bigger than " + descriptionMaxLength + " characters"));
                }
                break;
            default:
                // Only care about the DESCRIPTION element
                break;
            }
        }
        this.indexedFields = new HashSet<>();
        this.principalFields = new HashSet<>();
    }

    /** {@inheritDoc} */
    @Override
    protected void processParams(Collection<VisitableVoTableParam> params)
    {
        this.params = params;
        Optional<VisitableVoTableParam> catalogueNameParam = getParamWithName(params, "Catalogue Name");
        if (catalogueNameParam.isPresent() && !hasErrorsForParam(catalogueNameParam.get()))
        {
            this.catalogueName = StringUtils.lowerCase(catalogueNameParam.get().getConvertedValue());

            try
            {
                if (StringUtils.isBlank(this.catalogueName))
                {
                    recordParamError("Catalogue Name", new MalformedVoTableException(this, VisitorAction.VISIT_PARAM,
                            catalogueNameParam.get(), "value cannot be blank"));
                }
                else if (!this.catalogueName.matches("^[a-z0-9_]+$"))
                {
                    recordParamError("Catalogue Name", new MalformedVoTableException(this, VisitorAction.VISIT_PARAM,
                            catalogueNameParam.get(), "value contains forbidden characters ("
                                    + "it must contain only letters, numbers and underscores)"));
                }
                else if (repository.tableExists(this.catalogueName))
                {
                    recordParamError("Catalogue Name",
                            new MalformedVoTableException(this, VisitorAction.VISIT_PARAM, catalogueNameParam.get(),
                                    String.format("catalogue with name '%s' already exists", this.catalogueName)));
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e.getMessage());
            }
        }

        Optional<VisitableVoTableParam> indexedFieldsParam = getParamWithName(params, "Indexed Fields");
        if (indexedFieldsParam.isPresent() && !hasErrorsForParam(indexedFieldsParam.get()))
        {
            indexedFields.addAll(splitAndTrim(indexedFieldsParam.get().getConvertedValue()));
        }

        Optional<VisitableVoTableParam> principalFieldsParam = getParamWithName(params, "Principal Fields");
        if (principalFieldsParam.isPresent() && !hasErrorsForParam(principalFieldsParam.get()))
        {
            principalFields.addAll(splitAndTrim(principalFieldsParam.get().getConvertedValue()));
        }
    }

    private List<String> splitAndTrim(String param)
    {
        List<String> values = new ArrayList<>();
        if (param != null)
        {
            for (String fieldName : param.trim().split(","))
            {
                if (StringUtils.isNotBlank(fieldName))
                {
                    values.add(fieldName.trim());
                }
            }
        }
        return values;
    }

    /** {@inheritDoc} */
    @Override
    protected void processFields(List<VisitableVoTableField> fields)
    {
        final Set<String> unmatchedIndexedFields = new HashSet<>(indexedFields);
        final Set<String> unmatchedPrincipalFields = new HashSet<>(principalFields);
        this.columns =
                fields.stream()
                        .map((field) -> {
                            Map<String, Object> column = new HashMap<>();
                            column.put("db_column_name", field.getName());
                            column.put("datatype", field.getDatatype().toString());
                            column.put("size", getSqlSizeForField(field));
                            column.put("column_name", field.getName());
                            column.put("nullable", true);
                            column.put("indexed", indexedFields.contains(field.getName()));
                            column.put("principal", principalFields.contains(field.getName()));
                            column.put("description", getFieldDescription(field));
                            column.put("ucd", StringUtils.isBlank(field.getUcd()) ? null : field.getUcd());
                            column.put("unit", StringUtils.isBlank(field.getUnit()) ? null : field.getUnit());
                            unmatchedIndexedFields.remove(field.getName());
                            unmatchedPrincipalFields.remove(field.getName());

                            if (!StringUtils.isBlank(field.getName()) && !field.getName().matches("^[A-Za-z0-9_]+$"))
                            {
                                recordParamError(field.getName(), new MalformedVoTableException(this,
                                        VisitorAction.VISIT_FIELD, field,
                                        "Attribute 'name' ('" + field.getName()
                                        + "') contains forbidden characters ("
                                                + "it must contain only letters, numbers and underscores)"));
                            }
                            if (StringUtils.isNotBlank(getFieldDescription(field))
                                    && getFieldDescription(field).length() > descriptionMaxLength)
                            {
                                recordParamError(field.getName(), new MalformedVoTableException(this,
                                        VisitorAction.VISIT_FIELD, field, "Description element must be no bigger than "
                                                + descriptionMaxLength + " characters"));
                            }
                            if (this.hasErrorsForField(field))
                            {
                                return null;
                            }
                            else
                            {
                                return column;
                            }
                        }).filter((c) -> c != null).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(unmatchedIndexedFields))
        {
            recordParamError("Indexed Fields", new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                    "Unknown indexed fields " + unmatchedIndexedFields.stream().map((s) -> {
                        return "'" + s + "'";
                    }).collect(Collectors.joining(", "))));
        }
        if (CollectionUtils.isNotEmpty(unmatchedPrincipalFields))
        {
            recordParamError("Principal Fields", new MalformedVoTableException(this, VisitorAction.VISIT_TABLE, null,
                    "Unknown principal fields " + unmatchedPrincipalFields.stream().map((s) -> {
                        return "'" + s + "'";
                    }).collect(Collectors.joining(", "))));
        }
        if (!this.getErrors().isEmpty())
        {
            return;
        }
        repository.executeStatement(this.getCreateLevel7CatalogueTableDdl(this.projectCode, this.level7CollectionId,
                this.catalogueName, this.description, this.columns, this.filename));
        repository.executeStatement(getUpdateVotapMetadataForLevel7CatalogueDdl(this.projectCode, this.catalogueName,
                this.description, this.columns));
    }

    /** {@inheritDoc} */
    @Override
    protected void processRow(Map<VisitableVoTableField, String> row)
    {
        if (hasErrorsForParam("Catalogue Name") || hasErrorsForParam("Project Code"))
        {
            return; // we won't be able to process the row
        }
        Map<String, String> valuesMap = new HashMap<>();
        for (VisitableVoTableField field : row.keySet())
        {
            String value = row.get(field) == null ? "" : row.get(field);
            value = value.replaceAll("'", "''");
            valuesMap.put(field.getName(), value);
        }
        if (!this.getErrors().isEmpty())
        {
            return;
        }
        repository.executeStatement(this.getCreateLevel7CatalogueItemDdl(this.catalogueName, this.columns, valuesMap));
    }

    /**
     * Generate the DDL required to update the entry in the catalogue table and create a new Level 7 catalogue 'entry'
     * table
     * 
     * @param projectCode
     *            the projectCode of the Project owning the Catalogue
     * @param level7CollectionId
     *            the level 7 collection id associated with this catalogue
     * @param catalogueName
     *            the name of the Catalogue
     * @param catalogueDescription
     *            a description of the Catalogue
     * @param columns
     *            a List of Maps containing the definition of the Catalogue entry table's columns
     * @param filename
     *            the filename of the catalogue
     * @return the DDL
     */
    String getCreateLevel7CatalogueTableDdl(String projectCode, long level7CollectionId, String catalogueName,
            String catalogueDescription, List<Map<String, Object>> columns, String filename)
    {
        Map<String, Object> model = new HashMap<>();
        model.put("schema", "casda");
        model.put("level7CollectionId", level7CollectionId);
        model.put("filename", filename);
        model.put("projectCode", projectCode);
        model.put("level7CatalogueName", catalogueName);
        model.put("level7CatalogueTableDescription", StringUtils.replace(catalogueDescription, "'", "''"));
        model.put("generationDate", new SimpleDateFormat("yyyy-MM-dd").format(this.generationDate));
        model.put("columns", columns);
        StringWriter result = new StringWriter();
        Template template;
        String templatePath = "templates/createLevel7CatalogueTable.ftl";
        try
        {
            template = freemarkerConfiguration.getTemplate(templatePath, CharEncoding.UTF_8);
            template.process(model, result);
        }
        catch (IOException | TemplateException e)
        {
            throw new RuntimeException("Error processing FTL at path " + templatePath, e);
        }
        return result.toString();
    }

    /**
     * Generate the DDL required to insert entries into a Level 7 catalogue's 'entry' table
     * 
     * @param catalogueName
     *            the name of the Catalogue
     * @param columns
     *            a List of Maps containing the definition of the Catalogue entry table's columns
     * @param valuesMap
     *            a Map of the values for the columns
     * @return the DDL
     */
    String getCreateLevel7CatalogueItemDdl(String catalogueName, List<Map<String, Object>> columns,
            Map<String, String> valuesMap)
    {
        Map<String, Object> model = new HashMap<>();
        model.put("schema", "casda");
        model.put("level7CatalogueName", catalogueName);
        model.put("columns", columns);
        Map<String, String> values = new HashMap<>();
        for (String fieldName : valuesMap.keySet())
        {
            if (valuesMap.get(fieldName) != null)
            {
                values.put(fieldName, valuesMap.get(fieldName));
            }
            else
            {
                values.put(fieldName, "");
            }
        }
        model.put("values", values);
        StringWriter result = new StringWriter();
        String templatePath = "templates/createLevel7CatalogueItems.ftl";
        Template template;
        try
        {
            template = freemarkerConfiguration.getTemplate(templatePath, CharEncoding.UTF_8);
            template.process(model, result);
        }
        catch (IOException | TemplateException e)
        {
            throw new RuntimeException("Error processing FTL at path " + templatePath, e);
        }
        return result.toString();
    }

    /**
     * Generate the DDL required to update the VOTAP metadata for a Level7 catalogue
     * 
     * @param projectCode
     *            the projectCode of the Project owning the Catalogue
     * @param catalogueName
     *            the name of the Catalogue
     * @param catalogueDescription
     *            a description of the Catalogue
     * @param columns
     *            a List of Maps containing the definition of the Catalogue entry table's columns
     * @return the DDL
     */
    String getUpdateVotapMetadataForLevel7CatalogueDdl(String projectCode, String catalogueName,
            String catalogueDescription, List<Map<String, Object>> columns)
    {
        Map<String, Object> model = new HashMap<>();
        model.put("schema", "casda");
        model.put("projectCode", projectCode);
        model.put("level7CatalogueName", catalogueName);
        model.put("level7CatalogueTableDescription", catalogueDescription);
        model.put("generationDate", new SimpleDateFormat("yyyy-MM-dd").format(this.generationDate));
        model.put("columns", columns);
        model.put("params", params.stream().map(VisitableVoTableParam::toString).collect(Collectors.joining(" | ")));
        StringWriter result = new StringWriter();
        Template template;
        String templatePath = "templates/createVotapMetadataForLevel7Catalogue.ftl";
        try
        {
            template = freemarkerConfiguration.getTemplate(templatePath, CharEncoding.UTF_8);
            template.process(model, result);
        }
        catch (IOException | TemplateException e)
        {
            throw new RuntimeException("Error processing FTL at path " + templatePath, e);
        }
        return result.toString();
    }

    private String getSqlSizeForField(VisitableVoTableField field)
    {
        String result = field.getWidthForDatatype(getConstraintsForField(field));
        if (field.getDatatype() == DataType.CHAR && result == null)
        {
            throw new IllegalStateException("Missing maxarraysize definition of char. "
                    + "Won't be able to create sensibly-sized VARCHAR columns.");
        }
        return result;
    }

    private String getFieldDescription(VisitableVoTableField field)
    {
        return StringUtils.replace(convertAnyTEXTToString(field.getDESCRIPTION()), "'", "''");
    }

    private String convertAnyTEXTToString(AnyTEXT description)
    {
        if (description == null || description.getContent() == null)
        {
            return null;
        }
        return description.getContent().stream().map((o) -> o.toString().trim())
                .filter((s) -> StringUtils.isNotEmpty(s)).collect(Collectors.joining("\n"));
    }

    public void setProjectCode(String projectCode)
    {
        this.projectCode = projectCode;
    }

    public void setLevel7CollectionId(long level7CollectionId)
    {
        this.level7CollectionId = level7CollectionId;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

}
