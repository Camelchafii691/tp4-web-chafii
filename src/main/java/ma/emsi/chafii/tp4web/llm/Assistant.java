package ma.emsi.chafii.tp4web.llm;

/** LangChain4j fournit l’implémentation de ce service IA. */
public interface Assistant {
    String chat(String prompt);
}
