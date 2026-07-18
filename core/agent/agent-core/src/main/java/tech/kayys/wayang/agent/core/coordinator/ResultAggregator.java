package tech.kayys.wayang.agent.core.coordinator;

import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates and combines results from multiple agents.
 * Different strategies for combining agent results depending on use case.
 */
public interface ResultAggregator {

    /**
     * Aggregate results from multiple agents.
     * 
     * @param results Map of agent name to their execution result
     * @return Aggregated result combining all agent outputs
     */
    AggregatedResult aggregate(Map<String, AgentResponse> results);

    private static String answerOf(AgentResponse response) {
        return response != null && response.answer() != null ? response.answer() : "";
    }

    /**
     * Result of aggregation across multiple agents.
     */
    record AggregatedResult(
        String finalAnswer,
        Map<String, String> agentContributions,  // agent -> their contribution
        Map<String, Object> metadata,
        long aggregationTimeMs,
        boolean success,
        String error
    ) {}

    /**
     * Returns the first successful result, ignoring others.
     * Useful for failover scenarios.
     */
    class FirstSuccessAggregator implements ResultAggregator {
        @Override
        public AggregatedResult aggregate(Map<String, AgentResponse> results) {
            long startTime = System.currentTimeMillis();

            for (Map.Entry<String, AgentResponse> entry : results.entrySet()) {
                AgentResponse response = entry.getValue();
                String answer = answerOf(response);
                if (!answer.isBlank()) {
                    return new AggregatedResult(
                            answer,
                            Map.of(entry.getKey(), answer),
                            Map.of("strategy", "first_success"),
                            System.currentTimeMillis() - startTime,
                            true,
                            null
                    );
                }
            }

            return new AggregatedResult(
                    "No successful results from any agent",
                    Map.of(),
                    Map.of("strategy", "first_success"),
                    System.currentTimeMillis() - startTime,
                    false,
                    "All agents failed or returned empty results"
            );
        }
    }

    /**
     * Uses majority voting if results differ significantly.
     * Useful for consensus scenarios.
     */
    class MajorityVoteAggregator implements ResultAggregator {
        @Override
        public AggregatedResult aggregate(Map<String, AgentResponse> results) {
            long startTime = System.currentTimeMillis();

            if (results.isEmpty()) {
                return new AggregatedResult(
                        "No results to aggregate",
                        Map.of(),
                        Map.of("strategy", "majority_vote"),
                        System.currentTimeMillis() - startTime,
                        false,
                        "Empty results map"
                );
            }

            // Count vote frequencies
            Map<String, List<String>> voteGroups = results.entrySet().stream()
                    .filter(e -> !answerOf(e.getValue()).isBlank())
                    .collect(Collectors.groupingBy(
                            e -> answerOf(e.getValue()),
                            Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                    ));

            if (voteGroups.isEmpty()) {
                return new AggregatedResult(
                        "No valid results",
                        Map.of(),
                        Map.of("strategy", "majority_vote"),
                        System.currentTimeMillis() - startTime,
                        false,
                        "All results were empty"
                );
            }

            // Find majority
            String majorityAnswer = voteGroups.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey)
                    .orElse("Unknown");

            Map<String, String> contributions = new HashMap<>();
            voteGroups.forEach((answer, agents) -> {
                agents.forEach(agent -> contributions.put(agent, answer));
            });

            return new AggregatedResult(
                    majorityAnswer,
                    contributions,
                    Map.of(
                        "strategy", "majority_vote",
                        "total_votes", results.size(),
                        "vote_groups", voteGroups.size()
                    ),
                    System.currentTimeMillis() - startTime,
                    true,
                    null
            );
        }
    }

    /**
     * Concatenates all agent results with agent names.
     * Useful for complementary information gathering.
     */
    class ConcatenationAggregator implements ResultAggregator {
        @Override
        public AggregatedResult aggregate(Map<String, AgentResponse> results) {
            long startTime = System.currentTimeMillis();

            StringBuilder combined = new StringBuilder();
            Map<String, String> contributions = new HashMap<>();

            for (Map.Entry<String, AgentResponse> entry : results.entrySet()) {
                String agentName = entry.getKey();
                AgentResponse response = entry.getValue();

                String answer = answerOf(response);
                if (!answer.isBlank()) {
                    combined.append("[").append(agentName).append("] ")
                            .append(answer).append("\n\n");
                    contributions.put(agentName, answer);
                }
            }

            return new AggregatedResult(
                    combined.toString().trim(),
                    contributions,
                    Map.of(
                        "strategy", "concatenation",
                        "agent_count", contributions.size()
                    ),
                    System.currentTimeMillis() - startTime,
                    !contributions.isEmpty(),
                    contributions.isEmpty() ? "All agents returned empty results" : null
            );
        }
    }

    /**
     * Weighted aggregation based on agent confidence/priority.
     * First agent is most important, subsequent agents add supplementary info.
     */
    class WeightedAggregator implements ResultAggregator {
        private final List<Double> weights;

        public WeightedAggregator(List<Double> weights) {
            this.weights = Objects.requireNonNull(weights, "weights");
        }

        @Override
        public AggregatedResult aggregate(Map<String, AgentResponse> results) {
            long startTime = System.currentTimeMillis();

            List<AgentResponse> responses = results.values().stream()
                    .filter(r -> !answerOf(r).isBlank())
                    .toList();

            if (responses.isEmpty()) {
                return new AggregatedResult(
                        "No results to aggregate",
                        Map.of(),
                        Map.of("strategy", "weighted"),
                        System.currentTimeMillis() - startTime,
                        false,
                        "All agents returned empty results"
                );
            }

            // Use primary agent's answer plus supplementary info
            StringBuilder combined = new StringBuilder(answerOf(responses.get(0)));

            if (responses.size() > 1) {
                combined.append("\n\nAdditional context:\n");
                for (int i = 1; i < responses.size(); i++) {
                    double weight = i < weights.size() ? weights.get(i) : 0.5;
                    combined.append("- [weight: ").append(String.format("%.1f", weight)).append("] ")
                            .append(answerOf(responses.get(i))).append("\n");
                }
            }

            Map<String, String> contributions = new HashMap<>();
            int index = 0;
            for (String agentName : results.keySet()) {
                if (index < responses.size()) {
                    contributions.put(agentName, answerOf(responses.get(index)));
                    index++;
                }
            }

            return new AggregatedResult(
                    combined.toString(),
                    contributions,
                    Map.of(
                        "strategy", "weighted",
                        "primary_agent", results.keySet().iterator().next(),
                        "response_count", responses.size()
                    ),
                    System.currentTimeMillis() - startTime,
                    true,
                    null
            );
        }
    }
}
