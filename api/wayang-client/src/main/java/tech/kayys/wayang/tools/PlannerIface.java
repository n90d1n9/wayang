package tech.kayys.wayang.tools;

import java.util.List;

public interface PlannerIface {
    public static final class PlanStep {
        public final String id;
        public final int index;
        public final String text;
        public PlanStep(String id, int index, String text) { this.id = id; this.index = index; this.text = text; }
    }

    List<PlanStep> makePlan(String goal);
}
