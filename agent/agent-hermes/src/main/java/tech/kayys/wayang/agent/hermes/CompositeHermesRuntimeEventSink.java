package tech.kayys.wayang.agent.hermes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Best-effort runtime event sink that fans out to multiple downstream sinks.
 */
public final class CompositeHermesRuntimeEventSink implements HermesRuntimeEventSink, HermesRuntimeEventReader {

    private final List<HermesRuntimeEventSink> sinks;

    public CompositeHermesRuntimeEventSink(List<? extends HermesRuntimeEventSink> sinks) {
        this.sinks = HermesCollections.copyNonNull(sinks);
    }

    public CompositeHermesRuntimeEventSink(HermesRuntimeEventSink... sinks) {
        this(sinks == null ? List.of() : Arrays.asList(sinks));
    }

    public List<HermesRuntimeEventSink> sinks() {
        return sinks;
    }

    @Override
    public void emit(HermesRuntimeEvent event) {
        if (event == null) {
            return;
        }
        for (HermesRuntimeEventSink sink : sinks) {
            try {
                sink.emit(event);
            } catch (RuntimeException ignored) {
                // A failing diagnostic target must not block other event sinks.
            }
        }
    }

    @Override
    public HermesRuntimeEventPage query(HermesRuntimeEventQuery query) {
        for (HermesRuntimeEventSink sink : sinks) {
            Optional<HermesRuntimeEventReader> reader = HermesRuntimeEventReader.readableSink(sink);
            if (reader.isPresent()) {
                try {
                    return reader.get().query(query);
                } catch (RuntimeException ignored) {
                    // A failing reader must not hide later readable event stores.
                }
            }
        }
        return HermesRuntimeEventReader.empty().query(query);
    }
}
