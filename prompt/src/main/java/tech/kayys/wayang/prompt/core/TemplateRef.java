package tech.kayys.wayang.prompt.core;

import java.util.Objects;

/**
 * ============================================================================
 * TemplateRef — lightweight reference to a prompt template.
 * ============================================================================
 *
 * Two resolution modes:
 * • {@link #latest(String)} – resolve the latest PUBLISHED version
 * of the given template ID.
 * • {@link #pinned(String, String)} – resolve an exact version (SemVer).
 *
 * Pinning is useful for reproducibility: a workflow run always uses the
 * exact prompt that was tested, even if a newer version has been published.
 */
public record TemplateRef(String id, String version) {

    /**
     * Reference that resolves to the latest published version.
     * {@code version} is {@code null}.
     */
    public static TemplateRef latest(String id) {
        return new TemplateRef(Objects.requireNonNull(id), null);
    }

    /**
     * Reference pinned to an exact SemVer version.
     */
    public static TemplateRef pinned(String id, String version) {
        return new TemplateRef(
                Objects.requireNonNull(id),
                Objects.requireNonNull(version));
    }

    /** {@code true} when this ref resolves to the latest published version. */
    public boolean isLatest() {
        return version == null;
    }
}