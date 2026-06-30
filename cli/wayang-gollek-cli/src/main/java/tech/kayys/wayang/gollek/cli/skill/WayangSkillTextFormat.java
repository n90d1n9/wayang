package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentSkillDescriptor;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscovery;
import tech.kayys.wayang.gollek.sdk.AgentSkillEnvelopes;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Text renderer for skill discovery and detail responses shown by the Wayang CLI.
 */
final class WayangSkillTextFormat {

    private WayangSkillTextFormat() {
    }

    static String text(String productName, AgentSkillDiscovery discovery) {
        AgentSkillDiscovery model = AgentSkillEnvelopes.normalize(discovery);
        StringBuilder output = new StringBuilder("Wayang skills\n");
        output.append("product: ").append(productName).append('\n');
        output.append("totalSkills: ").append(model.totalSkills()).append('\n');
        output.append("matchingSkills: ").append(model.matchingSkills()).append('\n');
        appendQuery(output, model.query(), model.search());
        for (Map.Entry<String, List<RegisteredSkill>> entry : groupedByCategory(model.skills()).entrySet()) {
            output.append('\n').append(entry.getKey()).append('\n');
            for (RegisteredSkill skill : entry.getValue()) {
                AgentSkillDescriptor descriptor = skill.descriptor();
                output.append("  - ")
                        .append(skill.id())
                        .append(": ")
                        .append(descriptor.name())
                        .append(" [")
                        .append(skill.state().name().toLowerCase(Locale.ROOT))
                        .append("]")
                        .append('\n');
                if (!descriptor.description().isBlank()) {
                    output.append("    ").append(descriptor.description()).append('\n');
                }
                output.append("    source: ").append(descriptor.source()).append('\n');
                CliText.appendIndentedListLine(output, "surfaces", descriptor.surfaceIds());
                CliText.appendIndentedListLine(output, "tags", descriptor.tags());
                CliText.appendIndentedListLine(output, "aliases", skill.aliases());
            }
        }
        return output.append('\n').toString();
    }

    static String detailText(String productName, RegisteredSkill skill) {
        AgentSkillDescriptor descriptor = skill.descriptor();
        StringBuilder output = new StringBuilder("Wayang skill\n");
        output.append("product: ").append(productName).append('\n');
        output.append("id: ").append(skill.id()).append('\n');
        output.append("name: ").append(descriptor.name()).append('\n');
        output.append("state: ").append(skill.state().name().toLowerCase(Locale.ROOT)).append('\n');
        output.append("category: ").append(descriptor.category()).append('\n');
        output.append("source: ").append(descriptor.source()).append('\n');
        output.append("version: ").append(descriptor.version()).append('\n');
        if (!descriptor.description().isBlank()) {
            output.append("description: ").append(descriptor.description()).append('\n');
        }
        CliText.appendListLine(output, "surfaces", descriptor.surfaceIds());
        CliText.appendListLine(output, "inputs", descriptor.inputKeys());
        CliText.appendListLine(output, "outputs", descriptor.outputKeys());
        CliText.appendListLine(output, "tags", descriptor.tags());
        CliText.appendListLine(output, "aliases", skill.aliases());
        return output.append('\n').toString();
    }

    private static void appendQuery(StringBuilder output, AgentSkillQuery query, String search) {
        if (query.hasSurfaceId()) {
            output.append("surface: ").append(query.surfaceId()).append('\n');
        }
        if (query.hasProfileId()) {
            output.append("profile: ").append(query.profileId()).append('\n');
        }
        String resolvedSurfaceId = query.resolvedSurfaceId();
        if (!resolvedSurfaceId.isBlank() && !resolvedSurfaceId.equals(query.surfaceId())) {
            output.append("resolvedSurface: ").append(resolvedSurfaceId).append('\n');
        }
        if (query.hasCategory()) {
            output.append("category: ").append(query.category()).append('\n');
        }
        if (query.hasSource()) {
            output.append("source: ").append(query.source()).append('\n');
        }
        if (query.state() != null) {
            output.append("state: ").append(query.state().name().toLowerCase(Locale.ROOT)).append('\n');
        }
        if (query.hasTag()) {
            output.append("tag: ").append(query.tag()).append('\n');
        }
        String normalizedSearch = CliText.blankToNull(search);
        if (normalizedSearch != null) {
            output.append("search: ").append(normalizedSearch).append('\n');
        }
    }

    private static Map<String, List<RegisteredSkill>> groupedByCategory(List<RegisteredSkill> skills) {
        Map<String, List<RegisteredSkill>> grouped = new LinkedHashMap<>();
        for (RegisteredSkill skill : skills == null ? List.<RegisteredSkill>of() : skills) {
            grouped.computeIfAbsent(skill.descriptor().category(), ignored -> new ArrayList<>()).add(skill);
        }
        return grouped;
    }
}
