package tech.kayys.wayang.rag.core.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PgVectorStoreTest {

    @Test
    void shouldBuildVectorLiteral() {
        String literal = PgVectorStore.toVectorLiteral(new float[] { 1.0f, 0.25f, -2.5f });
        assertEquals("[1.0,0.25,-2.5]", literal);
    }

    @Test
    void shouldRejectEmptyVectorLiteral() {
        assertThrows(IllegalArgumentException.class, () -> PgVectorStore.toVectorLiteral(new float[] {}));
    }
}
