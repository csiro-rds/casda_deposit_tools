<#ftl encoding="UTF-8" strip_whitespace=true >
<#import "level7Common.ftl" as common>
<#-- View model is: -->
<#--   schema -->
<#--   level7CatalogueName -->
<#--   columns -->
<#--   values -->
<#assign fullTableName=schema+"."+level7CatalogueName>
INSERT INTO ${fullTableName} (catalogue_id, <#t>
<#list columns as column>
	${column.db_column_name}<#if column_has_next>, </#if><#t>
</#list>)
VALUES ((SELECT id from ${schema}.catalogue where catalogue_type = 'LEVEL7' and entries_table_name = '${schema}.${level7CatalogueName}'), <#t>
<#compress>
<#list columns as column>
    <@common.formatColumnValueAsSqlValue column=column value=values[column.db_column_name] /><#if column_has_next>, </#if><#t>
</#list>
);
</#compress>

