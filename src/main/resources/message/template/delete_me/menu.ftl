<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "menu_ru.ftl">
<#else>
    <#include "menu_en.ftl">
</#if>
