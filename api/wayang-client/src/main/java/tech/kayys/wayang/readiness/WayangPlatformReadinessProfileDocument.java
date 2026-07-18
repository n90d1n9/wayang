package tech.kayys.wayang.readiness;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import tech.kayys.wayang.client.SdkText;

public final class WayangPlatformReadinessProfileDocument {

    public static final String SCHEMA = "wayang.platform.readiness-profiles";
    public static final String VERSION = "1";

    private WayangPlatformReadinessProfileDocument() {
    }

    public static List<WayangPlatformReadinessProfileDescriptor> fromProperties(Properties properties) {
        Properties model = properties == null ? new Properties() : properties;
        validateHeader(model);
        List<String> profileIds = csv(model.getProperty("profileIds"));
        if (profileIds.isEmpty()) {
            throw new IllegalArgumentException("Readiness profile document must define profileIds.");
        }

        String defaultProfileId = SdkText.trimToEmpty(model.getProperty("defaultProfileId"));
        String productionProfileId = SdkText.trimToEmpty(model.getProperty("productionProfileId"));
        List<WayangPlatformReadinessProfileDescriptor> profiles = new ArrayList<>();
        for (String profileId : profileIds) {
            String prefix = "profile." + profileId + ".";
            String description = model.getProperty(prefix + "description");
            List<String> readinessIds = csv(model.getProperty(prefix + "readinessIds"));
            boolean defaultProfile = profileId.equals(defaultProfileId)
                    || booleanProperty(model, prefix + "default", false);
            boolean productionProfile = profileId.equals(productionProfileId)
                    || booleanProperty(model, prefix + "production", false);
            profiles.add(new WayangPlatformReadinessProfileDescriptor(
                    profileId,
                    description,
                    defaultProfile,
                    productionProfile,
                    readinessIds));
        }
        return List.copyOf(profiles);
    }

    private static void validateHeader(Properties properties) {
        String schema = SdkText.trimToDefault(properties.getProperty("schema"), SCHEMA);
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException(
                    "Unsupported readiness profile document schema '" + schema + "'.");
        }
        String version = SdkText.trimToDefault(properties.getProperty("version"), VERSION);
        if (!VERSION.equals(version)) {
            throw new IllegalArgumentException(
                    "Unsupported readiness profile document version '" + version + "'.");
        }
    }

    private static List<String> csv(String value) {
        String text = SdkText.trimToEmpty(value);
        if (text.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : text.split(",")) {
            String normalized = SdkText.trimToEmpty(part);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return List.copyOf(values);
    }

    private static boolean booleanProperty(Properties properties, String key, boolean defaultValue) {
        String value = SdkText.trimToEmpty(properties.getProperty(key));
        if (value.isEmpty()) {
            return defaultValue;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "1" -> true;
            case "false", "no", "0" -> false;
            default -> throw new IllegalArgumentException(
                    "Invalid readiness profile boolean property '" + key + "' value '" + value + "'.");
        };
    }
}
