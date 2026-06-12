package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Safety options for copying persisted Agentic Commerce Wayang state.
 */
public record AgenticCommerceWayangPersistenceTransferOptions(
        boolean dryRun,
        boolean overwriteExisting,
        boolean verifyAfterCopy) {

    public static AgenticCommerceWayangPersistenceTransferOptions defaults() {
        return new AgenticCommerceWayangPersistenceTransferOptions(false, true, true);
    }

    public static AgenticCommerceWayangPersistenceTransferOptions dryRunOnly() {
        return defaults().withDryRun(true);
    }

    public static AgenticCommerceWayangPersistenceTransferOptions noOverwrite() {
        return defaults().withOverwriteExisting(false);
    }

    public AgenticCommerceWayangPersistenceTransferOptions withDryRun(boolean dryRun) {
        return new AgenticCommerceWayangPersistenceTransferOptions(dryRun, overwriteExisting, verifyAfterCopy);
    }

    public AgenticCommerceWayangPersistenceTransferOptions withOverwriteExisting(boolean overwriteExisting) {
        return new AgenticCommerceWayangPersistenceTransferOptions(dryRun, overwriteExisting, verifyAfterCopy);
    }

    public AgenticCommerceWayangPersistenceTransferOptions withVerifyAfterCopy(boolean verifyAfterCopy) {
        return new AgenticCommerceWayangPersistenceTransferOptions(dryRun, overwriteExisting, verifyAfterCopy);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("dryRun", dryRun);
        values.put("overwriteExisting", overwriteExisting);
        values.put("verifyAfterCopy", verifyAfterCopy);
        return Map.copyOf(values);
    }
}
