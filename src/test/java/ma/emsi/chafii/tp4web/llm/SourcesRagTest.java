package ma.emsi.chafii.tp4web.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourcesRagTest {
    @Test
    void routeVersLesDeuxPdfEtReformuleUneQuestionDeSuivi() {
        var sources = new SourcesRag();
        sources.indexerSources();
        var modele = new ModeleTemoin();
        var assistant = creerAssistant(sources, modele);

        assistant.chat("Quels sont les horaires de la médiathèque Atlas ?");
        assistant.chat("Combien de livres peut-on y emprunter ?");
        creerAssistant(sources, modele).chat("À quoi sert le RAG selon le support de cours ?");

        assertTrue(modele.routages.get(1).contains("emprunter à la médiathèque Atlas"));
        assertTrue(modele.contextes.get(0).contains("mardi au samedi"));
        assertTrue(modele.contextes.get(1).contains("quatre livres"));
        assertTrue(modele.contextes.get(2).contains("Answer using the following information:"));
        assertTrue(modele.contextes.get(2).contains("Fine-tuning et RAG"));
        assertFalse(modele.contextes.get(2).contains("quatre livres"));
    }

    private static Assistant creerAssistant(SourcesRag sources, ChatModel modele) {
        return AiServices.builder(Assistant.class)
                .chatModel(modele)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(sources.creerAugmentor(modele))
                .build();
    }

    private static class ModeleTemoin implements ChatModel {
        private final List<String> routages = new ArrayList<>();
        private final List<String> contextes = new ArrayList<>();

        @Override
        public ChatResponse doChat(ChatRequest requete) {
            String message = ((UserMessage) requete.messages().getLast()).singleText();
            String reponse;
            if (message.startsWith("Based on the user query")) {
                String question = message.substring(message.lastIndexOf("User query:"));
                routages.add(question);
                reponse = question.contains("Atlas") ? "2" : "1";
            } else if (message.startsWith("Read and understand the conversation")) {
                reponse = "Combien de livres peut-on emprunter à la médiathèque Atlas ?";
            } else {
                contextes.add(message);
                reponse = "Réponse de vérification.";
            }
            return ChatResponse.builder().aiMessage(AiMessage.from(reponse)).build();
        }
    }
}
