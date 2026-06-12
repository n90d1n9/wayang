package tech.kayys.wayang.agent.hermes;

import java.util.List;

/**
 * Port for observing Hermes lifecycle events.
 */
@FunctionalInterface
public interface HermesRuntimeEventSink {

    void emit(HermesRuntimeEvent event);

    default void emitAll(List<HermesRuntimeEvent> events) {
        if (events == null) {
            return;
        }
        events.stream()
                .filter(event -> event != null)
                .forEach(this::emit);
    }

    static HermesRuntimeEventSink noop() {
        return event -> {
        };
    }

    static HermesRuntimeEventSink composite(List<? extends HermesRuntimeEventSink> sinks) {
        return new CompositeHermesRuntimeEventSink(sinks);
    }

    static HermesRuntimeEventSink composite(HermesRuntimeEventSink... sinks) {
        return new CompositeHermesRuntimeEventSink(sinks);
    }
}
