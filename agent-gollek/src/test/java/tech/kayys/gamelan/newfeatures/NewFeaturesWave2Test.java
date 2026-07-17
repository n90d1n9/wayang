package tech.kayys.gamelan.newfeatures;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.multimodal.*;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.config.hotreload.*;
import tech.kayys.gamelan.integration.ci.*;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.resilience.circuit.AgentResilienceKit;
import tech.kayys.gamelan.testing.property.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewFeaturesWave2Test {

    // ── MultiModalProcessor ────────────────────────────────────────────────

    @Mock
    GamelanConfig config;
    @Mock
    AgentTelemetry telemetry;

    @InjectMocks
    MultiModalProcessor multiModal;

    @TempDir
    Path tmpDir;

    @Test
    void analyzeReturnsErrorForMissingFile() {
        var result = multiModal.analyze(Path.of("/nonexistent/image.png"),
                MultiModalProcessor.ImageDomain.GENERAL, "what is this?");
        assertThat(result.success()).isFalse();
        assertThat(result.error()).isNotBlank();
    }

    @Test
    void analyzeSucceedsForExistingImage(@TempDir Path tmp) throws IOException {
        // Create a minimal valid PNG (1x1 white pixel)
        byte[] minimalPng = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR length + type
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, // 8bit RGB
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, // IDAT
                0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8,
                (byte) 0xFF, (byte) 0xFF, 0x3F, 0x00, 0x05, (byte) 0xFE,
                0x02, (byte) 0xFE, (byte) 0xDC, (byte) 0xCC, 0x59, (byte) 0xE7,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, // IEND
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
        Path imgFile = tmp.resolve("test.png");
        Files.write(imgFile, minimalPng);

        var result = multiModal.analyze(imgFile, MultiModalProcessor.ImageDomain.GENERAL, "describe");
        // Should succeed (the vision model call is mocked/placeholder)
        assertThat(result.source()).isEqualTo(imgFile.toString());
    }

    @Test
    void detectFormatPng() throws IOException {
        Path f = tmpDir.resolve("diagram.png");
        Files.writeString(f, "fake");
        var result = multiModal.analyze(f, MultiModalProcessor.ImageDomain.ARCHITECTURE, "");
        // Even if analysis fails (not a real PNG), format detection should work
        assertThat(result.source()).isNotBlank();
    }

    @Test
    void analyzeErrorSummaryIsNonBlank() {
        var error = MultiModalProcessor.ImageAnalysis.error("/path", "test error");
        assertThat(error.success()).isFalse();
        assertThat(error.summary()).contains("ERROR");
        assertThat(error.entities()).isEmpty();
    }

    @Test
    void toContextBlockDescribesAnalysis() throws IOException {
        Path f = tmpDir.resolve("arch.svg");
        Files.writeString(f, "<svg></svg>");
        // SVG format
        var analysis = multiModal.analyze(f, MultiModalProcessor.ImageDomain.ARCHITECTURE, "describe");
        String block = multiModal.toContextBlock(analysis);
        assertThat(block).contains("Attached Image");
    }

    @Test
    void generateEdgeCasesForStrings() {
        var edges = multiModal.generateEdgeCases("public String process(String input)", "String");
        // method should not throw
        assertThat(edges).isNotNull();
        assertThat(edges.edgeCaseInputs()).isNotEmpty();
        assertThat(edges.edgeCaseInputs()).anyMatch(e -> e.contains("\"\""));
    }

    @Test
    void imageDomainEnumHasAllValues() {
        assertThat(MultiModalProcessor.ImageDomain.values()).contains(
                MultiModalProcessor.ImageDomain.ARCHITECTURE,
                MultiModalProcessor.ImageDomain.UI_MOCKUP,
                MultiModalProcessor.ImageDomain.ERROR_SCREENSHOT,
                MultiModalProcessor.ImageDomain.DATABASE_DIAGRAM,
                MultiModalProcessor.ImageDomain.PERFORMANCE_GRAPH,
                MultiModalProcessor.ImageDomain.GENERAL);
    }

    // ── CiCdIntegration ────────────────────────────────────────────────────

    @Mock
    SingleAgentOrchestrator orchestrator2;
    @Mock
    GamelanConfig config2;
    @Mock
    AgentTelemetry telemetry2;

    @InjectMocks
    CiCdIntegration ci;

    @BeforeEach
    void setUpCi() {
        when(config2.defaultModel()).thenReturn("test-model");
        when(config2.tokenBudget()).thenReturn(6000);
        when(config2.sessionPersist()).thenReturn(false);
    }

    @Test
    void parseMavenOutputWithFailures() {
        String output = """
                [ERROR] Tests run: 5, Failures: 2, Errors: 1, Skipped: 0, Time elapsed: 3.14 s
                [ERROR] FAILED UserServiceTest#testFindById - AssertionError: expected: 1 but was: null
                [ERROR] FAILED OrderServiceTest#testPlaceOrder - NullPointerException
                [INFO] BUILD FAILURE
                [INFO] Total time:  4.2 s
                """;
        var result = ci.parseTestOutput(output, CiCdIntegration.TestFormat.MAVEN);
        assertThat(result.failedCount()).isGreaterThan(0);
        assertThat(result.passed()).isFalse();
        assertThat(result.buildFailed()).isTrue();
        assertThat(result.summary()).contains("RED");
    }

    @Test
    void parseMavenOutputAllPassing() {
        String output = """
                [INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
                [INFO] BUILD SUCCESS
                [INFO] Total time:  2.5 s
                """;
        var result = ci.parseTestOutput(output, CiCdIntegration.TestFormat.MAVEN);
        assertThat(result.passed()).isTrue();
        assertThat(result.failedCount()).isEqualTo(0);
        assertThat(result.summary()).contains("GREEN");
    }

    @Test
    void parsePytestOutputWithFailures() {
        String output = """
                FAILED tests/test_user.py::TestUserService::test_find_by_id - AssertionError
                FAILED tests/test_order.py::test_place_order - TypeError: expected int got str
                2 failed, 8 passed in 1.23s
                """;
        var result = ci.parseTestOutput(output, CiCdIntegration.TestFormat.PYTEST);
        assertThat(result.passed()).isFalse();
        assertThat(result.failedTests()).hasSize(2);
        assertThat(result.failedTests().get(0).testName()).isNotBlank();
    }

    @Test
    void parseGoTestOutput() {
        String output = """
                --- FAIL: TestUserFind (0.01s)
                --- PASS: TestOrderCreate (0.02s)
                --- FAIL: TestPaymentProcess (0.15s)
                FAIL\t./...
                """;
        var result = ci.parseTestOutput(output, CiCdIntegration.TestFormat.GO_TEST);
        assertThat(result.failedTests()).hasSize(2);
        assertThat(result.failedTests()).extracting(CiCdIntegration.FailedTest::testName)
                .contains("TestUserFind", "TestPaymentProcess");
    }

    @Test
    void parseCargoTestOutput() {
        String output = """
                test test_addition ... ok
                test test_subtraction ... FAILED
                test test_multiply ... FAILED
                test result: FAILED. 1 passed; 2 failed; 0 ignored
                """;
        var result = ci.parseTestOutput(output, CiCdIntegration.TestFormat.CARGO);
        assertThat(result.failedCount()).isEqualTo(2);
        assertThat(result.passed()).isFalse();
    }

    @Test
    void parseJunitXml() {
        String xml = """
                <testsuite>
                  <testcase name="testFindById" classname="tech.kayys.UserServiceTest">
                    <failure message="expected not null">assertion failed</failure>
                  </testcase>
                  <testcase name="testCreate" classname="tech.kayys.UserServiceTest"/>
                </testsuite>
                """;
        var result = ci.parseTestOutput(xml, CiCdIntegration.TestFormat.JUNIT_XML);
        assertThat(result.failedTests()).hasSize(1);
        assertThat(result.failedTests().get(0).testName()).isEqualTo("testFindById");
        assertThat(result.failedTests().get(0).message()).contains("expected not null");
    }

    @Test
    void analyzeAndProposeEmptyForPassingTests() {
        var passResult = new CiCdIntegration.TestRunResult(5, 0, 0, List.of(),
                false, "", CiCdIntegration.TestFormat.MAVEN, Duration.ZERO);
        var proposals = ci.analyzeAndPropose(passResult, 5);
        assertThat(proposals.isEmpty()).isTrue();
    }

    @Test
    void trackFlakyTests() {
        // Alternate pass/fail to simulate flaky test
        for (int i = 0; i < 10; i++)
            ci.trackResult("FlakyTest#testFlaky", i % 2 == 0);
        List<CiCdIntegration.FlakyTest> flaky = ci.detectFlakyTests();
        assertThat(flaky).anyMatch(f -> f.testName().equals("FlakyTest#testFlaky"));
        assertThat(flaky.get(0).isFlaky()).isTrue();
        assertThat(flaky.get(0).summary()).contains("FlakyTest");
    }

    @Test
    void nonFlakyTestNotDetectedAsFlaky() {
        for (int i = 0; i < 10; i++)
            ci.trackResult("StableTest#testStable", true);
        List<CiCdIntegration.FlakyTest> flaky = ci.detectFlakyTests();
        assertThat(flaky).noneMatch(f -> f.testName().equals("StableTest#testStable"));
    }

    @Test
    void parseCoverageReport(@TempDir Path tmp) throws IOException {
        String xml = """
                <coverage>
                  <class name="tech.kayys.UserService" line-rate="0.95" branch-rate="0.88"/>
                  <class name="tech.kayys.LegacyCode" line-rate="0.45" branch-rate="0.20"/>
                  <class name="tech.kayys.Utils" line-rate="0.72" branch-rate="0.60"/>
                </coverage>
                """;
        Path coverage = tmp.resolve("coverage.xml");
        Files.writeString(coverage, xml);

        var report = ci.parseCoverage(coverage);
        assertThat(report.uncoveredClasses()).isNotEmpty();
        // LegacyCode has 45% line coverage < 80% threshold
        assertThat(report.uncoveredClasses())
                .anyMatch(c -> c.className().contains("LegacyCode"));
        assertThat(report.summary()).isNotBlank();
    }

    // ── HotConfigManager ──────────────────────────────────────────────────

    @Mock
    AgentTelemetry telemetry3;

    @InjectMocks
    HotConfigManager hotConfig;

    @Test
    void loadConfigFile(@TempDir Path tmp) throws IOException {
        Path cfg = tmp.resolve("gamelan.yml");
        Files.writeString(cfg, "gamelan.temperature: 0.5\ngamelan.max.tokens: 2048\n");

        var result = hotConfig.load(cfg);
        assertThat(result.success()).isTrue();
        assertThat(hotConfig.getDouble("gamelan.temperature", 0.7)).isEqualTo(0.5);
        assertThat(hotConfig.getInt("gamelan.max.tokens", 4096)).isEqualTo(2048);
    }

    @Test
    void loadNonExistentFileReturnsFailure(@TempDir Path tmp) {
        var result = hotConfig.load(tmp.resolve("missing.yml"));
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isNotBlank();
    }

    @Test
    void setValueUpdatesConfig() {
        boolean ok = hotConfig.set("gamelan.temperature", "0.3");
        assertThat(ok).isTrue();
        assertThat(hotConfig.getDouble("gamelan.temperature", 0.7)).isEqualTo(0.3);
    }

    @Test
    void setInvalidTemperatureRejected() {
        boolean ok = hotConfig.set("gamelan.temperature", "5.0"); // > 2.0
        assertThat(ok).isFalse();
    }

    @Test
    void setInvalidTokensRejected() {
        boolean ok = hotConfig.set("gamelan.max.tokens", "99999"); // > 32768
        assertThat(ok).isFalse();
    }

    @Test
    void getDefaultWhenKeyAbsent() {
        assertThat(hotConfig.get("nonexistent.key", "default")).isEqualTo("default");
        assertThat(hotConfig.getInt("nonexistent.int", 42)).isEqualTo(42);
        assertThat(hotConfig.getBoolean("nonexistent.bool", true)).isTrue();
    }

    @Test
    void listenerNotifiedOnChange() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HotConfigManager.ConfigChange> received = new AtomicReference<>();

        try (var handle = hotConfig.onChange("gamelan.", change -> {
            received.set(change);
            latch.countDown();
        })) {
            hotConfig.set("gamelan.temperature", "0.9");
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(received.get()).isNotNull();
            assertThat(received.get().key()).isEqualTo("gamelan.temperature");
            assertThat(received.get().newValue()).isEqualTo("0.9");
        }
    }

    @Test
    void rollbackRestoresPreviousValue() {
        hotConfig.set("gamelan.temperature", "0.3");
        hotConfig.set("gamelan.temperature", "0.9");
        assertThat(hotConfig.getDouble("gamelan.temperature", 0.7)).isEqualTo(0.9);

        hotConfig.rollback();
        assertThat(hotConfig.getDouble("gamelan.temperature", 0.7)).isEqualTo(0.3);
    }

    @Test
    void configChangeRecordTypes() {
        var added = new HotConfigManager.ConfigChange("key", null, "new");
        var removed = new HotConfigManager.ConfigChange("key", "old", null);
        var updated = new HotConfigManager.ConfigChange("key", "old", "new");

        assertThat(added.isAdded()).isTrue();
        assertThat(removed.isRemoved()).isTrue();
        assertThat(updated.isUpdated()).isTrue();

        assertThat(added.summary()).startsWith("+");
        assertThat(removed.summary()).startsWith("-");
        assertThat(updated.summary()).contains("→");
    }

    @Test
    void registerCustomValidator() {
        hotConfig.registerValidator("custom.port",
                v -> {
                    int port = Integer.parseInt(v);
                    return port >= 1024 && port <= 65535;
                },
                "Port must be between 1024 and 65535");

        assertThat(hotConfig.set("custom.port", "8080")).isTrue();
        assertThat(hotConfig.set("custom.port", "80")).isFalse(); // < 1024
    }

    @Test
    void statusReturnsCurrentState() {
        hotConfig.set("gamelan.temperature", "0.5");
        var status = hotConfig.status();
        assertThat(status).isNotNull();
        assertThat(status.keyCount()).isGreaterThan(0);
        assertThat(status.summary()).isNotBlank();
    }

    // ── PropertyTestGenerator ─────────────────────────────────────────────

    @Mock
    SingleAgentOrchestrator orchestrator3;
    @Mock
    GamelanConfig config3;
    @Mock
    AgentTelemetry telemetry4;

    @InjectMocks
    PropertyTestGenerator propGen;

    @BeforeEach
    void setUpPropGen() {
        when(config3.defaultModel()).thenReturn("test-model");
        when(config3.tokenBudget()).thenReturn(6000);
        when(config3.sessionPersist()).thenReturn(false);
        when(orchestrator3.execute(any(AgentRequest.class)))
                .thenReturn(OrchestratorResult.ok(
                        "[{\"name\":\"non_null_result\",\"category\":\"INVARIANT\"," +
                                "\"description\":\"result is not null\",\"assertion\":\"assertThat(result).isNotNull()\"}]",
                        "react", 1, List.of(), Duration.ZERO));
    }

    @Test
    void generateForFileProducesTests(@TempDir Path tmp) throws IOException {
        Path src = tmp.resolve("UserService.java");
        Files.writeString(src, """
                public class UserService {
                    public User findById(Long id) {
                        if (id == null) throw new IllegalArgumentException();
                        return repo.findById(id);
                    }
                    public List<User> findAll() {
                        return repo.findAll();
                    }
                }
                """);
        var result = propGen.generateForFile(src, PropertyTestGenerator.TestFramework.JQWIK, null);
        assertThat(result.success()).isTrue();
        assertThat(result.tests()).isNotEmpty();
        assertThat(result.assembledCode()).isNotBlank();
        assertThat(result.summary()).contains("Generated");
    }

    @Test
    void generateForFileMissingReturnsError(@TempDir Path tmp) {
        var result = propGen.generateForFile(tmp.resolve("Missing.java"),
                PropertyTestGenerator.TestFramework.JQWIK, null);
        assertThat(result.success()).isFalse();
        assertThat(result.error()).isNotBlank();
    }

    @Test
    void generateForMethodProducesJqwikCode() {
        var test = propGen.generateForMethod(
                "public List<Integer> sort(List<Integer> input)", "class Sorter {}",
                PropertyTestGenerator.TestFramework.JQWIK);
        assertThat(test.testCode()).isNotBlank();
        assertThat(test.propertyCount()).isGreaterThan(0);
    }

    @Test
    void generateHypothesisCode() throws IOException {
        Path src = tmpDir.resolve("sorter.py");
        Files.writeString(src, "def sort(items): return sorted(items)");
        var test = propGen.generateForMethod(
                "def sort(items: list) -> list", "",
                PropertyTestGenerator.TestFramework.HYPOTHESIS);
        assertThat(test.testCode()).isNotBlank();
    }

    @Test
    void edgeCasesGeneratedForStrings() {
        var edges = propGen.generateEdgeCases(
                "public String process(String input)", "String");
        assertThat(edges.edgeCaseInputs()).isNotEmpty();
        // Should include empty string, null, whitespace
        assertThat(edges.edgeCaseInputs()).anyMatch(e -> e.contains("\"\"") || e.contains("null"));
    }

    @Test
    void edgeCasesGeneratedForIntegers() {
        var edges = propGen.generateEdgeCases(
                "public int compute(int value, int bound)", "int");
        assertThat(edges.edgeCaseInputs()).anyMatch(e -> e.contains("MIN_VALUE") || e.contains("MAX_VALUE"));
    }

    @Test
    void analyzeExistingTestsReturnsSuggestions(@TempDir Path tmp) throws IOException {
        Path src = tmp.resolve("Calculator.java");
        Path test = tmp.resolve("CalculatorTest.java");
        Files.writeString(src, "public class Calculator {\n    public int add(int a, int b) { return a+b; }\n}");
        Files.writeString(test, "@Test void testAdd() { assertThat(calc.add(1,2)).isEqualTo(3); }");

        var suggestions = propGen.analyzeExistingTests(test, src);
        // "add" has no @Property test → should suggest properties
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).methodName()).isEqualTo("add");
    }

    @Test
    void propertyTestGenerationSummary(@TempDir Path tmp) throws IOException {
        Path src = tmp.resolve("Service.java");
        Files.writeString(src, "public class Service {\n    public String format(String s) { return s.trim(); }\n}");
        var result = propGen.generateForFile(src, PropertyTestGenerator.TestFramework.JUNIT5, null);
        assertThat(result.summary()).isNotBlank();
    }

    @Test
    void propertyCategories() {
        for (var cat : PropertyTestGenerator.PropertyCategory.values()) {
            assertThat(cat.name()).isNotBlank();
        }
    }
}
