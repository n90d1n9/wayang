package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Option;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.AgentSkillState;

final class WayangSkillQueryOptions {

    @Option(names = "--surface", description = "Filter skills to a product surface.")
    String surfaceId;

    @Option(names = "--profile", description = "Filter skills through a product profile.")
    String profileId;

    @Option(names = "--category", description = "Filter skills to one category.")
    String category;

    @Option(names = "--source", description = "Filter skills to one source, for example rag or mcp.")
    String source;

    @Option(names = "--state", description = "Filter skills by state: active, preview, disabled, or deprecated.")
    String state;

    @Option(names = "--tag", description = "Filter skills by tag.")
    String tag;

    @Option(names = "--input", description = "Filter skills by input key.")
    String inputKey;

    @Option(names = "--output", description = "Filter skills by output key.")
    String outputKey;

    AgentSkillQuery toQuery(String skillId) {
        return new AgentSkillQuery(
                surfaceId,
                profileId,
                category,
                source,
                state == null || state.isBlank() ? null : AgentSkillState.from(state),
                skillId,
                tag,
                inputKey,
                outputKey);
    }
}
