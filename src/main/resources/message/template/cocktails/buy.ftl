<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "buy_ru.ftl">
<#else>
    <#include "buy_en.ftl">
</#if>
