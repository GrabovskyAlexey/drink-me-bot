🛒 <b>Einkaufsliste</b>

<#if data.total gt 0>
Einträge: ${data.total?c}, gekauft: ${data.bought?c}.

Tippen Sie auf einen Eintrag, um ihn als gekauft zu markieren. Gekauftes lässt sich zu „Meine Zutaten“ verschieben, oder Sie leeren die Liste.
<#else>
Die Liste ist leer. Fügen Sie Fehlendes aus einer Cocktailkarte oder von der Seite „Was kaufen“ in /cocktails hinzu.
</#if>
