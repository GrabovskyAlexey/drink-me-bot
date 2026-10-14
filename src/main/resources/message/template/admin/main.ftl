📊 <b>Статистика</b>
<i>на ${data.generatedAt} МСК</i>

👥 <b>Аудитория</b>
Всего: <b>${data.stats.audience.total}</b>
Новые: сегодня ${data.stats.audience.new1} · 7 дн. ${data.stats.audience.new7} · 30 дн. ${data.stats.audience.new30}
Активные: сегодня ${data.stats.audience.active1} · 7 дн. ${data.stats.audience.active7} · 30 дн. ${data.stats.audience.active30}

🧪 <b>Ингредиенты</b>
Отметили хоть один: ${data.stats.ingredients.usersWithIngredients} · в среднем ${data.averageMarks}
Без единого готового коктейля: ${data.stats.ingredients.usersWithoutReady} (${data.withoutReadyPercent}%)
<#if data.stats.popularIngredients?has_content>
Популярные:
<#list data.stats.popularIngredients as item>
${item?counter}. ${item.name?html} — ${item.count}
</#list>
</#if>

🍸 <b>Коктейли</b>
Открытий карточек: ${data.stats.cocktailViews}
<#if data.stats.popularCocktails?has_content>
Популярные:
<#list data.stats.popularCocktails as item>
${item?counter}. ${item.name?html} — ${item.count}
</#list>
</#if>

📈 <b>По неделям</b> (с понедельника, последняя строка — текущая неделя)
<pre>${data.weekTable?html}</pre>
