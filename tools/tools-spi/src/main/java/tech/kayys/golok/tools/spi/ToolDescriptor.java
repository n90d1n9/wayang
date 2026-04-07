package tech.kayys.golok.tools.spi;

import java.util.List;
import java.util.Map;

/**
 * Descriptor for a tool containing metadata and schema information.
 *
 * <p>
 * Tool descriptors are used for:
 * <ul>
 * <li>Tool discovery and listing</li>
 * <li>LLM prompt generation</li>
 * <li>Tool routing and selection</li>
 * <li>Documentation generation</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * 
 * <pre>{@code
 * ToolDescriptor descriptor = ToolDescriptor.builder()
 *         .id("read_file")
 *         .name("Read File")
 *         .description("Read contents of a file")
 *         .schema(JsonSchema.object(Map.of(
 *                 "path", JsonSchema.string("File path", true))))
 *         .source(ToolSource.INTERNAL_SKILL)
 *         .tags(List.of("file", "io"))
 *         .build();
 * }</pre>
 *
 * @author golok Team
 * @version 2.0.0
 */
public record ToolDescriptor(
        /**
         * Unique tool identifier (e.g., "read_file", "skill:rag").
         */
        String id,

        /**
         * Human-readable tool name.
         */
        String name,

        /**
         * Description for LLM consumption.
         */
        String description,

        /**
         * JSON Schema for tool inputs.
         */
        JsonSchema schema,

        /**
         * Source of this tool.
         */
        ToolSource source,

        /**
         * Tool-specific metadata.
         */
        Map<String, Object> metadata,

        /**
         * Tags for categorization and search.
         */
        List<String> tags) {

    /**
     * Create a new builder for tool descriptors.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ToolDescriptor.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private JsonSchema schema;
        private ToolSource source = ToolSource.INTERNAL_SKILL;
        private Map<String, Object> metadata = Map.of();
        private List<String> tags = List.of();

        /**
         * Set the tool ID.
         *
         * @param id unique identifier
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the tool name.
         *
         * @param name human-readable name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the tool description.
         *
         * @param description tool description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set the input schema.
         *
         * @param schema JSON schema
         * @return this builder
         */
        public Builder schema(JsonSchema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Set the tool source.
         *
         * @param source tool source
         * @return this builder
         */
        public Builder source(ToolSource source) {
            this.source = source;
            return this;
        }

        /**
         * Set the metadata.
         *
         * @param metadata metadata map
         * @return this builder
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Add a metadata entry.
         *
         * @param key   metadata key
         * @param value metadata value
         * @return this builder
         */
        public Builder metadata(String key, Object value) {
            this.metadata = new java.util.HashMap<>(this.metadata);
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Set the tags.
         *
         * @param tags list of tags
         * @return this builder
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Add a tag.
         *
         * @param tag tag to add
         * @return this builder
         */
        public Builder tag(String tag) {
            this.tags = new java.util.ArrayList<>(this.tags);
            this.tags.add(tag);
            return this;
        }

        /**
         * Build the tool descriptor.
         *
         * @return tool descriptor
         */
        public ToolDescriptor build() {
            return new ToolDescriptor(id, name, description, schema, source, metadata, tags);
        }
    }
}
