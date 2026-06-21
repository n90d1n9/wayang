package tech.kayys.wayang.tools.impl;

import tech.kayys.wayang.sdk.gollek.tools.Scanner;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class CodeScannerImpl implements Scanner {
    private final Path baseDir;

    public CodeScannerImpl(Path baseDir) { this.baseDir = baseDir; }

    @Override
    public List<Path> findFiles(String glob) throws IOException {
        if (glob == null || glob.isBlank()) glob = "**/*";
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        try (var stream = Files.walk(baseDir)) {
            return stream.filter(p -> Files.isRegularFile(p) && matcher.matches(baseDir.relativize(p)))
                    .collect(Collectors.toList());
        }
    }
}
