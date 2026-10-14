<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "reset_confirm_ru.ftl">
<#else>
    <#include "reset_confirm_en.ftl">
</#if>
