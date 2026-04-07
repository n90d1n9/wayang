package tech.kayys.golok.code.skills.loader;

import java.util.List;

/**
 * Result of installing skills from a git repository.
 *
 * @param repoUrl         the source repository URL
 * @param localPath       local filesystem path where the repo was cloned
 * @param skillsInstalled names of skills that were installed
 * @param errors          any errors that occurred during installation
 * @param updated         true if this was an update (git pull) rather than
 *                        fresh clone
 *
 * @author Bhangun
 */
public record SkillInstallResult(
        String repoUrl,
        String localPath,
        List<String> skillsInstalled,
        List<String> errors,
        boolean updated) {
    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }

    public int skillCount() {
        return skillsInstalled != null ? skillsInstalled.size() : 0;
    }
}
