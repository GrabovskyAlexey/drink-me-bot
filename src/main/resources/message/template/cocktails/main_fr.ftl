🍸 <b>Quoi préparer</b>

Ingrédients cochés : <b>${data.selected?c}</b>.
<#if data.filter?has_content>
🔎 Filtre : <b>${data.filter?html}</b>
</#if>
<#if data.selected == 0>

Vous n'avez encore rien coché. Ouvrez /ingredients et choisissez ce que vous avez.
<#else>

✅ Réalisables : <b>${data.ready?c}</b>
🛒 Il manque un ingrédient : <b>${data.missingOne?c}</b>
🛒 Il en manque deux : <b>${data.missingTwo?c}</b>
</#if>

<#if data.strict>
<i>Sélection stricte : les ingrédients facultatifs (jus, fruits, sodas, herbes et épices) comptent aussi. Vous pouvez la désactiver dans /settings.</i>
<#else>
<i>Seuls les ingrédients essentiels comptent : alcools forts, liqueurs, vermouths, bitters et sirops. Les jus, les agrumes et la glace s'achètent dans n'importe quel magasin. La sélection stricte s'active dans /settings.</i>
</#if>
