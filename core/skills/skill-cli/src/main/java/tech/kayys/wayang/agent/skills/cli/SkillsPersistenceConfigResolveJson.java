package tech.kayys.wayang.agent.skills.cli;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.arrayField;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsPersistenceConfigResolveJson {

    private SkillsPersistenceConfigResolveJson() {
    }

    static String toJson(SkillsPersistenceConfigResolveReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "source", report.source());
        field(builder, "profile", report.profile());
        field(builder, "runtime", report.runtime());
        field(builder, "valid", report.valid());
        field(builder, "errorCount", report.errorCount());
        arrayField(builder, "errors", report.errors());
        SkillsPersistenceConfigDiagnosticsJson.diagnosticsField(builder, "diagnostics", report.diagnostics());
        field(builder, "strategy", report.persistence().strategy());
        field(builder, "fullyDurable", report.persistence().fullyDurable());
        field(builder, "warningCount", report.warningCount());
        arrayField(builder, "warnings", report.warnings());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }
}
