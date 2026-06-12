package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Optional;

/**
 * Read-side contract for querying Hermes runtime event history.
 */
public interface HermesRuntimeEventReader {

    HermesRuntimeEventPage query(HermesRuntimeEventQuery query);

    default HermesRuntimeEventPage latest() {
        return query(HermesRuntimeEventQuery.latest());
    }

    static HermesRuntimeEventReader empty() {
        return query -> new HermesRuntimeEventPage(List.of(), 0);
    }

    static HermesRuntimeEventReader forSink(HermesRuntimeEventSink sink) {
        return readableSink(sink).orElseGet(HermesRuntimeEventReader::empty);
    }

    static Optional<HermesRuntimeEventReader> readableSink(HermesRuntimeEventSink sink) {
        return sink instanceof HermesRuntimeEventReader reader
                ? Optional.of(reader)
                : Optional.empty();
    }
}
