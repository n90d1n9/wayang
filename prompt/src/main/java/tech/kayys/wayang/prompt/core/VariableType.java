package tech.kayys.wayang.prompt.core;

// ======================================================================
// VariableType
// ======================================================================
// The expected runtime type of a template variable's value.
// Used for pre-render validation and for the editor's type-safe form.
// ======================================================================

/**
 * Expected runtime type of a {@link VariableDescriptor}'s value.
 *
 * <p>
 * The renderer coerces the supplied value to a string via each type's
 * {@link #coerce(Object, JsonCoercer)} method before interpolation. For OBJECT and
 * ARRAY the coerced form is compact JSON.
 */
public enum VariableType {

    /** Plain text. toString() is used directly. */
    STRING {
        @Override
        public String coerce(Object value, JsonCoercer jsonCoercer) {
            return value == null ? "" : value.toString();
        }
    },

    /** Numeric value. Rendered without trailing zeros where possible. */
    NUMBER {
        @Override
        public String coerce(Object value, JsonCoercer jsonCoercer) {
            if (value == null)
                return "0";
            if (value instanceof Number n) {
                double d = n.doubleValue();
                return d == Math.floor(d) && !Double.isInfinite(d)
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            return value.toString();
        }
    },

    /** Boolean. Rendered as "true" / "false". */
    BOOLEAN {
        @Override
        public String coerce(Object value, JsonCoercer jsonCoercer) {
            return value == null ? "false" : String.valueOf(Boolean.parseBoolean(value.toString()));
        }
    },

    /**
     * Structured object. Rendered as compact JSON.
     * The renderer delegates to the platform's JSON serialiser.
     */
    OBJECT {
        @Override
        public String coerce(Object value, JsonCoercer jsonCoercer) {
            if (value == null) return "{}";
            if (jsonCoercer != null) {
                return jsonCoercer.toJson(value);
            }
            // Fallback: actual JSON serialisation is handled by
            // PromptRenderer which has access to the Jackson ObjectMapper.
            // This fallback is only used in standalone contexts without Jackson.
            return value.toString();
        }
    },

    /**
     * Array of values. Rendered as a JSON array string.
     */
    ARRAY {
        @Override
        public String coerce(Object value, JsonCoercer jsonCoercer) {
            if (value == null) return "[]";
            if (jsonCoercer != null) {
                return jsonCoercer.toJson(value);
            }
            return value.toString();
        }
    };

    /**
     * Coerce a runtime value to its string representation for interpolation.
     * Subclasses override for type-specific formatting.
     */
    public abstract String coerce(Object value, JsonCoercer jsonCoercer);

    /**
     * Convenience method for backward compatibility - uses null for jsonCoercer.
     */
    public String coerce(Object value) {
        return coerce(value, null);
    }
}
