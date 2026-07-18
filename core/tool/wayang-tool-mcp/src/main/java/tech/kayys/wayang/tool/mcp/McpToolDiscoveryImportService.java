package tech.kayys.wayang.tool.mcp;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.tool.repository.McpServerRegistryRepository;
import tech.kayys.wayang.tool.repository.ToolRepository;

@ApplicationScoped
public class McpToolDiscoveryImportService {

    @Inject
    McpToolDiscoveryClient discoveryClient;

    @Inject
    ToolRepository toolRepository;

    @Inject
    McpServerRegistryRepository serverRegistryRepository;

    @WithTransaction
    public Uni<McpToolDiscoveryImportResult> discoverAndImport(
            String requestId,
            McpToolDiscoveryImportRequest request) {
        String fallbackNamespace = request.effectiveNamespace();
        return McpToolDiscoveryImportResolver.resolve(requestId, request, serverRegistryRepository)
                .flatMap(resolved -> discoveryClient.discoverTools(resolved.request().discoveryRequest())
                .flatMap(discovery -> {
                    String namespace = resolved.request().effectiveNamespace();
                    if (!discovery.success()) {
                        return Uni.createFrom().item(McpToolDiscoveryImportCompletion.discoveryFailure(
                                resolved,
                                discovery));
                    }
                    return McpToolDiscoveryImportUpserter.upsertTools(
                            toolRepository,
                            requestId,
                            resolved.request(),
                            namespace,
                            discovery.tools())
                            .flatMap(changes -> McpToolDiscoveryImportStaleMarker.markStaleTools(
                                    toolRepository,
                                    requestId,
                                    resolved.request(),
                                    namespace,
                                    changes.toolIds())
                                    .map(staleToolIds -> McpToolDiscoveryImportCompletion.success(
                                            resolved,
                                            namespace,
                                            discovery,
                                            changes,
                                            staleToolIds)));
                }))
                .onFailure().recoverWithItem(error -> McpToolDiscoveryImportCompletion.failure(
                        request,
                        fallbackNamespace,
                        error));
    }

}
