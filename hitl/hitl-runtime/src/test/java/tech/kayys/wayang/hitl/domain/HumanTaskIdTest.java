package tech.kayys.wayang.hitl.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HumanTaskIdTest {

    @Test
    void shouldCreateHumanTaskIdSuccessfully() {
        // Given
        String value = "TASK-123";

        // When
        HumanTaskId id = new HumanTaskId(value);

        // Then
        assertEquals(value, id.value());
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new HumanTaskId(null);
        });
    }

    @Test
    void shouldGenerateUniqueId() {
        // When
        HumanTaskId id1 = HumanTaskId.generate();
        HumanTaskId id2 = HumanTaskId.generate();

        // Then
        assertNotNull(id1.value());
        assertNotNull(id2.value());
        assertNotEquals(id1.value(), id2.value());
        assertTrue(id1.value().startsWith("TASK-"));
        assertTrue(id2.value().startsWith("TASK-"));
    }

    @Test
    void shouldCreateFromExistingValue() {
        // Given
        String value = "TASK-456";

        // When
        HumanTaskId id = HumanTaskId.of(value);

        // Then
        assertEquals(value, id.value());
    }
}