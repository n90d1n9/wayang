package tech.kayys.gamelan.transfer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.EpisodicMemory;
import tech.kayys.gamelan.memory.hierarchy.ProceduralMemory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrossDomainTransferEngineTest {

    @Mock GollekSdk         sdk;
    @Mock ProceduralMemory  procedural;
    @Mock EpisodicMemory    episodic;
    @Mock GamelanConfig     config;

    @InjectMocks CrossDomainTransferEngine engine;

    @BeforeEach
    void setUp() {
        when(config.defaultModel()).thenReturn("test-model");
    }

    @Test
    void transferReturnsEmptyWhenNoProcedures() {
        when(procedural.all()).thenReturn(List.of());
        assertThat(engine.transfer("fix null pointer", "python")).isEmpty();
    }

    @Test
    void transferSkipsSameDomain() {
        // A procedure that looks like it's from Java domain
        var javaProc = new ProceduralMemory.Procedure(1L, "fix-npe",
                "Java NullPointerException fix using Optional and null check",
                List.of("read_file", "apply_patch"),
                List.of("NullPointerException", "null pointer"),
                0.9, 5, Instant.now());
        when(procedural.all()).thenReturn(List.of(javaProc));
        when(procedural.findApplicable(anyString(), anyInt())).thenReturn(List.of(javaProc));

        // Same domain (java) → no transfer
        assertThat(engine.transfer("fix NullPointerException in UserService", "java")).isEmpty();
    }

    @Test
    void transferAdaptsJavaProcedureToPython() throws SdkException {
        var javaProc = new ProceduralMemory.Procedure(1L, "fix-npe",
                "Java NullPointerException: use Optional.ofNullable, add null check",
                List.of("read_file", "apply_patch"),
                List.of("NullPointerException", "null", "fix"),
                0.9, 5, Instant.now());
        when(procedural.all()).thenReturn(List.of(javaProc));
        when(procedural.findApplicable(anyString(), anyInt())).thenReturn(List.of(javaProc));

        InferenceResponse resp = mock(InferenceResponse.class);
        when(resp.getContent()).thenReturn(
                "Check for None before attribute access using hasattr() or 'if x is not None' pattern.");
        when(sdk.createCompletion(any(InferenceRequest.class))).thenReturn(resp);

        var result = engine.transfer("fix AttributeError in login.py", "python");

        assertThat(result).isPresent();
        assertThat(result.get().sourceDomain()).isEqualTo("java");
        assertThat(result.get().targetDomain()).isEqualTo("python");
        assertThat(result.get().adaptedStrategy()).isNotBlank();
    }

    @Test
    void transferReturnsEmptyWhenLlmSaysNotApplicable() throws SdkException {
        var proc = new ProceduralMemory.Procedure(1L, "java-build",
                "Maven build: mvn clean package -DskipTests",
                List.of("run_command"), List.of("maven", "build"),
                0.9, 3, Instant.now());
        when(procedural.all()).thenReturn(List.of(proc));
        when(procedural.findApplicable(anyString(), anyInt())).thenReturn(List.of(proc));

        InferenceResponse resp = mock(InferenceResponse.class);
        when(resp.getContent()).thenReturn("NOT_APPLICABLE");
        when(sdk.createCompletion(any())).thenReturn(resp);

        // A completely different domain (Kubernetes deployment → not related to Java build)
        assertThat(engine.transfer("deploy to Kubernetes cluster", "go")).isEmpty();
    }

    @Test
    void transferHandlesLlmFailureGracefully() throws SdkException {
        var proc = new ProceduralMemory.Procedure(1L, "test-proc",
                "Java: some procedure description here",
                List.of("read_file"), List.of("java"),
                0.9, 1, Instant.now());
        when(procedural.all()).thenReturn(List.of(proc));
        when(procedural.findApplicable(anyString(), anyInt())).thenReturn(List.of(proc));
        when(sdk.createCompletion(any())).thenThrow(new SdkException("Network error"));

        // Should not throw — graceful empty return
        assertThat(engine.transfer("do something in python", "python")).isEmpty();
    }

    @Test
    void transferContextToPromptBlockContainsBothDomains() throws SdkException {
        var proc = new ProceduralMemory.Procedure(1L, "java-fix",
                "Java NullPointerException fix",
                List.of("read_file"), List.of("null"),
                0.9, 2, Instant.now());
        when(procedural.all()).thenReturn(List.of(proc));
        when(procedural.findApplicable(anyString(), anyInt())).thenReturn(List.of(proc));

        InferenceResponse resp = mock(InferenceResponse.class);
        when(resp.getContent()).thenReturn("Use None check in Python");
        when(sdk.createCompletion(any())).thenReturn(resp);

        var result = engine.transfer("fix None error in Python", "python");
        if (result.isPresent()) {
            String block = result.get().toPromptBlock();
            assertThat(block).contains("java");
            assertThat(block).contains("python");
            assertThat(block).containsIgnoringCase("Adapted");
        }
    }

    @Test
    void statsAreInitiallyEmpty() {
        CrossDomainTransferEngine.TransferStats stats = engine.stats();
        assertThat(stats.totalTransfers()).isEqualTo(0);
        assertThat(stats.byRoute()).isEmpty();
        assertThat(stats.summary()).contains("No cross-domain transfers");
    }

    @Test
    void registerTransferMapDoesNotThrow() {
        assertThatCode(() -> engine.registerTransferMap(
                "kotlin", "data class",
                "python", "dataclass"))
                .doesNotThrowAnyException();
    }

    @Test
    void transferRecordHasAllFields() {
        var record = new CrossDomainTransferEngine.TransferRecord(
                "fix-npe", "java", "python",
                "Use Optional", "Use None check", Instant.now());
        assertThat(record.procedureName()).isEqualTo("fix-npe");
        assertThat(record.sourceDomain()).isEqualTo("java");
        assertThat(record.targetDomain()).isEqualTo("python");
        assertThat(record.originalStrategy()).isEqualTo("Use Optional");
        assertThat(record.adaptedStrategy()).isEqualTo("Use None check");
    }
}
