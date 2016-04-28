package au.csiro.casda.datadeposit.catalogue.level7;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ivoa.vo.DataType;

import org.junit.Before;
import org.junit.Test;

import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableParam;
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
 * Verify the DDL generation functions of the Level7VoTableVisitor class.
 * 
 * Copyright 2015, CSIRO Australia All rights reserved.
 */
public class Level7VoTableVisitorTest
{
    private Level7VoTableVisitor visitor;

    @Before
    public void setup() throws IOException, TemplateException
    {
        visitor = new Level7VoTableVisitor(mock(SimpleJdbcRepository.class), 255);
    }

    @Test
    public void testCreateTableScript() throws Exception
    {
        ArrayList<Map<String, Object>> columns = new ArrayList<>();
        columns.add(buildColumn("source_name", "CHAR", "19", true, false, 
        		"Designation for the radio component", true, "this is a label"));
        columns.add(buildColumn("ra_deg_cont", "DOUBLE", null, true, false, "J2000 right ascension in decimal degrees",
                true, "this is a label"));
        columns.add(buildColumn("dec_deg_cont", "DOUBLE", null, true, false, "J2000 declination in decimal degrees",
                true, "this is a label"));
        columns.add(buildColumn("flag1", "BOOLEAN", null, true, false, "", false, "this is a label"));
        String tableScript =
                visitor.getCreateLevel7CatalogueTableDdl("AS007", 123456, "testTable", "someDescription", columns,
                        "filename");
        assertThat(tableScript, containsString("source_name VARCHAR(19)"));
        assertThat(tableScript, containsString("ra_deg_cont DOUBLE PRECISION"));
        assertThat(tableScript, containsString("COMMENT ON TABLE casda.testTable"));
        assertThat(tableScript, containsString("COMMENT ON COLUMN casda.testTable.ra_deg_cont is "
                + "'J2000 right ascension in decimal degrees';"));
    }

    @Test
    public void testGenerateInsertStatement() throws Exception
    {
        ArrayList<Map<String, Object>> columns = new ArrayList<>();
        columns.add(buildColumn("source_name", "CHAR", "19", true, false, 
        		"Designation for the radio component", true, "source_name"));
        columns.add(buildColumn("ra_deg_cont", "DOUBLE", null, true, false, "J2000 right ascension in decimal degrees",
                true, "ra_deg_cont"));
        columns.add(buildColumn("dec_deg_cont", "DOUBLE", null, true, false, "J2000 declination in decimal degrees",
                true, "dec_deg_cont"));
        columns.add(buildColumn("flag1", "BOOLEAN", null, true, false, "", false, "flag1"));

        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("source_name", "Sgr A*");
        valueMap.put("ra_deg_cont", "266.4166667");
        valueMap.put("dec_deg_cont", "89.26410833");

        String tableScript = visitor.getCreateLevel7CatalogueItemDdl("testTable", columns, valueMap);

        assertThat(tableScript, startsWith("INSERT INTO casda.testTable"));
        assertThat(tableScript, containsString("source_name"));
        assertThat(tableScript, containsString("ra_deg_cont"));
        assertThat(tableScript, containsString("'Sgr A*', 266.4166667, 89.26410833, NULL);"));
    }

    @Test
    public void testGenerateTapMetadata() throws Exception
    {
        ArrayList<Map<String, Object>> columns = new ArrayList<>();
        columns.add(buildColumn("source_name", "CHAR", "19", true, false, 
        		"Designation for the radio component", true, "source_name"));
        columns.add(buildColumn("ra_deg_cont", "DOUBLE", null, true, false, "J2000 right ascension in decimal degrees",
                true, "ra_deg_cont"));
        columns.add(buildColumn("dec_deg_cont", "DOUBLE", null, true, false, "J2000 declination in decimal degrees",
                true, "dec_deg_cont"));
        columns.add(buildColumn("flag1", "BOOLEAN", null, true, false, "", false, "flag1"));

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam v = new VisitableVoTableParam();
        v.setName("taptap");
        v.setValue("meta");
        params.add(v);
        visitor.processParams(params);
        String tableScript =
                visitor.getUpdateVotapMetadataForLevel7CatalogueDdl("AS007", "testTable",
                        "Foreign key from testTable to catalogue table", columns);

        assertThat(tableScript, containsString("INSERT INTO casda.tap_tables"));
        assertThat(tableScript, containsString("INSERT INTO casda.tap_columns"));
        assertThat(tableScript, containsString("INSERT INTO casda.tap_keys"));
        assertThat(tableScript, containsString("source_name"));
        assertThat(tableScript, containsString("ra_deg_cont"));
        assertThat(tableScript, containsString("VALUES ((SELECT max(cast(numericalkeys.nums[1] as int)) + 1 from "
                + "(SELECT regexp_matches(key_id, '^\\d+$') as nums from casda.tap_keys) as numericalkeys), "
                + "'AS007.testTable', 'casda.catalogue', 'Foreign key from testTable to catalogue table');"));
    }

    @Test
    public void testValidateTableNameBlank() throws Exception
    {
        assertThat(visitor.getErrors(), is(empty()));
        visitor.setFailFast(false);

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam param = buildParam("Catalogue Name", null);
        params.add(param);
        visitor.processParams(params);

        List<Throwable> errors = visitor.getErrors();
        assertThat(errors, is(not(empty())));
        assertThat(errors.get(0).getMessage(), is("Error in PARAM 'Catalogue Name' : value cannot be blank"));
    }

    @Test
    public void testValidateTableNameSpaces() throws Exception
    {
        assertThat(visitor.getErrors(), is(empty()));
        visitor.setFailFast(false);

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam param = buildParam("Catalogue Name", "has spaces");
        params.add(param);
        visitor.processParams(params);

        List<Throwable> errors = visitor.getErrors();
        assertThat(errors, is(not(empty())));
        assertThat(errors.get(0).getMessage(),
                is("Error in PARAM 'Catalogue Name' : value contains forbidden characters "
                        + "(it must contain only letters, numbers and underscores)"));
    }

    @Test
    public void testValidateTableNameHyphen() throws Exception
    {
        assertThat(visitor.getErrors(), is(empty()));
        visitor.setFailFast(false);

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam param = buildParam("Catalogue Name", "has-hyphen");
        params.add(param);
        visitor.processParams(params);

        List<Throwable> errors = visitor.getErrors();
        assertThat(errors, is(not(empty())));
        assertThat(errors.get(0).getMessage(), is("Error in PARAM 'Catalogue Name' : "
                + "value contains forbidden characters (it must contain only letters, numbers and underscores)"));
    }

    @Test
    public void testValidateTableNameValid() throws Exception
    {
        assertThat(visitor.getErrors(), is(empty()));
        visitor.setFailFast(false);

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam param = buildParam("Catalogue Name", "A_valid_And_correct_name");
        params.add(param);
        visitor.processParams(params);

        assertThat(visitor.getErrors(), is(empty()));
    }

    private VisitableVoTableParam buildParam(String name, String value)
    {
        VisitableVoTableParam param = new VisitableVoTableParam();
        param.setName(name);
        param.setValue(value);
        param.setDatatype(DataType.CHAR);
        return param;
    }

    private Map<String, Object> buildColumn(String db_column_name, String datatype, String size, boolean nullable,
            boolean indexed, String description, boolean principal, String column_name)
    {
        Map<String, Object> column = new HashMap<>();
        column.put("db_column_name", db_column_name);
        column.put("datatype", datatype);
        column.put("size", size);
        column.put("nullable", nullable);
        column.put("indexed", indexed);
        column.put("principal", principal);
        column.put("description", description);
        column.put("column_name", column_name);
        return column;
    }

}
