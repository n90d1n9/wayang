package tech.kayys.wayang.gollek.sdk;

import java.util.Locale;

public enum WayangPlatformReadinessProfileRegistryMode {
    BUILTIN,
    FILE,
    DATABASE,
    OBJECT_STORAGE,
    HYBRID;

    public static WayangPlatformReadinessProfileRegistryMode from(String value) {
        String normalized = SdkText.trimToEmpty(value)
                .replace('-', '_')
                .replace('.', '_')
                .toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return BUILTIN;
        }
        return switch (normalized) {
            case "LOCAL", "DEFAULT", "BUILT_INS", "BUILTIN" -> BUILTIN;
            case "FILES", "FILESYSTEM", "FS" -> FILE;
            case "DB", "POSTGRES", "POSTGRESQL", "SQL" -> DATABASE;
            case "CLOUD", "OBJECT", "OBJECT_STORE", "OBJECT_STORAGE", "S3", "RUSTFS", "MINIO" -> OBJECT_STORAGE;
            case "MIXED", "PRIMARY_WITH_FALLBACK", "FILE_WITH_FALLBACK" -> HYBRID;
            default -> valueOf(normalized);
        };
    }
}
