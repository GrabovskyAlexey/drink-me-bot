<#assign lang = (locale.language!"")?lower_case>
<#if ["ru", "uk", "de", "es", "fr", "it"]?seq_contains(lang)>
    <#include "reset_confirm_${lang}.ftl">
<#else>
    <#include "reset_confirm_en.ftl">
</#if>
