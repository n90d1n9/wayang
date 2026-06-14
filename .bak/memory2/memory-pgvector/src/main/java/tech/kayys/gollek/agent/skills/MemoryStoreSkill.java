package tech.kayys.gollek.agent.skills;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gollek.agent.memory.AgentMemory;
import tech.kayys.gollek.agent.spi.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Allows agents to explicitly store and retrieve facts in persistent memory.
 *
 * <p>
 * This skill bridges the agent reasoning loop and the {@link AgentMemory}
 * backend, letting the LLM decide which facts are worth preserving beyond the
 * current run's working memory.
 * </p>
 *
 * <h2>Operations</h2>
 * <table border="1">
 * <tr>
 * <th>operation</th>
 * <th>Required inputs</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>store</td>
 * <td>key, value</td>
 * <td>Persist a key-value fact</td>
 * </tr>
 * <tr>
 * <td>recall</td>
 * <td>key</td>
 * <td>Retrieve a previously stored fact</td>
 * </tr>
 * <tr>
 * <td>forget</td>
 * <td>key</td>
 * <td>Delete a stored fact</td>
 * </tr>
 * <tr>
 * <td>store_working</td>
 * <td>key, value</td>
 * <td>Write to working (run-scoped) memory</td>
 * </tr>
 * <tr>
 * <td>recall_working</td>
 * <td>key</td>
 * <td>Read from working memory</td>
 * </tr>
 * </table>
 */
@ApplicationScoped
@SkillDescriptor(id = "memory_store", name = "Memory Store", description = "Stores, retrieves, and forgets facts in persistent or working agent memory.", version = "1.0.0", category = SkillCategory.MEMORY, inputs = {
                @SkillDescriptor.Input(name = "operation", description = "store | recall | forget | store_working | recall_working"),
                @SkillDescriptor.Input(name = "key", description = "Target fact identifier"),
                @SkillDescriptor.Input(name = "value", required = false, description = "Fact content (required for store operations)")
}, outputs = {
                @SkillDescriptor.Output(name = "observation", description = "Result of the memory operation")
}, triggers = { "remember", "store", "recall", "forget", "memory",
                "memorize" }, aliases = { "remember", "recall" }, priority = 65)
public class MemoryStoreSkill implements AgentSkill {

        @Inject
        AgentMemory memory;

        @Override
        public String id() {
                return "memory_store";
        }

        @Override
        public String name() {
                return "Memory Store";
        }

        @Override
        public String description() {
                return "Stores and retrieves facts in agent memory.";
        }

        @Override
        public String version() {
                return "1.0.0";
        }

        @Override
        public SkillCategory category() {
                return SkillCategory.MEMORY;
        }

        @Override
        public boolean canHandle(Map<String, Object> inputs) {
                return inputs.containsKey("operation") && inputs.containsKey("key");
        }

        @Override
        public Uni<SkillResult> execute(SkillContext ctx) {
                Instant start = Instant.now();
                String op = ctx.requireInput("operation", String.class);
                String key = ctx.requireInput("key", String.class);

                return switch (op.toLowerCase()) {
                        case "store" -> {
                                String value = ctx.requireInput("value", String.class);
                                String tenantId = ctx.tenantId() != null ? ctx.tenantId() : "default";
                                yield memory.storeFact(tenantId, key, value, null, Map.of("agent_run", ctx.runId()))
                                                .map(v -> ok(ctx, "Stored: " + key + " = " + value, start));
                        }
                        case "recall" -> {
                                String tenantId = ctx.tenantId() != null ? ctx.tenantId() : "default";
                                yield memory.searchFacts(tenantId, new float[0], 100)
                                                .map(facts -> facts.stream()
                                                                .filter(f -> f.key().equals(key)).findFirst())
                                                .map(opt -> opt.isPresent()
                                                                ? ok(ctx, opt.get().text(), start)
                                                                : ok(ctx, "No fact stored for key: " + key, start));
                        }
                        case "forget" -> {
                                String tenantId = ctx.tenantId() != null ? ctx.tenantId() : "default";
                                yield memory.deleteFact(tenantId, key)
                                                .map(v -> ok(ctx, "Forgotten: " + key, start));
                        }
                        case "store_working" -> {
                                String value = ctx.requireInput("value", String.class);
                                yield memory.setWorking(ctx.runId(), key, value)
                                                .map(v -> ok(ctx, "Stored in working memory: " + key, start));
                        }
                        case "recall_working" ->
                                memory.getWorking(ctx.runId(), key, String.class)
                                                .map(opt -> ok(ctx,
                                                                opt.map(v -> key + " = " + v).orElse(
                                                                                "Not found in working memory: " + key),
                                                                start));
                        default -> Uni.createFrom().item(
                                        SkillResult.builder()
                                                        .skillId(id())
                                                        .invocationId(ctx.invocationId())
                                                        .status(SkillResult.Status.FAILURE)
                                                        .observation("Unknown operation: " + op
                                                                        + ". Use store|recall|forget|store_working|recall_working")
                                                        .durationMs(Duration.between(start, Instant.now()).toMillis())
                                                        .build());
                };
        }

        private SkillResult ok(SkillContext ctx, String observation, Instant start) {
                return SkillResult.builder()
                                .skillId(id())
                                .invocationId(ctx.invocationId())
                                .status(SkillResult.Status.SUCCESS)
                                .observation(observation)
                                .output("observation", observation)
                                .durationMs(Duration.between(start, Instant.now()).toMillis())
                                .build();
        }
}
