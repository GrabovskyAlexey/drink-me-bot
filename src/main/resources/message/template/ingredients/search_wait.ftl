<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "search_wait_${lang}.ftl">
<#else>
    <#include "search_wait_en.ftl">
</#if>
