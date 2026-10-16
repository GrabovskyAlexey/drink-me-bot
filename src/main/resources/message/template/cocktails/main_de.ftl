🍸 <b>Was mixen</b>

Zutaten markiert: <b>${data.selected?c}</b>.
<#if data.filter?has_content>
🔎 Filter: <b>${data.filter?html}</b>
</#if>
<#if data.selected == 0>

Du hast noch nichts markiert. Öffne /ingredients und wähle, was du hast.
<#else>

✅ Machbar: <b>${data.ready?c}</b>
🛒 Eine Zutat fehlt: <b>${data.missingOne?c}</b>
🛒 Zwei fehlen: <b>${data.missingTwo?c}</b>
</#if>

<#if data.strict>
<i>Strenge Auswahl: Auch optionale Zutaten (Säfte, Obst, Limonaden, Kräuter und Gewürze) zählen. Du kannst sie in /settings ausschalten.</i>
<#else>
<i>Es zählen nur die wesentlichen Zutaten: Spirituosen, Liköre, Wermut, Bitter und Sirupe. Säfte, Zitrusfrüchte und Eis gibt es in jedem Laden. Die strenge Auswahl kannst du in /settings einschalten.</i>
</#if>
