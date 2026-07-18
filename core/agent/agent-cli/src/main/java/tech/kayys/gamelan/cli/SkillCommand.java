package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import tech.kayys.gamelan.agent.skill.Skill;
import tech.kayys.gamelan.agent.skill.SkillRegistry;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Skill management commands — install, remove, list, show, enable, disable.
 *
 * <pre>
 * Usage:
 *   gamelan skill list                    # List all skills
 *   gamelan skill show read-file          # Show skill details
 *   gamelan skill install /path/to/skill  # Install from directory
 *   gamelan skill enable read-file        # Enable a skill
 *   gamelan skill disable shell           # Disable a skill
 *   gamelan skill remove old-skill        # Remove installed skill
 * </pre>
 */
@Command(
    name = "skill",
    description = "Manage skills (install, remove, list, enable, disable)",
    mixinStandardHelpOptions = true,
    subcommands = {
        SkillListCommand.class,
        SkillShowCommand.class,
        SkillInstallCommand.class,
        SkillRemoveCommand.class,
        SkillEnableCommand.class,
        SkillDisableCommand.class
    }
)
public class SkillCommand implements Runnable {
    @Override
    public void run() { new SkillListCommand().run(); }
}

@Command(name = "list", aliases = {"ls", "l"}, description = "List all available skills")
class SkillListCommand implements Runnable {

    @Inject SkillRegistry skillRegistry;

    @Option(names = {"--enabled"}, description = "Show only enabled skills")
    boolean enabledOnly;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean jsonOutput;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        List<Skill> skills = enabledOnly ? skillRegistry.listEnabled() : skillRegistry.listAll();

        if (jsonOutput) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                System.out.println(mapper.writeValueAsString(skills));
            } catch (Exception e) { System.out.println("[]"); }
            return;
        }

        printer.header("Skills", skills.size() + " total");
        printer.println();

        skills.stream()
            .collect(java.util.stream.Collectors.groupingBy(s -> s.source() != null ? s.source() : "unknown"))
            .forEach((source, skillList) -> {
                printer.section("Source: " + source);
                skillList.forEach(skill -> {
                    String status = skill.enabled() ? "✓" : "✗";
                    printer.info(String.format("  %s %-20s - %s", status, skill.name(), skill.description()));
                });
                printer.println();
            });

        printer.info("Tip: Use 'gamelan skill show <name>' for details");
    }
}

@Command(name = "show", aliases = {"info", "s"}, description = "Show detailed skill information")
class SkillShowCommand implements Runnable {

    @Inject SkillRegistry skillRegistry;

    @Parameters(index = "0", description = "Skill name", paramLabel = "SKILL")
    String skillName;

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean jsonOutput;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        Skill skill = skillRegistry.find(skillName).orElse(null);
        if (skill == null) {
            printer.error("Skill not found: " + skillName);
            printer.info("Use 'gamelan skill list' to see available skills");
            return;
        }

        if (jsonOutput) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                System.out.println(mapper.writeValueAsString(skill));
            } catch (Exception e) { System.out.println("{}"); }
            return;
        }

        printer.header("Skill: " + skill.name());
        printer.println();
        printer.info("Description: " + skill.description());
        printer.info("Version:     " + (skill.version() != null ? skill.version() : "unknown"));
        printer.info("Source:      " + skill.source());
        printer.info("Enabled:     " + (skill.enabled() ? "Yes" : "No"));
        printer.info("Path:        " + skill.path());

        if (skill.commands() != null && !skill.commands().isEmpty()) {
            printer.println();
            printer.section("Commands");
            skill.commands().forEach(cmd -> printer.info("  • " + cmd));
        }

        if (skill.dependencies() != null && !skill.dependencies().isEmpty()) {
            printer.println();
            printer.section("Dependencies");
            skill.dependencies().forEach(dep -> printer.info("  • " + dep));
        }
    }
}

@Command(name = "install", aliases = {"add", "i"}, description = "Install a skill from directory or URL")
class SkillInstallCommand implements Runnable {

    @Inject SkillRegistry skillRegistry;

    @Parameters(index = "0", description = "Skill path or URL", paramLabel = "PATH")
    String skillPath;

    @Option(names = {"--global"}, description = "Install to user-global directory (~/.gamelan/skills/)")
    boolean global;

    @Option(names = {"--name"}, description = "Override skill name", paramLabel = "NAME")
    String overrideName;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        Path targetDir = global
            ? Path.of(System.getProperty("user.home"), ".gamelan", "skills")
            : Path.of(".gamelan", "skills");

        printer.info("Installing skill from: " + skillPath);
        try {
            Path sourcePath = Path.of(skillPath);
            if (Files.exists(sourcePath)) {
                Skill installed = skillRegistry.install(sourcePath, targetDir, overrideName);
                printer.success("Installed skill: " + installed.name());
                printer.info("Location: " + targetDir.resolve(installed.name()));
            } else {
                printer.error("Remote skill installation not yet implemented");
                printer.info("Please download the skill manually and install from local path");
            }
        } catch (Exception e) {
            printer.error("Failed to install skill: " + e.getMessage());
            System.exit(1);
        }
    }
}

@Command(name = "remove", aliases = {"rm", "delete", "r"}, description = "Remove an installed skill")
class SkillRemoveCommand implements Runnable {

    @Inject SkillRegistry skillRegistry;

    @Parameters(index = "0", description = "Skill name to remove", paramLabel = "SKILL")
    String skillName;

    @Option(names = {"-y", "--yes"}, description = "Skip confirmation")
    boolean skipConfirm;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        if (!skipConfirm) printer.warn("This will remove skill: " + skillName);
        try {
            boolean removed = skillRegistry.remove(skillName);
            if (removed) printer.success("Removed skill: " + skillName);
            else printer.error("Skill not found: " + skillName);
        } catch (Exception e) {
            printer.error("Failed to remove skill: " + e.getMessage());
            System.exit(1);
        }
    }
}

@Command(name = "enable", aliases = {"e"}, description = "Enable a skill")
class SkillEnableCommand implements Runnable {

    @Inject SkillRegistry skillRegistry;

    @Parameters(index = "0", description = "Skill name", paramLabel = "SKILL")
    String skillName;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        try {
            skillRegistry.enable(skillName);
            printer.success("Enabled skill: " + skillName);
        } catch (Exception e) {
            printer.error("Failed to enable skill: " + e.getMessage());
            System.exit(1);
        }
    }
}

@Command(name = "disable", aliases = {"d"}, description = "Disable a skill")
class SkillDisableCommand implements Runnable {

    @Inject SkillRegistry skillRegistry;

    @Parameters(index = "0", description = "Skill name", paramLabel = "SKILL")
    String skillName;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        try {
            skillRegistry.disable(skillName);
            printer.success("Disabled skill: " + skillName);
        } catch (Exception e) {
            printer.error("Failed to disable skill: " + e.getMessage());
            System.exit(1);
        }
    }
}
