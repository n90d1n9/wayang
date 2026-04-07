package tech.kayys.wayang.agent.shaker;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * CLI entry point for agent packaging and tree-shaking.
 *
 * <h3>Usage:</h3>
 * <pre>
 * # Analyze workflow dependencies
 * java -jar agent-shaker.jar analyze workflow.yaml
 *
 * # Package standalone agent JAR
 * java -jar agent-shaker.jar package workflow.yaml --output agent.jar
 *
 * # Package with optimization
 * java -jar agent-shaker.jar package workflow.yaml --optimize --output agent.jar
 *
 * # Generate Docker image
 * java -jar agent-shaker.jar docker workflow.yaml --tag my-agent:latest
 *
 * # Generate native image (GraalVM)
 * java -jar agent-shaker.jar native workflow.yaml --output agent-native
 * </pre>
 *
 * @author Wayang Team
 * @version 1.0.0
 * @since 2026-04-06
 */
public class AgentShakerCli {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        Path workflowPath = Path.of(args[1]);

        try {
            switch (command) {
                case "analyze" -> cmdAnalyze(workflowPath);
                case "package" -> cmdPackage(workflowPath, args);
                case "docker" -> cmdDocker(workflowPath, args);
                case "native" -> cmdNative(workflowPath, args);
                default -> {
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Analyze workflow dependencies.
     */
    private static void cmdAnalyze(Path workflowPath) throws Exception {
        System.out.println("=== Analyzing Workflow ===");
        System.out.println("Workflow: " + workflowPath);

        WorkflowAnalyzer analyzer = new WorkflowAnalyzer();
        DependencyReport report = analyzer.analyze(workflowPath);

        System.out.println(report);
    }

    /**
     * Package standalone agent JAR.
     */
    private static void cmdPackage(Path workflowPath, String[] args) throws Exception {
        System.out.println("=== Packaging Agent ===");
        System.out.println("Workflow: " + workflowPath);

        // Parse arguments
        Path outputPath = Path.of("output/agent.jar");
        boolean optimize = true;
        String mainClass = null;
        int maxSteps = 10;
        String timeout = "PT60S";

        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "--output", "-o" -> outputPath = Path.of(args[++i]);
                case "--no-optimize" -> optimize = false;
                case "--main-class" -> mainClass = args[++i];
                case "--max-steps" -> maxSteps = Integer.parseInt(args[++i]);
                case "--timeout" -> timeout = args[++i];
            }
        }

        // Package agent
        AgentPackager packager = new AgentPackager();
        PackageConfig config = PackageConfig.builder()
            .mainClass(mainClass)
            .optimize(optimize)
            .maxSteps(maxSteps)
            .timeout(timeout)
            .outputPath(outputPath)
            .build();

        Path jarPath = packager.packageAgent(workflowPath, outputPath, config);

        System.out.println("✅ Agent packaged successfully: " + jarPath);
    }

    /**
     * Generate Docker image.
     */
    private static void cmdDocker(Path workflowPath, String[] args) throws Exception {
        System.out.println("=== Generating Docker Image ===");
        System.out.println("Workflow: " + workflowPath);

        // Parse arguments
        String tag = "wayang-agent:latest";
        Path dockerDir = Path.of("output/docker");

        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "--tag", "-t" -> tag = args[++i];
                case "--output" -> dockerDir = Path.of(args[++i]);
            }
        }

        // TODO: Implement Docker image generation
        System.out.println("⏳ Docker image generation coming soon...");
        System.out.println("   Tag: " + tag);
        System.out.println("   Output: " + dockerDir);
    }

    /**
     * Generate native image (GraalVM).
     */
    private static void cmdNative(Path workflowPath, String[] args) throws Exception {
        System.out.println("=== Generating Native Image ===");
        System.out.println("Workflow: " + workflowPath);

        // Parse arguments
        Path outputPath = Path.of("output/agent-native");

        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "--output", "-o" -> outputPath = Path.of(args[++i]);
            }
        }

        // TODO: Implement native image generation
        System.out.println("⏳ Native image generation coming soon...");
        System.out.println("   Output: " + outputPath);
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("""
            Wayang Agent Shaker - Tree-Shaking & Packaging

            Usage:
              agent-shaker <command> <workflow.yaml> [options]

            Commands:
              analyze    Analyze workflow dependencies
              package    Package standalone agent JAR
              docker     Generate Docker image
              native     Generate native image (GraalVM)

            Options:
              --output, -o <path>     Output path (default: output/agent.jar)
              --no-optimize           Disable tree-shaking optimization
              --main-class <class>    Main class for JAR manifest
              --max-steps <n>         Default max steps (default: 10)
              --timeout <duration>    Default timeout (default: PT60S)
              --tag, -t <tag>         Docker image tag (default: wayang-agent:latest)

            Examples:
              agent-shaker analyze workflow.yaml
              agent-shaker package workflow.yaml --output agent.jar
              agent-shaker package workflow.yaml --optimize --output agent.jar
              agent-shaker docker workflow.yaml --tag my-agent:latest
              agent-shaker native workflow.yaml --output agent-native
            """);
    }
}
