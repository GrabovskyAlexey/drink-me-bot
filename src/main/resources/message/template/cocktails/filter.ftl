<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "filter_ru.ftl">
<#else>
    <#include "filter_en.ftl">
</#if>
