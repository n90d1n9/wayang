package tech.kayys.wayang.agent.core.skills.loader;

import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.nio.file.Path;
import java.util.List;

/**
 * Unified service for loading skills from various sources.
 *
 * <p>Supports:
 * <ul>
 *   <li>Local directory loading and installation</li>
 *   <li>Git repository cloning and updating</li>
 *   <li>Skill discovery and validation</li>
 *   <li>Repository management (list, update, remove)</li>
 * </ul>
 *
 * <p>Consolidates functionality previously split across LocalSkillLoader
 * and GitSkillLoader.
 *
 * @author Bhangun
 */
public interface SkillsLoaderService {

    /**
     * Install skills from a local directory.
     *
     * @param localPath   path to local directory containing skills
     * @param skillFilter optional glob filter for specific skills
     * @return installation result
     */
    SkillLoaderResult installLocal(String localPath, String skillFilter);

    /**
     * Install skills from a git repository.
     *
     * @param repoUrl     git repository URL
     * @param skillFilter optional glob filter for specific skills
     * @return installation result
     */
    SkillLoaderResult installGit(String repoUrl, String skillFilter);

    /**
     * Update a previously installed repository.
     *
     * @param repoName repository name (directory name under skills base)
     * @return update result
     */
    SkillLoaderResult update(String repoName);

    /**
     * Remove an installed repository.
     *
     * @param repoName repository name
     * @return true if successfully removed
     */
    boolean remove(String repoName);

    /**
     * List all installed repositories.
     *
     * @return list of repository directory names
     */
    List<String> listInstalledRepos();

    /**
     * Load and parse all skills from a directory.
     *
     * @param directory directory to scan for SKILL.md files
     * @return list of parsed skill manifests
     */
    List<SkillManifest> loadSkillsFromDirectory(Path directory);

    /**
     * Get the base directory where skills are stored.
     *
     * @return skills base directory
     */
    Path getSkillsBaseDir();
}
