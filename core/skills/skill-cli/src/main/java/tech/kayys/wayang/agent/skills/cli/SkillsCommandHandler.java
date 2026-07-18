package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import java.io.PrintStream;
import java.util.Objects;

public class SkillsCommandHandler {

    private final SkillsCommandServices services;
    private final SkillsCommandRenderSupport renderSupport;
    private final PrintStream out;
    private final PrintStream err;

    public SkillsCommandHandler(SkillManagementService managementService) {
        this(managementService, SkillManagementServiceConfig.defaults(), System.out, System.err);
    }

    public SkillsCommandHandler(SkillManagementService managementService, PrintStream out, PrintStream err) {
        this(managementService, SkillManagementServiceConfig.defaults(), out, err);
    }

    public SkillsCommandHandler(
            SkillManagementService managementService,
            SkillManagementServiceConfig managementConfig,
            PrintStream out,
            PrintStream err) {
        this(
                managementService,
                managementConfig,
                new SkillManagementServiceFactory(new InMemoryCliSkillRegistry()),
                out,
                err);
    }

    public SkillsCommandHandler(
            SkillManagementService managementService,
            SkillManagementServiceConfig managementConfig,
            SkillManagementServiceFactory preflightFactory,
            PrintStream out,
            PrintStream err) {
        this(SkillsCommandServices.from(managementService, managementConfig, preflightFactory), out, err);
    }

    SkillsCommandHandler(SkillsCommandServices services, PrintStream out, PrintStream err) {
        this.services = Objects.requireNonNull(services, "services");
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
        this.renderSupport = new SkillsCommandRenderSupport(this.out, this.err);
    }

    public static SkillsCommandHandler inMemory(PrintStream out, PrintStream err) {
        return new SkillsCommandHandler(
                new SkillManagementService(new InMemoryCliSkillRegistry()),
                out,
                err);
    }

    public int register(String id, String name, String description, String category, String systemPrompt) {
        return register(SkillsDefinitionRequest.fromOptions(id, name, description, category, systemPrompt));
    }

    public int register(SkillsDefinitionRequest request) {
        SkillsDefinitionCommandText.renderRegistration(
                services.definitionCommandService().register(request),
                out);
        return 0;
    }

    public int list(String category, boolean includeDisabled) {
        return list(SkillsDefinitionListRequest.fromOptions(category, includeDisabled));
    }

    public int list(SkillsDefinitionListRequest request) {
        SkillsDefinitionListText.render(services.definitionQueryService().list(request), out);
        return 0;
    }

    public int info(String skillId) {
        return info(SkillsDefinitionInfoRequest.fromOptions(skillId));
    }

    public int info(SkillsDefinitionInfoRequest request) {
        SkillsDefinitionInfoCommandReport report = services.definitionInfoCommandService().report(request);
        SkillsDefinitionInfoCommandText.render(report, out, err);
        return report.found() ? 0 : 1;
    }

    public int profiles(boolean json) {
        return renderSupport.render(
                services.profileCatalogService().catalog(),
                json,
                SkillsPersistenceProfileCatalogJson::toJson,
                SkillsPersistenceProfileCatalogText::render);
    }

    public int configExplain(boolean json) {
        return configExplain(SkillsConfigCatalogRequest.defaults(), json);
    }

    public int configExplain(boolean json, String groupName) {
        return configExplain(SkillsConfigCatalogRequest.forGroup(groupName), json);
    }

    public int configExplain(SkillsConfigCatalogRequest request, boolean json) {
        return renderSupport.renderSafely(
                () -> services.configCatalogService().catalog(request),
                json,
                SkillsConfigExplainJson::toJson,
                SkillsConfigExplainText::render);
    }

    public int configValidate(String profileName, boolean runtimeConfig, boolean json) {
        return configValidate(
                SkillsPersistenceConfigValidationRequest.fromOptions(profileName, runtimeConfig, false),
                json);
    }

    public int configValidate(
            String profileName,
            boolean runtimeConfig,
            boolean requireDurable,
            boolean json) {
        return configValidate(
                SkillsPersistenceConfigValidationRequest.fromOptions(
                        profileName,
                        runtimeConfig,
                        requireDurable),
                json);
    }

    public int configValidate(SkillsPersistenceConfigValidationRequest request, boolean json) {
        return renderSupport.renderSafely(
                () -> services.configValidationService().report(request),
                json,
                SkillsPersistenceConfigValidationJson::toJson,
                SkillsPersistenceConfigValidationText::render,
                SkillsPersistenceConfigValidationReport::passed);
    }

    public int configResolve(String profileName, boolean runtimeConfig, boolean json) {
        return configResolve(
                SkillsPersistenceConfigResolveRequest.fromOptions(profileName, runtimeConfig),
                json);
    }

    public int configResolve(SkillsPersistenceConfigResolveRequest request, boolean json) {
        return renderSupport.renderSafely(
                () -> services.configResolveService().report(request),
                json,
                SkillsPersistenceConfigResolveJson::toJson,
                SkillsPersistenceConfigResolveText::render,
                SkillsPersistenceConfigResolveReport::valid);
    }

    public int configGroups(boolean json) {
        return renderSupport.render(
                services.configCatalogService().catalog(SkillsConfigCatalogRequest.defaults()),
                json,
                SkillsConfigGroupsJson::toJson,
                SkillsConfigGroupsText::render);
    }

    public int configSample(String profileName, String formatName) {
        return configSample(profileName, formatName, false);
    }

    public int configSample(String profileName, String formatName, boolean json) {
        return configSample(SkillsConfigSampleRequest.fromOptions(profileName, formatName), json);
    }

    public int configSample(SkillsConfigSampleRequest request, boolean json) {
        return renderSupport.renderSafely(
                () -> services.configSampleService().report(request),
                json,
                report -> SkillsConfigSampleJson.toJson(report.sample()),
                (report, stream) -> SkillsConfigSampleText.render(report.sample(), report.format(), stream));
    }

    public int configSamples(boolean json) {
        return renderSupport.render(
                services.configSampleCatalogService().report(),
                json,
                SkillsConfigSamplesJson::toJson,
                SkillsConfigSamplesText::render);
    }

    public int profileInspect(
            String profileName,
            boolean json,
            boolean includePreflight,
            boolean includeDiagnostics) {
        return profileInspect(
                SkillsPersistenceProfileInspectRequest.fromOptions(
                        profileName,
                        includePreflight,
                        includeDiagnostics),
                json);
    }

    public int profileInspect(SkillsPersistenceProfileInspectRequest request, boolean json) {
        return renderSupport.renderSafely(
                () -> services.profileInspectService().report(request),
                json,
                SkillsPersistenceProfileInspectJson::toJson,
                SkillsPersistenceProfileInspectText::render);
    }

    public int validate(String id, String name, String description, String category, String systemPrompt) {
        return validate(SkillsDefinitionRequest.fromOptions(id, name, description, category, systemPrompt));
    }

    public int validate(SkillsDefinitionRequest request) {
        SkillsDefinitionValidationReport report = services.definitionCommandService().validate(request);
        SkillsDefinitionCommandText.renderValidation(report, out, err);
        return report.valid() ? 0 : 1;
    }

    public int status(String profileName, boolean runtimeConfig) {
        return status(profileName, runtimeConfig, false, false, false);
    }

    public int status(String profileName, boolean runtimeConfig, boolean json) {
        return status(profileName, runtimeConfig, json, false, false);
    }

    public int status(String profileName, boolean runtimeConfig, boolean json, boolean includePreflight) {
        return status(profileName, runtimeConfig, json, includePreflight, false);
    }

    public int status(
            String profileName,
            boolean runtimeConfig,
            boolean json,
            boolean includePreflight,
            boolean includeDiagnostics) {
        return status(SkillsPersistenceStatusRequest.fromOptions(
                profileName,
                runtimeConfig,
                includePreflight,
                includeDiagnostics), json);
    }

    public int status(SkillsPersistenceStatusRequest request, boolean json) {
        return renderSupport.renderSafely(
                () -> services.statusService().report(request),
                json,
                SkillsPersistenceStatusJson::toJson,
                SkillsPersistenceStatusText::render);
    }

    public int enable(String skillId) {
        return enable(SkillsLifecycleCommandRequest.enable(skillId));
    }

    public int enable(SkillsLifecycleCommandRequest request) {
        return lifecycle(request);
    }

    public int disable(String skillId) {
        return disable(SkillsLifecycleCommandRequest.disable(skillId));
    }

    public int disable(SkillsLifecycleCommandRequest request) {
        return lifecycle(request);
    }

    private int lifecycle(SkillsLifecycleCommandRequest request) {
        SkillsLifecycleCommandText.render(services.lifecycleCommandService().execute(request), out);
        return 0;
    }

}
