package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HarnessContext {

    private HarnessContext() {
    }

    static Map<String, Object> from(HarnessPlan plan) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("workspaceRoot", plan.workspace().rootPath());
        context.put("checks", plan.checks().stream()
                .map(HarnessContext::check)
                .toList());
        context.put("notes", plan.notes());
        return Collections.unmodifiableMap(context);
    }

    private static Map<String, Object> check(HarnessCheck check) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("id", check.id());
        context.put("label", check.label());
        context.put("command", List.copyOf(check.command()));
        context.put("commandLine", check.commandLine());
        context.put("workingDirectory", check.workingDirectory());
        context.put("optional", check.optional());
        if (!check.reason().isBlank()) {
            context.put("reason", check.reason());
        }
        return Collections.unmodifiableMap(context);
    }
}
