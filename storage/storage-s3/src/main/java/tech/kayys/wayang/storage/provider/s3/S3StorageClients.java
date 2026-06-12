package tech.kayys.wayang.storage.provider.s3;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.Objects;

/**
 * Creates AWS SDK S3 clients from typed S3-compatible storage configuration.
 */
final class S3StorageClients {

    private S3StorageClients() {
    }

    static S3Client create(S3StorageConfig config) {
        S3StorageConfig resolvedConfig = Objects.requireNonNull(config, "config");
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                resolvedConfig.accessKeyId(),
                resolvedConfig.secretAccessKey());
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(resolvedConfig.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(resolvedConfig.pathStyleAccess())
                        .build());

        if (!resolvedConfig.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(resolvedConfig.endpoint()));
        }

        return builder.build();
    }
}
