package tech.kayys.wayang.gollek.sdk;

import java.util.Optional;

public record WayangContractKey(String schema, int version, String envelope) {

    public static final String JSON_SCHEMA_ID_PREFIX = "urn:wayang:contract:";

    public WayangContractKey {
        schema = SdkText.trimToEmpty(schema);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangContractKey of(String schema, int version, String envelope) {
        return new WayangContractKey(schema, version, envelope);
    }

    public static WayangContractKey from(WayangContractDescriptor contract) {
        if (contract == null) {
            return of("", 1, "");
        }
        return of(contract.schema(), contract.version(), contract.envelope());
    }

    public static WayangContractKey from(WorkbenchCommandContract contract) {
        if (contract == null) {
            return of("", 1, "");
        }
        return of(contract.schema(), contract.version(), contract.envelope());
    }

    public static Optional<WayangContractKey> parseJsonSchemaId(String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (!normalized.startsWith(JSON_SCHEMA_ID_PREFIX)) {
            return Optional.empty();
        }

        String body = normalized.substring(JSON_SCHEMA_ID_PREFIX.length());
        int versionMarker = body.indexOf(":v");
        if (versionMarker <= 0) {
            return Optional.empty();
        }

        int versionStart = versionMarker + 2;
        int envelopeSeparator = body.indexOf(':', versionStart);
        if (envelopeSeparator <= versionStart) {
            return Optional.empty();
        }

        String schema = body.substring(0, versionMarker);
        String versionText = body.substring(versionStart, envelopeSeparator);
        String envelope = body.substring(envelopeSeparator + 1);
        if (schema.isBlank() || envelope.isBlank() || !versionText.matches("[0-9]+")) {
            return Optional.empty();
        }

        int parsedVersion;
        try {
            parsedVersion = Integer.parseInt(versionText);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (parsedVersion < 1) {
            return Optional.empty();
        }

        return Optional.of(of(schema, parsedVersion, envelope));
    }

    public String jsonSchemaId() {
        return JSON_SCHEMA_ID_PREFIX + schema + ":v" + version + ":" + envelope;
    }

    public String label() {
        return schema + "/" + envelope + " v" + version;
    }

    public boolean matches(WayangContractDescriptor contract) {
        return equals(from(contract));
    }

    public boolean matches(WorkbenchCommandContract contract) {
        return equals(from(contract));
    }
}
