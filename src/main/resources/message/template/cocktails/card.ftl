<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("ru")>
    <#include "card_ru.ftl">
<#else>
    <#include "card_en.ftl">
</#if>
