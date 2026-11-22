package ma.emsi.chafii.tp4web.llm;

import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15.BgeSmallEnV15EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RagSupport {
    public static final String COURS_RAG = "rag.pdf";
    public static final String GUIDE_MEDIATHEQUE = "mediatheque-atlas.pdf";

    private RagSupport() {
    }

    public static EmbeddingModel modeleEmbeddings() {
        return new BgeSmallEnV15EmbeddingModel();
    }

    public static EmbeddingStoreContentRetriever indexer(String fichier, EmbeddingModel modele) {
        var document = ClassPathDocumentLoader.loadDocument(fichier, new ApacheTikaDocumentParser());
        var segments = DocumentSplitters.recursive(1000, 120).split(document);
        var embeddings = modele.embedAll(segments).content();

        var magasin = new InMemoryEmbeddingStore<TextSegment>();
        magasin.addAll(embeddings, segments);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(magasin)
                .embeddingModel(modele)
                .maxResults(2)
                .minScore(0.5)
                .build();
    }

    public static QueryRouter routeurDocuments(ChatModel modele, ContentRetriever cours,
                                                ContentRetriever mediatheque) {
        Map<ContentRetriever, String> sources = new LinkedHashMap<>();
        sources.put(cours, "Support du cours sur l’IA : RAG, fine-tuning, embeddings et recherche vectorielle.");
        sources.put(mediatheque, "Guide de la médiathèque Atlas : horaires, prêts et règles pour les lecteurs.");
        return new LanguageModelQueryRouter(modele, sources);
    }
}
