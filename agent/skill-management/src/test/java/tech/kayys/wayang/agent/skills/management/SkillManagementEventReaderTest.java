package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventReaderTest {

    @Test
    void adaptsReadableEventSink() {
        InMemorySkillManagementEventSink readable = new InMemorySkillManagementEventSink();

        assertThat(SkillManagementEventReader.forSink(readable)).isSameAs(readable);
        assertThat(SkillManagementEventReader.readableSink(readable)).containsSame(readable);
    }

    @Test
    void fallsBackToEmptyReaderForWriteOnlySink() {
        SkillManagementEventSink writeOnly = event -> {
        };

        assertThat(SkillManagementEventReader.forSink(writeOnly).latest().events()).isEmpty();
        assertThat(SkillManagementEventReader.readableSink(writeOnly)).isEmpty();
    }

    @Test
    void handlesNullSinkAsUnreadable() {
        assertThat(SkillManagementEventReader.forSink(null).latest().events()).isEmpty();
        assertThat(SkillManagementEventReader.readableSink(null)).isEmpty();
    }
}
