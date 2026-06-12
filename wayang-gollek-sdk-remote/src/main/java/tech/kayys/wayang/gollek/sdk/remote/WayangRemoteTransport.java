package tech.kayys.wayang.gollek.sdk.remote;

import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunPreview;
import tech.kayys.wayang.gollek.sdk.AgentRunResult;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscovery;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscoveryService;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.HarnessPlan;
import tech.kayys.wayang.gollek.sdk.HarnessPlanRequest;
import tech.kayys.wayang.gollek.sdk.ProductSurface;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;
import tech.kayys.wayang.gollek.sdk.SkillRegistry;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangSkillCatalog;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

import java.util.List;

public interface WayangRemoteTransport {

    WayangPlatformStatus status();

    List<ProductSurface> productSurfaces();

    WayangWorkbenchModel workbench();

    default SkillRegistry skillRegistry() {
        return WayangSkillCatalog.defaultRegistry();
    }

    default RegisteredSkill skill(String skillId) {
        return skillRegistry().require(skillId);
    }

    default AgentSkillDiscovery skillDiscovery(AgentSkillQuery query, String search) {
        return AgentSkillDiscoveryService.create().discover(skillRegistry(), query, search);
    }

    WorkspaceSnapshot inspectWorkspace(WorkspaceInspectionRequest request);

    HarnessPlan planHarness(HarnessPlanRequest request);

    AgentRunPreview previewRun(AgentRunRequest request);

    AgentRunResult run(AgentRunRequest request);

    AgentRunStatus runStatus(String runId);

    AgentRunHistory runHistory(AgentRunHistoryQuery query);

    AgentRunEvents runEvents(String runId);

    default AgentRunEvents runEvents(String runId, AgentRunEventsQuery query) {
        return runEvents(runId);
    }

    AgentRunCancelResult cancelRun(String runId, String reason);

    default AgentRunForgetResult forgetRun(String runId) {
        return AgentRunForgetResult.notFound(
                runId,
                "Remote Wayang run forget is not configured for this transport.");
    }
}
