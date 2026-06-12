package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsDirective;

/**
 * Maps diagnostics request parameters to Hermes runtime directives.
 */
final class HermesDiagnosticsDirectiveMapper {

    private final HermesDiagnosticsViewMapper viewMapper = new HermesDiagnosticsViewMapper();

    HermesRuntimeDiagnosticsDirective directive(HermesDiagnosticsRequest request) {
        return HermesRuntimeDiagnosticsDirective.inspect(viewMapper.view(request));
    }

    HermesRuntimeDiagnosticsDirective capabilities() {
        return HermesRuntimeDiagnosticsDirective.inspect(viewMapper.capabilities());
    }

    HermesRuntimeDiagnosticsDirective lifecycle() {
        return HermesRuntimeDiagnosticsDirective.inspect(viewMapper.lifecycle());
    }

    HermesRuntimeDiagnosticsDirective runtimePorts() {
        return HermesRuntimeDiagnosticsDirective.inspect(viewMapper.runtimePorts());
    }

    HermesRuntimeDiagnosticsDirective skillPersistence() {
        return HermesRuntimeDiagnosticsDirective.inspect(viewMapper.skillPersistence());
    }

    HermesRuntimeDiagnosticsDirective learningAudit() {
        return HermesRuntimeDiagnosticsDirective.inspect(viewMapper.learningAudit());
    }
}
