package tech.kayys.wayang.storage.provider.azure;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.storage.spi.ModelStorageObjects;
import tech.kayys.wayang.storage.spi.ModelStorageService;
import tech.kayys.wayang.storage.spi.StorageServiceInitialization;

import java.util.Objects;

@ApplicationScoped
public class AzureModelStorageService implements ModelStorageService {

    private static final String SERVICE_NAME = "Azure model storage service";

    private BlobContainerClient containerClient;
    private ModelStorageObjects modelObjects;

    public void initialize(String connectionString, String containerName, String pathPrefix) {
        initialize(AzureStorageConfig.of(connectionString, containerName, pathPrefix));
    }

    public void initialize(AzureStorageConfig config) {
        AzureStorageConfig resolvedConfig = Objects.requireNonNull(config, "config");
        ModelStorageObjects resolvedModelObjects = ModelStorageObjects.forContainer(
                "azure",
                resolvedConfig.containerName(),
                resolvedConfig.pathPrefix());

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(resolvedConfig.connectionString())
                .buildClient();
        BlobContainerClient resolvedContainerClient = blobServiceClient.getBlobContainerClient(
                resolvedModelObjects.containerName());

        this.modelObjects = resolvedModelObjects;
        this.containerClient = resolvedContainerClient;
    }

    @Override
    public Uni<String> uploadModel(String namespace, String modelId, String version, byte[] data) {
        return Uni.createFrom().item(() -> {
            BlobContainerClient container = containerClient();
            ModelStorageObjects objects = modelObjects();
            String blobName = objects.objectName(namespace, modelId, version);
            BlobClient blobClient = container.getBlobClient(blobName);

            blobClient.upload(BinaryData.fromBytes(data), true);
            return objects.storageUri(blobName);
        });
    }

    @Override
    public Uni<byte[]> downloadModel(String storageUri) {
        return Uni.createFrom().item(() -> {
            String blobName = modelObjects().objectNameFromUri(storageUri);
            BlobClient blobClient = containerClient().getBlobClient(blobName);

            return blobClient.downloadContent().toBytes();
        });
    }

    @Override
    public Uni<Void> deleteModel(String storageUri) {
        return Uni.createFrom().item(() -> {
            String blobName = modelObjects().objectNameFromUri(storageUri);
            BlobClient blobClient = containerClient().getBlobClient(blobName);

            blobClient.delete();
            return (Void) null;
        });
    }

    private BlobContainerClient containerClient() {
        return StorageServiceInitialization.requireInitialized(containerClient, SERVICE_NAME);
    }

    private ModelStorageObjects modelObjects() {
        return StorageServiceInitialization.requireInitialized(modelObjects, SERVICE_NAME);
    }
}
