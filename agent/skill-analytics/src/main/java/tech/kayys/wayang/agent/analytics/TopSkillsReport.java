package tech.kayys.wayang.agent.analytics;

import java.time.Instant;
import java.util.List;

/**
 * Report of top skills by usage.
 */
public record TopSkillsReport(
    List<TopSkillEntry> topSkills,
    Instant generatedAt
) {}
