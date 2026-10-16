<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "buy_${lang}.ftl">
<#else>
    <#include "buy_en.ftl">
</#if>
