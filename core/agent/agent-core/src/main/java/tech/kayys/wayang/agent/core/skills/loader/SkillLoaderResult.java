package tech.kayys.wayang.agent.core.skills.loader;

import java.util.List;

/**
 * Result of a skill loading operation (install, update, or list).
 *
 * @param sourceUrl       source URL or path (null if not applicable)
 * @param localPath       local filesystem path where skills were installed
 * @param skillsInstalled names of skills that were installed/found
 * @param errors          any errors that occurred during the operation
 * @param updated         true if this was an update rather than fresh install
 * @author Bhangun
 */
public record SkillLoaderResult(
        String sourceUrl,
        String localPath,
        List<String> skillsInstalled,
        List<String> errors,
        boolean updated) {

    /**
     * Check if the operation succeeded.
     *
     * @return true if no errors occurred
     */
    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }

    /**
     * Get the count of installed skills.
     *
     * @return number of skills installed
     */
    public int skillCount() {
        return skillsInstalled != null ? skillsInstalled.size() : 0;
    }

    /**
     * Get error message summary.
     *
     * @return concatenated error messages, or empty string if no errors
     */
    public String getErrorMessage() {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        return String.join("; ", errors);
    }
}
