package tech.kayys.wayang.memory.impl;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.memory.spi.DirectAgentContext;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DirectAgentContextImplTest {

    @Test
    public void testZeroCopyContextFlow() {
        // DirectAgentContextImpl uses RocksDB by default in its constructor
        DirectAgentContext context = new DirectAgentContextImpl();
        String agentId = "agent-007";

        // 1. Append to context asynchronously
        context.appendToContext(agentId, "User: Hello there!")
                .await().atMost(Duration.ofSeconds(5));

        context.appendToContext(agentId, "Agent: General Kenobi!")
                .await().atMost(Duration.ofSeconds(5));

        // 2. Retrieve natively
        try (Arena testArena = Arena.ofShared()) {
            Optional<MemorySegment> optSegment = context.getNativeContext(agentId, testArena)
                    .await().atMost(Duration.ofSeconds(5));

            assertTrue(optSegment.isPresent(), "Memory segment should exist in RocksDB");

            // Verify content
            MemorySegment segment = optSegment.get();
            byte[] retrievedBytes = segment.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
            String retrievedText = new String(retrievedBytes);

            System.out.println("Retrieved Native Context:\n" + retrievedText);
            assertTrue(retrievedText.contains("Hello there!"));
            assertTrue(retrievedText.contains("General Kenobi!"));
        }
    }
}
