package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metadata for one persisted Agentic Commerce Wayang document.
 */
public record AgenticCommerceWayangPersistenceDocument(
        String id,
        String fileName,
        String pathStatusKey,
        String objectKeyStatusKey,
        String availabilityStatusKey,
        String missingWarning,
        String loadFailureIssue) {

    public AgenticCommerceWayangPersistenceDocument {
        id = AgenticCommerceWayangMaps.required(id, "id");
        fileName = AgenticCommerceWayangMaps.required(fileName, "fileName");
        pathStatusKey = AgenticCommerceWayangMaps.required(pathStatusKey, "pathStatusKey");
        objectKeyStatusKey = AgenticCommerceWayangMaps.required(objectKeyStatusKey, "objectKeyStatusKey");
        availabilityStatusKey = AgenticCommerceWayangMaps.required(availabilityStatusKey, "availabilityStatusKey");
        missingWarning = AgenticCommerceWayangMaps.required(missingWarning, "missingWarning");
        loadFailureIssue = AgenticCommerceWayangMaps.required(loadFailureIssue, "loadFailureIssue");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("fileName", fileName);
        values.put("pathStatusKey", pathStatusKey);
        values.put("objectKeyStatusKey", objectKeyStatusKey);
        values.put("availabilityStatusKey", availabilityStatusKey);
        values.put("missingWarning", missingWarning);
        values.put("loadFailureIssue", loadFailureIssue);
        return Map.copyOf(values);
    }
}
