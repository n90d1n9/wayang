package tech.kayys.wayang.gollek.sdk.storage.s3;

/**
 * Access key credentials used to initialize S3-compatible readiness profile storage.
 */
public record WayangReadinessProfileS3Credentials(
        String accessKeyId,
        String secretAccessKey) {

    public WayangReadinessProfileS3Credentials {
        accessKeyId = require(accessKeyId, "accessKeyId");
        secretAccessKey = require(secretAccessKey, "secretAccessKey");
    }

    public static WayangReadinessProfileS3Credentials of(
            String accessKeyId,
            String secretAccessKey) {
        return new WayangReadinessProfileS3Credentials(accessKeyId, secretAccessKey);
    }

    private static String require(String value, String name) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }
}
