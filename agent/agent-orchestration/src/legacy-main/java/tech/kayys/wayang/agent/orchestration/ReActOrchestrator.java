package tech.kayys.wayang.agent.orchestration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import tech.kayys.wayang.agent.spi.*;

import tech.kayys.gollek.engine.inference.InferenceService;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.Message;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct (Reasoning + Acting) orchestrator.
 *
 * <p>
 * Implements the ReAct loop:
 * 
 * <pre>
 *   Thought: [reasoning about current situation]
 *   Action: [skill_id]
 *   Action Input: { "key": "value" }
 *   Observation: [result of action]
 *   ... repeat until ...
 *   Final Answer: [answer to the original question]
 * </pre>
 *
 * <p>
 * The orchestrator:
 * <ol>
 * <li>Builds a system prompt with skill descriptions and ReAct format
 * instructions</li>
 * <li>Calls the LLM to generate a thought + action</li>
 * <li>Parses the LLM output for skill invocation</li>
 * <li>Executes the skill and appends the observation</li>
 * <li>Repeats until "Final Answer:" is found or maxSteps is reached</li>
 * </ol>
 *
 * @author Bhangun
 */
@ApplicationScoped
public class ReActOrchestrator implements AgentOrchestrator {

    private static final Logger LOG = Logger.getLogger(ReActOrchestrator.class);

    // Regex patterns for parsing LLM ReAct output
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+?)(?=Action:|Final Answer:|$)",
            Pattern.DOTALL);
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(.+?)\\s*(?=Action Input:|$)",
            Pattern.DOTALL);
    private static final Pattern ACTION_IN_PATTERN = Pattern.compile("Action Input:\\s*(\\{.+?\\})", Pattern.DOTALL);
    private static final Pattern FINAL_ANS_PATTERN = Pattern.compile("Final Answer:\\s*(.+)$", Pattern.DOTALL);

    @Inject
    DefaultSkillRegistry skillRegistry;

    @Inject
    InferenceService inferenceService;

    @Override
    public String strategyId() {
        return "react";
    }

    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        LOG.infof("ReAct: starting run for tenant=%s, maxSteps=%d",
                request.tenantId(), request.getMaxSteps());

        AgentState initial = AgentState.initial(request);
        return loop(initial)
                .map(finalState -> buildResponse(finalState, request));
    }

    @Override
    public Multi<AgentEvent> stream(AgentRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            AgentState state = AgentState.initial(request);
            emitter.emit(AgentEvent.started(state.getRunId(), request.prompt()));

            executeStreaming(state, emitter);
        });
    }

    @Override
    public Uni<AgentState> step(AgentState state) {
        if (state.isTerminal())
            return Uni.createFrom().item(state);

        return generateThoughtAndAction(state)
                .chain(parsed -> {
                    if (parsed.isFinalAnswer()) {
                        return Uni.createFrom().item(state.withFinalAnswer(parsed.finalAnswer));
                    }
                    if (parsed.skillId == null) {
                        // No parseable action — treat LLM output as observation and continue
                        return Uni.createFrom().item(state.withObservation(
                                "Could not parse action. Raw output: " + parsed.rawOutput,
                                new AgentState.ReasoningStep(
                                        state.getStep(), parsed.thought, null,
                                        "Parse error", 0, false)));
                    }
                    return executeSkillAction(state, parsed);
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "ReAct step %d failed", state.getStep());
                    return state.withError("Step " + state.getStep() + " failed: " + err.getMessage());
                });
    }

    @Override
    public boolean isTerminal(AgentState state) {
        return state.isTerminal() || state.atMaxSteps();
    }

    @Override
    public String getSystemPromptFragment() {
        return REACT_SYSTEM_PROMPT;
    }

    // ── Core loop ──────────────────────────────────────────────────────────────

    private Uni<AgentState> loop(AgentState state) {
        if (isTerminal(state)) {
            if (state.atMaxSteps() && !state.isComplete()) {
                return Uni.createFrom().item(
                        state.withFinalAnswer("Max steps reached. Last observation: " +
                                state.getLastObservation().orElse("none")));
            }
            return Uni.createFrom().item(state);
        }
        return step(state).chain(next -> loop(next));
    }

    private void executeStreaming(AgentState state,
            io.smallrye.mutiny.subscription.MultiEmitter<? super AgentEvent> emitter) {
        if (isTerminal(state)) {
            if (state.isComplete()) {
                emitter.emit(AgentEvent.finalAnswer(state.getRunId(), state.getFinalAnswer().orElse("")));
            } else if (state.isFailed()) {
                emitter.emit(AgentEvent.error(state.getRunId(), state.getErrorMessage().orElse("Unknown")));
            }
            emitter.complete();
            return;
        }

        step(state).subscribe().with(
                next -> {
                    next.getLastThought().ifPresent(t -> emitter.emit(AgentEvent.thought(next.getRunId(), t)));
                    next.getPendingAction().ifPresent(a -> emitter.emit(AgentEvent.action(next.getRunId(), a)));
                    next.getLastObservation().ifPresent(o -> emitter.emit(AgentEvent.observation(next.getRunId(), o)));
                    executeStreaming(next, emitter);
                },
                err -> {
                    emitter.emit(AgentEvent.error(state.getRunId(), err.getMessage()));
                    emitter.complete();
                });
    }

    // ── LLM interaction ────────────────────────────────────────────────────────

    private Uni<ParsedOutput> generateThoughtAndAction(AgentState state) {
        String systemPrompt = buildSystemPrompt(state.getRequest());
        String userPrompt = buildUserPrompt(state);

        InferenceRequest llmReq = InferenceRequest.builder()
                .requestId("agent-" + state.getRunId() + "-" + state.getStep())
                .model(state.getRequest().modelId())
                .message(Message.system(systemPrompt))
                .message(Message.user(userPrompt))
                .parameter("max_tokens", 1024)
                .parameter("temperature", 0.1)
                .parameter("stop", List.of("Observation:", "\nObservation:"))
                .metadata("tenantId", state.getRequest().tenantId())
                .build();

        return inferenceService.inferAsync(llmReq)
                .map(resp -> parseOutput(resp.getContent()));
    }

    // ── Skill execution ────────────────────────────────────────────────────────

    private Uni<AgentState> executeSkillAction(AgentState state, ParsedOutput parsed) {
        long actionStart = System.currentTimeMillis();

        AgentSkill skill;
        try {
            skill = skillRegistry.findOrThrow(parsed.skillId);
        } catch (DefaultSkillRegistry.SkillNotFoundException e) {
            String obs = "Skill '" + parsed.skillId + "' not found. Available: " +
                    skillRegistry.listAll().stream().map(AgentSkill::id).toList();
            return Uni.createFrom().item(
                    state.withObservation(obs,
                            new AgentState.ReasoningStep(state.getStep(), parsed.thought,
                                    AgentState.AgentAction.of(parsed.skillId, parsed.inputs),
                                    obs, 0, false)));
        }

        AgentState.AgentAction action = AgentState.AgentAction.of(parsed.skillId, parsed.inputs);
        AgentState withAction = state.withAction(action);

        SkillContext ctx = SkillContext.builder()
                .skillId(parsed.skillId)
                .inputs(parsed.inputs)
                .agentContext(state.getRequest().context())
                .workingMemory(state.getWorkingMemory())
                .runId(state.getRunId())
                .stepNumber(state.getStep())
                .timeout(state.getRequest().getTimeout().dividedBy(state.getRequest().getMaxSteps()))
                .build();

        return skill.execute(ctx)
                .map(result -> {
                    long duration = System.currentTimeMillis() - actionStart;
                    AgentState.ReasoningStep step = new AgentState.ReasoningStep(
                            state.getStep(), parsed.thought, action,
                            result.getObservation(), duration, result.isSuccess());

                    AgentState nextState = withAction.withObservation(result.getObservation(), step);

                    // Apply memory updates from skill
                    if (result.hasMemoryUpdates()) {
                        for (Map.Entry<String, Object> e : result.getMemoryUpdates().entrySet()) {
                            nextState = nextState.withMemory(e.getKey(), e.getValue());
                        }
                    }
                    return nextState;
                })
                .onFailure().recoverWithItem(err -> {
                    String obs = "Error executing skill '" + parsed.skillId + "': " + err.getMessage();
                    return state.withObservation(obs,
                            new AgentState.ReasoningStep(state.getStep(), parsed.thought, action,
                                    obs, 0, false));
                });
    }

    // ── Prompt building ────────────────────────────────────────────────────────

    private String buildSystemPrompt(AgentRequest request) {
        StringBuilder sb = new StringBuilder(REACT_SYSTEM_PROMPT);
        sb.append("\n\n## Available Tools\n\n");
        List<AgentSkill> skills = request.hasSkillFilter()
                ? request.allowedSkills().stream().map(skillRegistry::find)
                        .filter(Optional::isPresent).map(Optional::get).toList()
                : skillRegistry.listAll();

        for (AgentSkill skill : skills) {
            sb.append("### ").append(skill.id()).append("\n");
            sb.append("Description: ").append(skill.description()).append("\n");
            sb.append("Category: ").append(skill.category()).append("\n\n");
        }
        java.util.Optional.ofNullable(request.systemPrompt()).ifPresent(sp -> sb.append("\n## Additional Instructions\n").append(sp));
        return sb.toString();
    }

    private String buildUserPrompt(AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(state.getRequest().prompt()).append("\n\n");

        for (AgentState.ReasoningStep step : state.getHistory()) {
            sb.append("Thought: ").append(step.thought()).append("\n");
            if (step.action() != null) {
                sb.append("Action: ").append(step.action().skillId()).append("\n");
                sb.append("Action Input: ").append(toJson(step.action().inputs())).append("\n");
            }
            sb.append("Observation: ").append(step.observation()).append("\n\n");
        }
        sb.append("Thought:");
        return sb.toString();
    }

    // ── Output parsing ─────────────────────────────────────────────────────────

    private ParsedOutput parseOutput(String raw) {
        ParsedOutput result = new ParsedOutput();
        result.rawOutput = raw;

        // Check for final answer first
        Matcher finalMatcher = FINAL_ANS_PATTERN.matcher(raw);
        if (finalMatcher.find()) {
            result.finalAnswer = finalMatcher.group(1).trim();
            return result;
        }

        // Extract thought
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(raw);
        if (thoughtMatcher.find())
            result.thought = thoughtMatcher.group(1).trim();

        // Extract action
        Matcher actionMatcher = ACTION_PATTERN.matcher(raw);
        if (actionMatcher.find())
            result.skillId = actionMatcher.group(1).trim();

        // Extract action input
        Matcher inputMatcher = ACTION_IN_PATTERN.matcher(raw);
        if (inputMatcher.find()) {
            try {
                result.inputs = parseJsonMap(inputMatcher.group(1));
            } catch (Exception e) {
                result.inputs = Map.of("input", inputMatcher.group(1));
            }
        }
        return result;
    }

    private Map<String, Object> parseJsonMap(String json) {
        // Simple JSON object parser (production: use Jackson)
        Map<String, Object> map = new LinkedHashMap<>();
        String inner = json.trim().replaceAll("^\\{|\\}$", "");
        for (String pair : inner.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("\"", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> sb.append("\"").append(k).append("\": \"").append(v).append("\", "));
        if (sb.length() > 1)
            sb.setLength(sb.length() - 2);
        sb.append("}");
        return sb.toString();
    }

    // ── Response building ──────────────────────────────────────────────────────

    private AgentResponse buildResponse(AgentState state, AgentRequest request) {
        return AgentResponse.builder()
                .runId(state.getRunId())
                .requestId(request.requestId())
                .answer(state.getFinalAnswer().orElse(""))
                .steps(state.getHistory())
                .totalSteps(state.getStep())
                .successful(!state.isFailed())
                .error(state.getErrorMessage().orElse(null))
                .strategy(strategyId())
                .build();
    }

    // ── Nested types ───────────────────────────────────────────────────────────

    private static class ParsedOutput {
        String rawOutput;
        String thought;
        String skillId;
        Map<String, Object> inputs = new HashMap<>();
        String finalAnswer;

        boolean isFinalAnswer() {
            return finalAnswer != null;
        }
    }

    // ── System prompt ──────────────────────────────────────────────────────────

    private static final String REACT_SYSTEM_PROMPT = """
            You are an intelligent agent that solves problems step-by-step using available tools.

            Use this format EXACTLY:

            Thought: [Your reasoning about what to do next]
            Action: [exact_tool_id]
            Action Input: {"key": "value"}
            Observation: [result will be filled in]

            Repeat Thought/Action/Action Input until you have enough information, then:

            Final Answer: [Your complete answer to the original question]

            Rules:
            - Always start with a Thought
            - Action must be exactly one of the available tool IDs
            - Action Input must be valid JSON
            - Never make up observations — wait for actual results
            - When you have enough information, use Final Answer
            """;
}
