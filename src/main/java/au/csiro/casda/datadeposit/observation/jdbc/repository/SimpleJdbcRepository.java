package au.csiro.casda.datadeposit.observation.jdbc.repository;

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


import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 
 * Simple JDBC repository for executing arbitrary SQL statements
 * <p>
 * Copyright 2015, CSIRO Australia. All rights reserved.
 */
@Repository
public class SimpleJdbcRepository
{

    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public void setDataSource(DataSource dataSource) 
    {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
    /**
     * Execute any SQL statement without a return type
     * @param statement
     *          SQL String
     */
    public void executeStatement(String statement)
    {
        this.jdbcTemplate.execute(statement);
    }

    /**
     * Check if a table exists in the database
     * @param tableName
     *          The name of the table
     * @return
     *          True if the table exists
     * @throws SQLException
     *          Probably a connection failure
     */
    public boolean tableExists(String tableName) throws SQLException
    {
        DatabaseMetaData md = this.jdbcTemplate.getDataSource().getConnection().getMetaData();
        ResultSet rs = md.getTables(null, null, tableName, null);
        if (rs.next()) 
        {
            return true;
        }
        
        return false;        
    }
    
    /**
     * @return a list of valid image types from the database
     */
    public List<String> getImageTypes()
    {
        //TODO 
        //Once script has been created to remove Unknown types from the database this where condition can be removed.
        return this.jdbcTemplate.queryForList(
                "select distinct type_name from casda.image_type where type_name != 'Unknown'", String.class);
    }
}
