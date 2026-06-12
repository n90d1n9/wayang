package tech.kayys.gamelan.hyperagent;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.role.AgentRole;
import tech.kayys.gamelan.communication.AgentMessageBus;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.hyperagent.role.*;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HyperagentLayerTest {

    @Mock GollekSdk       sdk;
    @Mock GamelanConfig   config;

    @BeforeEach
    void setUp() throws SdkException {
        when(config.defaultModel()).thenReturn("test-model");
        when(config.temperature()).thenReturn(0.7);
        when(config.maxTokens()).thenReturn(2048);

        InferenceResponse defaultResp = mock(InferenceResponse.class);
        when(defaultResp.getContent()).thenReturn("Task complete.");
        when(sdk.createCompletion(any(InferenceRequest.class))).thenReturn(defaultResp);
    }

    // ── AgentRole ─────────────────────────────────────────────────────────

    @Test
    void allRolesHaveNonBlankPersona() {
        for (AgentRole role : AgentRole.values()) {
            assertThat(role.persona()).isNotBlank()
                    .as("Role " + role + " must have a persona");
        }
    }

    @Test
    void allRolesHaveAtLeastOneAllowedTool() {
        for (AgentRole role : AgentRole.values()) {
            assertThat(role.allowedTools()).isNotEmpty()
                    .as("Role " + role + " must have allowed tools");
        }
    }

    @Test
    void allRolesHaveComplementaryRoles() {
        for (AgentRole role : AgentRole.values()) {
            assertThat(role.complementaryRoles()).isNotEmpty()
                    .as("Role " + role + " must have complementary roles");
        }
    }

    @Test
    void complementaryRolesAreValidEnumValues() {
        for (AgentRole role : AgentRole.values()) {
            role.complementaryRoles().forEach(cr ->
                    assertThatCode(() -> AgentRole.valueOf(cr))
                            .as("Complementary role '" + cr + "' must be valid for " + role)
                            .doesNotThrowAnyException());
        }
    }

    @Test
    void systemPromptBlockContainsRoleName() {
        for (AgentRole role : AgentRole.values()) {
            assertThat(role.toSystemPromptBlock()).contains(role.name());
        }
    }

    @Test
    void criticHasSearchTools() {
        assertThat(AgentRole.CRITIC.allowedTools()).contains("read_file", "search_files");
    }

    @Test
    void executorHasWriteTools() {
        assertThat(AgentRole.EXECUTOR.allowedTools()).contains("write_file", "apply_patch");
    }

    @Test
    void orchestratorCannotWriteFiles() {
        assertThat(AgentRole.ORCHESTRATOR.allowedTools()).doesNotContain("write_file");
    }

    @ParameterizedTest
    @EnumSource(AgentRole.class)
    void counterpartsReturnNonEmptyList(AgentRole role) {
        assertThat(role.counterparts()).isNotEmpty();
    }

    // ── RoleAgent ─────────────────────────────────────────────────────────

    @Test
    void roleAgentBuilderCreatesAgentWithCorrectRole() {
        RoleAgent agent = RoleAgent.builder("test-id", AgentRole.CRITIC)
                .sdk(sdk).config(config).name("critic-agent").build();

        assertThat(agent.agentId()).isEqualTo("test-id");
        assertThat(agent.role()).isEqualTo(AgentRole.CRITIC);
        assertThat(agent.name()).isEqualTo("critic-agent");
        assertThat(agent.state()).isEqualTo(RoleAgent.State.IDLE);
        assertThat(agent.turnCount()).isEqualTo(0);
    }

    @Test
    void roleAgentTurnProducesResult() throws SdkException {
        RoleAgent agent = RoleAgent.builder("a1", AgentRole.RESEARCHER)
                .sdk(sdk).config(config).build();

        RoleAgent.TurnResult result = agent.turn("Analyze UserService.java for security issues");

        assertThat(result.success()).isTrue();
        assertThat(result.agentId()).isEqualTo("a1");
        assertThat(result.role()).isEqualTo(AgentRole.RESEARCHER);
        assertThat(result.response()).isNotBlank();
        assertThat(result.elapsed().toMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void roleAgentBlocksForbiddenTools() throws SdkException {
        // RESEARCHER is not allowed to use write_file
        InferenceResponse withTool = mock(InferenceResponse.class);
        when(withTool.getContent()).thenReturn(
                "<tool_call><n>write_file</n><path>x.java</path><content>hack</content></tool_call>");
        // Second call returns final answer after forbidden tool
        InferenceResponse finalResp = mock(InferenceResponse.class);
        when(finalResp.getContent()).thenReturn("Analysis complete.");
        when(sdk.createCompletion(any())).thenReturn(withTool).thenReturn(finalResp);

        RoleAgent agent = RoleAgent.builder("a2", AgentRole.RESEARCHER)
                .sdk(sdk).config(config).build();
        RoleAgent.TurnResult result = agent.turn("read UserService.java");

        // The forbidden tool should be recorded but not executed
        assertThat(result.toolExecutions()).anyMatch(t ->
                t.toolName().equals("write_file") && t.forbidden());
    }

    @Test
    void roleAgentAllowsPermittedTools() throws SdkException {
        InferenceResponse withTool = mock(InferenceResponse.class);
        when(withTool.getContent()).thenReturn(
                "<tool_call><n>read_file</n><path>Main.java</path></tool_call>");
        InferenceResponse finalResp = mock(InferenceResponse.class);
        when(finalResp.getContent()).thenReturn("Analysis done.");
        when(sdk.createCompletion(any())).thenReturn(withTool).thenReturn(finalResp);

        RoleAgent agent = RoleAgent.builder("a3", AgentRole.RESEARCHER)
                .sdk(sdk).config(config)
                .stubTool("read_file", params -> "public class Main{}")
                .build();
        RoleAgent.TurnResult result = agent.turn("analyze Main.java");

        assertThat(result.toolExecutions()).anyMatch(t ->
                t.toolName().equals("read_file") && t.success());
    }

    @Test
    void roleAgentStubOverridesToolExecution() throws SdkException {
        InferenceResponse withTool = mock(InferenceResponse.class);
        when(withTool.getContent()).thenReturn(
                "<tool_call><n>search_files</n><pattern>TODO</pattern></tool_call>");
        InferenceResponse finalResp = mock(InferenceResponse.class);
        when(finalResp.getContent()).thenReturn("Found TODOs.");
        when(sdk.createCompletion(any())).thenReturn(withTool).thenReturn(finalResp);

        RoleAgent agent = RoleAgent.builder("a4", AgentRole.CRITIC)
                .sdk(sdk).config(config)
                .stubTool("search_files", params -> "Found 5 TODOs in src/")
                .build();
        RoleAgent.TurnResult result = agent.turn("find all TODOs");

        assertThat(result.toolExecutions()).anyMatch(t ->
                t.toolName().equals("search_files") && t.output().contains("5 TODOs"));
    }

    @Test
    void roleAgentTurnCountIncrementsPerTurn() throws SdkException {
        RoleAgent agent = RoleAgent.builder("a5", AgentRole.SYNTHESIZER)
                .sdk(sdk).config(config).build();

        agent.turn("first turn");
        agent.turn("second turn");

        assertThat(agent.turnCount()).isEqualTo(2);
    }

    @Test
    void roleAgentResetClearsState() throws SdkException {
        RoleAgent agent = RoleAgent.builder("a6", AgentRole.PLANNER)
                .sdk(sdk).config(config).build();

        agent.turn("some task");
        assertThat(agent.turnCount()).isEqualTo(1);
        assertThat(agent.lastResponse()).isPresent();

        agent.reset();

        assertThat(agent.turnCount()).isEqualTo(0);
        assertThat(agent.lastResponse()).isEmpty();
        assertThat(agent.state()).isEqualTo(RoleAgent.State.IDLE);
    }

    @Test
    void roleAgentHandlesLlmErrorGracefully() throws SdkException {
        when(sdk.createCompletion(any())).thenThrow(new SdkException("Network error"));

        RoleAgent agent = RoleAgent.builder("a7", AgentRole.VERIFIER)
                .sdk(sdk).config(config).build();
        RoleAgent.TurnResult result = agent.turn("verify the implementation");

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isNotBlank();
        assertThat(agent.state()).isEqualTo(RoleAgent.State.FAILED);
    }

    // ── AsymmetricRoleSession ─────────────────────────────────────────────

    @Test
    void sessionProducesResultWithBothAgents() {
        RoleAgent generator = RoleAgent.builder("gen", AgentRole.GENERATOR)
                .sdk(sdk).config(config).build();
        RoleAgent critic = RoleAgent.builder("crit", AgentRole.CRITIC)
                .sdk(sdk).config(config).build();

        AsymmetricRoleSession session = AsymmetricRoleSession.builder(generator, critic)
                .maxRounds(2).build();
        AsymmetricRoleSession.SessionResult result = session.run("implement a user login method");

        assertThat(result.agentA()).isSameAs(generator);
        assertThat(result.agentB()).isSameAs(critic);
        assertThat(result.rounds()).isNotEmpty();
        assertThat(result.elapsed().toMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void sessionConvergesWhenCriticSaysPass() throws SdkException {
        // First LLM call: generator produces initial code
        // Second: critic says PASS
        when(sdk.createCompletion(any()))
                .thenAnswer(inv -> {
                    InferenceResponse r = mock(InferenceResponse.class);
                    when(r.getContent()).thenReturn("PASS — no critical issues found.");
                    return r;
                });

        RoleAgent gen  = RoleAgent.builder("g", AgentRole.GENERATOR).sdk(sdk).config(config).build();
        RoleAgent crit = RoleAgent.builder("c", AgentRole.CRITIC).sdk(sdk).config(config).build();
        AsymmetricRoleSession session = AsymmetricRoleSession.builder(gen, crit)
                .maxRounds(5).build();

        AsymmetricRoleSession.SessionResult result = session.run("write a sort algorithm");
        assertThat(result.converged()).isTrue();
    }

    @Test
    void sessionRespectsMaxRounds() {
        // LLM never says PASS → should hit max rounds
        RoleAgent gen  = RoleAgent.builder("g2", AgentRole.GENERATOR).sdk(sdk).config(config).build();
        RoleAgent crit = RoleAgent.builder("c2", AgentRole.CRITIC).sdk(sdk).config(config).build();
        AsymmetricRoleSession session = AsymmetricRoleSession.builder(gen, crit)
                .maxRounds(2).build();

        AsymmetricRoleSession.SessionResult result = session.run("implement caching");
        assertThat(result.rounds().size()).isLessThanOrEqualTo(2);
    }

    @Test
    void sessionCallsOnRoundCallback() throws SdkException {
        java.util.concurrent.atomic.AtomicInteger callbackCount = new java.util.concurrent.atomic.AtomicInteger(0);

        RoleAgent gen  = RoleAgent.builder("g3", AgentRole.GENERATOR).sdk(sdk).config(config).build();
        RoleAgent crit = RoleAgent.builder("c3", AgentRole.CRITIC).sdk(sdk).config(config).build();
        AsymmetricRoleSession session = AsymmetricRoleSession.builder(gen, crit)
                .maxRounds(2)
                .onRound((agent, r) -> callbackCount.incrementAndGet())
                .build();

        session.run("task");
        assertThat(callbackCount.get()).isGreaterThan(0);
    }

    @Test
    void sessionSummaryIsNonBlank() {
        RoleAgent gen  = RoleAgent.builder("g4", AgentRole.GENERATOR).sdk(sdk).config(config).build();
        RoleAgent crit = RoleAgent.builder("c4", AgentRole.CRITIC).sdk(sdk).config(config).build();

        AsymmetricRoleSession.SessionResult result =
                AsymmetricRoleSession.builder(gen, crit).maxRounds(1).build()
                        .run("create a method");
        assertThat(result.summary()).isNotBlank();
    }
}
