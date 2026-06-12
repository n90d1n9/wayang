package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesObjectStorageLayoutTest {

    @Test
    void normalizesObjectStoragePrefixes() {
        assertThat(HermesObjectStorageLayout.normalizePrefix(null, "hermes/default"))
                .isEqualTo("hermes/default/");
        assertThat(HermesObjectStorageLayout.normalizePrefix(" /tenant-a/hermes/events ", "fallback"))
                .isEqualTo("tenant-a/hermes/events/");
        assertThat(HermesObjectStorageLayout.normalizePrefix("/", "fallback"))
                .isEmpty();
    }

    @Test
    void buildsStableJsonlObjectKeys() {
        assertThat(HermesObjectStorageLayout.jsonlKey(
                        "tenant-a/hermes/",
                        HermesObjectStorageLayout.objectId("Req 001!*", "event")))
                .isEqualTo("tenant-a/hermes/req-001.jsonl");
        assertThat(HermesObjectStorageLayout.hashedObjectId("repair key 001"))
                .startsWith("repair-key-001-");
        assertThat(HermesObjectStorageLayout.hashedObjectId(""))
                .isNotBlank();
    }

    @Test
    void listsKeysByPrefixAndSuffix() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        storage.objects.put("tenant-a/hermes/001.jsonl", new byte[] {1});
        storage.objects.put("tenant-a/hermes/002.json", new byte[] {2});
        storage.objects.put("tenant-a/hermes/003.txt", new byte[] {3});
        storage.objects.put("tenant-b/hermes/004.jsonl", new byte[] {4});

        assertThat(HermesObjectStorageLayout.listKeys(
                        storage,
                        "tenant-a/hermes/",
                        ".jsonl",
                        ".json"))
                .containsExactly(
                        "tenant-a/hermes/001.jsonl",
                        "tenant-a/hermes/002.json");
    }

    @Test
    void readsWritesAndDeletesObjects() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();

        HermesObjectStorageLayout.put(storage, "tenant-a/hermes/object.jsonl", "body".getBytes());

        assertThat(HermesObjectStorageLayout.read(storage, "tenant-a/hermes/object.jsonl"))
                .hasValue("body".getBytes());
        HermesObjectStorageLayout.delete(storage, "tenant-a/hermes/object.jsonl");
        assertThat(HermesObjectStorageLayout.read(storage, "tenant-a/hermes/object.jsonl"))
                .isEmpty();
    }
}
