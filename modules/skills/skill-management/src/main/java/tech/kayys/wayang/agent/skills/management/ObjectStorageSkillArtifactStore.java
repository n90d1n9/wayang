package tech.kayys.wayang.agent.skills.management;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-store-backed artifact store for cloud and S3-compatible backends.
 */
public final class ObjectStorageSkillArtifactStore implements SkillArtifactStore {

    public static final String DEFAULT_PREFIX = SkillArtifactReference.DEFAULT_STORAGE_PREFIX;
    public static final String CONTENT_FILE_NAME = "content.bin";

    private final SkillManagementObjectStore objectStore;
    private final String prefix;
    private final SkillArtifactManifestCodec manifestCodec;

    public ObjectStorageSkillArtifactStore(SkillManagementObjectStore objectStore) {
        this(objectStore, DEFAULT_PREFIX);
    }

    public ObjectStorageSkillArtifactStore(SkillManagementObjectStore objectStore, String prefix) {
        this(objectStore, prefix, new SkillArtifactManifestCodec());
    }

    ObjectStorageSkillArtifactStore(
            SkillManagementObjectStore objectStore,
            String prefix,
            SkillArtifactManifestCodec manifestCodec) {
        this.objectStore = Objects.requireNonNull(objectStore, "objectStore");
        this.prefix = SkillManagementObjectKeys.normalizePrefix(prefix, DEFAULT_PREFIX);
        this.manifestCodec = Objects.requireNonNull(manifestCodec, "manifestCodec");
    }

    @Override
    public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
        SkillArtifactReference resolved = requireReference(reference);
        Optional<byte[]> content = objectStore.get(contentKey(resolved));
        if (content == null || content.isEmpty()) {
            return Optional.empty();
        }
        SkillArtifactManifest manifest = readManifest(resolved)
                .orElseGet(() -> new SkillArtifactManifest(
                        resolved,
                        SkillArtifact.DEFAULT_CONTENT_TYPE,
                        Map.of(),
                        -1,
                        ""));
        return Optional.of(manifest.toArtifact(content.get()));
    }

    @Override
    public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
        SkillArtifactQuery resolved = query == null ? SkillArtifactQuery.all() : query;
        return SkillManagementObjectStoreSupport.keys(
                        objectStore,
                        prefix,
                        key -> key.endsWith("/" + SkillArtifactManifestCodec.FILE_NAME))
                .stream()
                .map(this::readManifestKey)
                .flatMap(Optional::stream)
                .map(SkillArtifactManifest::reference)
                .filter(resolved::matches)
                .sorted(Comparator.comparing(SkillArtifactReference::qualifiedName))
                .limit(resolved.limit())
                .toList();
    }

    @Override
    public void putArtifact(SkillArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact");
        SkillManagementObjectStoreSupport.put(objectStore, contentKey(artifact.reference()), artifact.content());
        SkillManagementObjectStoreSupport.put(
                objectStore,
                manifestKey(artifact.reference()),
                manifestCodec.toBytes(artifact));
    }

    @Override
    public boolean deleteArtifact(SkillArtifactReference reference) {
        SkillArtifactReference resolved = requireReference(reference);
        boolean contentDeleted = SkillManagementObjectStoreSupport.delete(objectStore, contentKey(resolved));
        boolean manifestDeleted = SkillManagementObjectStoreSupport.delete(objectStore, manifestKey(resolved));
        return contentDeleted || manifestDeleted;
    }

    private Optional<SkillArtifactManifest> readManifest(SkillArtifactReference reference) {
        return readManifestKey(manifestKey(reference));
    }

    private Optional<SkillArtifactManifest> readManifestKey(String key) {
        return SkillManagementObjectStoreSupport.read(
                objectStore,
                key,
                content -> manifestCodec.fromBytes(content, key));
    }

    private String contentKey(SkillArtifactReference reference) {
        return baseKey(reference) + "/" + CONTENT_FILE_NAME;
    }

    private String manifestKey(SkillArtifactReference reference) {
        return baseKey(reference) + "/" + SkillArtifactManifestCodec.FILE_NAME;
    }

    private String baseKey(SkillArtifactReference reference) {
        return prefix + requireReference(reference).relativePath();
    }

    private static SkillArtifactReference requireReference(SkillArtifactReference reference) {
        return Objects.requireNonNull(reference, "reference");
    }
}
