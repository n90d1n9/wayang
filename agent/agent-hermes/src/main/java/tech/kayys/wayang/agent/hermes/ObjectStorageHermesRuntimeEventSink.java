package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-store-backed runtime event journal for cloud and S3-compatible storage.
 */
public final class ObjectStorageHermesRuntimeEventSink
        implements HermesRuntimeEventSink, HermesRuntimeEventReader {

    public static final String DEFAULT_PREFIX = "hermes/runtime-events";

    private final ObjectStorageService objectStorageService;
    private final String prefix;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public ObjectStorageHermesRuntimeEventSink(ObjectStorageService objectStorageService) {
        this(objectStorageService, DEFAULT_PREFIX, FileSystemHermesRuntimeEventSink.DEFAULT_MAX_EVENTS);
    }

    public ObjectStorageHermesRuntimeEventSink(
            ObjectStorageService objectStorageService,
            String prefix,
            int maxEvents) {
        this.objectStorageService = Objects.requireNonNull(objectStorageService, "objectStorageService");
        this.prefix = HermesObjectStorageLayout.normalizePrefix(prefix, DEFAULT_PREFIX);
        this.retentionPolicy = HermesRecordRetentionPolicy.bounded(maxEvents);
    }

    public String prefix() {
        return prefix;
    }

    public int maxEvents() {
        return retentionPolicy.maxEntries();
    }

    @Override
    public synchronized void emit(HermesRuntimeEvent event) {
        if (event == null) {
            return;
        }
        put(keyFor(event), HermesRuntimeEventJsonCodec.toJsonLine(event).getBytes(StandardCharsets.UTF_8));
        pruneToCapacity();
    }

    @Override
    public synchronized HermesRuntimeEventPage query(HermesRuntimeEventQuery query) {
        return HermesRuntimeEventPages.from(events(), query);
    }

    public synchronized List<HermesRuntimeEvent> events() {
        return eventKeys().stream()
                .map(this::readKey)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<HermesRuntimeEvent> readKey(String key) {
        return HermesObjectStorageLayout.read(objectStorageService, key)
                .map(bytes -> HermesRuntimeEventJsonCodec.fromJsonLine(
                        new String(bytes, StandardCharsets.UTF_8)));
    }

    private List<String> eventKeys() {
        return HermesObjectStorageLayout.listKeys(objectStorageService, prefix, ".jsonl");
    }

    private void pruneToCapacity() {
        retentionPolicy.staleFromOldestFirst(eventKeys(), Comparator.naturalOrder())
                .forEach(key -> HermesObjectStorageLayout.delete(objectStorageService, key));
    }

    private void put(String key, byte[] content) {
        HermesObjectStorageLayout.put(objectStorageService, key, content);
    }

    private String keyFor(HermesRuntimeEvent event) {
        return HermesObjectStorageLayout.jsonlKey(
                prefix,
                String.format("%020d", event.occurredAt().toEpochMilli())
                        + "-"
                        + HermesObjectStorageLayout.objectId(event.eventId(), "event"));
    }
}
