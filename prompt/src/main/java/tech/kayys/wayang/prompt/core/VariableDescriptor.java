package tech.kayys.wayang.prompt.core;

import java.util.Objects;

/**
 * ============================================================================
 * VariableDescriptor — design-time declaration of a template slot.
 * ============================================================================
 *
 * Each variable in a PromptTemplate body (e.g. {@code {{user_query}}}) must
 * have a corresponding VariableDescriptor. The descriptor is part of
 * the template's *schema*; the renderer validates that every required variable
 * has been supplied before expansion begins.
 *
 * Source mapping
 * --------------
 * {@link VariableSource} mirrors {@code PortDescriptorV2.data.source} from the
 * Core Schema so that the visual builder can auto-wire variables to the
 * correct upstream port without custom logic.
 *
 * Validation
 * ----------
 * - {@code required == true} → render fails with a ValidationError if missing.
 * - {@code maxLength} → render fails if the resolved value exceeds the cap.
 * This is the primary defence against prompt-injection via oversized RAG
 * chunks.
 * - {@code sensitive == true} → the audit layer redacts the value before
 * persisting.
 */
public final class VariableDescriptor {

    /** Variable name as it appears in the template body (e.g. "user_query"). */
    private final String name;

    /** Human-readable label for the visual builder's variable panel. */
    private final String displayName;

    /** Short help text shown next to the variable in the designer. */
    private final String description;

    /** Data type hint — used by the renderer for coercion and validation. */
    private final VariableType type;

    /**
     * Where the Engine should look for the concrete value at runtime.
     * Maps 1-to-1 to {@code PortDescriptorV2.data.source}.
     */
    private final VariableSource source;

    /** If true, rendering aborts when this variable has no value. */
    private final boolean required;

    /**
     * Fallback value used when {@code required == false} and no value is supplied.
     * Null means the variable is simply omitted from the rendered prompt.
     */
    private final String defaultValue;

    /**
     * Maximum character length of the resolved value.
     * Null means no explicit cap. Prevents runaway context from RAG or memory.
     */
    private final Integer maxLength;

    /**
     * When true, the Provenance / Audit layer redacts the concrete value
     * in all logs and snapshots. Use for API keys, PII, etc.
     */
    private final boolean sensitive;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public VariableDescriptor(
            String name,
            String displayName,
            String description,
            VariableType type,
            VariableSource source,
            boolean required,
            String defaultValue,
            Integer maxLength,
            boolean sensitive) {

        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(source, "source must not be null");

        if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException(
                    "Variable name must match ^[a-zA-Z_][a-zA-Z0-9_]*$: " + name);
        }
        if (required && defaultValue != null) {
            throw new IllegalArgumentException(
                    "Variable '" + name + "' cannot be both required and have a defaultValue");
        }

        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.source = source;
        this.required = required;
        this.defaultValue = defaultValue;
        this.maxLength = maxLength;
        this.sensitive = sensitive;
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String name() {
        return name;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public VariableType type() {
        return type;
    }

    public VariableSource source() {
        return source;
    }

    public boolean required() {
        return required;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public Integer maxLength() {
        return maxLength;
    }

    public boolean sensitive() {
        return sensitive;
    }

    public boolean hasDefault() {
        return defaultValue != null;
    }

    // -----------------------------------------------------------------------
    // Object contract
    // -----------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof VariableDescriptor that))
            return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "VariableDescriptor{name='%s', type=%s, source=%s, required=%b}"
                .formatted(name, type, source, required);
    }

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    /**
     * Where the Engine resolves the concrete value at runtime.
     * Mirrors {@code PortDescriptorV2.data.source} exactly.
     */
    public enum VariableSource {
        /** Supplied directly by the workflow trigger or previous node output. */
        INPUT,
        /** Resolved from the current NodeContext (merged bag). */
        CONTEXT,
        /** Fetched via RAG retrieval at render time. */
        RAG,
        /** Fetched from the agent's persistent memory store. */
        MEMORY,
        /** Read from the runtime environment (env vars, config). */
        ENVIRONMENT,
        /** Read from the secrets vault; value is always sensitive. */
        SECRET
    }
}