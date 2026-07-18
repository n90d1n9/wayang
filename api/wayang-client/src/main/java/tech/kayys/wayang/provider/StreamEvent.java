package tech.kayys.wayang.provider;

import tech.kayys.wayang.json.JsonValue;

/**
 * Events emitted incrementally while a provider streams a response.
 * The UI/agent loop consumes these without caring which backend (Anthropic,
 * OpenAI, Ollama) produced them.
 */
public sealed interface StreamEvent {

    /** A chunk of assistant-visible text to append to the current text block. */
    record TextDelta(String text) implements StreamEvent {}

    /** A new tool call has begun; the name is known but input may still be streaming. */
    record ToolUseStart(String id, String name) implements StreamEvent {}

    /** Partial JSON for a tool call's input, to be concatenated and parsed once complete. */
    record ToolUseInputDelta(String id, String partialJson) implements StreamEvent {}

    /** A tool call's input is fully streamed; `input` is the parsed JSON object. */
    record ToolUseEnd(String id, JsonValue input) implements StreamEvent {}

    /** Token usage reported by the provider, if available. */
    record Usage(int inputTokens, int outputTokens) implements StreamEvent {}

    /** The model finished responding. reason is e.g. "end_turn", "tool_use", "max_tokens". */
    record MessageStop(String reason) implements StreamEvent {}

    /** A transport or API-level error occurred. */
    record Error(String message) implements StreamEvent {}

    /**
     * A chunk of the model's internal chain-of-thought reasoning.
     * Shown in the UI distinctly (dim/italic) but NOT stored in conversation history.
     * Only emitted by backends that support native thinking blocks (e.g. extended-thinking).
     */
    record ThinkingDelta(String text) implements StreamEvent {}

    /**
     * The model's thinking/reasoning block for this turn is complete.
     * The UI should flush any accumulated thinking text.
     */
    record ThinkingEnd() implements StreamEvent {}
}
