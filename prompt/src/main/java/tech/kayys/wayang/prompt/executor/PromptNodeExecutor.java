package tech.kayys.wayang.prompt.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.protocol.CommunicationType;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.prompt.core.PromptEngine;
import tech.kayys.wayang.prompt.core.PromptRequest;
import tech.kayys.wayang.prompt.core.RenderedChain;
import tech.kayys.wayang.prompt.core.RenderResult;
import tech.kayys.wayang.prompt.core.TemplateRef;
import tech.kayys.wayang.prompt.node.PromptNodeTypes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Node executor for rendering prompt chains.
 */
@ApplicationScoped
@Executor(
        executorType = "prompt-renderer",
        maxConcurrentTasks = 10,
        supportedNodeTypes = {PromptNodeTypes.PROMPT_RENDER},
        communicationType = CommunicationType.REST
)
public class PromptNodeExecutor extends AbstractWorkflowExecutor {

    private static final Logger LOG = Logger.getLogger(PromptNodeExecutor.class);

    @Inject
    PromptEngine promptEngine;

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        LOG.infof("Executing prompt render for node: %s", task.nodeId().value());

        Map<String, Object> config = task.context();

        List<Map<String, Object>> rawTemplateRefs = (List<Map<String, Object>>) config.get("templateRefs");
        if (rawTemplateRefs == null || rawTemplateRefs.isEmpty()) {
            return Uni.createFrom()
                    .failure(new IllegalArgumentException("Node configuration 'templateRefs' is required"));
        }

        List<TemplateRef> templateRefs = new ArrayList<>();
        for (Map<String, Object> rawRef : rawTemplateRefs) {
            String id = (String) rawRef.get("id");
            if (id == null || id.isBlank()) {
                return Uni.createFrom().failure(new IllegalArgumentException("Template reference 'id' is required"));
            }
            String version = (String) rawRef.get("version");
            if (version == null || version.isBlank()) {
                templateRefs.add(TemplateRef.latest(id));
            } else {
                templateRefs.add(TemplateRef.pinned(id, version));
            }
        }

        PromptRequest request = PromptRequest.builder()
                .tenantId(task.context().getOrDefault("tenantId", "default").toString())
                .runId(task.runId().value())
                .nodeId(task.nodeId().value())
                .templateRefs(templateRefs)
                .explicitValues(task.context())
                .contextValues(Map.of()) // Additional context can be mapped here if needed
                .build();

        return promptEngine.buildAndRender(request)
                .map(renderedChain -> {
                    // Convert rendered chain to output map
                    Map<String, Object> outputs = Map.of(
                            "messages", extractMessages(renderedChain),
                            "liveContent", renderedChain.liveContent(),
                            "redactedContent", renderedChain.redactedContent(),
                            "skippedTemplateIds", renderedChain.skippedTemplateIds()
                    );

                    return SimpleNodeExecutionResult.success(
                            task.runId(),
                            task.nodeId(),
                            task.attempt(),
                            outputs,
                            task.token(),
                            Duration.ZERO);
                })
                .onFailure().recoverWithUni(throwable -> {
                    LOG.error("Prompt rendering failed", throwable);
                    return Uni.createFrom().item(SimpleNodeExecutionResult.failure(
                            task.runId(),
                            task.nodeId(),
                            task.attempt(),
                            new tech.kayys.gamelan.engine.error.ErrorInfo(
                                    "PROMPT_RENDER_FAILURE",
                                    "Prompt rendering failed: " + throwable.getMessage(),
                                    "PromptNodeExecutor",
                                    Map.of()),
                            task.token()));
                });
    }

    private List<Map<String, Object>> extractMessages(RenderedChain renderedChain) {
        return renderedChain.messages().stream()
                .map(msg -> Map.<String, Object>of(
                        "content", msg.content(),
                        "redactedContent", msg.redactedContent(),
                        "resolutionSources", msg.resolutionSources(),
                        "resolvedValues", msg.resolvedValues()))
                .toList();
    }
}
