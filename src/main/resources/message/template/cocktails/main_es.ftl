🍸 <b>Qué preparar</b>

Ingredientes marcados: <b>${data.selected?c}</b>.
<#if data.filter?has_content>
🔎 Filtro: <b>${data.filter?html}</b>
</#if>
<#if data.selected == 0>

Todavía no has marcado nada. Abre /ingredients y elige lo que tienes.
<#else>

✅ Puedes preparar: <b>${data.ready?c}</b>
🛒 Falta un ingrediente: <b>${data.missingOne?c}</b>
🛒 Faltan dos: <b>${data.missingTwo?c}</b>
</#if>

<#if data.strict>
<i>Selección estricta: también cuentan los ingredientes opcionales (zumos, frutas, refrescos, hierbas y especias). Puedes desactivarla en /settings.</i>
<#else>
<i>Solo cuentan los ingredientes esenciales: licores fuertes, licores dulces, vermuts, bitters y jarabes. Los zumos, los cítricos y el hielo se compran en cualquier tienda. La selección estricta se activa en /settings.</i>
</#if>
