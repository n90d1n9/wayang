package tech.kayys.wayang.prompt.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ============================================================================
 * RenderedChain — final output of {@link PromptChain#render()}.
 * ============================================================================
 *
 * A RenderedChain is the fully-assembled, ordered list of messages that will
 * be sent to the LLM. It is the *only* object that crosses the boundary
 * between the Prompt Engine and the LLM Adapter layer.
 *
 * Structure:
 * • {@link #messages()} – ordered list of {@link RenderResult}, one per
 * included template. Position 0 is always the
 * merged SYSTEM message (if any SYSTEM templates
 * were present).
 * • {@link #skippedTemplateIds()} – IDs of templates whose CEL condition
 * evaluated to {@code false} and were
 * therefore excluded. Useful for
 * provenance / debugging.
 *
 * Convenience accessors:
 * • {@link #liveContent()} – concatenation of all
 * {@link RenderResult#content()} values.
 * • {@link #redactedContent()} – concatenation of all redacted copies
 * (audit-safe).
 *
 * Immutability:
 * All collections are wrapped in unmodifiable views. The object is safe
 * to cache and share across threads.
 */
public record RenderedChain(
        List<RenderResult> messages,
        Set<String> skippedTemplateIds) {
    /** Compact constructor — defensive copies + null checks. */
    public RenderedChain {
        Objects.requireNonNull(messages, "messages must not be null");
        messages = Collections.unmodifiableList(List.copyOf(messages));
        skippedTemplateIds = skippedTemplateIds != null
                ? Collections.unmodifiableSet(Set.copyOf(skippedTemplateIds))
                : Collections.emptySet();
    }

    // ------------------------------------------------------------------
    // Convenience accessors
    // ------------------------------------------------------------------

    /**
     * Returns the live (non-redacted) content of every message joined by
     * {@code \n\n}. This is the string that ultimately reaches the LLM.
     */
    public String liveContent() {
        return messages.stream()
                .map(RenderResult::content)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n\n" + b);
    }

    /**
     * Returns the redacted content of every message joined by {@code \n\n}.
     * Safe for audit logs and provenance stores.
     */
    public String redactedContent() {
        return messages.stream()
                .map(RenderResult::redactedContent)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n\n" + b);
    }

    /** Total number of messages in the chain. */
    public int messageCount() {
        return messages.size();
    }

    /**
     * {@code true} when no messages survived rendering (all skipped or empty
     * input).
     */
    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
