package au.csiro.casda.datadeposit.catalogue.level7;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import au.csiro.casda.datadeposit.observation.jdbc.repository.SimpleJdbcRepository;
import au.csiro.casda.datadeposit.votable.parser.VisitableVoTableParam;
import freemarker.template.TemplateException;
import net.ivoa.vo.DataType;

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
    
    @Mock
    private SimpleJdbcRepository simpleJdbcRepository;

    @Before
    public void setup() throws IOException, TemplateException
    {
        MockitoAnnotations.initMocks(this);
        visitor = new Level7VoTableVisitor(simpleJdbcRepository, 255);
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
                        "filename", 123455);
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
                visitor.getUpdateVotapMetadataForLevel7CatalogueDdl("AS007", "testTable_v01",
                        "Foreign key from testTable to catalogue table", columns, "testTable", 1, 1234);

        assertThat(tableScript, containsString("INSERT INTO casda.tap_tables"));
        assertThat(tableScript, containsString("INSERT INTO casda.tap_columns"));
        assertThat(tableScript, containsString("INSERT INTO casda.tap_keys"));
        assertThat(tableScript, containsString("source_name"));
        assertThat(tableScript, containsString("ra_deg_cont"));
        assertThat(tableScript, containsString("VALUES ((SELECT max(cast(numericalkeys.nums[1] as int)) + 1 from "
                + "(SELECT regexp_matches(key_id, '^\\d+$') as nums from casda.tap_keys) as numericalkeys), "
                + "'AS007.testTable_v01', 'casda.catalogue', 'Foreign key from testTable_v01 to catalogue table');"));
        assertThat(tableScript, containsString("WHERE lc.dc_common_id = 1234"));
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

    @Test
    public void testValidateTableNameVersion() throws Exception
    {
        assertThat(visitor.getErrors(), is(empty()));
        visitor.setFailFast(false);
        int dcCommonId = 42;
        visitor.setDcCommonId(dcCommonId);
        
        String tableName = "A_valid_And_correct_name";
        String[] firstVer = new String[] { tableName.toLowerCase() + "_v01", String.valueOf(dcCommonId) };
        List<Map<String, Object>> mapList = createTableVersionList(
                new String[][] { firstVer, { tableName.toLowerCase() + "_v02", String.valueOf(dcCommonId) } });

        doReturn(true).when(simpleJdbcRepository).tableExists(firstVer[0].toLowerCase());
        doReturn(mapList).when(simpleJdbcRepository).findTableVersions(anyString());

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam param = buildParam("Catalogue Name", tableName);
        params.add(param);
        visitor.processParams(params);

        assertThat(visitor.getErrors(), is(empty()));
        assertThat(visitor.getCatalogueName(), is(tableName.toLowerCase()+"_v03".toLowerCase()));
    }

    @Test
    public void testValidateTableNameVersionDuplicate() throws Exception
    {
        assertThat(visitor.getErrors(), is(empty()));
        visitor.setFailFast(false);
        int dcCommonId = 42;
        int dcCommonIdNew = 100;
        visitor.setDcCommonId(dcCommonIdNew);

        String tableName = "A_valid_And_correct_name";
        String[] firstVer = new String[] { tableName.toLowerCase() + "_v01", String.valueOf(dcCommonId) };
        List<Map<String, Object>> mapList = createTableVersionList(
                new String[][] { firstVer, { tableName.toLowerCase() + "_v02", String.valueOf(dcCommonId) } });

        doReturn(true).when(simpleJdbcRepository).tableExists(firstVer[0].toLowerCase());
        doReturn(mapList).when(simpleJdbcRepository).findTableVersions(anyString());

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam param = buildParam("Catalogue Name", tableName);
        params.add(param);
        visitor.processParams(params);

        List<Throwable> errors = visitor.getErrors();
        assertThat(errors, is(not(empty())));
        assertThat(errors.get(0).getMessage(), is("Error in PARAM 'Catalogue Name' : " + "catalogue with name '"
                + tableName.toLowerCase() + "_v01" + "' already exists"));
    }

    private List<Map<String, Object>> createTableVersionList(String[][] strings)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] entry : strings)
        {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("entries_table_name", entry[0]);
            entryMap.put("dc_common_id", Integer.parseInt(entry[1]));
            list.add(entryMap);
        }
        return list;
    }

    @Test
    public void testValidateTableManualVersion() throws Exception
    {
        assertThat(visitor.getErrors(), is(empty()));
        visitor.setFailFast(false);

        Collection<VisitableVoTableParam> params = new ArrayList<>();
        VisitableVoTableParam param = buildParam("Catalogue Name", "has_version_v01");
        params.add(param);
        visitor.processParams(params);

        List<Throwable> errors = visitor.getErrors();
        assertThat(errors, is(not(empty())));
        assertThat(errors.get(0).getMessage(),
                is("Error in PARAM 'Catalogue Name' : "
                        + "value contains a trailing version. Table versions are "
                        + "managed automatically and should not be manually provided."));
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
