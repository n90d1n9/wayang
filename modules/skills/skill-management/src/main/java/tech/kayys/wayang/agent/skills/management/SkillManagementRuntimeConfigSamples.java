package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.List;

/**
 * Starter runtime config samples for profile-driven skill persistence.
 */
public final class SkillManagementRuntimeConfigSamples {

    private static final String PROFILE_PROPERTY = "wayang.skills.profile";
    private static final String BASE_DIRECTORY_PROPERTY =
            SkillManagementServiceProfileOptions.PREFIX + "base-directory";
    private static final String OBJECT_PREFIX_PROPERTY =
            SkillManagementServiceProfileOptions.PREFIX + "object-prefix";
    private static final String MAX_EVENTS_PROPERTY =
            SkillManagementServiceProfileOptions.PREFIX + "max-events";
    private static final String INITIALIZE_JDBC_SCHEMA_PROPERTY =
            SkillManagementServiceProfileOptions.PREFIX + "initialize-jdbc-schema";
    private static final String RECONCILE_MODE_PROPERTY =
            SkillLifecycleStateReconcileConfigs.PREFIX + "mode";

    private SkillManagementRuntimeConfigSamples() {
    }

    public static List<SkillManagementRuntimeConfigSampleDescriptor> samples() {
        return SkillManagementRuntimeConfigSampleCatalog.samples();
    }

    public static SkillManagementRuntimeConfigSample forProfile(String profileName) {
        SkillManagementRuntimeConfigSampleSelection selection =
                SkillManagementRuntimeConfigSampleCatalog.selection(profileName);
        SkillManagementServiceProfileDescriptor descriptor = selection.descriptor();
        return new SkillManagementRuntimeConfigSample(
                descriptor.label(),
                selection.description(),
                properties(descriptor.profile(), descriptor.label(), selection.objectStorageProvider()),
                environment(descriptor.profile(), descriptor.label(), selection.objectStorageProvider()));
    }

    private static List<SkillManagementRuntimeConfigSampleEntry> properties(
            SkillManagementServiceProfile profile,
            String label,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider) {
        SkillManagementServiceProfileOptions defaults = SkillManagementServiceProfileOptions.defaults();
        List<SkillManagementRuntimeConfigSampleEntry> entries = new ArrayList<>();
        entries.add(entry(PROFILE_PROPERTY, label, "Named persistence profile selector."));
        profileOptions(profile, defaults, entries, false, objectStorageProvider);
        entries.add(entry(RECONCILE_MODE_PROPERTY, "inspect-only", "Lifecycle state reconciliation mode."));
        return List.copyOf(entries);
    }

    private static List<SkillManagementRuntimeConfigSampleEntry> environment(
            SkillManagementServiceProfile profile,
            String label,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider) {
        SkillManagementServiceProfileOptions defaults = SkillManagementServiceProfileOptions.defaults();
        List<SkillManagementRuntimeConfigSampleEntry> entries = new ArrayList<>();
        entries.add(entry(
                SkillManagementServiceProfiles.PROFILE_ENV,
                label,
                "Named persistence profile selector."));
        profileOptions(profile, defaults, entries, true, objectStorageProvider);
        entries.add(entry(
                SkillLifecycleStateReconcileConfigs.ENV_PREFIX + "MODE",
                "inspect-only",
                "Lifecycle state reconciliation mode."));
        return List.copyOf(entries);
    }

    private static void profileOptions(
            SkillManagementServiceProfile profile,
            SkillManagementServiceProfileOptions defaults,
            List<SkillManagementRuntimeConfigSampleEntry> entries,
            boolean environment,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider) {
        switch (profile) {
            case DEFAULT -> {
            }
            case LOCAL_FILESYSTEM -> {
                addBaseDirectory(entries, defaults, environment);
                addMaxEvents(entries, defaults, environment);
            }
            case OBJECT_STORAGE -> {
                addObjectPrefix(entries, defaults, environment);
                addObjectStorageProvider(entries, environment, objectStorageProvider);
                addMaxEvents(entries, defaults, environment);
            }
            case JDBC -> {
                addMaxEvents(entries, defaults, environment);
                addInitializeJdbcSchema(entries, defaults, environment);
            }
            case HYBRID_OBJECT_FILE, MIRRORED_OBJECT_FILE -> {
                addBaseDirectory(entries, defaults, environment);
                addObjectPrefix(entries, defaults, environment);
                addObjectStorageProvider(entries, environment, objectStorageProvider);
                addMaxEvents(entries, defaults, environment);
            }
        }
    }

    private static void addBaseDirectory(
            List<SkillManagementRuntimeConfigSampleEntry> entries,
            SkillManagementServiceProfileOptions defaults,
            boolean environment) {
        entries.add(entry(
                environment ? "WAYANG_SKILLS_PROFILE_BASE_DIRECTORY" : BASE_DIRECTORY_PROPERTY,
                defaults.baseDirectory().toString(),
                "Base directory for file-backed profile roles."));
    }

    private static void addObjectPrefix(
            List<SkillManagementRuntimeConfigSampleEntry> entries,
            SkillManagementServiceProfileOptions defaults,
            boolean environment) {
        entries.add(entry(
                environment ? "WAYANG_SKILLS_PROFILE_OBJECT_PREFIX" : OBJECT_PREFIX_PROPERTY,
                defaults.objectPrefix(),
                "Object prefix for cloud-backed profile roles."));
    }

    private static void addMaxEvents(
            List<SkillManagementRuntimeConfigSampleEntry> entries,
            SkillManagementServiceProfileOptions defaults,
            boolean environment) {
        entries.add(entry(
                environment ? "WAYANG_SKILLS_PROFILE_MAX_EVENTS" : MAX_EVENTS_PROPERTY,
                String.valueOf(defaults.maxEvents()),
                "Event-history retention capacity."));
    }

    private static void addObjectStorageProvider(
            List<SkillManagementRuntimeConfigSampleEntry> entries,
            boolean environment,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider) {
        SkillManagementRuntimeConfigSampleProvider provider = objectStorageProvider == null
                ? SkillManagementRuntimeConfigSampleProvider.NONE
                : objectStorageProvider;
        entries.addAll(provider.sampleEntries(environment));
    }

    private static void addInitializeJdbcSchema(
            List<SkillManagementRuntimeConfigSampleEntry> entries,
            SkillManagementServiceProfileOptions defaults,
            boolean environment) {
        entries.add(entry(
                environment ? "WAYANG_SKILLS_PROFILE_INITIALIZE_JDBC_SCHEMA" : INITIALIZE_JDBC_SCHEMA_PROPERTY,
                String.valueOf(defaults.initializeJdbcSchema()),
                "Initialize JDBC schema on startup."));
    }

    private static SkillManagementRuntimeConfigSampleEntry entry(
            String key,
            String value,
            String description) {
        return new SkillManagementRuntimeConfigSampleEntry(key, value, description);
    }
}
