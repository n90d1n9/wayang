package tech.kayys.wayang.sdk.gollek.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Grep {
    public static final class Match {
        public final Path file;
        public final int lineNumber;
        public final String line;
        public Match(Path file, int lineNumber, String line) {
            this.file = file; this.lineNumber = lineNumber; this.line = line;
        }
    }

    List<Match> grep(String regex, List<Path> files) throws IOException;
}
