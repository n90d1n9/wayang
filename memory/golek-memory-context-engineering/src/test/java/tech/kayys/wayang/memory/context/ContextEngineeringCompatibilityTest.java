package tech.kayys.wayang.memory.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextEngineeringCompatibilityTest {

    @Test
    void exposesCanonicalCompressedContextFromMemoryCore() {
        CompressedContext context = new CompressedContext(
                "release blockers: database migration and rollout window",
                8,
                2,
                0.25,
                CompressionStrategy.EXTRACTIVE_SUMMARIZATION,
                0.92);

        assertThat(context.getCompressedContent()).contains("release blockers");
        assertThat(context.getOriginalMemoryCount()).isEqualTo(8);
        assertThat(context.getCompressedUnitCount()).isEqualTo(2);
        assertThat(context.getCompressionRatio()).isEqualTo(0.25);
        assertThat(context.getStrategy()).isEqualTo(CompressionStrategy.EXTRACTIVE_SUMMARIZATION);
        assertThat(context.getInformationRetention()).isEqualTo(0.92);
    }
}
