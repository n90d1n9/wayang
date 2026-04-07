package tech.kayys.wayang.prompt.core;

import java.util.Objects;

/**
 * ============================================================================
 * PromptVariableValue — the concrete, resolved value for one variable.
 * ============================================================================
 *
 * Produced by the VariableResolver at render time and consumed by the
 * TemplateRenderer. The {@link #value} field holds the raw object; the
 * renderer coerces it to a String according to the VariableType declared in
 * the matching PromptVariableDefinition.
 *
 * Audit contract
 * --------------
 * If {@link #sensitive} is true, the Provenance layer writes
 * {@code "<REDACTED>"} in place of the actual value. The flag is copied from
 * the PromptVariableDefinition at resolution time so that the audit layer
 * never needs to look up the schema again.
 *
 * Null semantics
 * --------------
 * A null {@link #value} is legal *only* when the variable is not required.
 * The renderer treats it as "omit this variable from the rendered body".
 */
public final class PromptVariableValue {

    /** Name of the variable — matches PromptVariableDefinition.name. */
    private final String name;

    /**
     * The resolved value. May be:
     * - String, Number, Boolean for scalar types
     * - java.util.List<String> for STRING_LIST
     * - java.util.Map<String,Object> for JSON
     * - null when the variable is optional and no value was found
     */
    private final Object value;

    /** The source that provided this value (for provenance). */
    private final PromptVariableDefinition.VariableSource resolvedFrom;

    /**
     * Mirror of PromptVariableDefinition.sensitive. When true, the audit
     * layer must redact this value before persisting.
     */
    private final boolean sensitive;

    /** Timestamp at which this value was resolved (millis precision). */
    private final long resolvedAtEpochMs;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public PromptVariableValue(
            String name,
            Object value,
            PromptVariableDefinition.VariableSource resolvedFrom,
            boolean sensitive,
            long resolvedAtEpochMs) {

        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(resolvedFrom, "resolvedFrom must not be null");

        this.name = name;
        this.value = value;
        this.resolvedFrom = resolvedFrom;
        this.sensitive = sensitive;
        this.resolvedAtEpochMs = resolvedAtEpochMs;
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public PromptVariableDefinition.VariableSource getResolvedFrom() {
        return resolvedFrom;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public long getResolvedAtEpochMs() {
        return resolvedAtEpochMs;
    }

    /** Returns true when this value is absent (optional variable with no data). */
    public boolean isAbsent() {
        return value == null;
    }

    /**
     * Returns the value suitable for audit logging.
     * Sensitive values are replaced with the redaction sentinel.
     */
    public Object auditSafeValue() {
        return sensitive ? "<REDACTED>" : value;
    }

    // -----------------------------------------------------------------------
    // Object contract
    // -----------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PromptVariableValue that))
            return false;
        return Objects.equals(name, that.name)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "PromptVariableValue{name='%s', source=%s, sensitive=%b, absent=%b}"
                .formatted(name, resolvedFrom, sensitive, isAbsent());
    }
}
