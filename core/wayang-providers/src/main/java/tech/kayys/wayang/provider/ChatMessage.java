package tech.kayys.wayang.provider;

import java.util.*;

/** A single turn in the conversation, provider-agnostic. */
public final class ChatMessage {

    public enum Role { USER, ASSISTANT }

    public final Role role;
    public final List<ContentBlock> content;

    public ChatMessage(Role role, List<ContentBlock> content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage userText(String text) {
        return new ChatMessage(Role.USER, List.of(new ContentBlock.Text(text)));
    }

    public static ChatMessage assistantText(String text) {
        return new ChatMessage(Role.ASSISTANT, List.of(new ContentBlock.Text(text)));
    }

    public static ChatMessage assistant(List<ContentBlock> blocks) {
        return new ChatMessage(Role.ASSISTANT, blocks);
    }

    public static ChatMessage toolResults(List<ContentBlock.ToolResult> results) {
        return new ChatMessage(Role.USER, new ArrayList<>(results));
    }

    /** Concatenates all text blocks; useful for transcript rendering. */
    public String textOnly() {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock b : content) {
            if (b instanceof ContentBlock.Text t) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(t.text());
            }
        }
        return sb.toString();
    }
}
