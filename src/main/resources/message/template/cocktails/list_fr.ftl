<b>${data.title?html}</b> (${data.total?c})<#if (data.pages > 1)>
Page ${data.page?c} sur ${data.pages?c}</#if>
<#if data.filter?has_content>
🔎 Filtre : ${data.filter?html}
</#if>
<#if data.items?has_content>

<#list data.items as item>
${item.number?c}. <b>${item.name?html}</b><#if item.fallback> 🇺🇸</#if><#if item.missing?has_content> — manque : ${item.missing?html}</#if>
</#list>
<#if data.hasFallback>

<i>🇺🇸 — non disponible dans votre langue, affiché en anglais.</i>
</#if>
<#else>

Il n'y a encore rien ici.
</#if>
