package tech.kayys.wayang.sdk.provider;

import tech.kayys.wayang.sdk.json.JsonValue;

/**
 * A single piece of content within a {@link ChatMessage}. A message can
 * contain multiple blocks, e.g. assistant text followed by a tool call,
 * or a user turn containing one or more tool results.
 */
public sealed interface ContentBlock {

    record Text(String text) implements ContentBlock {}

    /** An assistant request to invoke a tool. */
    record ToolUse(String id, String name, JsonValue input) implements ContentBlock {}

    /** The result of a tool execution, sent back as a user-role message. */
    record ToolResult(String toolUseId, String content, boolean isError) implements ContentBlock {}
}
