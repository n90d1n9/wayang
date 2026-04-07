package tech.kayys.wayang.agent.shaker;

import java.nio.file.Path;

/**
 * Configuration for agent packaging.
 *
 * @param mainClass main class for JAR manifest
 * @param optimize whether to enable tree-shaking optimization
 * @param maxSteps default max steps for agent
 * @param timeout default timeout for agent execution
 * @param skillsDir directory containing skill definitions (SKILL.md files)
 * @param outputPath output directory for generated artifacts
 */
public record PackageConfig(
        String mainClass,
        boolean optimize,
        int maxSteps,
        String timeout,
        Path skillsDir,
        Path outputPath) {

    /**
     * Create builder for package configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default configuration.
     */
    public static PackageConfig defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private String mainClass = "tech.kayys.wayang.agent.core.core.AgentClient";
        private boolean optimize = true;
        private int maxSteps = 10;
        private String timeout = "PT60S";
        private Path skillsDir = Path.of("skills");
        private Path outputPath = Path.of("output");

        private Builder() {}

        public Builder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public Builder optimize(boolean optimize) {
            this.optimize = optimize;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder timeout(String timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder skillsDir(Path skillsDir) {
            this.skillsDir = skillsDir;
            return this;
        }

        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public PackageConfig build() {
            return new PackageConfig(mainClass, optimize, maxSteps, timeout, skillsDir, outputPath);
        }
    }
}
