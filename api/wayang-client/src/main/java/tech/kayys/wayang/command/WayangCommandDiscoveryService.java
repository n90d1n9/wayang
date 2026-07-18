package tech.kayys.wayang.command;

import java.util.List;

import tech.kayys.wayang.workbench.WorkbenchCommand;
import tech.kayys.wayang.workbench.WorkbenchCommandDiscovery;
import tech.kayys.wayang.workbench.WorkbenchCommandIndex;
import tech.kayys.wayang.workbench.WorkbenchCommandQuery;
import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.WayangWorkbenchModel;

public final class WayangCommandDiscoveryService {

    private static final WayangCommandDiscoveryService INSTANCE = new WayangCommandDiscoveryService();

    private WayangCommandDiscoveryService() {
    }

    public static WayangCommandDiscoveryService create() {
        return INSTANCE;
    }

    public List<WorkbenchCommand> discover(WayangWorkbenchModel workbench, WorkbenchCommandQuery query) {
        return discover(workbench == null ? List.of() : workbench.commands(), query);
    }

    public WorkbenchCommandDiscovery commandDiscovery(WayangWorkbenchModel workbench, WorkbenchCommandQuery query) {
        return commandDiscovery(workbench == null ? List.of() : workbench.commands(), query);
    }

    public WorkbenchCommandDiscovery commandDiscovery(List<WorkbenchCommand> commands, WorkbenchCommandQuery query) {
        WorkbenchCommandQuery normalized = query == null ? WorkbenchCommandQuery.all() : query;
        List<WorkbenchCommand> source = SdkLists.copy(commands);
        return WorkbenchCommandDiscovery.of(normalized, discover(source, normalized), source.size());
    }

    public WayangWorkbenchModel filterWorkbench(WayangWorkbenchModel workbench, WorkbenchCommandQuery query) {
        WayangWorkbenchModel source = workbench == null ? new WayangWorkbenchModel(null, List.of(), List.of(), List.of()) : workbench;
        WorkbenchCommandQuery normalized = query == null ? WorkbenchCommandQuery.all() : query;
        if (!normalized.filtered()) {
            return source;
        }
        return new WayangWorkbenchModel(
                source.status(),
                source.productSurfaces(),
                List.of(),
                discover(source.commands(), normalized),
                source.nextActions());
    }

    public List<WorkbenchCommand> discover(List<WorkbenchCommand> commands, WorkbenchCommandQuery query) {
        WorkbenchCommandQuery normalized = query == null ? WorkbenchCommandQuery.all() : query;
        return WorkbenchCommandIndex.of(commands).commandsForQuery(normalized);
    }
}
