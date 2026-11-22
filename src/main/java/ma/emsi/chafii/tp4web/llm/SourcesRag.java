package ma.emsi.chafii.tp4web.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SourcesRag {
    private ContentRetriever cours;
    private ContentRetriever mediatheque;

    @PostConstruct
    void indexerSources() {
        var embeddings = RagSupport.modeleEmbeddings();
        cours = RagSupport.indexer(RagSupport.COURS_RAG, embeddings);
        mediatheque = RagSupport.indexer(RagSupport.GUIDE_MEDIATHEQUE, embeddings);
    }

    public RetrievalAugmentor creerAugmentor(ChatModel modele) {
        return DefaultRetrievalAugmentor.builder()
                .queryTransformer(new CompressingQueryTransformer(modele))
                .queryRouter(RagSupport.routeurDocuments(modele, cours, mediatheque))
                .build();
    }
}
