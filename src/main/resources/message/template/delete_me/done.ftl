<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "done_ru.ftl">
<#else>
    <#include "done_en.ftl">
</#if>
