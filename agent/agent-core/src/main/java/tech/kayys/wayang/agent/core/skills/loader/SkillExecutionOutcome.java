package tech.kayys.wayang.agent.core.skills.loader;

import java.util.Map;

/**
 * Stable read-only contract for filesystem skill execution results.
 */
public interface SkillExecutionOutcome {

    String skillName();

    String output();

    long executionTimeMs();

    boolean success();

    String error();

    Map<String, Object> metadata();
}
