package tech.kayys.wayang.prompt.core;

/**
 * ============================================================================
 * PromptRole — semantic role of a prompt in a conversation.
 * ============================================================================
 *
 * Used by LLM providers that distinguish between system, user, and assistant
 * messages in their APIs (OpenAI, Anthropic, etc.).
 */
public enum PromptRole {
    /** System message — sets the LLM's persona, instructions, or context. */
    SYSTEM,
    /** User message — the human's input or query. */
    USER,
    /** Assistant message — the LLM's response in a conversation history. */
    ASSISTANT
}