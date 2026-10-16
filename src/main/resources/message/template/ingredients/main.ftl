<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "main_${lang}.ftl">
<#else>
    <#include "main_en.ftl">
</#if>
