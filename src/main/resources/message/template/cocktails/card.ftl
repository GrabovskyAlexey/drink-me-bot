<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "card_${lang}.ftl">
<#else>
    <#include "card_en.ftl">
</#if>
