<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "search_wait_ru.ftl">
<#else>
    <#include "search_wait_en.ftl">
</#if>
