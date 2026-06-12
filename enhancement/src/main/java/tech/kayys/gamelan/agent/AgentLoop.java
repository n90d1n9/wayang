package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gollek.sdk.core.GollekSdk;

/**
 * Legacy entry point for the agent loop, now a thin adapter over
 * {@link SingleAgentOrchestrator}.
 *
 * <h2>Migration path</h2>
 * All callers have been migrated to use {@link tech.kayys.gamelan.agent.orchestration.OrchestratorSelector}
 * or {@link SingleAgentOrchestrator} directly. This class is retained for:
 * <ul>
 *   <li>Unit tests that mock it via {@code @InjectMocks}</li>
 *   <li>Any external integrations that reference it by name</li>
 * </ul>
 *
 * <h2>Why we don't delete it</h2>
 * {@link AgentLoopLoopGuardTest} tests behaviour that is logically part of
 * {@link SingleAgentOrchestrator} (loop guard, error propagation, cancellation).
 * Those tests will be migrated in a follow-up; until then this adapter keeps
 * compilation clean.
 *
 * <p><b>Do not add new callers of this class.</b> Use
 * {@link tech.kayys.gamelan.agent.orchestration.OrchestratorSelector} instead.
 *
 * @deprecated Use {@link SingleAgentOrchestrator} via {@link tech.kayys.gamelan.agent.orchestration.OrchestratorSelector}
 */
@Deprecated
@ApplicationScoped
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject SkillRegistry           skillRegistry;
    @Inject GollekSdk               sdk;
    @Inject GamelanConfig           config;

    /**
     * @deprecated Use {@link SingleAgentOrchestrator#execute(AgentRequest)} instead.
     */
    @Deprecated
    public AgentResponse process(String userMessage, ConversationSession session,
                                  String model, boolean streamToStdout) {
        log.debug("[AgentLoop.process] delegating to SingleAgentOrchestrator");
        AgentRequest req = AgentRequest.builder(userMessage)
                .model(model)
                .session(session)
                .stream(streamToStdout)
                .maxSteps(10)
                .build();

        OrchestratorResult result = orchestrator.execute(req);

        return AgentResponse.builder()
                .text(result.answer())
                .toolResults(result.toolResults())
                .error(!result.success())
                .build();
    }

    /** @deprecated Use {@link SingleAgentOrchestrator#cancelCurrentThread()}. */
    @Deprecated
    public void cancelCurrentTask() { orchestrator.cancelCurrentThread(); }

    public SkillRegistry getSkillRegistry() { return skillRegistry; }
    public GollekSdk     getSdk()           { return sdk; }
}
