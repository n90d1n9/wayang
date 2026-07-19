package tech.kayys.wayang.agent.skills.cli;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.arrayField;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsPersistenceConfigValidationJson {

    private SkillsPersistenceConfigValidationJson() {
    }

    static String toJson(SkillsPersistenceConfigValidationReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "source", report.source());
        field(builder, "profile", report.profile());
        field(builder, "runtime", report.runtime());
        field(builder, "valid", report.valid());
        field(builder, "durabilityRequired", report.requireDurable());
        field(builder, "passed", report.passed());
        field(builder, "errorCount", report.errorCount());
        arrayField(builder, "errors", report.errors());
        field(builder, "policyErrorCount", report.policyErrorCount());
        arrayField(builder, "policyErrors", report.policyErrors());
        field(builder, "strategy", report.persistence().strategy());
        field(builder, "fullyDurable", report.persistence().fullyDurable());
        field(builder, "warningCount", report.warningCount());
        arrayField(builder, "warnings", report.warnings());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }
}
