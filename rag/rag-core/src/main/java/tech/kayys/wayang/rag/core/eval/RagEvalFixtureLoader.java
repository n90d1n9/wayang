package tech.kayys.wayang.rag.core.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RagEvalFixtureLoader {

    private RagEvalFixtureLoader() {
    }

    /**
     * TSV format: query<TAB>chunkId1,chunkId2,...
     * Lines starting with '#' are ignored.
     */
    public static List<RagEvalCase> loadTsv(Path path) {
        try {
            List<RagEvalCase> cases = new ArrayList<>();
            List<String> lines = Files.readAllLines(path);
            int lineNo = 0;
            for (String line : lines) {
                lineNo++;
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int tabIndex = trimmed.indexOf('\t');
                if (tabIndex <= 0 || tabIndex == trimmed.length() - 1) {
                    throw new IllegalArgumentException("Invalid eval fixture format at line " + lineNo + ": " + line);
                }
                String query = trimmed.substring(0, tabIndex).trim();
                String[] ids = trimmed.substring(tabIndex + 1).split(",");
                List<String> relevantIds = new ArrayList<>(ids.length);
                for (String id : ids) {
                    String value = id == null ? "" : id.trim();
                    if (!value.isEmpty()) {
                        relevantIds.add(value);
                    }
                }
                cases.add(new RagEvalCase(query, relevantIds));
            }
            return List.copyOf(cases);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load eval fixtures: " + path, e);
        }
    }
}
