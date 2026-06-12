package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeEventSinkFactoryTest {

    @Test
    void keepsExplicitSinkWhenRuntimeJournalIsDisabled() {
        List<HermesRuntimeEvent> events = new ArrayList<>();
        HermesRuntimeEventSink sink = HermesRuntimeEventSinkFactory.create(
                HermesAgentModeConfig.defaults(),
                Optional.of(events::add),
                Optional.empty(),
                Optional.empty());
        HermesRuntimeEvent event = event("req-explicit");

        sink.emit(event);

        assertThat(events).containsExactly(event);
        assertThat(HermesRuntimeEventReader.forSink(sink).latest().events()).isEmpty();
    }

    @Test
    void combinesExplicitSinkWithConfiguredFileJournal(@TempDir Path tempDir) throws Exception {
        List<HermesRuntimeEvent> events = new ArrayList<>();
        Path journal = tempDir.resolve("runtime/events.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalStore("file-system")
                .runtimeEventJournalPath(journal.toString())
                .runtimeEventJournalMaxEvents(10)
                .build();
        HermesRuntimeEventSink sink = HermesRuntimeEventSinkFactory.create(
                config,
                Optional.of(events::add),
                Optional.empty(),
                Optional.empty());
        HermesRuntimeEvent event = event("req-file-factory");

        sink.emit(event);

        assertThat(events).containsExactly(event);
        assertThat(Files.readString(journal)).contains("\"requestId\":\"req-file-factory\"");
        assertThat(HermesRuntimeEventReader.forSink(sink).latest().events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-file-factory");
    }

    private static HermesRuntimeEvent event(String requestId) {
        return HermesRuntimeEvent.requestPlanned(
                AgentRequest.builder().requestId(requestId).prompt("plan").build(),
                null);
    }
}
