package tech.kayys.gollek.agent.skills.skills.store;

import io.smallrye.mutiny.Uni;
import tech.kayys.gollek.agent.skills.skills.manifest.SkillManifest;

import java.util.List;
import java.util.Map;

/**
 * Adapts an external {@link SkillManifest} (parsed from SKILL.md) into
 * a format compatible with the golok agent skill system.
 *
 * <p>
 * External skills (from git repos) differ from internal Java skills:
 * <ul>
 * <li>They don't have a Java {@code AgentSkill} implementation</li>
 * <li>Their "execution" means injecting the SKILL.md body + references
 * into the LLM context as system instructions</li>
 * <li>They are triggered by description matching, not by tool calls</li>
 * </ul>
 *
 * <p>
 * This adapter bridges the gap, enabling external skills to participate
 * in the agent's skill registry alongside internal skills.
 *
 * @author Bhangun
 */
public class ExternalSkillAdapter {

    private final SkillManifest manifest;

    public ExternalSkillAdapter(SkillManifest manifest) {
        this.manifest = manifest;
    }

    // ── Identity ──────────────────────────────────────────────────

    public String id() {
        return manifest.getName();
    }

    public String name() {
        String emoji = manifest.getEmoji();
        return (emoji != null ? emoji + " " : "") + manifest.getName();
    }

    public String description() {
        return manifest.getDescription();
    }

    public String version() {
        return manifest.getVersion();
    }

    public boolean isUserInvocable() {
        return manifest.isUserInvocable();
    }

    // ── Context Injection ─────────────────────────────────────────

    /**
     * Build the full context to inject into the LLM prompt when this skill
     * triggers.
     *
     * <p>
     * Progressive disclosure:
     * <ol>
     * <li>Always inject the SKILL.md body</li>
     * <li>Optionally inject referenced documents based on relevance</li>
     * </ol>
     *
     * @return formatted skill context for LLM consumption
     */
    public String buildSkillContext() {
        StringBuilder ctx = new StringBuilder();

        ctx.append("## Skill: ").append(manifest.getName()).append("\n\n");

        // Inject SKILL.md body
        ctx.append(manifest.getBodyContent()).append("\n");

        return ctx.toString();
    }

    /**
     * Build extended context including referenced documents.
     *
     * @param referenceNames specific references to include (null = all)
     * @return extended context
     */
    public String buildExtendedContext(List<String> referenceNames) {
        StringBuilder ctx = new StringBuilder(buildSkillContext());

        Map<String, String> refs = manifest.getReferences();
        if (refs.isEmpty())
            return ctx.toString();

        ctx.append("\n---\n\n## References\n\n");

        for (Map.Entry<String, String> ref : refs.entrySet()) {
            if (referenceNames == null || referenceNames.contains(ref.getKey())) {
                ctx.append("### ").append(ref.getKey()).append("\n\n");
                ctx.append(ref.getValue()).append("\n\n");
            }
        }

        return ctx.toString();
    }

    /**
     * Check if this skill should trigger based on a user query.
     *
     * <p>
     * Uses simple keyword matching against the skill description.
     * In production, this should be replaced with embedding-based
     * semantic similarity.
     *
     * @param query user query or context
     * @return relevance score (0.0 to 1.0)
     */
    public double matchScore(String query) {
        if (query == null || query.isBlank())
            return 0.0;

        String lowerQuery = query.toLowerCase();
        String lowerDesc = manifest.getDescription().toLowerCase();
        String lowerName = manifest.getName().toLowerCase();

        // Exact name match = highest score
        if (lowerQuery.contains(lowerName))
            return 1.0;

        // Keyword overlap scoring
        String[] queryWords = lowerQuery.split("\\s+");
        String[] descWords = lowerDesc.split("\\s+");
        int matches = 0;
        for (String qw : queryWords) {
            if (qw.length() < 3)
                continue; // skip short words
            for (String dw : descWords) {
                if (dw.contains(qw) || qw.contains(dw)) {
                    matches++;
                    break;
                }
            }
        }

        return queryWords.length > 0 ? (double) matches / queryWords.length : 0.0;
    }

    /**
     * Estimated token cost of loading this skill's body.
     */
    public int estimatedTokens() {
        return manifest.estimateBodyTokens();
    }

    /**
     * Get the underlying manifest.
     */
    public SkillManifest getManifest() {
        return manifest;
    }

    @Override
    public String toString() {
        return "ExternalSkillAdapter{id=" + id() + ", version=" + version() + "}";
    }
}
