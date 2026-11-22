# TP4 Web — Chat RAG avec LangChain4j

Application Web JSF de Camel Chafii pour Jakarta EE 11 et Payara 7. Le chat
reprend l’interface du TP2 et utilise Gemini pour générer les réponses.

## Fonctionnalités

- Routage des questions vers le support `rag.pdf` ou le guide fictif
  `mediatheque-atlas.pdf`, selon le sujet.
- Reformulation des questions de suivi à partir de l’historique avant la recherche.
- Recherche vectorielle locale dans les deux PDF avec LangChain4j.

## Exécution

Utiliser Java 25 et Maven. À la racine du projet, lancer `mvn clean package`.
Cette commande compile l’application, exécute les tests et construit
`target/tp4-web-chafii.war`.

Configurer `GEMINI_API_KEY` dans l’environnement du serveur Payara. La variable
facultative `GEMINI_MODEL` permet de remplacer le modèle par défaut,
`gemini-3.5-flash`. Les clés restent hors du dépôt.

Pour démarrer avec Payara Micro, depuis un terminal qui dispose de la clé et en
adaptant le chemin vers le JAR du serveur :

```sh
java --enable-native-access=ALL-UNNAMED -jar /chemin/vers/payara-micro.jar \
  --nocluster --deploy target/tp4-web-chafii.war
```

Ouvrir [le chat local](http://localhost:8080/tp4-web-chafii/).

## Démonstration

1. Demander « Quels sont les horaires de la médiathèque Atlas ? » pour interroger
   le guide. Les horaires indiqués sont du mardi au samedi, de 9 h à 18 h.
2. Poursuivre avec « Combien de livres peut-on y emprunter ? » dans le même chat.
   Le guide indique quatre livres.
3. Démarrer un nouveau chat et demander « À quoi sert le RAG selon le support de
   cours ? » pour interroger le PDF du cours.

[Énoncé du TP4](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp4-rag/index.html)
