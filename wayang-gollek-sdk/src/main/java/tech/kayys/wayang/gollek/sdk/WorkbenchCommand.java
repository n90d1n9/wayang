package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record WorkbenchCommand(
        String id,
        String title,
        String command,
        String category,
        String description,
        List<String> surfaceIds,
        boolean localOnly,
        List<WorkbenchCommandContract> contracts) {

    public WorkbenchCommand {
        id = normalizeRequired("Command id", id);
        title = normalizeRequired("Command title", title);
        command = normalizeRequired("Command text", command);
        category = normalizeDefault(category, "General");
        description = normalizeDefault(description, "");
        surfaceIds = SdkLists.copy(surfaceIds);
        contracts = SdkLists.copy(contracts);
    }

    public static WorkbenchCommand shared(
            String id,
            String title,
            String command,
            String category,
            String description,
            List<String> surfaceIds) {
        return shared(id, title, command, category, description, surfaceIds, List.of());
    }

    public static WorkbenchCommand shared(
            String id,
            String title,
            String command,
            String category,
            String description,
            List<String> surfaceIds,
            List<WorkbenchCommandContract> contracts) {
        return new WorkbenchCommand(id, title, command, category, description, surfaceIds, false, contracts);
    }

    public static WorkbenchCommand local(
            String id,
            String title,
            String command,
            String category,
            String description,
            List<String> surfaceIds) {
        return local(id, title, command, category, description, surfaceIds, List.of());
    }

    public static WorkbenchCommand local(
            String id,
            String title,
            String command,
            String category,
            String description,
            List<String> surfaceIds,
            List<WorkbenchCommandContract> contracts) {
        return new WorkbenchCommand(id, title, command, category, description, surfaceIds, true, contracts);
    }

    private static String normalizeRequired(String label, String value) {
        String normalized = normalizeDefault(value, "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }

    private static String normalizeDefault(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
