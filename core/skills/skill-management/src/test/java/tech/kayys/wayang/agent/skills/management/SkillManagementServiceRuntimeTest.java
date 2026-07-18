package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementServiceRuntimeTest {

    @Test
    void assembleWiresServiceComponentsAroundProvidedStores() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        InMemorySkillArtifactStore artifacts = new InMemorySkillArtifactStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceRuntime runtime = runtime(
                definitions,
                states,
                artifacts,
                eventSink,
                eventSink);
        SkillDefinition skill = TestSkillDefinitions.namedCategory("planner", "Planner", "REASONING");
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");

        runtime.definitionMutationRunner().create(skill);
        runtime.lifecycleRunner().transition("planner", SkillLifecycleStatus.DISABLED);
        runtime.artifactMutationRunner().put(SkillArtifact.text(prompt, "hello"));

        assertThat(runtime.definitionStore()).isSameAs(definitions);
        assertThat(runtime.lifecycleStateStore()).isSameAs(states);
        assertThat(runtime.artifactStore()).isSameAs(artifacts);
        assertThat(runtime.eventSink()).isSameAs(eventSink);
        assertThat(runtime.definitionValidator().validate(skill).valid()).isTrue();
        assertThat(runtime.inspectionReader()).isNotNull();
        assertThat(runtime.operationTraceReader()).isNotNull();
        assertThat(runtime.artifactSyncWorkflow()).isNotNull();
        assertThat(runtime.maintenanceWorkflow()).isNotNull();
        assertThat(runtime.eventHistory().latest().events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.CREATE_SKILL,
                        SkillManagementEventOperation.TRANSITION_SKILL,
                        SkillManagementEventOperation.PUT_ARTIFACT);
        assertThat(runtime.eventHistory().supportsPruning()).isTrue();
        assertThat(runtime.catalogReader().get("planner")).contains(skill);
        assertThat(runtime.catalogReader().listActive()).isEmpty();
        assertThat(runtime.artifactReader().list("planner")).containsExactly(prompt);
        assertThat(runtime.artifactStore().getArtifact(prompt))
                .hasValueSatisfying(artifact -> assertThat(new String(
                        artifact.content(),
                        StandardCharsets.UTF_8)).isEqualTo("hello"));
        assertThat(runtime.inspectionReader().management().ready()).isTrue();
    }

    @Test
    void assembleNormalizesNullableEventHooks() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillManagementServiceRuntime runtime = runtime(
                definitions,
                new InMemorySkillLifecycleStateStore(),
                new InMemorySkillArtifactStore(),
                null,
                null);

        runtime.definitionMutationRunner().create(
                TestSkillDefinitions.namedCategory("planner", "Planner", "REASONING"));

        assertThat(runtime.eventSink()).isNotNull();
        assertThat(runtime.eventHistory().latest().events()).isEmpty();
        assertThat(runtime.eventHistory().supportsPruning()).isFalse();
        assertThat(runtime.catalogReader().list()).extracting(SkillDefinition::id)
                .containsExactly("planner");
    }

    @Test
    void assembleBindsOperationTraceReaderToRuntimeEventHistory() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        InMemorySkillManagementEventSink eventSink = new InMemorySkillManagementEventSink();
        SkillManagementServiceRuntime runtime = runtime(
                definitions,
                new InMemorySkillLifecycleStateStore(),
                new InMemorySkillArtifactStore(),
                eventSink,
                eventSink);

        runtime.definitionMutationRunner().create(
                TestSkillDefinitions.namedCategory("planner", "Planner", "REASONING"));

        SkillManagementEvent event = runtime.eventHistory().latest().events().get(0);
        String operationId = SkillManagementEventAttributeReader.from(event).operationId();
        SkillManagementAdminOperationTrace trace = runtime.operationTraceReader().trace(operationId, 10);

        assertThat(operationId).isNotBlank();
        assertThat(trace.operationId()).isEqualTo(operationId);
        assertThat(trace.rootEventAvailable()).isTrue();
        assertThat(trace.rootEvent().operation()).isEqualTo(SkillManagementEventOperation.CREATE_SKILL.name());
        assertThat(trace.childEvents()).isEmpty();
    }

    private SkillManagementServiceRuntime runtime(
            SkillDefinitionStore definitions,
            SkillLifecycleStateStore states,
            SkillArtifactStore artifacts,
            SkillManagementEventSink eventSink,
            SkillManagementEventReader eventReader) {
        return SkillManagementServiceRuntime.assemble(
                definitions,
                new SkillDefinitionStoreInspector(),
                states,
                new SkillLifecycleStateStoreInspector(),
                artifacts,
                eventSink,
                eventReader);
    }

}
