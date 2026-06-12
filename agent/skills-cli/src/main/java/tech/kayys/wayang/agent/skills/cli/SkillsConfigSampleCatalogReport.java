package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementRuntimeConfigSampleDescriptor;

import java.util.List;
import java.util.Objects;

/**
 * Discoverable runtime config sample catalog for CLI rendering.
 */
record SkillsConfigSampleCatalogReport(
        List<SkillManagementRuntimeConfigSampleDescriptor> samples) {

    SkillsConfigSampleCatalogReport {
        samples = samples == null
                ? List.of()
                : samples.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }
}
