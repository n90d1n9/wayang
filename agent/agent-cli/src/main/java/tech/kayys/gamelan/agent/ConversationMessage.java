package tech.kayys.gamelan.agent;

/**
 * Immutable conversation message (role + content pair) for building inference requests.
 */
public record ConversationMessage(String role, String content) {

    public static ConversationMessage user(String content) {
        return new ConversationMessage("user", content);
    }

    public static ConversationMessage assistant(String content) {
        return new ConversationMessage("assistant", content);
    }

    public static ConversationMessage system(String content) {
        return new ConversationMessage("system", content);
    }

    public static ConversationMessage tool(String toolName, String content) {
        return new ConversationMessage("tool", "[Tool: " + toolName + "] " + content);
    }
}
