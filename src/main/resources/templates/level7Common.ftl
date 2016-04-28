<#-- ----------------- -->
<#-- Macro definitions -->

<#macro getSqlTypeForVotableField field>
<#compress>
    <#switch field.datatype>
    <#case "BOOLEAN">
        BOOLEAN
        <#break>
    <#case "BIT">
        BIT VARYING
        <#break>
    <#case "UNSIGNED_BYTE">
    <#case "SHORT">
        SMALLINT
        <#break>
    <#case "INT">
        INTEGER
        <#break>
    <#case "LONG">
        BIGINT
        <#break>
    <#case "CHAR">
    <#case "UNICODE_CHAR">
        VARCHAR
        <#break>
    <#case "FLOAT">
        REAL
        <#break>
    <#case "DOUBLE">
        DOUBLE PRECISION
        <#break>
    <#default>
        'Unhandled type: ${field.type}'
    </#switch>
</#compress>
</#macro>

<#macro getVotapColumnTypeForVotableField field>
<#compress>
    <#switch field.datatype>
    <#case "BOOLEAN">
        BOOLEAN
        <#break>
    <#case "BIT">
        VARBINARY
        <#break>
    <#case "UNSIGNED_BYTE">
    <#case "SHORT">
        SMALLINT
        <#break>
    <#case "INT">
        INTEGER
        <#break>
    <#case "LONG">
        BIGINT
        <#break>
    <#case "CHAR">
    <#case "UNICODE_CHAR">
        VARCHAR
        <#break>
    <#case "FLOAT">
        REAL
        <#break>
    <#case "DOUBLE">
        DOUBLE
        <#break>
    <#default>
        'Unhandled type: ${field.type}'
    </#switch>
</#compress>
</#macro>

<#macro getVotapColumnSizeForVotableField field>
<#compress>
    <#switch field.datatype>
    <#case "BOOLEAN">
    <#case "UNSIGNED_BYTE">
    <#case "SHORT">
        4
        <#break>
    <#case "INT">
        15
        <#break>
    <#case "LONG">
        19
        <#break>
    <#case "BIT">
    <#case "CHAR">
    <#case "UNICODE_CHAR">
        ${field.size!1}
        <#break>
    <#case "FLOAT">
        10
        <#break>
    <#case "DOUBLE">
        19
        <#break>
    </#switch>
</#compress>
</#macro>

<#macro formatColumnValueAsSqlValue column value=''>
<#compress>
    <#if value=''>
        NULL
        <#return>
    </#if>
    <#switch column.datatype>
    <#case "CHAR">
    <#case "UNICODECHAR">
        '${value}'
        <#break>
    <#case "BOOLEAN">
		<#if value='0' || value?lower_case?starts_with('f')>
		'f'
		<#elseif value='1' || value?lower_case?starts_with('t')>
		't'
		<#else>
		NULL
		</#if>
        <#break>
    <#case "BIT">
        B'${value}'
        <#break>
    <#default>
        ${value}
    </#switch>
</#compress>
</#macro>

