package tech.kayys.wayang.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Scanner {
    List<Path> findFiles(String glob) throws IOException;
}
