package tech.kayys.wayang.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple file scanner utility: list files by glob under a base directory.
 */
public final class CodeScanner {
    private final Path baseDir;

    public CodeScanner(Path baseDir) {
        this.baseDir = baseDir;
    }

    public List<Path> findFiles(String glob) throws IOException {
        if (glob == null || glob.isBlank()) glob = "**/*";
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        try (var stream = Files.walk(baseDir)) {
            return stream.filter(p -> Files.isRegularFile(p) && matcher.matches(baseDir.relativize(p)))
                    .collect(Collectors.toList());
        }
    }
}
