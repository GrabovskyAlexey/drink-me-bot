<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "list_${lang}.ftl">
<#else>
    <#include "list_en.ftl">
</#if>
