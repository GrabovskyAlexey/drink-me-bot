🍸 <b>What to make</b>

Ingredients marked: <b>${data.selected?c}</b>.
<#if data.filter?has_content>
🔎 Filter: <b>${data.filter?html}</b>
</#if>
<#if data.selected == 0>

You have not marked anything yet. Open /ingredients and choose what you have.
<#else>

✅ You can make: <b>${data.ready?c}</b>
🛒 Missing one ingredient: <b>${data.missingOne?c}</b>
🛒 Missing two: <b>${data.missingTwo?c}</b>
</#if>

<#if data.strict>
<i>Strict matching: optional ingredients (juices, fruit, sodas, herbs and spices) count too. You can turn it off in /settings.</i>
<#else>
<i>Only the essential ingredients count: spirits, liqueurs, vermouths, bitters and syrups. Juices, citrus and ice can be bought in any shop. Strict matching can be turned on in /settings.</i>
</#if>
