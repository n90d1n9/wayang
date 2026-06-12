package tech.kayys.wayang.agent.core.skills.manifest;

import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Unified representation of a SKILL.md file following the
 * <a href="https://agentskills.io/specification">Agent Skills specification</a>.
 *
 * <p>
 * A SkillManifest captures both the YAML frontmatter metadata and the
 * markdown body content. Optional reference files (from {@code references/})
 * are loaded by the {@link SkillManifestParser}.
 *
 * <p>
 * Supports extended metadata for skill discovery, versioning, and lifecycle management.
 */
public final class SkillManifest {

    // ── Frontmatter fields (AgentSkills.io spec) ──────────────────

    private final String name;
    private final String description;
    private final String license;
    private final String compatibility;
    private final String allowedToolsString;
    private final List<String> allowedTools;

    // ── Extended metadata ─────────────────────────────────────────

    private final String version;
    private final boolean userInvocable;
    private final String author;
    private final String metadataVersion;
    private final String emoji;
    private final String homepage;
    private final List<String> requiredBins;
    private final Map<String, Object> metadata;

    // ── Content ───────────────────────────────────────────────────

    private final String bodyContent;
    private final Path sourceDirectory;
    private final Map<String, String> references;

    // ── Installation tracking ─────────────────────────────────────

    private final String sourceRepo;
    private final Instant installedAt;

    private SkillManifest(Builder b) {
        this.name = Objects.requireNonNull(b.name, "name is required");
        this.description = b.description != null ? b.description : "";
        this.license = b.license;
        this.compatibility = b.compatibility;
        this.allowedToolsString = b.allowedToolsString;
        this.allowedTools = b.allowedTools != null
                ? Collections.unmodifiableList(new ArrayList<>(b.allowedTools))
                : List.of();
        this.version = b.version != null ? b.version : "1.0.0";
        this.userInvocable = b.userInvocable;
        this.author = b.author;
        this.metadataVersion = b.metadataVersion;
        this.emoji = b.emoji;
        this.homepage = b.homepage;
        this.requiredBins = b.requiredBins != null
                ? Collections.unmodifiableList(new ArrayList<>(b.requiredBins))
                : List.of();
        this.metadata = b.metadata != null
                ? deepImmutableMap(b.metadata)
                : Map.of();
        this.bodyContent = b.bodyContent != null ? b.bodyContent : "";
        this.sourceDirectory = b.sourceDirectory;
        this.references = b.references != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(b.references))
                : Map.of();
        this.sourceRepo = b.sourceRepo;
        this.installedAt = b.installedAt;
    }

    // ── AgentSkills.io Spec Fields ────────────────────────────────

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLicense() {
        return license;
    }

    public String getCompatibility() {
        return compatibility;
    }

    public String getAllowedToolsString() {
        return allowedToolsString;
    }

    public List<String> getAllowedTools() {
        return allowedTools;
    }

    // ── Extended Metadata ─────────────────────────────────────────

    public String getVersion() {
        return version;
    }

    public boolean isUserInvocable() {
        return userInvocable;
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

    public Map<String, Object> getRawMetadata() {
        return metadata;
    }

    // ── Content ───────────────────────────────────────────────────

    public String getBodyContent() {
        return bodyContent;
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public Map<String, String> getReferences() {
        return references;
    }

    public Map<String, Object> getMetadata() {
        Map<String, Object> merged = new LinkedHashMap<>(metadata);
        merged.putIfAbsent(SkillMetadataKeys.KEY_VERSION, version);
        merged.putIfAbsent("userInvocable", userInvocable);
        merged.putIfAbsent("author", author);
        merged.putIfAbsent("metadataVersion", metadataVersion);
        merged.putIfAbsent("emoji", emoji);
        merged.putIfAbsent("homepage", homepage);
        merged.putIfAbsent("requiredBins", requiredBins);
        merged.putIfAbsent("allowedTools", allowedTools);
        return Collections.unmodifiableMap(merged);
    }

    // ── Installation Tracking ────────────────────────────────────

    public String getSourceRepo() {
        return sourceRepo;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }

    // ── Computed Properties ───────────────────────────────────────

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

    /**
     * Total estimated tokens (metadata + body, for context budgeting).
     */
    public int estimateTotalTokens() {
        return estimateDescriptionTokens() + estimateBodyTokens() + (name.length() / 4);
    }

    // ── Builder ───────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String description;
        private String license;
        private String compatibility;
        private String allowedToolsString;
        private List<String> allowedTools;
        private String version;
        private boolean userInvocable;
        private String author;
        private String metadataVersion;
        private String emoji;
        private String homepage;
        private List<String> requiredBins;
        private Map<String, Object> metadata;
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

        public Builder license(String v) {
            license = v;
            return this;
        }

        public Builder compatibility(String v) {
            compatibility = v;
            return this;
        }

        public Builder allowedToolsString(String v) {
            allowedToolsString = v;
            return this;
        }

        public Builder allowedTools(List<String> v) {
            allowedTools = v;
            return this;
        }

        public Builder version(String v) {
            version = v;
            return this;
        }

        public Builder userInvocable(boolean v) {
            userInvocable = v;
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

        public Builder metadata(Map<String, Object> v) {
            metadata = v;
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

    private static Map<String, Object> deepImmutableMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, deepImmutableValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static Object deepImmutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> copy.put(String.valueOf(key), deepImmutableValue(nestedValue)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(SkillManifest::deepImmutableValue)
                    .toList();
        }
        return value;
    }

    @Override
    public String toString() {
        return "SkillManifest{name=" + name + ", version=" + version + ", author=" + author + "}";
    }
}
