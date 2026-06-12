package tech.kayys.wayang.gollek.sdk;

import java.util.Optional;

public record WorkbenchCommandQuery(
        String surfaceId,
        String profileId,
        String category,
        String commandId,
        String contractJsonSchemaId) {

    public WorkbenchCommandQuery {
        surfaceId = normalize(surfaceId);
        profileId = normalize(profileId);
        category = normalize(category);
        commandId = normalize(commandId);
        contractJsonSchemaId = normalize(contractJsonSchemaId);
    }

    public WorkbenchCommandQuery(String surfaceId, String profileId, String category, String commandId) {
        this(surfaceId, profileId, category, commandId, null);
    }

    public WorkbenchCommandQuery(String surfaceId, String category, String commandId) {
        this(surfaceId, null, category, commandId, null);
    }

    public static WorkbenchCommandQuery all() {
        return new WorkbenchCommandQuery(null, null, null, null, null);
    }

    public static WorkbenchCommandQuery of(String surfaceId, String category, String commandId) {
        return new WorkbenchCommandQuery(surfaceId, null, category, commandId, null);
    }

    public static WorkbenchCommandQuery of(String surfaceId, String profileId, String category, String commandId) {
        return new WorkbenchCommandQuery(surfaceId, profileId, category, commandId, null);
    }

    public static WorkbenchCommandQuery of(
            String surfaceId,
            String profileId,
            String category,
            String commandId,
            String contractJsonSchemaId) {
        return new WorkbenchCommandQuery(surfaceId, profileId, category, commandId, contractJsonSchemaId);
    }

    public static WorkbenchCommandQuery forSurface(String surfaceId) {
        return new WorkbenchCommandQuery(surfaceId, null, null, null, null);
    }

    public static WorkbenchCommandQuery forCategory(String category) {
        return new WorkbenchCommandQuery(null, null, category, null, null);
    }

    public static WorkbenchCommandQuery forCommandId(String commandId) {
        return new WorkbenchCommandQuery(null, null, null, commandId, null);
    }

    public static WorkbenchCommandQuery forProfile(String profileId) {
        return forProfile(profileId, null, null);
    }

    public static WorkbenchCommandQuery forProfile(String profileId, String category, String commandId) {
        return new WorkbenchCommandQuery(null, profileId, category, commandId, null);
    }

    public static WorkbenchCommandQuery forContractKey(WayangContractKey key) {
        return forContractJsonSchemaId(key == null ? null : key.jsonSchemaId());
    }

    public static WorkbenchCommandQuery forContractJsonSchemaId(String contractJsonSchemaId) {
        return new WorkbenchCommandQuery(null, null, null, null, contractJsonSchemaId);
    }

    public boolean hasSurfaceId() {
        return surfaceId != null;
    }

    public boolean hasProfileId() {
        return profileId != null;
    }

    public boolean hasCategory() {
        return category != null;
    }

    public boolean hasCommandId() {
        return commandId != null;
    }

    public boolean hasContractJsonSchemaId() {
        return contractJsonSchemaId != null;
    }

    public Optional<WayangContractKey> contractJsonSchemaKey() {
        return WayangContractKey.parseJsonSchemaId(contractJsonSchemaId);
    }

    public boolean filtered() {
        return hasSurfaceId() || hasProfileId() || hasCategory() || hasCommandId() || hasContractJsonSchemaId();
    }

    public String resolvedSurfaceId() {
        if (!hasProfileId()) {
            return hasSurfaceId() ? WayangProductCatalog.requireKnownSurfaceId(surfaceId) : null;
        }

        ProductProfile profile = WayangProductCatalog.profileFor(profileId);
        if (!hasSurfaceId()) {
            return profile.surfaceId();
        }

        String normalizedSurfaceId = WayangProductCatalog.requireKnownSurfaceId(surfaceId);
        if (!normalizedSurfaceId.equals(profile.surfaceId())) {
            throw new IllegalArgumentException(
                    "Wayang product profile '" + profile.id() + "' belongs to surface '"
                            + profile.surfaceId() + "', not '" + normalizedSurfaceId + "'.");
        }
        return normalizedSurfaceId;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
