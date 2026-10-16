🍸 <b>Cosa preparare</b>

Ingredienti selezionati: <b>${data.selected?c}</b>.
<#if data.filter?has_content>
🔎 Filtro: <b>${data.filter?html}</b>
</#if>
<#if data.selected == 0>

Non hai ancora selezionato niente. Apri /ingredients e scegli cosa hai.
<#else>

✅ Puoi preparare: <b>${data.ready?c}</b>
🛒 Manca un ingrediente: <b>${data.missingOne?c}</b>
🛒 Ne mancano due: <b>${data.missingTwo?c}</b>
</#if>

<#if data.strict>
<i>Selezione rigorosa: contano anche gli ingredienti facoltativi (succhi, frutta, bibite gassate, erbe e spezie). Puoi disattivarla in /settings.</i>
<#else>
<i>Contano solo gli ingredienti essenziali: superalcolici, liquori, vermut, bitter e sciroppi. Succhi, agrumi e ghiaccio si comprano in qualsiasi negozio. La selezione rigorosa si attiva in /settings.</i>
</#if>
