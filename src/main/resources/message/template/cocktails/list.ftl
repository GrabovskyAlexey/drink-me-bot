<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "list_ru.ftl">
<#else>
    <#include "list_en.ftl">
</#if>
