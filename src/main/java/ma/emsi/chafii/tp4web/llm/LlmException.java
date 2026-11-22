package ma.emsi.chafii.tp4web.llm;

/** Message destiné à l’utilisateur, sans détails confidentiels de l’API. */
public class LlmException extends RuntimeException {
    public LlmException(String message) { super(message); }
    public LlmException(String message, Throwable cause) { super(message, cause); }
}
