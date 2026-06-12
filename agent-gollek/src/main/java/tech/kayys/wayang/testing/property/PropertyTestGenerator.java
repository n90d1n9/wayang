package tech.kayys.gamelan.testing.property;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * PropertyTestGenerator — automatically generates property-based tests from code analysis.
 *
 * <h2>Property-based vs example-based testing</h2>
 * <pre>
 * Example-based test:
 *   assert sort([3,1,2]) == [1,2,3]
 *
 * Property-based test:
 *   for all lists L:
 *     let S = sort(L)
 *     assert len(S) == len(L)           // length preserved
 *     assert all elements in S          // elements preserved
 *     assert S[i] <= S[i+1] for all i  // sorted order
 *     assert sort(sort(L)) == sort(L)  // idempotent
 * </pre>
 * Properties catch entire categories of bugs instead of individual cases.
 *
 * <h2>Generation strategy</h2>
 * <ol>
 *   <li>Parse the source method's signature and JavaDoc</li>
 *   <li>Identify the method's input domain (type constraints)</li>
 *   <li>LLM proposes mathematical properties the method should satisfy</li>
 *   <li>Generate QuickCheck/Jqwik/Hypothesis test code implementing those properties</li>
 *   <li>Generate adversarial edge cases (empty, null, max, overflow, special chars)</li>
 * </ol>
 *
 * <h2>Supported frameworks</h2>
 * <ul>
 *   <li>Java: Jqwik ({@code @Property} annotation)</li>
 *   <li>Java: JUnit 5 with {@code @ParameterizedTest + @MethodSource}</li>
 *   <li>Python: Hypothesis ({@code @given} decorator)</li>
 *   <li>Kotlin: Kotest property testing</li>
 * </ul>
 *
 * <h2>Property categories</h2>
 * <ul>
 *   <li>INVARIANT: a condition that always holds (sorted list is always sorted)</li>
 *   <li>IDEMPOTENT: applying twice == applying once (parse(format(x)) == x)</li>
 *   <li>ROUNDTRIP: encode → decode yields original (serialize → deserialize)</li>
 *   <li>METAMORPHIC: output relationship under input transformation (scale input → scale output)</li>
 *   <li>ORACLE: comparison against a reference implementation</li>
 * </ul>
 */
@ApplicationScoped
public class PropertyTestGenerator {

    private static final Logger log = LoggerFactory.getLogger(PropertyTestGenerator.class);

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig            config;
    @Inject AgentTelemetry           telemetry;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Generates property-based tests for all methods in a source file.
     *
     * @param sourceFile   the source file to generate tests for
     * @param framework    the test framework to target
     * @param outputFile   where to write the generated test file (null = return as string)
     * @return the generation result
     */
    public GenerationResult generateForFile(Path sourceFile, TestFramework framework,
                                             Path outputFile) {
        if (!Files.exists(sourceFile)) {
            return GenerationResult.error("File not found: " + sourceFile);
        }

        log.info("[prop-test] generating tests for {} ({})", sourceFile.getFileName(), framework);
        telemetry.count("proptest.generate.total");

        try {
            String source = Files.readString(sourceFile);
            List<MethodSignature> methods = extractMethods(source, sourceFile);

            if (methods.isEmpty()) {
                return GenerationResult.error("No testable public methods found in " + sourceFile);
            }

            log.info("[prop-test] found {} methods to test", methods.size());

            // Generate properties for each method
            List<GeneratedPropertyTest> tests = methods.stream()
                    .map(m -> generateForMethod(m, source, framework))
                    .toList();

            // Assemble the full test file
            String testCode = assembleTestFile(sourceFile, tests, framework);

            if (outputFile != null) {
                Files.createDirectories(outputFile.getParent());
                Files.writeString(outputFile, testCode);
                log.info("[prop-test] written to {}", outputFile);
            }

            telemetry.count("proptest.generate.success");
            return new GenerationResult(true, null, tests, testCode, sourceFile, outputFile, Instant.now());

        } catch (IOException e) {
            log.error("[prop-test] generation failed: {}", e.getMessage());
            return GenerationResult.error(e.getMessage());
        }
    }

    /**
     * Generates property-based tests for a single method.
     *
     * @param methodSignature  the full method signature (e.g., "public List<Integer> sort(List<Integer> input)")
     * @param classContext     the surrounding class source (for context)
     * @param framework        the test framework
     */
    public GeneratedPropertyTest generateForMethod(String methodSignature, String classContext,
                                                    TestFramework framework) {
        MethodSignature method = new MethodSignature(methodSignature, "", List.of(), "");
        return generateForMethod(method, classContext, framework);
    }

    /**
     * Analyzes an existing test file and identifies missing property tests.
     * Returns suggestions for each method that only has example-based tests.
     */
    public List<PropertySuggestion> analyzeExistingTests(Path testFile, Path sourceFile) {
        if (!Files.exists(testFile) || !Files.exists(sourceFile)) return List.of();

        try {
            String testSource   = Files.readString(testFile);
            String sourceCode   = Files.readString(sourceFile);
            List<MethodSignature> methods = extractMethods(sourceCode, sourceFile);

            return methods.stream()
                    .filter(m -> !hasPropertyTest(testSource, m.name()))
                    .map(m -> suggestProperties(m, sourceCode))
                    .toList();
        } catch (IOException e) {
            log.warn("[prop-test] analysis failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Generates adversarial edge-case inputs for a method's parameter types.
     */
    public EdgeCaseSet generateEdgeCases(String methodSignature, String returnType) {
        List<String> intEdges    = List.of("0", "-1", "1", "Integer.MIN_VALUE", "Integer.MAX_VALUE");
        List<String> stringEdges = List.of("\"\"", "null", "\" \"",
                "\"\\n\"", "\"a\".repeat(10000)", "\"<script>alert(1)</script>\"",
                "\"\\u0000\"", "\"'; DROP TABLE users;--\"");
        List<String> listEdges   = List.of("List.of()", "null", "List.of(null)",
                "Collections.unmodifiableList(List.of(1))", "new ArrayList<>(){{for(int i=0;i<10000;i++)add(i);}}");
        List<String> mapEdges    = List.of("Map.of()", "null",
                "Collections.unmodifiableMap(Map.of())");

        Map<String, List<String>> allEdges = Map.of(
                "int|Integer|long|Long|short|Short|byte|Byte", intEdges,
                "String",   stringEdges,
                "List|Collection|Iterable|Set", listEdges,
                "Map",      mapEdges
        );

        List<String> applicable = allEdges.entrySet().stream()
                .filter(e -> Arrays.stream(e.getKey().split("\\|"))
                        .anyMatch(t -> methodSignature.contains(t)))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .toList();

        return new EdgeCaseSet(methodSignature, applicable, Instant.now());
    }

    // ── Private ────────────────────────────────────────────────────────────

    private GeneratedPropertyTest generateForMethod(MethodSignature method,
                                                      String classSource,
                                                      TestFramework framework) {
        List<Property> properties = identifyProperties(method, classSource);
        EdgeCaseSet edgeCases     = generateEdgeCases(method.signature(), method.returnType());
        String testCode           = generateTestCode(method, properties, edgeCases, framework);

        return new GeneratedPropertyTest(method.name(), method.className(),
                properties, edgeCases, testCode, framework);
    }

    private List<Property> identifyProperties(MethodSignature method, String classSource) {
        String prompt = """
                Identify mathematical properties for this Java method.
                For each property, state: name, category, and what assertion implements it.
                
                Method: %s
                Class context (abbreviated):
                %s
                
                Output JSON array:
                [{"name":"sorted_order","category":"INVARIANT",
                  "description":"result[i] <= result[i+1] for all i",
                  "assertion":"assertThat(result).isSortedAccordingTo(Comparator.naturalOrder())"}]
                """.formatted(method.signature(),
                classSource.length() > 1000 ? classSource.substring(0, 1000) + "…" : classSource);

        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, config.tokenBudget()))
                            .stream(false).maxSteps(1).build());

            return parseProperties(result.answer());
        } catch (Exception e) {
            return List.of(
                    new Property("non_null_result", PropertyCategory.INVARIANT,
                            "result should not be null for valid inputs",
                            "assertThat(result).isNotNull()"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Property> parseProperties(String raw) {
        try {
            String json = raw.replaceAll("(?s)```json\\s*","").replaceAll("```","").strip();
            int s = json.indexOf('['), e = json.lastIndexOf(']');
            if (s < 0) return List.of();
            List<Map<String, String>> items = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json.substring(s, e+1), List.class);
            return items.stream().map(m -> new Property(
                    (String) m.getOrDefault("name", "property"),
                    PropertyCategory.valueOf(((String) m.getOrDefault("category", "INVARIANT")).toUpperCase()),
                    (String) m.getOrDefault("description", ""),
                    (String) m.getOrDefault("assertion", "assertThat(result).isNotNull()")))
                    .toList();
        } catch (Exception e) { return List.of(); }
    }

    private String generateTestCode(MethodSignature method, List<Property> properties,
                                     EdgeCaseSet edgeCases, TestFramework framework) {
        return switch (framework) {
            case JQWIK   -> generateJqwikCode(method, properties, edgeCases);
            case JUNIT5  -> generateJunit5Code(method, properties, edgeCases);
            case HYPOTHESIS -> generateHypothesisCode(method, properties, edgeCases);
            default      -> generateJqwikCode(method, properties, edgeCases);
        };
    }

    private String generateJqwikCode(MethodSignature m, List<Property> props, EdgeCaseSet edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Property tests for ").append(m.name()).append("\n");
        sb.append("// Generated by Gamelan PropertyTestGenerator\n\n");

        for (Property prop : props) {
            sb.append("@Property\n");
            sb.append("void ").append(m.name()).append("_").append(prop.name())
              .append("(@ForAll ").append(inferArbitraryType(m.signature())).append(" input) {\n");
            sb.append("    // ").append(prop.description()).append("\n");
            sb.append("    var result = sut.").append(m.name()).append("(input);\n");
            sb.append("    ").append(prop.assertion()).append(";\n");
            sb.append("}\n\n");
        }

        // Edge case tests
        sb.append("@Test\n");
        sb.append("void ").append(m.name()).append("_edgeCases() {\n");
        edges.edgeCaseInputs().stream().limit(5).forEach(edge ->
                sb.append("    // Edge case: ").append(edge).append("\n"));
        sb.append("}\n");

        return sb.toString();
    }

    private String generateJunit5Code(MethodSignature m, List<Property> props, EdgeCaseSet edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("@ParameterizedTest\n@MethodSource(\"").append(m.name()).append("Inputs\")\n");
        sb.append("void ").append(m.name()).append("_properties(Object input) {\n");
        props.stream().limit(3).forEach(p -> sb.append("    // ").append(p.description()).append("\n"));
        sb.append("}\n\nstatic Stream<Arguments> ").append(m.name()).append("Inputs() {\n");
        sb.append("    return Stream.of(\n");
        edges.edgeCaseInputs().stream().limit(5)
                .forEach(e -> sb.append("        Arguments.of(").append(e).append("),\n"));
        sb.append("    );\n}\n");
        return sb.toString();
    }

    private String generateHypothesisCode(MethodSignature m, List<Property> props, EdgeCaseSet edges) {
        StringBuilder sb = new StringBuilder();
        sb.append("from hypothesis import given, strategies as st\n\n");
        props.forEach(p -> {
            sb.append("@given(st.text())\n");
            sb.append("def test_").append(m.name()).append("_").append(p.name())
              .append("(input):\n");
            sb.append("    # ").append(p.description()).append("\n");
            sb.append("    result = sut.").append(m.name()).append("(input)\n");
            sb.append("    assert result is not None\n\n");
        });
        return sb.toString();
    }

    private String assembleTestFile(Path sourceFile, List<GeneratedPropertyTest> tests,
                                     TestFramework framework) {
        String className = sourceFile.getFileName().toString()
                .replace(".java", "").replace(".py", "");
        StringBuilder sb = new StringBuilder();

        if (framework == TestFramework.HYPOTHESIS) {
            sb.append("# Property-based tests for ").append(className).append("\n");
            sb.append("# Generated by Gamelan\n\n");
            tests.forEach(t -> sb.append(t.testCode()).append("\n\n"));
        } else {
            sb.append("// Property-based tests for ").append(className).append("\n");
            sb.append("// Generated by Gamelan PropertyTestGenerator\n\n");
            if (framework == TestFramework.JQWIK) {
                sb.append("import net.jqwik.api.*;\n");
                sb.append("import net.jqwik.api.constraints.*;\n\n");
            }
            sb.append("class ").append(className).append("PropertyTest {\n");
            sb.append("    private final ").append(className).append(" sut = new ").append(className).append("();\n\n");
            tests.forEach(t -> sb.append(indent(t.testCode(), "    ")).append("\n"));
            sb.append("}\n");
        }
        return sb.toString();
    }

    private List<MethodSignature> extractMethods(String source, Path file) {
        List<MethodSignature> methods = new ArrayList<>();
        String lang = file.toString().endsWith(".py") ? "python" : "java";

        if ("java".equals(lang)) {
            Pattern p = Pattern.compile(
                    "(?:public|protected)\\s+(?:static\\s+)?([\\w<>\\[\\],\\s]+?)\\s+(\\w+)\\s*\\(([^)]*?)\\)\\s*\\{",
                    Pattern.MULTILINE);
            Matcher m = p.matcher(source);
            String className = extractClassName(source);
            while (m.find()) {
                String returnType = m.group(1).strip();
                String methodName = m.group(2).strip();
                String params     = m.group(3).strip();
                if (!returnType.equals("class") && !returnType.equals("interface") &&
                        !methodName.equals(className)) {
                    List<String> paramTypes = Arrays.stream(params.split(","))
                            .map(String::strip).filter(s -> !s.isEmpty())
                            .map(s -> s.contains(" ") ? s.substring(0, s.lastIndexOf(' ')).strip() : s)
                            .toList();
                    methods.add(new MethodSignature(
                            "public " + returnType + " " + methodName + "(" + params + ")",
                            methodName, paramTypes, returnType));
                }
            }
        }
        return methods;
    }

    private String extractClassName(String source) {
        Pattern p = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
        Matcher m = p.matcher(source);
        return m.find() ? m.group(1) : "Unknown";
    }

    private boolean hasPropertyTest(String testSource, String methodName) {
        return testSource.contains("@Property") && testSource.contains(methodName) ||
               testSource.contains("@given") && testSource.contains(methodName);
    }

    private PropertySuggestion suggestProperties(MethodSignature method, String source) {
        List<Property> props = identifyProperties(method, source);
        return new PropertySuggestion(method.name(), method.className(), props,
                "Add @Property tests for: " + props.stream().map(Property::name)
                        .collect(Collectors.joining(", ")));
    }

    private String inferArbitraryType(String signature) {
        if (signature.contains("List<")) return "@Size(min=0, max=100) List<Integer>";
        if (signature.contains("String")) return "String";
        if (signature.contains("int") || signature.contains("Integer")) return "int";
        if (signature.contains("long") || signature.contains("Long")) return "long";
        return "Object";
    }

    private String indent(String code, String prefix) {
        return Arrays.stream(code.split("\n"))
                .map(l -> prefix + l)
                .collect(Collectors.joining("\n"));
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum TestFramework  { JQWIK, JUNIT5, HYPOTHESIS, KOTEST }
    public enum PropertyCategory { INVARIANT, IDEMPOTENT, ROUNDTRIP, METAMORPHIC, ORACLE }

    public record Property(String name, PropertyCategory category,
                            String description, String assertion) {}

    public record MethodSignature(String signature, String name,
                                   List<String> paramTypes, String returnType) {
        String className() { return "Unknown"; }
    }

    public record GeneratedPropertyTest(
            String            methodName,
            String            className,
            List<Property>    properties,
            EdgeCaseSet       edgeCases,
            String            testCode,
            TestFramework     framework
    ) {
        public int propertyCount() { return properties.size(); }
    }

    public record EdgeCaseSet(
            String       methodSignature,
            List<String> edgeCaseInputs,
            Instant      generatedAt
    ) {}

    public record PropertySuggestion(
            String       methodName,
            String       className,
            List<Property> suggestedProperties,
            String       summary
    ) {}

    public record GenerationResult(
            boolean                       success,
            String                        error,
            List<GeneratedPropertyTest>   tests,
            String                        assembledCode,
            Path                          sourceFile,
            Path                          outputFile,
            Instant                       generatedAt
    ) {
        static GenerationResult error(String msg) {
            return new GenerationResult(false, msg, List.of(), "", null, null, Instant.now());
        }
        public int totalProperties() { return tests.stream().mapToInt(GeneratedPropertyTest::propertyCount).sum(); }
        public String summary() {
            return success
                    ? String.format("Generated %d property tests for %d methods (%d properties total)",
                            tests.size(), tests.size(), totalProperties())
                    : "FAILED: " + error;
        }
    }
}
