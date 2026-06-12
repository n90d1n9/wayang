package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Updates a surface-scoped A2UI data model.
 */
public record A2uiDataModelUpdate(String surfaceId, String path, List<A2uiDataEntry> contents)
        implements A2uiServerMessage {

    public A2uiDataModelUpdate {
        surfaceId = A2uiValues.optional(surfaceId);
        path = A2uiValues.optional(path);
        contents = contents == null ? List.of() : List.copyOf(contents);
    }

    public static A2uiDataModelUpdate root(String surfaceId, A2uiDataEntry... contents) {
        return new A2uiDataModelUpdate(surfaceId, null, contents == null ? List.of() : List.of(contents));
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> body = new LinkedHashMap<>();
        A2uiValues.putOptional(body, "surfaceId", surfaceId);
        A2uiValues.putOptional(body, "path", path);
        body.put("contents", contents.stream()
                .map(A2uiDataEntry::toPayload)
                .toList());
        return A2uiValues.payload("dataModelUpdate", body);
    }
}
