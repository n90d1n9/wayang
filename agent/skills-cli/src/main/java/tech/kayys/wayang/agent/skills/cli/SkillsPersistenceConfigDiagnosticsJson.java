package tech.kayys.wayang.agent.skills.cli;

import java.util.List;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

final class SkillsPersistenceConfigDiagnosticsJson {

    private SkillsPersistenceConfigDiagnosticsJson() {
    }

    static void diagnosticsField(
            StringBuilder builder,
            String name,
            SkillsPersistenceConfigDiagnostics diagnostics) {
        if (diagnostics == null) {
            return;
        }
        name(builder, name);
        builder.append('{');
        field(builder, "lifecycleReconcile", diagnostics.lifecycleReconcile());
        field(builder, "createMissingStates", diagnostics.createMissingStates());
        field(builder, "removeOrphanedStates", diagnostics.removeOrphanedStates());
        storesField(builder, "stores", diagnostics.stores());
        trimComma(builder);
        builder.append("},");
    }

    private static void storesField(
            StringBuilder builder,
            String name,
            List<SkillsPersistenceConfigDiagnostics.Store> stores) {
        name(builder, name);
        builder.append('[');
        for (SkillsPersistenceConfigDiagnostics.Store store : stores) {
            store(builder, store);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void store(
            StringBuilder builder,
            SkillsPersistenceConfigDiagnostics.Store store) {
        builder.append('{');
        field(builder, "role", store.role());
        field(builder, "kind", store.kind());
        field(builder, "target", store.target());
        field(builder, "initializeJdbcSchema", store.initializeJdbcSchema());
        field(builder, "maxEvents", store.maxEvents());
        storesField(builder, "children", store.children());
        trimComma(builder);
        builder.append('}');
    }
}
