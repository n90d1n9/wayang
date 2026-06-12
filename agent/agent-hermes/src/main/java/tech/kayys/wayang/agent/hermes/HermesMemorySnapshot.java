package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Map;

/**
 * Frozen memory view injected at the beginning of a Hermes-mode request.
 */
public record HermesMemorySnapshot(
        String agentNotes,
        String userProfile,
        List<String> retrievedMemories,
        Map<String, Object> metadata) {

    public HermesMemorySnapshot {
        agentNotes = HermesText.trimToEmpty(agentNotes);
        userProfile = HermesText.trimToEmpty(userProfile);
        retrievedMemories = HermesText.trimmedList(retrievedMemories);
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesMemorySnapshot empty() {
        return new HermesMemorySnapshot("", "", List.of(), Map.of());
    }

    public boolean isEmpty() {
        return agentNotes.isBlank() && userProfile.isBlank() && retrievedMemories.isEmpty();
    }

    public String render(int memoryEntryLimit) {
        if (isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("## Persistent Memory Snapshot\n");
        if (!agentNotes.isBlank()) {
            builder.append("Agent notes:\n").append(agentNotes).append('\n');
        }
        if (!userProfile.isBlank()) {
            builder.append("User profile:\n").append(userProfile).append('\n');
        }
        int limit = Math.max(0, memoryEntryLimit);
        if (limit > 0 && !retrievedMemories.isEmpty()) {
            builder.append("Retrieved memories:\n");
            retrievedMemories.stream()
                    .limit(limit)
                    .forEach(memory -> builder.append("- ").append(memory).append('\n'));
        }
        return builder.toString().trim();
    }

}
