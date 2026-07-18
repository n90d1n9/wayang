package tech.kayys.wayang.skill;

import tech.kayys.wayang.client.SdkText;

public record WayangSkillContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.skill.catalog";
    public static final int VERSION = 1;
    public static final String SKILL_DISCOVERY = "skill-discovery";
    public static final String SKILL_DETAIL = "skill-detail";

    public WayangSkillContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangSkillContract skillDiscovery() {
        return new WayangSkillContract(SCHEMA, VERSION, SKILL_DISCOVERY);
    }

    public static WayangSkillContract skillDetail() {
        return new WayangSkillContract(SCHEMA, VERSION, SKILL_DETAIL);
    }
}
