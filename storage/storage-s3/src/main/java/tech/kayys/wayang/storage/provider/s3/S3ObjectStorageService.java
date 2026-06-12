package tech.kayys.wayang.storage.provider.s3;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import tech.kayys.wayang.storage.spi.ObjectStorageNames;
import tech.kayys.wayang.storage.spi.ObjectStorageService;
import tech.kayys.wayang.storage.spi.StorageServiceInitialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class S3ObjectStorageService implements ObjectStorageService {

    private static final String SERVICE_NAME = "S3 object storage service";

    private S3Client s3Client;
    private String bucketName;
    private ObjectStorageNames objectNames = ObjectStorageNames.unprefixed();

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
        ObjectStorageNames resolvedObjectNames = ObjectStorageNames.fromPrefix(resolvedConfig.pathPrefix());
        S3Client resolvedClient = S3StorageClients.create(resolvedConfig);

        this.bucketName = resolvedConfig.bucketName();
        this.objectNames = resolvedObjectNames;
        this.s3Client = resolvedClient;
    }

    @Override
    public Uni<Optional<byte[]>> getObject(String key) {
        return Uni.createFrom().item(() -> {
            S3Client client = s3Client();
            String bucket = bucketName();
            ObjectStorageNames names = objectNames();
            try {
                GetObjectRequest request = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(names.objectName(key))
                        .build();
                return Optional.of(client.getObjectAsBytes(request).asByteArray());
            } catch (S3Exception error) {
                if (isNotFound(error)) {
                    return Optional.empty();
                }
                throw error;
            }
        });
    }

    @Override
    public Uni<List<String>> listObjects(String prefix) {
        return Uni.createFrom().item(() -> {
            S3Client client = s3Client();
            String bucket = bucketName();
            ObjectStorageNames names = objectNames();
            List<String> keys = new ArrayList<>();
            String continuationToken = null;
            do {
                ListObjectsV2Request request = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(names.objectName(prefix))
                        .continuationToken(continuationToken)
                        .build();
                ListObjectsV2Response response = client.listObjectsV2(request);
                response.contents().stream()
                        .map(S3Object::key)
                        .map(names::logicalKey)
                        .forEach(keys::add);
                continuationToken = response.nextContinuationToken();
            } while (continuationToken != null);
            return List.copyOf(keys);
        });
    }

    @Override
    public Uni<Void> putObject(String key, byte[] data) {
        return Uni.createFrom().item(() -> {
            S3Client client = s3Client();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName())
                    .key(objectNames().objectName(key))
                    .build();
            client.putObject(request, RequestBody.fromBytes(data == null ? new byte[0] : data));
            return (Void) null;
        });
    }

    @Override
    public Uni<Boolean> deleteObject(String key) {
        return Uni.createFrom().item(() -> {
            S3Client client = s3Client();
            String bucket = bucketName();
            String objectName = objectNames().objectName(key);
            try {
                client.headObject(HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectName)
                        .build());
            } catch (S3Exception error) {
                if (isNotFound(error)) {
                    return false;
                }
                throw error;
            }
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectName)
                    .build());
            return true;
        });
    }

    private S3Client s3Client() {
        return StorageServiceInitialization.requireInitialized(s3Client, SERVICE_NAME);
    }

    private String bucketName() {
        return StorageServiceInitialization.requireTextInitialized(bucketName, SERVICE_NAME);
    }

    private ObjectStorageNames objectNames() {
        return StorageServiceInitialization.requireInitialized(objectNames, SERVICE_NAME);
    }

    private static boolean isNotFound(S3Exception error) {
        return error.statusCode() == 404;
    }

}
