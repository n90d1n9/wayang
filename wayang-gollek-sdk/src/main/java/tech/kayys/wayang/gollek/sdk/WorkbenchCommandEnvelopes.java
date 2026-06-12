package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire envelope factory for command discovery and workbench projections.
 *
 * <p>Workbench command envelopes live in the SDK so shell, TUI, HTTP, and
 * embedded product surfaces can share one ordered JSON contract.</p>
 */
public final class WorkbenchCommandEnvelopes {

    private WorkbenchCommandEnvelopes() {
    }

    public static Map<String, Object> discovery(String productName, WorkbenchCommandDiscovery discovery) {
        return discoveryValues(productName, discovery, true);
    }

    public static Map<String, Object> index(String productName, WorkbenchCommandDiscovery discovery) {
        return discoveryValues(productName, discovery, false);
    }

    public static Map<String, Object> workbench(
            WayangWorkbenchModel model,
            List<ProductSurfacePolicy> policies,
            WorkbenchCommandQuery query) {
        WayangWorkbenchModel source = normalizeWorkbench(model);
        WorkbenchCommandQuery normalized = query == null ? WorkbenchCommandQuery.all() : query;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", source.status().productName());
        values.put("status", status(source.status()));
        values.put("catalog", WayangPlatformEnvelopes.productCatalog(source.productSurfaces(), policies));
        values.put("commandQuery", query(normalized));
        values.put("commandPalette", source.commandPalette());
        values.put("commands", source.commands().stream()
                .map(WorkbenchCommandEnvelopes::command)
                .toList());
        values.put("nextActions", source.nextActions());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> query(WorkbenchCommandQuery query) {
        WorkbenchCommandQuery normalized = query == null ? WorkbenchCommandQuery.all() : query;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surfaceId", normalized.surfaceId());
        values.put("profileId", normalized.profileId());
        values.put("resolvedSurfaceId", normalized.resolvedSurfaceId());
        values.put("category", normalized.category());
        values.put("commandId", normalized.commandId());
        values.put("contractJsonSchemaId", normalized.contractJsonSchemaId());
        values.put("filtered", normalized.filtered());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> categorySummary(WorkbenchCommandCategorySummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", summary.name());
        values.put("count", summary.count());
        values.put("commandIds", summary.commandIds());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> contractSummary(WorkbenchCommandContractSummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("jsonSchemaId", summary.jsonSchemaId());
        values.put("schema", summary.schema());
        values.put("version", summary.version());
        values.put("envelope", summary.envelope());
        values.put("count", summary.count());
        values.put("commandIds", summary.commandIds());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> command(WorkbenchCommand command) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", command.id());
        values.put("title", command.title());
        values.put("command", command.command());
        values.put("category", command.category());
        values.put("description", command.description());
        values.put("surfaceIds", command.surfaceIds());
        values.put("localOnly", command.localOnly());
        if (!command.contracts().isEmpty()) {
            values.put("contracts", command.contracts().stream()
                    .map(WorkbenchCommandEnvelopes::contract)
                    .toList());
        }
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> contract(WorkbenchCommandContract contract) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", contract.schema());
        values.put("version", contract.version());
        values.put("envelope", contract.envelope());
        values.put("jsonSchemaId", contract.jsonSchemaId());
        return SdkMaps.orderedCopy(values);
    }

    private static Map<String, Object> status(WayangPlatformStatus status) {
        WayangPlatformStatus model = status == null ? normalizeWorkbench(null).status() : status;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", model.productName());
        values.put("version", model.version());
        values.put("components", List.of(
                component(model.gollek()),
                component(model.gamelan()),
                component(model.agentCore()),
                component(model.rag()),
                component(model.mcp())));
        values.put("activeSkills", model.activeSkills());
        values.put("notes", model.notes());
        return SdkMaps.orderedCopy(values);
    }

    private static Map<String, Object> component(ComponentStatus component) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", component.name());
        values.put("role", component.role());
        values.put("state", component.state());
        values.put("endpoint", component.endpoint());
        values.put("healthPercent", component.healthPercent());
        return SdkMaps.orderedCopy(values);
    }

    private static Map<String, Object> discoveryValues(
            String productName,
            WorkbenchCommandDiscovery discovery,
            boolean includeCommands) {
        WorkbenchCommandDiscovery model = normalize(discovery);
        WorkbenchCommandQuery commandQuery = model.query();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("surfaceId", commandQuery.surfaceId());
        values.put("profileId", commandQuery.profileId());
        values.put("resolvedSurfaceId", commandQuery.resolvedSurfaceId());
        values.put("category", commandQuery.category());
        values.put("commandId", commandQuery.commandId());
        values.put("contractJsonSchemaId", commandQuery.contractJsonSchemaId());
        values.put("query", query(commandQuery));
        values.put("totalCommands", model.totalCommands());
        values.put("matchingCommands", model.matchingCommands());
        values.put("categories", model.categories());
        values.put("categoryCounts", model.categoryCounts());
        values.put("categorySummaries", model.categorySummaries().stream()
                .map(WorkbenchCommandEnvelopes::categorySummary)
                .toList());
        values.put("contractJsonSchemaIds", model.contractJsonSchemaIds());
        values.put("contractJsonSchemaIdCounts", model.contractJsonSchemaIdCounts());
        values.put("contractSummaries", model.contractSummaries().stream()
                .map(WorkbenchCommandEnvelopes::contractSummary)
                .toList());
        values.put("commandIds", model.commandIds());
        if (includeCommands) {
            values.put("commands", model.commands().stream()
                    .map(WorkbenchCommandEnvelopes::command)
                    .toList());
        }
        return SdkMaps.orderedCopy(values);
    }

    public static WorkbenchCommandDiscovery normalize(WorkbenchCommandDiscovery discovery) {
        return discovery == null
                ? WorkbenchCommandDiscovery.of(WorkbenchCommandQuery.all(), List.of(), 0)
                : discovery;
    }

    private static WayangWorkbenchModel normalizeWorkbench(WayangWorkbenchModel model) {
        return model == null ? new WayangWorkbenchModel(null, List.of(), List.of(), List.of()) : model;
    }
}
