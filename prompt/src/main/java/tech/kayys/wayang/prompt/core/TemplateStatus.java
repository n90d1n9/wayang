package tech.kayys.wayang.prompt.core;

// ======================================================================
// TemplateStatus
// ======================================================================
// Finite-state lifecycle of a PromptTemplate version. Mirrors the
// publish-gate pattern used by WorkflowDefinition and PluginDescriptor
// in the Core Schema.
//
// Transitions (valid only):
// DRAFT → REVIEWING (author submits for review)
// REVIEWING → PUBLISHED (reviewer approves)
// REVIEWING → DRAFT (reviewer rejects → back to author)
// PUBLISHED → DEPRECATED (superseded by a newer version)
//
// Only PUBLISHED templates are executable by the PromptEngine.
// ======================================================================

/**
 * Publication lifecycle of a {@link PromptTemplate} version.
 *
 * <p>
 * The {@link PromptRegistry} enforces these transitions. Attempting
 * an invalid transition throws {@link IllegalStateException}.
 */
public enum TemplateStatus {

    /** Initial state. Template is editable, not executable. */
    DRAFT,

    /** Submitted for review. Frozen; edits require rejection back to DRAFT. */
    REVIEWING,

    /** Approved and executable by the PromptEngine. */
    PUBLISHED,

    /** Superseded. Still queryable for history but not resolved as "latest". */
    DEPRECATED;

    /**
     * Returns true if transitioning from {@code current} to {@code target}
     * is a legal move in the status FSM.
     */
    public static boolean isValidTransition(TemplateStatus current, TemplateStatus target) {
        return switch (current) {
            case DRAFT -> target == REVIEWING;
            case REVIEWING -> target == PUBLISHED || target == DRAFT;
            case PUBLISHED -> target == DEPRECATED;
            case DEPRECATED -> false;
        };
    }
}