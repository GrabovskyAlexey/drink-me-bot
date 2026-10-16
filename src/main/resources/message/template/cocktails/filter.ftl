<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "filter_${lang}.ftl">
<#else>
    <#include "filter_en.ftl">
</#if>
