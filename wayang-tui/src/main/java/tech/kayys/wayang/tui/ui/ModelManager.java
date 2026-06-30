package tech.kayys.wayang.tui.ui;

import java.io.PrintStream;
import java.util.List;

public interface ModelManager {
    record ModelRow(String shortId, String name, String format, String sizeStr) {}
    
    List<ModelRow> listModels();
    
    int pullModel(PrintStream out, String modelSpec);
}
