package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsCommandTest {

    @Test
    void registersListsAndShowsSkills() {
        TestConsole console = new TestConsole();
        CommandLine command = command(console);

        assertThat(command.execute(
                "register",
                "planner",
                "--name",
                "Planner",
                "--category",
                "REASONING",
                "--system-prompt",
                "Plan carefully."))
                .isZero();
        assertThat(command.execute("list", "--all")).isZero();
        assertThat(command.execute("info", "planner")).isZero();

        assertThat(console.out())
                .contains("Registered skill: planner")
                .contains("planner\tREASONING\tPlanner")
                .contains("status: ACTIVE");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void togglesSkillLifecycleFromCli() {
        TestConsole console = new TestConsole();
        CommandLine command = command(console);

        assertThat(command.execute(
                "register",
                "planner",
                "--name",
                "Planner",
                "--system-prompt",
                "Plan carefully."))
                .isZero();
        assertThat(command.execute("disable", "planner")).isZero();
        assertThat(command.execute("info", "planner")).isZero();
        assertThat(command.execute("enable", "planner")).isZero();

        assertThat(console.out())
                .contains("Disabled skill: planner (DISABLED)")
                .contains("status: DISABLED")
                .contains("Enabled skill: planner (ACTIVE)");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void showsDefaultPersistenceStatus() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status")).isZero();

        assertThat(console.out())
                .contains("config source: default")
                .contains("persistence strategy: ephemeral")
                .contains("fully durable: false")
                .contains("roles: 4 durable=0 ephemeral=3 disabled=1 custom=0")
                .contains("warnings:")
                .contains("- Disabled skill persistence roles: event-history")
                .contains("- definition: provider=registry class=runtime-registry strategy=ephemeral");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void listsPersistenceProfiles() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("profiles")).isZero();

        assertThat(console.out())
                .contains("persistence profiles: 6 durable=5")
                .contains("- default: strategy=ephemeral durable=false roles=4 warnings=2")
                .contains("aliases: runtime,registry,memory,ephemeral,dev,development")
                .contains("- object-storage: strategy=object-storage durable=true roles=4 warnings=0")
                .contains("aliases: object,s3,rustfs,cloud,cloud-storage")
                .contains("- hybrid-object-file: strategy=hybrid-fallback durable=true roles=4 warnings=0")
                .contains("providers: external=true composite=true mirrored=false durable-fallback=true")
                .contains("- mirrored-object-file: strategy=mirrored durable=true roles=4 warnings=0");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersPersistenceProfilesAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("profiles", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"profileCount\":6")
                .contains("\"durableProfileCount\":5")
                .contains("\"profiles\":[")
                .contains("\"label\":\"default\"")
                .contains("\"aliases\":[\"runtime\",\"registry\",\"memory\",\"ephemeral\",\"dev\",\"development\"]")
                .contains("\"label\":\"object-storage\"")
                .contains("\"aliases\":[\"object\",\"s3\",\"rustfs\",\"cloud\",\"cloud-storage\"]")
                .contains("\"strategy\":\"hybrid-fallback\"")
                .contains("\"hasDurableFallback\":true")
                .contains("\"label\":\"mirrored-object-file\"")
                .contains("\"hasMirroredProvider\":true");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void explainsRuntimeConfigHints() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "explain")).isZero();

        assertThat(console.out())
                .contains("runtime config hints: groups=6 hints=33")
                .contains("runtime config sources:")
                .contains("- microprofile-config: When MicroProfile Config is present")
                .contains("profile selectors:")
                .contains("properties: wayang.skills.profile,wayang.skills.runtime.profile")
                .contains("environment: WAYANG_SKILLS_PROFILE,WAYANG_SKILLS_SERVICE_PROFILE")
                .contains("profile option defaults:")
                .contains("- object-prefix: Object key prefix used by object-storage")
                .contains("default: wayang/skills")
                .contains("object storage provider settings:")
                .contains("- gcs.bucket: GCS bucket used when the object-storage provider is Google Cloud Storage.")
                .contains("- azure.connection-string: Azure Blob Storage connection string used by the Azure object-storage provider.")
                .contains("role store override prefixes:")
                .contains("properties: wayang.skills.store.")
                .contains("environment: WAYANG_SKILLS_STORE_")
                .contains("store option suffixes:")
                .contains("- primary-and-fallback: Nested child prefixes required by hybrid and mirrored stores.");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRuntimeConfigHintsAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "explain", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"groupCount\":6")
                .contains("\"hintCount\":33")
                .contains("\"name\":\"profile-selectors\"")
                .contains("\"properties\":[\"wayang.skills.profile\",\"wayang.skills.runtime.profile\"")
                .contains("\"environment\":[\"WAYANG_SKILLS_PROFILE\",\"WAYANG_SKILLS_SERVICE_PROFILE\"")
                .contains("\"defaultValue\":\"default\"")
                .contains("\"name\":\"object-storage-provider\"")
                .contains("\"name\":\"gcs.bucket\"")
                .contains("\"name\":\"azure.connection-string\"")
                .contains("\"name\":\"store-overrides\"")
                .contains("\"properties\":[\"wayang.skills.store.\"]")
                .contains("\"environment\":[\"WAYANG_SKILLS_STORE_\"]")
                .contains("\"name\":\"store-option-suffixes\"")
                .contains("\"properties\":[\"primary.*\",\"fallback.*\"]");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void explainsOneRuntimeConfigHintGroup() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "explain", "--group", "store-overrides")).isZero();

        assertThat(console.out())
                .contains("runtime config hints: groups=1 hints=5")
                .contains("role store override prefixes:")
                .contains("- definition: Overrides the skill definition store role.")
                .contains("properties: wayang.skills.store.")
                .contains("- lifecycle-reconcile: Overrides lifecycle state reconciliation behavior");
        assertThat(console.out())
                .doesNotContain("profile selectors:")
                .doesNotContain("store option suffixes:");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersOneRuntimeConfigHintGroupAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "explain", "--group", "profile-options", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .contains("\"groupCount\":1")
                .contains("\"hintCount\":4")
                .contains("\"name\":\"profile-options\"")
                .contains("\"name\":\"base-directory\"")
                .contains("\"name\":\"initialize-jdbc-schema\"");
        assertThat(console.out())
                .doesNotContain("\"name\":\"store-overrides\"")
                .doesNotContain("\"name\":\"runtime-sources\"");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rejectsUnknownRuntimeConfigHintGroup() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "explain", "--group", "missing-group"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Unknown runtime config hint group: missing-group");
    }

    @Test
    void listsRuntimeConfigHintGroups() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "groups")).isZero();

        assertThat(console.out())
                .contains("runtime config groups: 6 hints=33")
                .contains("- runtime-sources: runtime config sources hints=2")
                .contains("- profile-selectors: profile selectors hints=1")
                .contains("- profile-options: profile option defaults hints=4")
                .contains("- object-storage-provider: object storage provider settings hints=13")
                .contains("- store-overrides: role store override prefixes hints=5")
                .contains("- store-option-suffixes: store option suffixes hints=8");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRuntimeConfigHintGroupsAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "groups", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"groupCount\":6")
                .contains("\"hintCount\":33")
                .contains("\"groups\":[{\"name\":\"runtime-sources\"")
                .contains("\"label\":\"runtime config sources\"")
                .contains("\"name\":\"profile-options\"")
                .contains("\"hintCount\":4")
                .contains("\"name\":\"object-storage-provider\"")
                .contains("\"hintCount\":13")
                .contains("\"name\":\"store-option-suffixes\"")
                .contains("\"hintCount\":8");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void listsRuntimeConfigSamples() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "samples")).isZero();

        assertThat(console.out())
                .contains("runtime config samples: 12")
                .contains("- object-storage: profile=object-storage provider=s3-rustfs")
                .contains("  aliases: object,s3,rustfs,cloud,cloud-storage")
                .contains("- gcs: profile=object-storage provider=gcs")
                .contains("  aliases: google-cloud-storage,gcs-object-storage")
                .contains("- hybrid-azure: profile=hybrid-object-file provider=azure")
                .contains("- mirrored-gcs: profile=mirrored-object-file provider=gcs");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRuntimeConfigSamplesAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "samples", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"sampleCount\":12")
                .contains("\"name\":\"gcs\"")
                .contains("\"profile\":\"object-storage\"")
                .contains("\"objectStorageProvider\":\"gcs\"")
                .contains("\"aliases\":[\"google-cloud-storage\",\"gcs-object-storage\"]")
                .contains("\"name\":\"mirrored-azure\"")
                .contains("\"objectStorageProvider\":\"azure\"");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void validatesDefaultRuntimeConfig() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "validate")).isZero();

        assertThat(console.out())
                .contains("config source: default")
                .contains("config valid: true")
                .contains("durability required: false")
                .contains("validation passed: true")
                .contains("errors: 0")
                .contains("policy errors: 0")
                .contains("persistence strategy: ephemeral")
                .contains("fully durable: false")
                .contains("warnings: 2")
                .contains("- Disabled skill persistence roles: event-history");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void validatesProfileRuntimeConfigAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "validate", "--profile", "rustfs", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"source\":\"profile\"")
                .contains("\"profile\":\"object-storage\"")
                .contains("\"runtime\":false")
                .contains("\"valid\":true")
                .contains("\"durabilityRequired\":false")
                .contains("\"passed\":true")
                .contains("\"errorCount\":0")
                .contains("\"errors\":[]")
                .contains("\"policyErrorCount\":0")
                .contains("\"policyErrors\":[]")
                .contains("\"strategy\":\"object-storage\"")
                .contains("\"fullyDurable\":true")
                .contains("\"warningCount\":0")
                .contains("\"warnings\":[]");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void requiresDurableRuntimeConfigWhenRequested() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "validate", "--require-durable"))
                .isEqualTo(1);

        assertThat(console.out())
                .contains("config source: default")
                .contains("config valid: true")
                .contains("durability required: true")
                .contains("validation passed: false")
                .contains("errors: 0")
                .contains("policy errors: 1")
                .contains("- Fully durable skill persistence is required.")
                .contains("fully durable: false");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void passesDurableRuntimeConfigRequirementForDurableProfile() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute(
                "config",
                "validate",
                "--profile",
                "rustfs",
                "--require-durable",
                "--json"))
                .isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .contains("\"profile\":\"object-storage\"")
                .contains("\"valid\":true")
                .contains("\"durabilityRequired\":true")
                .contains("\"passed\":true")
                .contains("\"policyErrorCount\":0")
                .contains("\"policyErrors\":[]")
                .contains("\"fullyDurable\":true");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rejectsConflictingRuntimeConfigValidationSources() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "validate", "--profile", "rustfs", "--runtime"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Choose only one skill persistence config source: --profile or --runtime.");
    }

    @Test
    void rejectsUnknownRuntimeConfigValidationProfile() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "validate", "--profile", "missing-profile"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Unknown skill-management service profile: missing-profile");
    }

    @Test
    void resolvesProfileRuntimeConfig() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "resolve", "--profile", "hybrid")).isZero();

        assertThat(console.out())
                .contains("config source: profile (hybrid-object-file)")
                .contains("config valid: true")
                .contains("errors: 0")
                .contains("resolved config:")
                .contains("lifecycle reconcile: inspect-only create-missing=false remove-orphans=false")
                .contains("- definition: kind=hybrid target=composed jdbc-init=false max-events=0")
                .contains("  - primary: kind=object-storage target=wayang/skills/definitions")
                .contains("  - fallback: kind=filesystem target=.wayang/skills/definitions")
                .contains("persistence strategy: hybrid-fallback")
                .contains("fully durable: true")
                .contains("warnings: -");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void resolvesProfileRuntimeConfigAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "resolve", "--profile", "rustfs", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"source\":\"profile\"")
                .contains("\"profile\":\"object-storage\"")
                .contains("\"runtime\":false")
                .contains("\"valid\":true")
                .contains("\"diagnostics\":{\"lifecycleReconcile\":\"inspect-only\"")
                .contains("\"stores\":[{\"role\":\"definition\",\"kind\":\"object-storage\"")
                .contains("\"target\":\"wayang/skills/definitions\"")
                .contains("\"strategy\":\"object-storage\"")
                .contains("\"fullyDurable\":true")
                .contains("\"warningCount\":0")
                .contains("\"warnings\":[]");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rejectsConflictingRuntimeConfigResolveSources() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "resolve", "--profile", "rustfs", "--runtime"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Choose only one skill persistence config source: --profile or --runtime.");
    }

    @Test
    void rendersRuntimeConfigSampleAsProperties() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "sample", "rustfs")).isZero();

        assertThat(console.out())
                .contains("# skill-management runtime config sample: object-storage")
                .contains("# Durable S3/RustFS-compatible object-storage profile")
                .contains("wayang.skills.profile=object-storage")
                .contains("wayang.skills.profile.object-prefix=wayang/skills")
                .contains("wayang.skills.profile.max-events=10000")
                .contains("wayang.skills.lifecycle.reconcile.mode=inspect-only");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRuntimeConfigSampleAsEnvironment() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "sample", "hybrid", "--format", "env")).isZero();

        assertThat(console.out())
                .contains("# skill-management runtime config sample: hybrid-object-file")
                .contains("WAYANG_SKILLS_PROFILE=hybrid-object-file")
                .contains("WAYANG_SKILLS_PROFILE_BASE_DIRECTORY=.wayang/skills")
                .contains("WAYANG_SKILLS_PROFILE_OBJECT_PREFIX=wayang/skills")
                .contains("WAYANG_SKILLS_PROFILE_MAX_EVENTS=10000")
                .contains("WAYANG_SKILLS_LIFECYCLE_RECONCILE_MODE=inspect-only");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersRuntimeConfigSampleAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "sample", "rustfs", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"profile\":\"object-storage\"")
                .contains("\"propertyCount\":11")
                .contains("\"environmentCount\":11")
                .contains("\"properties\":[{\"key\":\"wayang.skills.profile\"")
                .contains("\"key\":\"wayang.skills.profile.object-prefix\"")
                .contains("\"value\":\"wayang/skills\"")
                .contains("\"key\":\"wayang.storage.s3.endpoint\"")
                .contains("\"environment\":[{\"key\":\"WAYANG_SKILLS_PROFILE\"")
                .contains("\"key\":\"WAYANG_SKILLS_PROFILE_OBJECT_PREFIX\"")
                .contains("\"key\":\"WAYANG_STORAGE_S3_ENDPOINT\"");
        assertThat(console.out()).doesNotContain("# skill-management runtime config sample");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rejectsUnknownRuntimeConfigSampleFormat() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "sample", "rustfs", "--format", "yaml"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Unknown config sample format: yaml (expected properties or env)");
    }

    @Test
    void inspectsPersistenceProfile() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("profile", "inspect", "hybrid", "--diagnostics")).isZero();

        assertThat(console.out())
                .contains("profile: hybrid-object-file")
                .contains("aliases: hybrid,object-file,object-with-file-fallback,cloud-file-fallback")
                .contains("description: Durable hybrid profile that prefers object storage and falls back to local files.")
                .contains("config source: profile (hybrid-object-file)")
                .contains("config diagnostics:")
                .contains("- definition: kind=hybrid target=composed jdbc-init=false max-events=0")
                .contains("  - primary: kind=object-storage target=wayang/skills/definitions")
                .contains("  - fallback: kind=filesystem target=.wayang/skills/definitions")
                .contains("persistence strategy: hybrid-fallback");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersPersistenceProfileInspectionAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("profile", "inspect", "rustfs", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"label\":\"object-storage\"")
                .contains("\"aliases\":[\"object\",\"s3\",\"rustfs\",\"cloud\",\"cloud-storage\"]")
                .contains("\"status\":{\"source\":\"profile\"")
                .contains("\"profile\":\"object-storage\"")
                .contains("\"strategy\":\"object-storage\"")
                .contains("\"fullyDurable\":true")
                .contains("\"hasExternalProvider\":true");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void reportsUnknownProfileInspectionProfile() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("profile", "inspect", "missing-profile"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Unknown skill-management service profile: missing-profile");
    }

    @Test
    void previewsProfilePersistenceStatus() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--profile", "hybrid")).isZero();

        assertThat(console.out())
                .contains("config source: profile (hybrid-object-file)")
                .contains("persistence strategy: hybrid-fallback")
                .contains("fully durable: true")
                .contains("roles: 4 durable=4 ephemeral=0 disabled=0 custom=0")
                .contains("providers: external=true composite=true mirrored=false durable-fallback=true")
                .contains("- definition: provider=hybrid class=composed strategy=hybrid-fallback")
                .contains("  - definition: provider=object-storage class=object-storage strategy=object-storage")
                .contains("  - definition: provider=filesystem class=filesystem strategy=local-filesystem")
                .contains("warnings: -");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rejectsConflictingPersistenceStatusSources() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--profile", "hybrid", "--runtime"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Choose only one skill persistence config source: --profile or --runtime.");
    }

    @Test
    void reportsUnknownPersistenceStatusProfile() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--profile", "missing-profile"))
                .isEqualTo(1);

        assertThat(console.out()).isEmpty();
        assertThat(console.err())
                .contains("Unknown skill-management service profile: missing-profile");
    }

    @Test
    void includesPersistencePreflightStatusWhenRequested() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--preflight")).isZero();

        assertThat(console.out())
                .contains("config source: default")
                .contains("persistence strategy: ephemeral")
                .contains("preflight: ready=true deployable=true errors=0")
                .doesNotContain("preflight message:");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void includesPersistenceConfigDiagnosticsWhenRequested() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--diagnostics")).isZero();

        assertThat(console.out())
                .contains("config diagnostics:")
                .contains("lifecycle reconcile: inspect-only create-missing=false remove-orphans=false")
                .contains("- definition: kind=registry target=registry jdbc-init=false max-events=0")
                .contains("- lifecycle-state: kind=memory target=memory jdbc-init=false max-events=0")
                .contains("- event-history: kind=none target=none jdbc-init=false max-events=0")
                .contains("- artifact: kind=memory target=memory jdbc-init=false max-events=0");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersPersistenceStatusAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--profile", "hybrid", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .endsWith("}")
                .contains("\"source\":\"profile\"")
                .contains("\"profile\":\"hybrid-object-file\"")
                .contains("\"runtime\":false")
                .contains("\"preflightAvailable\":false")
                .contains("\"diagnosticsAvailable\":false")
                .contains("\"strategy\":\"hybrid-fallback\"")
                .contains("\"fullyDurable\":true")
                .contains("\"roleCount\":4")
                .contains("\"roles\":[")
                .contains("\"provider\":\"object-storage\"")
                .contains("\"children\":[");
        assertThat(console.out()).doesNotContain("persistence strategy:");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersPersistenceDiagnosticsAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--profile", "hybrid", "--diagnostics", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .contains("\"diagnosticsAvailable\":true")
                .contains("\"diagnostics\":{\"lifecycleReconcile\":\"inspect-only\"")
                .contains("\"stores\":[{\"role\":\"definition\",\"kind\":\"hybrid\",\"target\":\"composed\"")
                .contains("\"role\":\"primary\",\"kind\":\"object-storage\"")
                .contains("\"role\":\"fallback\",\"kind\":\"filesystem\"");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersPersistencePreflightStatusAsJson() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("status", "--preflight", "--json")).isZero();

        assertThat(console.out().trim())
                .startsWith("{")
                .contains("\"source\":\"default\"")
                .contains("\"preflightAvailable\":true")
                .contains("\"preflight\":{\"ready\":true")
                .contains("\"deployable\":true")
                .contains("\"errorCount\":0")
                .contains("\"configuration\":{\"valid\":true")
                .contains("\"targetStores\":{\"valid\":true")
                .contains("\"capabilities\":{\"valid\":true");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void validatesMissingFieldsThroughHandler() {
        TestConsole console = new TestConsole();
        SkillsCommandHandler handler = SkillsCommandHandler.inMemory(console.outStream(), console.errStream());

        assertThat(handler.validate("bad", "", "", "custom", "")).isEqualTo(1);

        assertThat(console.err())
                .contains("Skill name is required")
                .contains("System prompt is required");
    }

    @Test
    void returnsNonZeroForMissingSkill() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("info", "missing")).isEqualTo(1);

        assertThat(console.err()).contains("Skill not found: missing");
    }

    private CommandLine command(TestConsole console) {
        SkillsCommandHandler handler = SkillsCommandHandler.inMemory(console.outStream(), console.errStream());
        return new CommandLine(new SkillsCommand(handler));
    }

    private static final class TestConsole {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();
        private final PrintStream outStream = new PrintStream(out);
        private final PrintStream errStream = new PrintStream(err);

        PrintStream outStream() {
            return outStream;
        }

        PrintStream errStream() {
            return errStream;
        }

        String out() {
            return out.toString();
        }

        String err() {
            return err.toString();
        }
    }
}
