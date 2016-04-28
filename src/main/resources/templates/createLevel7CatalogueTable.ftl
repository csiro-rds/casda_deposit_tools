<#ftl encoding="UTF-8" strip_whitespace=true >
<#import "level7Common.ftl" as common>
<#-- View model is: -->
<#--   schema -->
<#--   projectCode -->
<#--   level7CatalogueName -->
<#--   level7CatalogueTableDescription -->
<#--   columns -->
-- Table creation script for ${level7CatalogueTableDescription!level7CatalogueName}
-- Generated by CASDA Data Deposit on ${generationDate}

UPDATE ${schema}.catalogue SET entries_table_name = '${schema}.${level7CatalogueName}', version = (version+1)
WHERE level7_collection_id = (SELECT id FROM ${schema}.level7_collection WHERE dap_collection_id = ${level7CollectionId?c})
  AND filename = '${filename}';

<#assign fullTableName=schema+"."+level7CatalogueName>
CREATE table ${fullTableName} (
    id BIGSERIAL PRIMARY KEY,
    catalogue_id BIGINT NOT NULL,
<#list columns as column>
    ${column.db_column_name} <@common.getSqlTypeForVotableField field=column /><#if column.size??>(${column.size})</#if><#if !column.nullable> NOT NULL</#if><#if column_has_next>,</#if>
</#list>
);
<#-- May need to set a table space here -->

CREATE INDEX ON ${fullTableName}(catalogue_id); 
<#list columns as column>
    <#if column.indexed>
CREATE INDEX ON ${fullTableName}(${column.db_column_name}); 
    </#if>
</#list>

COMMENT ON TABLE ${fullTableName} is '${level7CatalogueTableDescription!"ASKAP Level 7 catalogue"}';
COMMENT ON COLUMN ${fullTableName}.id is 'Primary key'; 
COMMENT ON COLUMN ${fullTableName}.catalogue_id is 'Foreign key to the catalogue table'; 
<#list columns as column>
    <#if column.description?? >
COMMENT ON COLUMN ${fullTableName}.${column.db_column_name} is '${column.description}'; 
    </#if>
</#list>