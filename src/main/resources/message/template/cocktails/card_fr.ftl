<#if data.part != "TAIL">
🍸 <b>${data.name?html}</b>
<#if data.fallback>
<i>🇺🇸 Non disponible dans votre langue, affiché en anglais.</i>
</#if>
<#if data.author??>
<i>Auteur : ${data.author?html}</i>
</#if>
<#if data.tags?has_content>
${data.tags?html}
</#if>

<b>Ingrédients</b>
<#list data.ingredients as line>
${line?html}
</#list>
<#if data.hasMarks>
<i>✅ vous l'avez · ❌ manquant · • en vente en magasin</i>
</#if>
</#if>
<#if data.part != "HEAD">
<#if data.gadgets?has_content>

<b>Matériel</b>
${data.gadgets?html}
</#if>
<#if data.steps?has_content>

<b>Préparation</b>
<#list data.steps as step>
${step?counter}. ${step?html}
</#list>
</#if>
<#if data.part == "TAIL" && !data.gadgets?has_content && !data.steps?has_content>
🍸 <b>${data.name?html}</b>
</#if>
</#if>
