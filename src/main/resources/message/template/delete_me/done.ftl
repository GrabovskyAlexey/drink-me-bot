<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "done_${lang}.ftl">
<#else>
    <#include "done_en.ftl">
</#if>
