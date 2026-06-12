package tech.kayys.wayang.agent.hermes;

import java.util.Locale;

final class HermesSkillPersistenceStoreClassifier {

    private HermesSkillPersistenceStoreClassifier() {
    }

    static String storeType(String store) {
        String normalized = normalize(store);
        if (normalized.isBlank() || "none".equals(normalized)) {
            return "none";
        }
        if (normalized.contains("hybrid")) {
            return "hybrid";
        }
        if (normalized.contains("skillmanagement")) {
            return "skill-management";
        }
        if (normalized.contains("database")
                || normalized.contains("jdbc")
                || normalized.contains("postgres")
                || normalized.contains("mysql")
                || normalized.contains("mariadb")) {
            return "database";
        }
        if (normalized.equals("file")
                || normalized.equals("files")
                || normalized.equals("filesystem")
                || normalized.contains("localfile")) {
            return "file";
        }
        if (canonicalCloudStore(store) != null
                || normalized.contains("objectstorage")
                || normalized.contains("objectstore")
                || normalized.contains("cloudstorage")) {
            return "object-storage";
        }
        return "custom";
    }

    static boolean sameStore(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    static boolean isDatabase(String store) {
        return "database".equals(storeType(store));
    }

    static boolean isObjectStorage(String store) {
        return "object-storage".equals(storeType(store));
    }

    static boolean isFile(String store) {
        return "file".equals(storeType(store));
    }

    static boolean isHybrid(String store) {
        return "hybrid".equals(storeType(store));
    }

    static String canonicalCloudStore(String value) {
        return switch (normalize(value)) {
            case "s3", "amazons3" -> "s3";
            case "rustfs" -> "rustfs";
            case "minio" -> "minio";
            case "gcs", "googlecloudstorage" -> "gcs";
            case "azureblob", "azureblobstorage" -> "azure-blob";
            case "objectstorage", "objectstore" -> "object-storage";
            case "s3compatible" -> "s3-compatible";
            default -> null;
        };
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        value.toLowerCase(Locale.ROOT).chars()
                .filter(Character::isLetterOrDigit)
                .forEach(codePoint -> builder.append((char) codePoint));
        return builder.toString();
    }
}
