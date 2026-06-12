package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalog and alias resolver for runtime config samples.
 */
final class SkillManagementRuntimeConfigSampleCatalog {

    private static final List<ProviderSampleDefinition> PROVIDER_SAMPLE_DEFINITIONS = List.of(
            providerSample(
                    "gcs",
                    SkillManagementServiceProfile.OBJECT_STORAGE,
                    SkillManagementRuntimeConfigSampleProvider.GCS,
                    List.of("google-cloud-storage", "gcs-object-storage")),
            providerSample(
                    "azure",
                    SkillManagementServiceProfile.OBJECT_STORAGE,
                    SkillManagementRuntimeConfigSampleProvider.AZURE,
                    List.of("azure-blob", "azure-blob-storage", "azure-object-storage")),
            providerSample(
                    "hybrid-gcs",
                    SkillManagementServiceProfile.HYBRID_OBJECT_FILE,
                    SkillManagementRuntimeConfigSampleProvider.GCS,
                    List.of("gcs-hybrid", "hybrid-google-cloud-storage")),
            providerSample(
                    "hybrid-azure",
                    SkillManagementServiceProfile.HYBRID_OBJECT_FILE,
                    SkillManagementRuntimeConfigSampleProvider.AZURE,
                    List.of("azure-hybrid", "hybrid-azure-blob-storage")),
            providerSample(
                    "mirrored-gcs",
                    SkillManagementServiceProfile.MIRRORED_OBJECT_FILE,
                    SkillManagementRuntimeConfigSampleProvider.GCS,
                    List.of("gcs-mirrored", "mirror-gcs", "gcs-mirror")),
            providerSample(
                    "mirrored-azure",
                    SkillManagementServiceProfile.MIRRORED_OBJECT_FILE,
                    SkillManagementRuntimeConfigSampleProvider.AZURE,
                    List.of("azure-mirrored", "mirror-azure", "azure-mirror")));

    private SkillManagementRuntimeConfigSampleCatalog() {
    }

    static List<SkillManagementRuntimeConfigSampleDescriptor> samples() {
        List<SkillManagementRuntimeConfigSampleDescriptor> descriptors = new ArrayList<>();
        SkillManagementServiceProfiles.profiles()
                .forEach(profile -> descriptors.add(sampleDescriptor(
                        profile.label(),
                        profile,
                        SkillManagementRuntimeConfigSampleProvider.defaultForProfile(profile.profile()),
                        profile.aliases())));
        PROVIDER_SAMPLE_DEFINITIONS.stream()
                .map(ProviderSampleDefinition::descriptor)
                .forEach(descriptors::add);
        return List.copyOf(descriptors);
    }

    static SkillManagementRuntimeConfigSampleSelection selection(String profileName) {
        String normalized = SkillStoreConfigValues.normalize(profileName);
        return PROVIDER_SAMPLE_DEFINITIONS.stream()
                .filter(definition -> definition.matches(normalized))
                .findFirst()
                .map(ProviderSampleDefinition::selection)
                .orElseGet(() -> {
                    SkillManagementServiceProfileDescriptor descriptor =
                            SkillManagementServiceProfiles.profileDescriptor(profileName);
                    SkillManagementRuntimeConfigSampleProvider provider =
                            SkillManagementRuntimeConfigSampleProvider.defaultForProfile(descriptor.profile());
                    return selection(descriptor, provider);
                });
    }

    private static ProviderSampleDefinition providerSample(
            String name,
            SkillManagementServiceProfile profile,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider,
            List<String> aliases) {
        return new ProviderSampleDefinition(name, profile, objectStorageProvider, aliases);
    }

    private static SkillManagementRuntimeConfigSampleSelection selection(
            SkillManagementServiceProfileDescriptor descriptor,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider) {
        return new SkillManagementRuntimeConfigSampleSelection(
                descriptor,
                objectStorageProvider,
                description(descriptor, objectStorageProvider));
    }

    private static SkillManagementRuntimeConfigSampleDescriptor sampleDescriptor(
            String name,
            SkillManagementServiceProfileDescriptor descriptor,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider,
            List<String> aliases) {
        return new SkillManagementRuntimeConfigSampleDescriptor(
                name,
                descriptor.label(),
                objectStorageProvider.configName(),
                description(descriptor, objectStorageProvider),
                aliases);
    }

    private static String description(
            SkillManagementServiceProfileDescriptor descriptor,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider) {
        if (objectStorageProvider == SkillManagementRuntimeConfigSampleProvider.S3_RUSTFS
                || objectStorageProvider == SkillManagementRuntimeConfigSampleProvider.NONE) {
            return descriptor.description();
        }
        return switch (descriptor.profile()) {
            case OBJECT_STORAGE -> "Object-storage skill persistence sample using "
                    + objectStorageProvider.label()
                    + " provider settings.";
            case HYBRID_OBJECT_FILE -> "Hybrid file/object skill persistence sample using "
                    + objectStorageProvider.label()
                    + " provider settings.";
            case MIRRORED_OBJECT_FILE -> "Mirrored file/object skill persistence sample using "
                    + objectStorageProvider.label()
                    + " provider settings.";
            default -> descriptor.description();
        };
    }

    private record ProviderSampleDefinition(
            String name,
            SkillManagementServiceProfile profile,
            SkillManagementRuntimeConfigSampleProvider objectStorageProvider,
            List<String> aliases) {

        ProviderSampleDefinition {
            name = name == null ? "" : name.trim();
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }

        boolean matches(String normalized) {
            return SkillStoreConfigValues.normalize(name).equals(normalized)
                    || aliases.stream()
                            .map(SkillStoreConfigValues::normalize)
                            .anyMatch(normalized::equals);
        }

        SkillManagementRuntimeConfigSampleDescriptor descriptor() {
            SkillManagementServiceProfileDescriptor descriptor =
                    SkillManagementServiceProfiles.profileDescriptor(profile);
            return sampleDescriptor(name, descriptor, objectStorageProvider, aliases);
        }

        SkillManagementRuntimeConfigSampleSelection selection() {
            return SkillManagementRuntimeConfigSampleCatalog.selection(
                    SkillManagementServiceProfiles.profileDescriptor(profile),
                    objectStorageProvider);
        }
    }
}
