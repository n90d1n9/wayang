package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesDirectiveDispatcherTest {

    @Test
    void dispatchesActiveDirectivesToConfiguredPorts() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .trajectoryExportEnabled(true)
                .build();
        HermesRequestPlan plan = new HermesRequestPlanner(config).plan(AgentRequest.builder()
                .requestId("req-runtime")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .prompt("""
                        Run this Docker sandbox job every day.
                        Use OpenRouter high context.
                        Split into parallel research, implementation, verification, review, and docs tracks.
                        Always remember this important preference.
                        Save the execution trace for audit trail.
                        Show the learned skill catalog.
                        """)
                .build());
        List<String> calls = new ArrayList<>();
        HermesRuntimePorts ports = new HermesRuntimePorts(
                directive -> captured(calls, "execution", directive.operation(), directive.backend(), directive.toMetadata()),
                directive -> captured(calls, "gateway", directive.operation(), directive.destinationId(), directive.toMetadata()),
                directive -> captured(calls, "automation", directive.operation(), directive.taskId(), directive.toMetadata()),
                directive -> captured(calls, "delegation", directive.operation(), directive.groupId(), directive.toMetadata()),
                directive -> captured(calls, "provider-routing", directive.operation(), directive.selectedProvider(), directive.toMetadata()),
                directive -> captured(calls, "memory-reflection", directive.operation(), directive.subjectId(), directive.toMetadata()),
                directive -> captured(calls, "trajectory-export", directive.operation(), directive.exportId(), directive.toMetadata()),
                HermesSkillPersistencePort.noop(),
                HermesRuntimeJournalPort.noop(),
                directive -> captured(calls, "skill-lineage", directive.operation(), directive.target(), directive.toMetadata()));

        HermesDirectiveDispatchReport report = new HermesDirectiveDispatcher(ports).dispatch(plan);

        assertThat(report.successful()).isTrue();
        assertThat(report.outcome()).isEqualTo("healthy");
        assertThat(report.totalCount()).isEqualTo(8);
        assertThat(report.activeCount()).isEqualTo(8);
        assertThat(report.dispatchedCount()).isEqualTo(8);
        assertThat(report.skippedCount()).isZero();
        assertThat(report.unavailableCount()).isZero();
        assertThat(report.failedCount()).isZero();
        assertThat(report.unsuccessfulCount()).isZero();
        assertThat(report.attentionCount()).isZero();
        assertThat(report.attention()).isEmpty();
        assertThat(report.remediationPlan().required()).isFalse();
        assertThat(report.remediationPlan().actionCount()).isZero();
        assertThat(calls).containsExactly(
                "execution",
                "gateway",
                "automation",
                "delegation",
                "provider-routing",
                "memory-reflection",
                "trajectory-export",
                "skill-lineage");
        assertThat(report.runtimePorts())
                .containsEntry("configuredCount", 8L)
                .containsEntry("readyCount", 11L)
                .containsEntry("noopCount", 3L);
        assertThat(report.toMetadata()).containsEntry("dispatchedCount", 8L);
        assertThat(report.toMetadata()).containsEntry("outcome", "healthy");
        assertThat(report.toMetadata()).containsEntry("attentionCount", 0);
        assertThat(report.toMetadata()).containsEntry("remediationPlan", report.remediationPlan().toMetadata());
        assertThat(report.toMetadata()).containsEntry("runtimePorts", report.runtimePorts());
        assertThat(report.toMetadata()).containsEntry("summary", report.summary().toMetadata());
        assertThat(report.summary().outcome()).isEqualTo("healthy");
        assertThat(report.summary().attention()).isEmpty();
        assertThat(report.summary().remediationPlan().required()).isFalse();
        assertThat(report.summary().statusCounts()).containsEntry("captured", 8L);
    }

    @Test
    void skipsInactiveDirectivesWithoutInvokingPorts() {
        HermesRequestPlan plan = new HermesRequestPlanner(HermesAgentModeConfig.defaults()).defaultPlan();

        HermesDirectiveDispatchReport report = new HermesDirectiveDispatcher(throwingPorts()).dispatch(plan);

        assertThat(report.successful()).isTrue();
        assertThat(report.outcome()).isEqualTo("idle");
        assertThat(report.totalCount()).isEqualTo(8);
        assertThat(report.activeCount()).isZero();
        assertThat(report.dispatchedCount()).isZero();
        assertThat(report.skippedCount()).isEqualTo(8);
        assertThat(report.unavailableCount()).isZero();
        assertThat(report.failedCount()).isZero();
        assertThat(report.attentionCount()).isZero();
        assertThat(report.attention()).isEmpty();
        assertThat(report.remediationPlan().strategy()).isEqualTo("none");
        assertThat(report.remediationPlan().actions()).isEmpty();
        assertThat(report.results())
                .extracting(HermesPortDispatchResult::status)
                .containsOnly("skipped");
        assertThat(report.summary().outcome()).isEqualTo("idle");
        assertThat(report.summary().attention()).isEmpty();
        assertThat(report.summary().remediationPlan().required()).isFalse();
        assertThat(report.summary().statusCounts()).containsEntry("skipped", 8L);
    }

    @Test
    void reportsRuntimePortFailures() {
        HermesRequestPlan plan = new HermesRequestPlanner(HermesAgentModeConfig.defaults()).plan(AgentRequest.builder()
                .requestId("req-failure")
                .prompt("Run this local task")
                .build());
        HermesRuntimePorts ports = new HermesRuntimePorts(
                directive -> {
                    throw new IllegalStateException("execution adapter unavailable");
                },
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        HermesDirectiveDispatchReport report = new HermesDirectiveDispatcher(ports).dispatch(plan);

        assertThat(report.successful()).isFalse();
        assertThat(report.outcome()).isEqualTo("degraded");
        HermesPortDispatchResult execution = report.results().stream()
                .filter(result -> "execution".equals(result.port()))
                .findFirst()
                .orElseThrow();
        assertThat(execution.status()).isEqualTo("failed");
        assertThat(execution.metadata()).containsEntry("error", "execution adapter unavailable");
        assertThat(report.activeCount()).isEqualTo(3);
        assertThat(report.dispatchedCount()).isEqualTo(3);
        assertThat(report.skippedCount()).isEqualTo(5);
        assertThat(report.failedCount()).isEqualTo(1);
        assertThat(report.unavailableCount()).isZero();
        assertThat(report.unsuccessfulCount()).isEqualTo(1);
        assertThat(report.attentionCount()).isEqualTo(1);
        assertThat(report.attention())
                .extracting(HermesDirectiveDispatchAttention::port)
                .containsExactly("execution");
        HermesDirectiveDispatchAttention attention = report.attention().getFirst();
        assertThat(attention.status()).isEqualTo("failed");
        assertThat(attention.severity()).isEqualTo("warning");
        assertThat(attention.recommendedAction()).isEqualTo("inspect-runtime-adapter");
        assertThat(attention.retryable()).isTrue();
        assertThat(attention.metadata())
                .containsEntry("error", "execution adapter unavailable");
        assertThat(attention.toMetadata())
                .containsEntry("recommendedAction", "inspect-runtime-adapter")
                .containsEntry("retryable", true);
        HermesRemediationPlan remediationPlan = report.remediationPlan();
        assertThat(remediationPlan.required()).isTrue();
        assertThat(remediationPlan.strategy()).isEqualTo("retry-runtime-adapter");
        assertThat(remediationPlan.actionCount()).isEqualTo(1);
        assertThat(remediationPlan.criticalCount()).isZero();
        assertThat(remediationPlan.retryableCount()).isEqualTo(1);
        assertThat(remediationPlan.actions().getFirst().action()).isEqualTo("inspect-runtime-adapter");
        assertThat(report.toMetadata()).containsEntry("attentionCount", 1);
        assertThat(report.toMetadata()).containsEntry("remediationPlan", remediationPlan.toMetadata());
        assertThat(report.summary().outcome()).isEqualTo("degraded");
        assertThat(report.summary().statusCounts())
                .containsEntry("failed", 1L)
                .containsEntry("noop", 2L)
                .containsEntry("skipped", 5L);
    }

    @Test
    void reportsUnavailableRuntimePortWithoutInvokingAdapter() {
        HermesRequestPlan plan = new HermesRequestPlanner(HermesAgentModeConfig.defaults()).plan(AgentRequest.builder()
                .requestId("req-unavailable")
                .prompt("Run this local task")
                .build());
        HermesRuntimePortDescriptor unavailableExecution = new HermesRuntimePortDescriptor(
                "execution",
                "test-execution",
                "test-adapter",
                true,
                false,
                false,
                "unavailable",
                "docker daemon offline",
                Map.of("backend", "docker"));
        HermesExecutionPort executionPort = new HermesExecutionPort() {
            @Override
            public HermesPortDispatchResult dispatch(HermesExecutionDirective directive) {
                throw new AssertionError("unavailable adapter should not be invoked");
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return unavailableExecution;
            }
        };
        HermesRuntimePorts ports = new HermesRuntimePorts(
                executionPort,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        HermesDirectiveDispatchReport report = new HermesDirectiveDispatcher(ports).dispatch(plan);

        assertThat(report.successful()).isFalse();
        assertThat(report.outcome()).isEqualTo("degraded");
        assertThat(report.activeCount()).isEqualTo(3);
        assertThat(report.dispatchedCount()).isEqualTo(2);
        assertThat(report.skippedCount()).isEqualTo(5);
        assertThat(report.unavailableCount()).isEqualTo(1);
        assertThat(report.failedCount()).isZero();
        assertThat(report.unsuccessfulCount()).isEqualTo(1);
        assertThat(report.attentionCount()).isEqualTo(1);
        HermesPortDispatchResult execution = report.results().stream()
                .filter(result -> "execution".equals(result.port()))
                .findFirst()
                .orElseThrow();
        assertThat(execution.status()).isEqualTo("unavailable");
        assertThat(execution.dispatched()).isFalse();
        assertThat(execution.reason()).isEqualTo("docker daemon offline");
        assertThat(execution.metadata())
                .containsEntry("runtimePort", unavailableExecution.toMetadata());
        HermesDirectiveDispatchAttention attention = report.attention().getFirst();
        assertThat(attention.port()).isEqualTo("execution");
        assertThat(attention.status()).isEqualTo("unavailable");
        assertThat(attention.reason()).isEqualTo("docker daemon offline");
        assertThat(attention.severity()).isEqualTo("critical");
        assertThat(attention.recommendedAction()).isEqualTo("restore-runtime-port");
        assertThat(attention.retryable()).isFalse();
        assertThat(attention.metadata())
                .containsEntry("runtimePort", unavailableExecution.toMetadata());
        HermesRemediationPlan remediationPlan = report.remediationPlan();
        assertThat(remediationPlan.required()).isTrue();
        assertThat(remediationPlan.strategy()).isEqualTo("restore-runtime-port");
        assertThat(remediationPlan.actionCount()).isEqualTo(1);
        assertThat(remediationPlan.criticalCount()).isEqualTo(1);
        assertThat(remediationPlan.retryableCount()).isZero();
        assertThat(remediationPlan.actions().getFirst().port()).isEqualTo("execution");
        assertThat(report.summary().outcome()).isEqualTo("degraded");
        assertThat(report.summary().statusCounts())
                .containsEntry("unavailable", 1L)
                .containsEntry("noop", 2L)
                .containsEntry("skipped", 5L);
    }

    @Test
    void summarizesBlockedDispatchWhenEveryActiveDirectiveIsUnsuccessful() {
        HermesDirectiveDispatchReport report = new HermesDirectiveDispatchReport(List.of(
                HermesPortDispatchResult.unavailable(
                        "execution",
                        "dispatch",
                        "docker",
                        "docker daemon offline",
                        Map.of()),
                HermesPortDispatchResult.skipped(
                        "gateway",
                        "none",
                        "",
                        "gateway disabled",
                        Map.of())));

        assertThat(report.successful()).isFalse();
        assertThat(report.outcome()).isEqualTo("blocked");
        assertThat(report.activeCount()).isEqualTo(1);
        assertThat(report.dispatchedCount()).isZero();
        assertThat(report.skippedCount()).isEqualTo(1);
        assertThat(report.unavailableCount()).isEqualTo(1);
        assertThat(report.unsuccessfulCount()).isEqualTo(1);
        assertThat(report.attentionCount()).isEqualTo(1);
        assertThat(report.attention().getFirst().status()).isEqualTo("unavailable");
        assertThat(report.remediationPlan().strategy()).isEqualTo("restore-runtime-port");
        assertThat(report.remediationPlan().criticalCount()).isEqualTo(1);
        assertThat(report.toMetadata())
                .containsEntry("outcome", "blocked")
                .containsEntry("attentionCount", 1)
                .containsEntry("remediationPlan", report.remediationPlan().toMetadata())
                .containsEntry("summary", report.summary().toMetadata());
    }

    @Test
    void recommendsConfigurationForMissingRuntimePort() {
        HermesPortDispatchResult result = HermesPortDispatchResult.unavailable(
                "gateway",
                "deliver",
                "telegram",
                "gateway adapter missing",
                Map.of("runtimePort", HermesRuntimePortDescriptor.noop("gateway").toMetadata()));

        HermesDirectiveDispatchAttention attention = HermesDirectiveDispatchAttention.from(result);

        assertThat(attention.severity()).isEqualTo("critical");
        assertThat(attention.recommendedAction()).isEqualTo("configure-runtime-port");
        assertThat(attention.retryable()).isFalse();
        assertThat(attention.toMetadata())
                .containsEntry("severity", "critical")
                .containsEntry("recommendedAction", "configure-runtime-port")
                .containsEntry("retryable", false);
        HermesRemediationPlan plan = HermesRemediationPlan.from(List.of(attention));
        assertThat(plan.strategy()).isEqualTo("configure-runtime-port");
        assertThat(plan.actionCount()).isEqualTo(1);
        assertThat(plan.criticalCount()).isEqualTo(1);
        assertThat(plan.retryableCount()).isZero();
        assertThat(plan.actions().getFirst().action()).isEqualTo("configure-runtime-port");
        assertThat(plan.actions().getFirst().toMetadata())
                .containsEntry("action", "configure-runtime-port");
    }

    @Test
    void dispatchesSkillPersistencePlanThroughDedicatedPort() {
        HermesSkillPersistencePlan plan = HermesSkillPersistencePlan.from(
                HermesAgentModeConfig.defaults().skillPersistenceStrategy());
        List<String> calls = new ArrayList<>();
        HermesRuntimePorts ports = new HermesRuntimePorts(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                persistencePlan -> captured(
                        calls,
                        "skill-persistence",
                        "configure",
                        persistencePlan.routes().getFirst().store(),
                        persistencePlan.toMetadata()));

        HermesPortDispatchResult result = new HermesDirectiveDispatcher(ports).dispatchSkillPersistence(plan);

        assertThat(result.successful()).isTrue();
        assertThat(result.dispatched()).isTrue();
        assertThat(calls).containsExactly("skill-persistence");
    }

    @Test
    void dispatchesRuntimeJournalDirectiveThroughDedicatedPort() {
        HermesRuntimeJournalDirective directive = HermesRuntimeJournalDirective.inspect(
                HermesRuntimeEventQuery.forSession("session-a", 5));
        List<String> calls = new ArrayList<>();
        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .runtimeJournalPort(journalDirective -> captured(
                        calls,
                        "runtime-journal",
                        journalDirective.operation(),
                        journalDirective.target(),
                        journalDirective.toMetadata()))
                .build();

        HermesPortDispatchResult result = new HermesDirectiveDispatcher(ports).dispatchRuntimeJournal(directive);

        assertThat(result.successful()).isTrue();
        assertThat(result.dispatched()).isTrue();
        assertThat(result.operation()).isEqualTo("inspect");
        assertThat(result.target()).isEqualTo("session:session-a");
        assertThat(result.metadata()).containsKeys("query", "runtimePort");
        assertThat(calls).containsExactly("runtime-journal");
    }

    @Test
    void dispatchesLearningAuditDirectiveThroughDedicatedPort() {
        HermesLearningAuditDirective directive = HermesLearningAuditDirective.skill("hermes-audit", 5);
        List<String> calls = new ArrayList<>();
        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .learningAuditPort(auditDirective -> captured(
                        calls,
                        "learning-audit",
                        auditDirective.operation(),
                        auditDirective.target(),
                        auditDirective.toMetadata()))
                .build();

        HermesPortDispatchResult result = new HermesDirectiveDispatcher(ports).dispatchLearningAudit(directive);

        assertThat(result.successful()).isTrue();
        assertThat(result.dispatched()).isTrue();
        assertThat(result.operation()).isEqualTo("inspect");
        assertThat(result.target()).isEqualTo("skill:hermes-audit");
        assertThat(result.metadata()).containsKeys("query", "runtimePort");
        assertThat(calls).containsExactly("learning-audit");
    }

    @Test
    void skipsInactiveLearningAuditDirective() {
        HermesPortDispatchResult result = new HermesDirectiveDispatcher(throwingPorts())
                .dispatchLearningAudit(HermesLearningAuditDirective.none());

        assertThat(result.dispatched()).isFalse();
        assertThat(result.status()).isEqualTo("skipped");
        assertThat(result.port()).isEqualTo("learning-audit");
    }

    @Test
    void dispatchesRuntimeDiagnosticsDirectiveThroughDedicatedPort() {
        HermesRuntimeDiagnosticsDirective directive = HermesRuntimeDiagnosticsDirective.runtimePorts();
        List<String> calls = new ArrayList<>();
        HermesRuntimeDiagnosticsPort diagnosticsPort = diagnosticsDirective -> captured(
                calls,
                "runtime-diagnostics",
                diagnosticsDirective.operation(),
                diagnosticsDirective.target(),
                diagnosticsDirective.toMetadata());

        HermesPortDispatchResult result = new HermesDirectiveDispatcher(
                HermesRuntimePorts.noop(),
                diagnosticsPort)
                .dispatchRuntimeDiagnostics(directive);

        assertThat(result.successful()).isTrue();
        assertThat(result.dispatched()).isTrue();
        assertThat(result.operation()).isEqualTo("inspect");
        assertThat(result.target()).isEqualTo("runtime-diagnostics:runtime-ports");
        assertThat(result.metadata()).containsKeys("view", "runtimePort");
        assertThat(calls).containsExactly("runtime-diagnostics");
    }

    @Test
    void dispatchesSkillLineageDirectiveThroughDedicatedPort() {
        HermesSkillLineageDirective directive = HermesSkillLineageDirective.inspect("hermes-audit");
        List<String> calls = new ArrayList<>();
        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .skillLineagePort(lineageDirective -> captured(
                        calls,
                        "skill-lineage",
                        lineageDirective.operation(),
                        lineageDirective.target(),
                        lineageDirective.toMetadata()))
                .build();

        HermesPortDispatchResult result = new HermesDirectiveDispatcher(ports).dispatchSkillLineage(directive);

        assertThat(result.successful()).isTrue();
        assertThat(result.dispatched()).isTrue();
        assertThat(result.operation()).isEqualTo("inspect");
        assertThat(result.target()).isEqualTo("skill:hermes-audit");
        assertThat(result.metadata()).containsKeys("skillId", "runtimePort");
        assertThat(calls).containsExactly("skill-lineage");
    }

    private static HermesPortDispatchResult captured(
            List<String> calls,
            String port,
            String operation,
            String target,
            Map<String, Object> metadata) {
        calls.add(port);
        return new HermesPortDispatchResult(
                port,
                operation,
                target,
                true,
                true,
                true,
                "captured",
                "captured by test",
                metadata);
    }

    private static HermesRuntimePorts throwingPorts() {
        return new HermesRuntimePorts(
                directive -> failInvocation(),
                directive -> failInvocation(),
                directive -> failInvocation(),
                directive -> failInvocation(),
                directive -> failInvocation(),
                directive -> failInvocation(),
                directive -> failInvocation(),
                plan -> failInvocation(),
                directive -> failInvocation(),
                directive -> failInvocation());
    }

    private static HermesPortDispatchResult failInvocation() {
        throw new AssertionError("inactive directive should not invoke runtime port");
    }
}
