package tech.kayys.golok.code.skills.manifest;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Parsed representation of a SKILL.md file following the
 * <a href="https://agentskills.io/specification.md">Agent Skills
 * specification</a>.
 *
 * <p>
 * A SkillManifest captures both the YAML frontmatter metadata and the
 * markdown body content. Optional reference files (from {@code references/})
 * are loaded lazily via {@link SkillManifestParser}.
 *
 * @author Bhangun
 */
public final class SkillManifest {

    // ── Frontmatter fields ────────────────────────────────────────

    private final String name;
    private final String description;
    private final String version;
    private final String license;
    private final String compatibility;
    private final boolean userInvocable;
    private final List<String> allowedTools;

    // ── Metadata sub-fields ───────────────────────────────────────

    private final String author;
    private final String metadataVersion;
    private final String emoji;
    private final String homepage;
    private final List<String> requiredBins;

    // ── Content ───────────────────────────────────────────────────

    private final String bodyContent;
    private final Path sourceDirectory;
    private final Map<String, String> references;

    // ── Installation tracking ─────────────────────────────────────

    private final String sourceRepo;
    private final Instant installedAt;

    private SkillManifest(Builder b) {
        this.name = Objects.requireNonNull(b.name, "name");
        this.description = b.description != null ? b.description : "";
        this.version = b.version != null ? b.version : "1.0.0";
        this.license = b.license;
        this.compatibility = b.compatibility;
        this.userInvocable = b.userInvocable;
        this.allowedTools = b.allowedTools != null
                ? Collections.unmodifiableList(new ArrayList<>(b.allowedTools))
                : List.of();
        this.author = b.author;
        this.metadataVersion = b.metadataVersion;
        this.emoji = b.emoji;
        this.homepage = b.homepage;
        this.requiredBins = b.requiredBins != null
                ? Collections.unmodifiableList(new ArrayList<>(b.requiredBins))
                : List.of();
        this.bodyContent = b.bodyContent != null ? b.bodyContent : "";
        this.sourceDirectory = b.sourceDirectory;
        this.references = b.references != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(b.references))
                : Map.of();
        this.sourceRepo = b.sourceRepo;
        this.installedAt = b.installedAt;
    }

    // ── Accessors ─────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getLicense() {
        return license;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public boolean isUserInvocable() {
        return userInvocable;
    }

    public List<String> getAllowedTools() {
        return allowedTools;
    }

    public String getAuthor() {
        return author;
    }

    public String getMetadataVersion() {
        return metadataVersion;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getHomepage() {
        return homepage;
    }

    public List<String> getRequiredBins() {
        return requiredBins;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public Map<String, String> getReferences() {
        return references;
    }

    public String getSourceRepo() {
        return sourceRepo;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }

    /**
     * Fully qualified identifier: {@code owner/repo@skill-name}.
     */
    public String getQualifiedId() {
        if (sourceRepo != null && !sourceRepo.isBlank()) {
            String repoName = extractRepoName(sourceRepo);
            return repoName + "@" + name;
        }
        return name;
    }

    /**
     * Estimated token count of the body (rough: ~4 chars per token).
     */
    public int estimateBodyTokens() {
        return bodyContent.length() / 4;
    }

    /**
     * Estimated token count of description (~4 chars per token).
     */
    public int estimateDescriptionTokens() {
        return description.length() / 4;
    }

    // ── Builder ───────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String description;
        private String version;
        private String license;
        private String compatibility;
        private boolean userInvocable;
        private List<String> allowedTools;
        private String author;
        private String metadataVersion;
        private String emoji;
        private String homepage;
        private List<String> requiredBins;
        private String bodyContent;
        private Path sourceDirectory;
        private Map<String, String> references;
        private String sourceRepo;
        private Instant installedAt;

        public Builder name(String v) {
            name = v;
            return this;
        }

        public Builder description(String v) {
            description = v;
            return this;
        }

        public Builder version(String v) {
            version = v;
            return this;
        }

        public Builder license(String v) {
            license = v;
            return this;
        }

        public Builder compatibility(String v) {
            compatibility = v;
            return this;
        }

        public Builder userInvocable(boolean v) {
            userInvocable = v;
            return this;
        }

        public Builder allowedTools(List<String> v) {
            allowedTools = v;
            return this;
        }

        public Builder author(String v) {
            author = v;
            return this;
        }

        public Builder metadataVersion(String v) {
            metadataVersion = v;
            return this;
        }

        public Builder emoji(String v) {
            emoji = v;
            return this;
        }

        public Builder homepage(String v) {
            homepage = v;
            return this;
        }

        public Builder requiredBins(List<String> v) {
            requiredBins = v;
            return this;
        }

        public Builder bodyContent(String v) {
            bodyContent = v;
            return this;
        }

        public Builder sourceDirectory(Path v) {
            sourceDirectory = v;
            return this;
        }

        public Builder references(Map<String, String> v) {
            references = v;
            return this;
        }

        public Builder sourceRepo(String v) {
            sourceRepo = v;
            return this;
        }

        public Builder installedAt(Instant v) {
            installedAt = v;
            return this;
        }

        public SkillManifest build() {
            return new SkillManifest(this);
        }
    }

    // ── Internal ──────────────────────────────────────────────────

    private static String extractRepoName(String url) {
        String clean = url.replaceAll("\\.git$", "");
        if (clean.contains("github.com/")) {
            return clean.substring(clean.indexOf("github.com/") + "github.com/".length());
        }
        int lastSlash = clean.lastIndexOf('/');
        return lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
    }

    @Override
    public String toString() {
        return "SkillManifest{name=" + name + ", version=" + version + ", author=" + author + "}";
    }
}
