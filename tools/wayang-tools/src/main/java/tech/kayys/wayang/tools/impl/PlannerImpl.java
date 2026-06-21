package tech.kayys.wayang.tools.impl;

import tech.kayys.wayang.sdk.gollek.tools.PlannerIface;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlannerImpl implements PlannerIface {
    @Override
    public List<PlanStep> makePlan(String goal) {
        List<PlanStep> steps = new ArrayList<>();
        if (goal == null || goal.isBlank()) {
            steps.add(new PlanStep(UUID.randomUUID().toString(), 1, "Inspect the repository layout and identify modules to change."));
            steps.add(new PlanStep(UUID.randomUUID().toString(), 2, "Run project tests locally for the target modules."));
            steps.add(new PlanStep(UUID.randomUUID().toString(), 3, "Implement the required changes and add unit tests."));
            steps.add(new PlanStep(UUID.randomUUID().toString(), 4, "Run tests and prepare a PR with changes."));
            return steps;
        }
        String[] sentences = goal.split("(?<=[.!?])\\\\s+");
        int idx = 1;
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            steps.add(new PlanStep(UUID.randomUUID().toString(), idx++, trimmed));
        }
        steps.add(new PlanStep(UUID.randomUUID().toString(), idx++, "Write unit tests and run the target module tests."));
        steps.add(new PlanStep(UUID.randomUUID().toString(), idx++, "Prepare a PR describing changes and test results."));
        return steps;
    }
}
