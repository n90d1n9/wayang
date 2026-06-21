package tech.kayys.wayang.sdk.gollek.tools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Naive planner: generate a numbered implementation plan from a goal string.
 * This is intentionally simple to be replaceable by an LLM-based planner later.
 */
public final class Planner {

    public static final class PlanStep {
        public final String id;
        public final int index;
        public final String text;

        public PlanStep(int idx, String text) {
            this.id = UUID.randomUUID().toString();
            this.index = idx;
            this.text = text;
        }
    }

    public List<PlanStep> makePlan(String goal) {
        List<PlanStep> steps = new ArrayList<>();
        if (goal == null || goal.isBlank()) {
            steps.add(new PlanStep(1, "Inspect the repository layout and identify modules to change."));
            steps.add(new PlanStep(2, "Run project tests locally for the target modules."));
            steps.add(new PlanStep(3, "Implement the required changes and add unit tests."));
            steps.add(new PlanStep(4, "Run tests and prepare a PR with changes."));
            return steps;
        }

        // Heuristic: split by sentences and create steps
        String[] sentences = goal.split("(?<=[.!?])\\s+");
        int idx = 1;
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            steps.add(new PlanStep(idx++, trimmed));
        }
        // Ensure there are final QA steps
        steps.add(new PlanStep(idx++, "Write unit tests and run the target module tests."));
        steps.add(new PlanStep(idx++, "Prepare a PR describing changes and test results."));
        return steps;
    }
}
