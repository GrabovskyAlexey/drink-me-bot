<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "confirm_${lang}.ftl">
<#else>
    <#include "confirm_en.ftl">
</#if>
