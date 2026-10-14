<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "confirm_ru.ftl">
<#else>
    <#include "confirm_en.ftl">
</#if>
