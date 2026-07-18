package tech.kayys.wayang.rag.core.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VectorStoreFactoryTest {

    @Test
    void shouldCreateInMemoryStoreByDefault() {
        VectorStore<String> store = VectorStoreFactory.create(
                VectorStoreOptions.defaults(512),
                null,
                null,
                null);

        assertInstanceOf(InMemoryVectorStore.class, store);
    }

    @Test
    void shouldFailWhenPgVectorMissingDataSource() {
        VectorStoreOptions options = new VectorStoreOptions("pgvector", "rag_vectors", 512, false, false);

        assertThrows(IllegalArgumentException.class, () -> VectorStoreFactory.create(
                options,
                null,
                new JsonPayloadCodec<>(new ObjectMapper(), String.class),
                new ObjectMapper()));
    }
}
