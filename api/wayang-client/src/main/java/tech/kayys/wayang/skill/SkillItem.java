package tech.kayys.wayang.skill;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Domain model representing a skill item in the skill catalog.
 */
public class SkillItem {
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final String source;
    private final String version;
    private final List<String> surfaceIds;
    private final List<String> inputKeys;
    private final List<String> outputKeys;
    private final List<String> tags;
    private final Map<String, Object> metadata;

    public SkillItem(
            String id,
            String name,
            String description,
            String category,
            String source,
            String version,
            List<String> surfaceIds,
            List<String> inputKeys,
            List<String> outputKeys,
            List<String> tags,
            Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = name;
        this.description = description;
        this.category = category;
        this.source = source;
        this.version = version;
        this.surfaceIds = surfaceIds != null ? List.copyOf(surfaceIds) : List.of();
        this.inputKeys = inputKeys != null ? List.copyOf(inputKeys) : List.of();
        this.outputKeys = outputKeys != null ? List.copyOf(outputKeys) : List.of();
        this.tags = tags != null ? List.copyOf(tags) : List.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getSource() {
        return source;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getSurfaceIds() {
        return surfaceIds;
    }

    public List<String> getInputKeys() {
        return inputKeys;
    }

    public List<String> getOutputKeys() {
        return outputKeys;
    }

    public List<String> getTags() {
        return tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "name", name,
                "description", description,
                "category", category,
                "source", source,
                "version", version,
                "surfaceIds", surfaceIds,
                "inputKeys", inputKeys,
                "outputKeys", outputKeys,
                "tags", tags,
                "metadata", metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillItem skillItem = (SkillItem) o;
        return Objects.equals(id, skillItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SkillItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
