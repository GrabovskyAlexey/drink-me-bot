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

<i>Only the essential ingredients count: spirits, liqueurs, vermouths, bitters and syrups. Juices, citrus and ice can be bought in any shop.</i>
