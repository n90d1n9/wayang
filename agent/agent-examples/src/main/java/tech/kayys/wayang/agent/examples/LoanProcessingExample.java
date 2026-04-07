package tech.kayys.wayang.agent.examples;

import tech.kayys.wayang.agent.core.Agent;
import tech.kayys.wayang.agent.core.AgentContext;
import tech.kayys.wayang.agent.core.AgentResponse;
import tech.kayys.wayang.agent.coordination.MultiAgentOrchestrator;
import tech.kayys.wayang.agent.coordination.ResultAggregator;

import java.util.*;

/**
 * Example: Loan Processing Workflow
 * 
 * Demonstrates multi-agent orchestration where:
 * - Legal Agent validates compliance
 * - Financial Agent calculates risk metrics
 * - Risk Agent makes final decision
 * 
 * All agents run in parallel, results aggregated using consensus voting.
 */
public class LoanProcessingExample {

    /**
     * Mock Legal Agent - validates regulatory compliance
     */
    public static class LegalAgent implements Agent {
        @Override
        public String getName() {
            return "legal";
        }

        @Override
        public String getType() {
            return "specialist";
        }

        @Override
        public AgentResponse execute(String query, AgentContext context) {
            // In real implementation, this would check regulations, contracts, etc.
            String analysis = "Legal Compliance Assessment:\n" +
                    "✓ All regulatory requirements met\n" +
                    "✓ Applicant has clean legal record\n" +
                    "✓ Documentation complete and valid";

            return AgentResponse.builder()
                    .agentName(getName())
                    .finalAnswer("APPROVED - Meets all legal requirements")
                    .metadata(Map.of("analysis", analysis))
                    .build();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public String getSystemPrompt() {
            return "You are a legal compliance expert. Assess whether the loan application meets all regulatory requirements.";
        }
    }

    /**
     * Mock Financial Agent - analyzes financial metrics
     */
    public static class FinancialAgent implements Agent {
        @Override
        public String getName() {
            return "financial";
        }

        @Override
        public String getType() {
            return "specialist";
        }

        @Override
        public AgentResponse execute(String query, AgentContext context) {
            String analysis = "Financial Analysis:\n" +
                    "• Debt-to-Income Ratio: 35% (Acceptable)\n" +
                    "• Credit Score: 750+ (Excellent)\n" +
                    "• Savings Ratio: 20% (Good)";

            return AgentResponse.builder()
                    .agentName(getName())
                    .finalAnswer("APPROVED - Strong financial metrics")
                    .metadata(Map.of("analysis", analysis))
                    .build();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public String getSystemPrompt() {
            return "You are a financial analyst. Assess the applicant's financial health and ability to repay.";
        }
    }

    /**
     * Mock Risk Agent - evaluates overall risk
     */
    public static class RiskAgent implements Agent {
        @Override
        public String getName() {
            return "risk";
        }

        @Override
        public String getType() {
            return "specialist";
        }

        @Override
        public AgentResponse execute(String query, AgentContext context) {
            String analysis = "Risk Assessment:\n" +
                    "• Default Probability: 2% (Very Low)\n" +
                    "• Market Risk: Minimal\n" +
                    "• Overall Risk Level: LOW";

            return AgentResponse.builder()
                    .agentName(getName())
                    .finalAnswer("APPROVED - Low risk profile")
                    .metadata(Map.of("analysis", analysis))
                    .build();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public String getSystemPrompt() {
            return "You are a risk analyst. Evaluate the overall risk of this loan application.";
        }
    }

    /**
     * Run loan processing workflow.
     */
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  LOAN PROCESSING WORKFLOW - Multi-Agent Orchestration");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // Create specialized agents
        Agent legalAgent = new LegalAgent();
        Agent financialAgent = new FinancialAgent();
        Agent riskAgent = new RiskAgent();

        // Create orchestrator with consensus voting
        MultiAgentOrchestrator orchestrator = MultiAgentOrchestrator.builder()
                .name("loan-processor")
                .addAgent("legal", legalAgent)
                .addAgent("financial", financialAgent)
                .addAgent("risk", riskAgent)
                .withMode(MultiAgentOrchestrator.OrchestrationMode.PARALLEL)
                .build();

        // Create context
        AgentContext context = AgentContext.builder()
                .build();

        // Process loan application
        String loanApplication = """
                Applicant: John Doe
                Loan Amount: $300,000
                Loan Term: 30 years
                Credit Score: 755
                Debt-to-Income: 35%
                """;

        System.out.println("PROCESSING LOAN APPLICATION:");
        System.out.println("─────────────────────────────");
        System.out.println(loanApplication);
        System.out.println();

        // Run parallel orchestration
        try {
            AgentResponse orchestratedResult = orchestrator.execute(
                    loanApplication,
                    context
            );

            System.out.println("ORCHESTRATION RESULTS:");
            System.out.println("──────────────────────");
            System.out.println("Final Decision: " + orchestratedResult.getFinalAnswer());
            System.out.println();

            System.out.println("Agent Contributions:");
            orchestratedResult.getMetadata().forEach((agent, contribution) -> {
                if (contribution instanceof String && !agent.equals("orchestration_time_ms")) {
                    System.out.println("  • " + agent + ": " + contribution);
                }
            });

            System.out.println();
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("WORKFLOW COMPLETED SUCCESSFULLY");
            System.out.println("═══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
