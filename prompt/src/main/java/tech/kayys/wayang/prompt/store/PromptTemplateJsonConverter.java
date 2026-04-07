package tech.kayys.wayang.prompt.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jboss.logging.Logger;

import tech.kayys.wayang.prompt.core.PromptTemplate;

/**
 * JPA AttributeConverter that converts {@link PromptTemplate} ↔ JSON string.
 *
 * <p>This converter is primarily for legacy compatibility. The preferred approach
 * is to use Hibernate's native {@code @JdbcTypeCode(SqlTypes.JSON)} annotation
 * on the entity field, which delegates serialisation to Hibernate's built-in
 * JSON mapping. When {@code @JdbcTypeCode(SqlTypes.JSON)} is used on the entity,
 * this converter should have {@code autoApply = false} to avoid conflicts.
 *
 * <p>The converter maintains its own {@link ObjectMapper} instance configured with:
 * <ul>
 *   <li>{@link JavaTimeModule} for {@code java.time.Instant} support</li>
 *   <li>Lenient unknown-property handling for forward compatibility</li>
 *   <li>ISO-8601 date serialization (no timestamps)</li>
 * </ul>
 */
@Converter(autoApply = false)
public class PromptTemplateJsonConverter implements AttributeConverter<PromptTemplate, String> {

    private static final Logger LOG = Logger.getLogger(PromptTemplateJsonConverter.class);

    private static final ObjectMapper MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    @Override
    public String convertToDatabaseColumn(PromptTemplate attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to serialise PromptTemplate '%s' to JSON: %s",
                    attribute.getTemplateId(), e.getMessage());
            throw new IllegalStateException(
                    "Cannot convert PromptTemplate to JSON column: " + attribute.getTemplateId(), e);
        }
    }

    @Override
    public PromptTemplate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, PromptTemplate.class);
        } catch (JsonProcessingException e) {
            LOG.errorf("Failed to deserialise PromptTemplate from JSON (first 200 chars): '%.200s': %s",
                    dbData, e.getMessage());
            throw new IllegalStateException(
                    "Cannot convert JSON column to PromptTemplate", e);
        }
    }
}

