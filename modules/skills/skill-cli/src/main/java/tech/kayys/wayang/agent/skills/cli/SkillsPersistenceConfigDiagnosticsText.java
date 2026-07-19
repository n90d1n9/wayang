package tech.kayys.wayang.agent.skills.cli;

import java.io.PrintStream;

final class SkillsPersistenceConfigDiagnosticsText {

    private SkillsPersistenceConfigDiagnosticsText() {
    }

    static void render(SkillsPersistenceConfigDiagnostics diagnostics, PrintStream out) {
        out.printf(
                "lifecycle reconcile: %s create-missing=%s remove-orphans=%s%n",
                diagnostics.lifecycleReconcile(),
                diagnostics.createMissingStates(),
                diagnostics.removeOrphanedStates());
        diagnostics.stores().forEach(store -> diagnosticStore(store, "", out));
    }

    private static void diagnosticStore(
            SkillsPersistenceConfigDiagnostics.Store store,
            String indent,
            PrintStream out) {
        out.printf(
                "%s- %s: kind=%s target=%s jdbc-init=%s max-events=%d%n",
                indent,
                store.role(),
                store.kind(),
                store.target().isBlank() ? "-" : store.target(),
                store.initializeJdbcSchema(),
                store.maxEvents());
        store.children().forEach(child -> diagnosticStore(child, indent + "  ", out));
    }
}
