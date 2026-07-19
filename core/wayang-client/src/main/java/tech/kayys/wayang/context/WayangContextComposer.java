package tech.kayys.wayang.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class WayangContextComposer {

    public String compose(String basePrompt, Path cwd) {
        StringBuilder sb = new StringBuilder();
        if (basePrompt != null) {
            sb.append(basePrompt).append("\n\n");
        }
        appendProjectContext(sb, cwd);
        appendSkills(sb, cwd);
        return sb.toString().trim();
    }

    private void appendProjectContext(StringBuilder sb, Path cwd) {
        Path wayangProject = cwd.resolve(".wayang/project.md");
        if (Files.exists(wayangProject)) {
            sb.append("## Project Context\n\n");
            try {
                sb.append(Files.readString(wayangProject)).append("\n\n");
                return; // Prefer .wayang/project.md
            } catch (IOException ignored) {}
        }
        
        Path readme = cwd.resolve("README.md");
        if (Files.exists(readme)) {
            sb.append("## Project Context (README.md)\n\n");
            try {
                sb.append(Files.readString(readme)).append("\n\n");
            } catch (IOException ignored) {}
        }
    }

    private void appendSkills(StringBuilder sb, Path cwd) {
        Path globalSkills = Path.of(System.getProperty("user.home"), ".wayang", "skills");
        Path localSkills = cwd.resolve(".wayang/skills");
        
        boolean hasSkills = false;
        
        if (Files.isDirectory(globalSkills)) {
            hasSkills = appendSkillsDir(sb, globalSkills, hasSkills);
        }
        if (Files.isDirectory(localSkills)) {
            appendSkillsDir(sb, localSkills, hasSkills);
        }
    }

    private boolean appendSkillsDir(StringBuilder sb, Path dir, boolean hasSkillsStarted) {
        boolean started = hasSkillsStarted;
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path p : stream.filter(p -> p.toString().endsWith(".md")).toList()) {
                if (!started) {
                    sb.append("## Skills\n\n");
                    started = true;
                }
                try {
                    String content = Files.readString(p);
                    sb.append("### Skill: ").append(p.getFileName().toString().replace(".md", "")).append("\n");
                    sb.append(content).append("\n\n");
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
        return started;
    }
}
