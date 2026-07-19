package tech.kayys.wayang.tool.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.dto.McpRegistryImportRequest;
import tech.kayys.wayang.tool.dto.RegistrySyncResponse;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.OpenApiSource;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.OpenApiSourceRepository;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@ApplicationScoped
public class RegistrySyncService {

    @Inject
    OpenApiSourceRepository openApiSourceRepository;

    @Inject
    DefaultToolGenerationService toolGenerationService;

    @Inject
    McpServerRegistryRepository mcpServerRegistryRepository;

    @Inject
    McpRegistryService mcpRegistryService;

    @Inject
    RegistrySyncHistoryRepository historyRepository;

    @Inject
    EditionModeService editionModeService;

    public Uni<RegistrySyncResponse> syncTenant(String requestId, String userId, boolean syncOpenApi, boolean syncMcp) {
        Uni<OpenApiSyncResult> openApiUni = syncOpenApi
                ? syncOpenApiSources(requestId, userId)
                : Uni.createFrom().item(new OpenApiSyncResult(0, 0, 0, List.of()));

        Uni<McpSyncResult> mcpUni = syncMcp && editionModeService.supportsMcpRegistryDatabase()
                ? syncMcpSources(requestId)
                : Uni.createFrom().item(new McpSyncResult(0, 0, List.of()));

        return Uni.combine().all().unis(openApiUni, mcpUni).asTuple()
                .map(tuple -> {
                    List<String> warnings = new ArrayList<>();
                    warnings.addAll(tuple.getItem1().warnings());
                    warnings.addAll(tuple.getItem2().warnings());
                    if (syncMcp && !editionModeService.supportsMcpRegistryDatabase()) {
                        warnings.add("MCP DB sync skipped: enterprise mode is required.");
                    }
                    return new RegistrySyncResponse(
                            tuple.getItem1().scanned(),
                            tuple.getItem1().updated(),
                            tuple.getItem1().toolsUpserted(),
                            tuple.getItem2().scanned(),
                            tuple.getItem2().imported(),
                            warnings);
                });
    }

    public Uni<RegistrySyncResponse> syncScheduled() {
        Uni<OpenApiSyncResult> openApiUni = syncScheduledOpenApiSources("system-scheduler");
        Uni<McpSyncResult> mcpUni = editionModeService.supportsMcpRegistryDatabase()
                ? syncScheduledMcpSources()
                : Uni.createFrom().item(new McpSyncResult(0, 0, List.of()));
        return Uni.combine().all().unis(openApiUni, mcpUni).asTuple()
                .map(tuple -> {
                    List<String> warnings = new ArrayList<>();
                    warnings.addAll(tuple.getItem1().warnings());
                    warnings.addAll(tuple.getItem2().warnings());
                    return new RegistrySyncResponse(
                            tuple.getItem1().scanned(),
                            tuple.getItem1().updated(),
                            tuple.getItem1().toolsUpserted(),
                            tuple.getItem2().scanned(),
                            tuple.getItem2().imported(),
                            warnings);
                });
    }

    private Uni<OpenApiSyncResult> syncOpenApiSources(String requestId, String userId) {
        return openApiSourceRepository.listSyncCandidates(requestId)
                .flatMap(sources -> {
                    if (sources.isEmpty()) {
                        return Uni.createFrom().item(new OpenApiSyncResult(0, 0, 0, List.of()));
                    }
                    Uni<Accumulator> chain = Uni.createFrom().item(new Accumulator());
                    for (OpenApiSource source : sources) {
                        chain = chain.flatMap(acc -> syncOpenApiSource(source, userId, true)
                                .map(result -> {
                                    acc.scanned++;
                                    if (result.updated()) {
                                        acc.updated++;
                                        acc.tools += result.items();
                                    }
                                    acc.warnings.addAll(result.warnings());
                                    return acc;
                                }));
                    }
                    return chain.map(acc -> new OpenApiSyncResult(acc.scanned, acc.updated, acc.tools, acc.warnings));
                });
    }

    private Uni<McpSyncResult> syncMcpSources(String requestId) {
        return mcpServerRegistryRepository.listByRequestId(requestId)
                .flatMap(entries -> {
                    Set<String> sources = entries.stream()
                            .map(e -> e.getSource() == null ? "" : e.getSource().trim())
                            .filter(s -> s.startsWith("http://") || s.startsWith("https://"))
                            .collect(Collectors.toSet());

                    if (sources.isEmpty()) {
                        return Uni.createFrom().item(new McpSyncResult(0, 0, List.of()));
                    }

                    Uni<McpAccumulator> chain = Uni.createFrom().item(new McpAccumulator());
                    for (String source : sources) {
                        chain = chain.flatMap(acc -> syncMcpSource(requestId, source, entries, true)
                                .map(result -> {
                                    acc.scanned++;
                                    acc.imported += result.items();
                                    acc.warnings.addAll(result.warnings());
                                    return acc;
                                }));
                    }
                    return chain.map(acc -> new McpSyncResult(acc.scanned, acc.imported, acc.warnings));
                });
    }

    private Uni<OpenApiSyncResult> syncScheduledOpenApiSources(String userId) {
        return openApiSourceRepository.listScheduledCandidates()
                .flatMap(sources -> {
                    Uni<Accumulator> chain = Uni.createFrom().item(new Accumulator());
                    for (OpenApiSource source : sources) {
                        if (!isDue(source.getSyncSchedule(), source.getLastSyncAt())) {
                            continue;
                        }
                        chain = chain.flatMap(acc -> syncOpenApiSource(source, userId, true)
                                .map(result -> {
                                    acc.scanned++;
                                    if (result.updated()) {
                                        acc.updated++;
                                        acc.tools += result.items();
                                    }
                                    acc.warnings.addAll(result.warnings());
                                    return acc;
                                }));
                    }
                    return chain.map(acc -> new OpenApiSyncResult(acc.scanned, acc.updated, acc.tools, acc.warnings));
                });
    }

    private Uni<McpSyncResult> syncScheduledMcpSources() {
        return mcpServerRegistryRepository.listScheduledCandidates()
                .flatMap(entries -> {
                    Map<String, List<McpServerRegistry>> grouped = new HashMap<>();
                    for (McpServerRegistry entry : entries) {
                        String source = entry.getSource() == null ? "" : entry.getSource().trim();
                        if (!source.startsWith("http://") && !source.startsWith("https://")) {
                            continue;
                        }
                        if (!isDue(entry.getSyncSchedule(), entry.getLastSyncAt())) {
                            continue;
                        }
                        String key = entry.getRequestId() + "||" + source;
                        grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
                    }

                    Uni<McpAccumulator> chain = Uni.createFrom().item(new McpAccumulator());
                    for (Map.Entry<String, List<McpServerRegistry>> group : grouped.entrySet()) {
                        List<McpServerRegistry> refs = group.getValue();
                        String requestId = refs.get(0).getRequestId();
                        String source = refs.get(0).getSource();
                        chain = chain.flatMap(acc -> syncMcpSource(requestId, source, refs, true)
                                .map(result -> {
                                    acc.scanned++;
                                    acc.imported += result.items();
                                    acc.warnings.addAll(result.warnings());
                                    return acc;
                                }));
                    }
                    return chain.map(acc -> new McpSyncResult(acc.scanned, acc.imported, acc.warnings));
                });
    }

    private Uni<SourceSyncResult> syncOpenApiSource(OpenApiSource source, String userId, boolean recordHistory) {
        Instant started = Instant.now();
        return toolGenerationService.syncSource(source, userId)
                .flatMap(result -> {
                    if (recordHistory) {
                        return record(source.getRequestId(), "OPENAPI", String.valueOf(source.getSourceId()),
                                "SUCCESS", "OpenAPI source synced", result.toolsGenerated(), started, Instant.now())
                                .replaceWith(new SourceSyncResult(result.toolsGenerated() > 0, result.toolsGenerated(), result.warnings()));
                    }
                    return Uni.createFrom().item(new SourceSyncResult(result.toolsGenerated() > 0, result.toolsGenerated(), result.warnings()));
                })
                .onFailure().recoverWithUni(error -> {
                    String warning = "OpenAPI sync failed for source " + source.getSourceId() + ": " + error.getMessage();
                    if (!recordHistory) {
                        return Uni.createFrom().item(new SourceSyncResult(false, 0, List.of(warning)));
                    }
                    return record(source.getRequestId(), "OPENAPI", String.valueOf(source.getSourceId()),
                            "ERROR", error.getMessage(), 0, started, Instant.now())
                            .replaceWith(new SourceSyncResult(false, 0, List.of(warning)));
                });
    }

    private Uni<SourceSyncResult> syncMcpSource(String requestId, String source, List<McpServerRegistry> refs, boolean recordHistory) {
        Instant started = Instant.now();
        return mcpRegistryService.importFromJson(requestId, new McpRegistryImportRequest("URL", source, null))
                .flatMap(imported -> {
                    Instant now = Instant.now();
                    Uni<Void> updateChain = Uni.createFrom().voidItem();
                    for (McpServerRegistry ref : refs) {
                        ref.setLastSyncAt(now);
                        updateChain = updateChain.flatMap(v -> mcpServerRegistryRepository.save(ref)).replaceWithVoid();
                    }
                    if (!recordHistory) {
                        return updateChain.replaceWith(new SourceSyncResult(imported.importedCount() > 0, imported.importedCount(), List.of()));
                    }
                    return updateChain
                            .flatMap(v -> record(requestId, "MCP", source, "SUCCESS", "MCP source synced",
                                    imported.importedCount(), started, Instant.now()))
                            .replaceWith(new SourceSyncResult(imported.importedCount() > 0, imported.importedCount(), List.of()));
                })
                .onFailure().recoverWithUni(error -> {
                    String warning = "MCP sync failed for source " + source + ": " + error.getMessage();
                    if (!recordHistory) {
                        return Uni.createFrom().item(new SourceSyncResult(false, 0, List.of(warning)));
                    }
                    return record(requestId, "MCP", source, "ERROR", error.getMessage(), 0, started, Instant.now())
                            .replaceWith(new SourceSyncResult(false, 0, List.of(warning)));
                });
    }

    private Uni<Void> record(
            String requestId,
            String sourceKind,
            String sourceRef,
            String status,
            String message,
            int items,
            Instant startedAt,
            Instant finishedAt) {
        RegistrySyncHistory history = new RegistrySyncHistory();
        history.setRequestId(requestId);
        history.setSourceKind(sourceKind);
        history.setSourceRef(sourceRef);
        history.setStatus(status);
        history.setMessage(message);
        history.setItemsAffected(items);
        history.setStartedAt(startedAt);
        history.setFinishedAt(finishedAt);
        return historyRepository.save(history).replaceWithVoid();
    }

    public static Duration parseInterval(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Interval must not be empty.");
        }
        String raw = expression.trim().toUpperCase();
        try {
            if (raw.startsWith("P")) {
                return Duration.parse(raw);
            }
            if (raw.endsWith("S")) {
                return Duration.ofSeconds(Long.parseLong(raw.substring(0, raw.length() - 1)));
            }
            if (raw.endsWith("M")) {
                return Duration.ofMinutes(Long.parseLong(raw.substring(0, raw.length() - 1)));
            }
            if (raw.endsWith("H")) {
                return Duration.ofHours(Long.parseLong(raw.substring(0, raw.length() - 1)));
            }
            if (raw.endsWith("D")) {
                return Duration.ofDays(Long.parseLong(raw.substring(0, raw.length() - 1)));
            }
            return Duration.ofMinutes(Long.parseLong(raw));
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid interval format: " + expression + ". Use ISO-8601 (e.g., PT15M) or shorthand (e.g., 15m).");
        }
    }

    private boolean isDue(String intervalExpr, Instant lastSyncAt) {
        if (intervalExpr == null || intervalExpr.isBlank()) {
            return false;
        }
        Duration interval = parseInterval(intervalExpr);
        if (lastSyncAt == null) {
            return true;
        }
        return Instant.now().isAfter(lastSyncAt.plus(interval));
    }

    private static final class Accumulator {
        int scanned;
        int updated;
        int tools;
        final List<String> warnings = new ArrayList<>();
    }

    private static final class McpAccumulator {
        int scanned;
        int imported;
        final List<String> warnings = new ArrayList<>();
    }

    private record SourceSyncResult(boolean updated, int items, List<String> warnings) {
    }

    private record OpenApiSyncResult(int scanned, int updated, int toolsUpserted, List<String> warnings) {
    }

    private record McpSyncResult(int scanned, int imported, List<String> warnings) {
    }
}
