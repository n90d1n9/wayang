package tech.kayys.wayang.storage.provider.s3;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tech.kayys.wayang.storage.spi.ModelStorageObjects;
import tech.kayys.wayang.storage.spi.ModelStorageService;
import tech.kayys.wayang.storage.spi.StorageServiceInitialization;

import java.util.Objects;

@ApplicationScoped
public class S3ModelStorageService implements ModelStorageService {

    private static final String SERVICE_NAME = "S3 model storage service";

    private S3Client s3Client;
    private String bucketName;
    private ModelStorageObjects modelObjects;

    public void initialize(String accessKeyId, String secretAccessKey, String bucketName,
            String region, String endpoint, String pathPrefix) {
        initialize(S3StorageConfig.of(accessKeyId, secretAccessKey, bucketName, region, endpoint, pathPrefix));
    }

    public void initialize(String accessKeyId, String secretAccessKey, String bucketName,
            String region, String endpoint, String pathPrefix, boolean pathStyleAccess) {
        initialize(S3StorageConfig.of(
                accessKeyId,
                secretAccessKey,
                bucketName,
                region,
                endpoint,
                pathPrefix,
                pathStyleAccess));
    }

    public void initialize(S3StorageConfig config) {
        S3StorageConfig resolvedConfig = Objects.requireNonNull(config, "config");
        ModelStorageObjects resolvedModelObjects = ModelStorageObjects.forContainer(
                "s3",
                resolvedConfig.bucketName(),
                resolvedConfig.pathPrefix());
        S3Client resolvedClient = S3StorageClients.create(resolvedConfig);

        this.bucketName = resolvedConfig.bucketName();
        this.modelObjects = resolvedModelObjects;
        this.s3Client = resolvedClient;
    }

    @Override
    public Uni<String> uploadModel(String namespace, String modelId, String version, byte[] data) {
        return Uni.createFrom().item(() -> {
            S3Client client = s3Client();
            String bucket = bucketName();
            ModelStorageObjects objects = modelObjects();
            String key = objects.objectName(namespace, modelId, version);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            client.putObject(request, RequestBody.fromBytes(data));
            return objects.storageUri(key);
        });
    }

    @Override
    public Uni<byte[]> downloadModel(String storageUri) {
        return Uni.createFrom().item(() -> {
            S3Client client = s3Client();
            String key = modelObjects().objectNameFromUri(storageUri);
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName())
                    .key(key)
                    .build();

            return client.getObjectAsBytes(request).asByteArray();
        });
    }

    @Override
    public Uni<Void> deleteModel(String storageUri) {
        return Uni.createFrom().item(() -> {
            S3Client client = s3Client();
            String key = modelObjects().objectNameFromUri(storageUri);
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName())
                    .key(key)
                    .build();

            client.deleteObject(request);
            return (Void) null;
        });
    }

    private S3Client s3Client() {
        return StorageServiceInitialization.requireInitialized(s3Client, SERVICE_NAME);
    }

    private String bucketName() {
        return StorageServiceInitialization.requireTextInitialized(bucketName, SERVICE_NAME);
    }

    private ModelStorageObjects modelObjects() {
        return StorageServiceInitialization.requireInitialized(modelObjects, SERVICE_NAME);
    }
}
