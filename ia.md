# IA — LangChain4j, RAG et JSF

> Notes et exemples de code issus de cette conversation. Les noms des PDF, des modèles et des stores sont à adapter à votre projet. Les extraits sont pédagogiques et supposent que les dépendances Jakarta Faces, CDI et LangChain4j sont présentes.

## Sommaire

1. Deux PDF : `EmbeddingStoreContentRetriever` et `LanguageModelQueryRouter`
2. Interface de chatbot JSF en AJAX et bean `@SessionScoped`
3. Améliorer la recherche RAG : `maxResults`, `minScore`, segments
4. Importer un PDF en JSF : `h:inputFile`, `Part`, Tika et embeddings
5. Points à retenir pour le contrôle

---

## 1. Deux retrievers et un `LanguageModelQueryRouter`

**Objectif :** utiliser un retriever par PDF et faire choisir au modèle de langage le ou les documents adaptés à une question.

**Hypothèses :** `embeddingModel`, `chatModel`, `storePdf1` et `storePdf2` sont déjà initialisés ; les deux stores contiennent les embeddings de leurs PDF.

```java
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;

import java.util.HashMap;
import java.util.Map;

// À placer dans une méthode ou dans une configuration,
// avec embeddingModel, chatModel, storePdf1 et storePdf2 initialisés.

ContentRetriever retrieverPdf1 =
    EmbeddingStoreContentRetriever.builder()
        .embeddingStore(storePdf1)
        .embeddingModel(embeddingModel)
        .maxResults(2)
        .minScore(0.5)
        .build();

ContentRetriever retrieverPdf2 =
    EmbeddingStoreContentRetriever.builder()
        .embeddingStore(storePdf2)
        .embeddingModel(embeddingModel)
        .maxResults(2)
        .minScore(0.5)
        .build();

Map<ContentRetriever, String> descriptions = new HashMap<>();
descriptions.put(
    retrieverPdf1,
    "Document PDF 1 : cours sur l'intelligence artificielle"
);
descriptions.put(
    retrieverPdf2,
    "Document PDF 2 : cours sur le Big Data et Spark"
);

QueryRouter router = new LanguageModelQueryRouter(
    chatModel,
    descriptions
);

RetrievalAugmentor augmentor =
    DefaultRetrievalAugmentor.builder()
        .queryRouter(router)
        .build();
```

**Rôle de chaque élément :**

- `retrieverPdf1` / `retrieverPdf2` recherchent des passages dans leurs stores respectifs.
- `Map<ContentRetriever, String>` associe chaque retriever à une description de son PDF.
- `LanguageModelQueryRouter` interroge un LLM pour sélectionner le ou les retrievers pertinents.
- `DefaultRetrievalAugmentor` injecte les contenus retrouvés dans le contexte de la réponse.

### Utiliser le RAG avec `AiServices`

```java
import dev.langchain4j.service.AiServices;

interface Assistant {
    String answer(String question);
}

Assistant assistant = AiServices.builder(Assistant.class)
    .chatModel(chatModel)
    .retrievalAugmentor(augmentor)
    .build();

System.out.println(assistant.answer("Explique Apache Spark"));
```

> Si plusieurs PDF sont consultés au fil de la conversation, veillez à conserver un index et une configuration de router cohérents. `DefaultRetrievalAugmentor` (avec **r** final) est le nom complet de la classe.

---

## 2. Chatbot JSF : page XHTML + bean `@Named @SessionScoped`

**Objectif :** permettre de saisir des questions, d'envoyer une demande en AJAX et d'afficher l'historique sans rechargement complet de la page.

### `chat.xhtml`

```xhtml
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:f="jakarta.faces.core"
      xmlns:ui="jakarta.faces.facelets">

<h:head>
    <title>Chatbot RAG</title>
</h:head>

<h:body>
    <h2>Chatbot intelligent</h2>

    <h:form id="chatForm">
        <h:panelGroup id="historique" layout="block">
            <h3>Historique</h3>

            <ui:repeat value="#{chatBean.messages}" var="msg">
                <p>
                    <strong><h:outputText value="#{msg.role}" /></strong>
                    : <h:outputText value="#{msg.contenu}" />
                </p>
            </ui:repeat>
        </h:panelGroup>

        <h:inputTextarea id="question"
                         value="#{chatBean.question}"
                         rows="3" cols="50" />

        <h:commandButton value="Envoyer"
                         action="#{chatBean.envoyer}">
            <f:ajax execute="@form" render="historique question" />
        </h:commandButton>
    </h:form>
</h:body>
</html>
```

### `ChatBean.java`

```java
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("chatBean")
@SessionScoped
public class ChatBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String question;
    private List<Message> messages = new ArrayList<>();

    public void envoyer() {
        if (question == null || question.trim().isEmpty()) {
            return;
        }

        String texte = question.trim();
        messages.add(new Message("Utilisateur", texte));

        // Démonstration : remplacer par assistant.answer(texte)
        String reponse = "Réponse à : " + texte;

        messages.add(new Message("Assistant", reponse));
        question = "";
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private String role;
        private String contenu;

        public Message(String role, String contenu) {
            this.role = role;
            this.contenu = contenu;
        }

        public String getRole() {
            return role;
        }

        public String getContenu() {
            return contenu;
        }
    }
}
```

### Ce qu'il faut comprendre

| Élément | Rôle |
|---|---|
| `@Named("chatBean")` | Rend le bean accessible par `#{chatBean}` |
| `@SessionScoped` | Conserve l'historique d'une session à l'autre requête |
| `Serializable` | Autorise la sérialisation d'un bean de session |
| `<h:inputTextarea>` | Saisie de la question |
| `<h:commandButton>` | Déclenche la méthode `envoyer()` |
| `<f:ajax execute="@form">` | Envoie le formulaire en AJAX |
| `render="historique question"` | Met à jour uniquement les composants visés |
| `<ui:repeat>` | Affiche chaque message |

**Raccordement au RAG :** remplacer la réponse fictive par :

```java
String reponse = assistant.answer(texte);
```

L'instance `assistant` doit être configurée et accessible au bean, par exemple à travers un service CDI injecté. Un historique affiché dans JSF n'est **pas automatiquement** un historique utilisé par le modèle : pour un véritable dialogue à plusieurs tours, ajouter une mémoire de chat dans LangChain4j.

---

## 3. Corriger un RAG qui ne retrouve pas la bonne information

Trois réglages principaux :

| Paramètre | Point de départ | Pourquoi ? |
|---|---:|---|
| `maxResults` | `5` | Jusqu'à cinq passages récupérés |
| `minScore` | `0.5` | Exclut les passages sous ce score |
| Taille des segments | `500` caractères | Réduit le risque de mélanger plusieurs sujets |
| Chevauchement | `100` caractères | Limite la perte de contexte aux frontières |

**Exemple d'ingestion et de recherche :**

```java
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

EmbeddingStoreIngestor ingestor1 =
    EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(500, 100))
        .embeddingModel(embeddingModel)
        .embeddingStore(storePdf1)
        .build();

ingestor1.ingest(documentPdf1);

EmbeddingStoreIngestor ingestor2 =
    EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(500, 100))
        .embeddingModel(embeddingModel)
        .embeddingStore(storePdf2)
        .build();

ingestor2.ingest(documentPdf2);

var retrieverPdf1 = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(storePdf1)
    .embeddingModel(embeddingModel)
    .maxResults(5)
    .minScore(0.5)
    .build();

var retrieverPdf2 = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(storePdf2)
    .embeddingModel(embeddingModel)
    .maxResults(5)
    .minScore(0.5)
    .build();
```

Dans `DocumentSplitters.recursive(500, 100)`, les valeurs représentent ici les **caractères** du texte et le **chevauchement**, pas des tokens.

### Comment diagnostiquer le problème

| Symptôme | Test ou correction |
|---|---|
| Le bon passage manque | Essayer `maxResults(5)`, puis `10` |
| Aucun passage n'est retourné | Baisser progressivement `minScore` de `0.7` à `0.5`, éventuellement `0.3` pour tester |
| Beaucoup de passages hors sujet | Augmenter `minScore` ou améliorer le découpage |
| Passages trop courts et sans contexte | Augmenter la taille (par exemple 1 000 caractères) |
| Phrases coupées entre segments | Augmenter le chevauchement (par exemple 150–200 caractères) |
| Les deux retrievers fonctionnent seuls, mais pas ensemble | Vérifier les descriptions de la `Map` et le choix du `LanguageModelQueryRouter` |

**Afficher les passages avant la génération :**

```java
import dev.langchain4j.rag.query.Query;

retrieverPdf1.retrieve(Query.from("Explique Apache Spark"))
    .forEach(content ->
        System.out.println(content.textSegment().text())
    );
```

**Important :** après modification de la taille des segments, reconstruire les index à partir des PDF originaux, plutôt que de réinsérer les mêmes documents dans les anciens index en créant des doublons. Vérifier également que le texte est bien extrait (les scans et les tableaux posent parfois problème). Il faut mesurer la qualité des réponses, pas seulement augmenter les paramètres.

---

## 4. Importer un PDF depuis JSF et l'ajouter à l'`EmbeddingStore`

**Chaîne de traitement :** PDF → réception `Part` → extraction Tika → découpage → embeddings → `EmbeddingStore` → RAG.

### `upload.xhtml`

```xhtml
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="jakarta.faces.html"
      xmlns:f="jakarta.faces.core">

<h:head>
    <title>Importer un PDF - RAG</title>
</h:head>

<h:body>
    <h2>Ajouter un document PDF au RAG</h2>

    <h:form id="uploadForm" enctype="multipart/form-data">
        <h:outputLabel for="fichier" value="Choisir un PDF :" />

        <h:inputFile id="fichier"
                     value="#{uploadBean.fichier}"
                     accept="application/pdf"
                     required="true" />

        <h:message for="fichier" />

        <h:commandButton value="Importer le PDF"
                         action="#{uploadBean.importer}">
            <f:ajax execute="@form" render="@form" />
        </h:commandButton>

        <h:messages globalOnly="true" />
    </h:form>
</h:body>
</html>
```

> Le support du téléversement multipart en AJAX dépend de la version et de l'implémentation de Jakarta Faces ; tester ce comportement dans le serveur cible. Le formulaire doit avoir `enctype="multipart/form-data"`.

### `RagIndexService.java`

Ce service centralise le modèle d'embedding et la base vectorielle. Il est partagé entre les requêtes.

```java
import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

@ApplicationScoped
public class RagIndexService {

    private final EmbeddingModel embeddingModel =
        new AllMiniLmL6V2EmbeddingModel();

    private final EmbeddingStore<TextSegment> embeddingStore =
        new InMemoryEmbeddingStore<>();

    private final EmbeddingStoreIngestor ingestor =
        EmbeddingStoreIngestor.builder()
            .documentSplitter(DocumentSplitters.recursive(500, 100))
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();

    public void indexer(Document document) {
        ingestor.ingest(document);
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
```

### `UploadBean.java`

```java
import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Part;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Named("uploadBean")
@RequestScoped
public class UploadBean {

    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private Part fichier;

    @Inject
    private RagIndexService ragIndexService;

    public void importer() {
        if (fichier == null || fichier.getSize() == 0) {
            message(FacesMessage.SEVERITY_ERROR,
                    "Veuillez sélectionner un PDF.");
            return;
        }

        String nom = fichier.getSubmittedFileName();

        if (nom == null ||
            !nom.toLowerCase(Locale.ROOT).endsWith(".pdf") ||
            fichier.getSize() > MAX_SIZE) {
            message(FacesMessage.SEVERITY_ERROR,
                    "PDF invalide ou fichier supérieur à 10 Mo.");
            return;
        }

        try (BufferedInputStream input =
                 new BufferedInputStream(fichier.getInputStream())) {

            // Contrôle simple de la signature PDF
            input.mark(5);
            byte[] signature = input.readNBytes(5);
            input.reset();

            if (!"%PDF-".equals(
                    new String(signature, StandardCharsets.US_ASCII))) {
                message(FacesMessage.SEVERITY_ERROR,
                        "Le fichier n'est pas un PDF valide.");
                return;
            }

            // Extraction du texte
            Document document =
                new ApacheTikaDocumentParser().parse(input);

            if (document.text() == null || document.text().isBlank()) {
                message(FacesMessage.SEVERITY_ERROR,
                        "Aucun texte extrait du PDF.");
                return;
            }

            document.metadata().put("source", nom);

            // Découpage + embeddings + stockage
            ragIndexService.indexer(document);

            message(FacesMessage.SEVERITY_INFO,
                    "PDF indexé avec succès !");

        } catch (Exception e) {
            message(FacesMessage.SEVERITY_ERROR,
                    "Erreur lors de l'importation du PDF.");
        } finally {
            try {
                fichier.delete();
            } catch (IOException ignored) {
                // Nettoyage du fichier temporaire
            }
        }
    }

    private void message(FacesMessage.Severity severite, String texte) {
        FacesContext.getCurrentInstance().addMessage(
            null,
            new FacesMessage(severite, texte, null)
        );
    }

    public Part getFichier() {
        return fichier;
    }

    public void setFichier(Part fichier) {
        this.fichier = fichier;
    }
}
```

**Attention :** le contrôle d'extension, de taille et de signature n'est qu'une première vérification. Pour une application accessible au public, fixer des limites côté serveur (multipart, temps, volume, mémoire), protéger l'import par des autorisations et gérer les erreurs de parsing. Le bean de transfert utilise `@RequestScoped` car `Part` est lié à la requête ; le service d'indexation utilise `@ApplicationScoped`.

### Utiliser ce store dans un retriever

```java
ContentRetriever retriever =
    EmbeddingStoreContentRetriever.builder()
        .embeddingStore(ragIndexService.getEmbeddingStore())
        .embeddingModel(ragIndexService.getEmbeddingModel())
        .maxResults(5)
        .minScore(0.5)
        .build();

RetrievalAugmentor augmentor =
    DefaultRetrievalAugmentor.builder()
        .contentRetriever(retriever)
        .build();
```

Le chatbot doit utiliser **la même instance** de store et le même modèle d'embedding. `InMemoryEmbeddingStore` perd ses données au redémarrage. Pour une conservation durable, utiliser un store persistant.

Si votre application initiale utilise un retriever par PDF et `LanguageModelQueryRouter`, il faut aussi mettre le nouvel index à disposition du router (retriever + description à jour), faute de quoi le nouveau PDF peut ne pas être consulté.

---

## 5. Résumé express pour le contrôle

- **`EmbeddingStoreContentRetriever`** : recherche les segments pertinents dans un store vectoriel.
- **`maxResults(n)`** : nombre maximal de segments recherchés.
- **`minScore(s)`** : filtre de pertinence ; calibrer selon le modèle.
- **`DocumentSplitters.recursive(taille, overlap)`** : découpe le texte tout en conservant un chevauchement.
- **`LanguageModelQueryRouter`** : oriente la requête vers le ou les retrievers décrits par `Map<ContentRetriever, String>`.
- **`DefaultRetrievalAugmentor`** : ajoute les informations récupérées au contexte envoyé au LLM.
- **`@Named`** : rend le bean accessible depuis la page JSF.
- **`@SessionScoped`** : garde l'historique pendant la session de chat.
- **`<f:ajax>`** : actualise une partie de la page sans rechargement complet.
- **`<h:inputFile>` + `multipart/form-data`** : transfert d'un PDF au serveur.
- **`jakarta.servlet.http.Part`** : représente le fichier reçu et expose `getInputStream()`.
- **`ApacheTikaDocumentParser`** : lit le PDF et extrait son texte.
- **`EmbeddingStoreIngestor`** : automatise le découpage, la vectorisation et l'indexation.

### Schéma global

```text
                         INDEXATION
PDF 1 ----\
           \--> Parser Tika --> Segments --> EmbeddingModel --> EmbeddingStore(s)
PDF 2 ----/
Upload ---/

                         QUESTION / RÉPONSE
Question JSF --> LanguageModelQueryRouter --> ContentRetriever(s)
       --> Segments retrouvés --> DefaultRetrievalAugmentor
       --> ChatModel --> Réponse --> Historique JSF (AJAX)
```

**Note de compatibilité :** les API LangChain4j évoluent. Vérifier les imports, noms de méthodes et versions des dépendances du projet avant compilation. Le code de cette fiche reprend les exemples pédagogiques de la conversation ; certaines classes doivent être intégrées à une configuration CDI réelle.
