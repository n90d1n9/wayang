package tech.kayys.wayang.storage.provider.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.storage.spi.ModelStorageObjects;
import tech.kayys.wayang.storage.spi.ModelStorageService;
import tech.kayys.wayang.storage.spi.StorageServiceInitialization;

import java.util.Objects;

@ApplicationScoped
public class GcsModelStorageService implements ModelStorageService {

    private static final String SERVICE_NAME = "GCS model storage service";

    private Storage storage;
    private String bucketName;
    private ModelStorageObjects modelObjects;

    public void initialize(String bucketName, String pathPrefix) {
        initialize(GcsStorageConfig.of(bucketName, pathPrefix));
    }

    public void initialize(String bucketName, String projectId, String pathPrefix) {
        initialize(GcsStorageConfig.of(bucketName, projectId, pathPrefix));
    }

    public void initialize(GcsStorageConfig config) {
        GcsStorageConfig resolvedConfig = Objects.requireNonNull(config, "config");
        ModelStorageObjects resolvedModelObjects = ModelStorageObjects.forContainer(
                "gs",
                resolvedConfig.bucketName(),
                resolvedConfig.pathPrefix());
        Storage resolvedStorage = storageOptions(resolvedConfig).getService();

        this.bucketName = resolvedModelObjects.containerName();
        this.modelObjects = resolvedModelObjects;
        this.storage = resolvedStorage;
    }

    @Override
    public Uni<String> uploadModel(String namespace, String modelId, String version, byte[] data) {
        return Uni.createFrom().item(() -> {
            Storage resolvedStorage = storage();
            String bucket = bucketName();
            ModelStorageObjects objects = modelObjects();
            String blobName = objects.objectName(namespace, modelId, version);
            BlobId blobId = BlobId.of(bucket, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            resolvedStorage.create(blobInfo, data);
            return objects.storageUri(blobName);
        });
    }

    @Override
    public Uni<byte[]> downloadModel(String storageUri) {
        return Uni.createFrom().item(() -> {
            Storage resolvedStorage = storage();
            String blobName = modelObjects().objectNameFromUri(storageUri);
            BlobId blobId = BlobId.of(bucketName(), blobName);

            return resolvedStorage.readAllBytes(blobId);
        });
    }

    @Override
    public Uni<Void> deleteModel(String storageUri) {
        return Uni.createFrom().item(() -> {
            Storage resolvedStorage = storage();
            String blobName = modelObjects().objectNameFromUri(storageUri);
            BlobId blobId = BlobId.of(bucketName(), blobName);

            resolvedStorage.delete(blobId);
            return (Void) null;
        });
    }

    private Storage storage() {
        return StorageServiceInitialization.requireInitialized(storage, SERVICE_NAME);
    }

    private String bucketName() {
        return StorageServiceInitialization.requireTextInitialized(bucketName, SERVICE_NAME);
    }

    private ModelStorageObjects modelObjects() {
        return StorageServiceInitialization.requireInitialized(modelObjects, SERVICE_NAME);
    }

    private static StorageOptions storageOptions(GcsStorageConfig config) {
        return config.projectId().isBlank()
                ? StorageOptions.getDefaultInstance()
                : StorageOptions.newBuilder().setProjectId(config.projectId()).build();
    }
}
