package tech.kayys.wayang.tools.impl;

import tech.kayys.wayang.sdk.gollek.tools.ToolsFactory;
import tech.kayys.wayang.sdk.gollek.tools.Scanner;
import tech.kayys.wayang.sdk.gollek.tools.Grep;
import tech.kayys.wayang.sdk.gollek.tools.PlannerIface;
import tech.kayys.wayang.sdk.gollek.tools.TaskStoreIface;

import java.nio.file.Path;

public class ToolsFactoryImpl implements ToolsFactory {
    @Override
    public Scanner createScanner(Path baseDir) {
        return new CodeScannerImpl(baseDir);
    }

    @Override
    public Grep createGrep(Path baseDir) {
        return new CodeGrepImpl(baseDir);
    }

    @Override
    public PlannerIface createPlanner() {
        return new PlannerImpl();
    }

    @Override
    public TaskStoreIface createTaskStore(Path projectDir) {
        return new TaskStoreImpl(projectDir);
    }
}
