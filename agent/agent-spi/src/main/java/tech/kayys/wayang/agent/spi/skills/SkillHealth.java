package tech.kayys.wayang.agent.spi.skills;

public record SkillHealth(String skillId, boolean healthy, String message) {
    public static SkillHealth healthy(String id) {
        return new SkillHealth(id, true, "OK");
    }

    public static SkillHealth unhealthy(String id, String msg) {
        return new SkillHealth(id, false, msg);
    }
}
