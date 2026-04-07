package tech.kayys.wayang.prompt.core;

/**
 * Classification of validation warnings emitted by the Prompt Engine.
 * Used by the visual editor to categorize issues and suggest fixes.
 */
public enum ValidationWarningType {
    /**
     * A variable is declared in the template's schema but never appears
     * as a placeholder in the template body.
     */
    DECLARED_BUT_MISSING,

    /**
     * A {{name}} placeholder appears in the template body but has no
     * corresponding variable declaration in the schema.
     */
    PLACEHOLDER_UNDECLARED,

    /** Template is PUBLISHED but has no active version set. */
    MISSING_ACTIVE_VERSION,

    /** A {{name}} placeholder in the body has no variable definition. */
    UNDECLARED_VARIABLE,

    /** A required variable definition is not referenced in the body. */
    UNUSED_VARIABLE
}