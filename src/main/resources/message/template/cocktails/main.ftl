<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "main_ru.ftl">
<#else>
    <#include "main_en.ftl">
</#if>
