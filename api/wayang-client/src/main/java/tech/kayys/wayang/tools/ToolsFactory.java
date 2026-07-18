package tech.kayys.wayang.tools;

import java.nio.file.Path;

public interface ToolsFactory {
    Scanner createScanner(Path baseDir);
    Grep createGrep(Path baseDir);
    PlannerIface createPlanner();
    TaskStoreIface createTaskStore(Path projectDir);
}
