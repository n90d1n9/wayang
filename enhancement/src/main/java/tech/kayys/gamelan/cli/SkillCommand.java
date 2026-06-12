package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gamelan.skill.SkillValidator;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Skill management subcommand.
 *
 * <p>Skills are directories following the
 * <a href="https://agentskills.io/specification">Agent Skills specification</a>.
 * Each skill contains a {@code SKILL.md} with YAML frontmatter and Markdown instructions,
 * plus optional {@code scripts/}, {@code references/}, and {@code assets/} directories.
 *
 * <pre>
 * Usage:
 *   gamelan skill list                    # List all installed skills
 *   gamelan skill show read-file          # Show full SKILL.md content
 *   gamelan skill install ./my-skill/     # Install a skill from local path
 *   gamelan skill install https://github.com/org/skills/tree/main/my-skill
 *   gamelan skill remove read-file        # Uninstall a skill
 *   gamelan skill validate ./my-skill/    # Validate SKILL.md format
 *   gamelan skill run read-file --args "path=README.md"
 * </pre>
 */
@Command(
    name = "skill",
    description = "Manage Agent Skills (agentskills.io specification)",
    mixinStandardHelpOptions = true,
    subcommands = {
        SkillCommand.ListCmd.class,
        SkillCommand.ShowCmd.class,
        SkillCommand.InstallCmd.class,
        SkillCommand.RemoveCmd.class,
        SkillCommand.ValidateCmd.class,
        SkillCommand.RunCmd.class,
        SkillCommand.VerifyCmd.class
    }
)
public class SkillCommand implements Runnable {

    @Override
    public void run() {
        new picocli.CommandLine(this).usage(System.out);
    }

    // ── skill list ─────────────────────────────────────────────────────────

    @Command(name = "list", description = "List all installed skills", aliases = {"ls"})
    static class ListCmd implements Runnable {

        @Inject
        SkillRegistry registry;

        @Option(names = {"-v", "--verbose"}, description = "Show full metadata")
        boolean verbose;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            List<Skill> skills = registry.listAll();

            if (skills.isEmpty()) {
                printer.warn("No skills installed. Run: gamelan skill install <path>");
                return;
            }

            printer.sectionHeader("Installed Skills (" + skills.size() + ")");
            for (Skill skill : skills) {
                if (verbose) {
                    printer.println("@|bold " + skill.name() + "|@");
                    printer.println("  Description : " + skill.description());
                    printer.println("  License     : " + skill.license());
                    printer.println("  Compat.     : " + skill.compatibility());
                    printer.println("  Scripts     : " + skill.scriptCount());
                    printer.println("  References  : " + skill.referenceCount());
                    printer.println();
                } else {
                    printer.listItem(skill.name(), skill.description());
                }
            }
        }
    }

    // ── skill show ─────────────────────────────────────────────────────────

    @Command(name = "show", description = "Show skill details and SKILL.md content")
    static class ShowCmd implements Runnable {

        @Inject
        SkillRegistry registry;

        @Parameters(index = "0", description = "Skill name")
        String skillName;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            Skill skill = registry.find(skillName)
                    .orElse(null);
            if (skill == null) {
                printer.error("Skill not found: " + skillName);
                System.exit(1);
            }
            printer.sectionHeader("Skill: " + skill.name());
            printer.println(skill.rawContent());
        }
    }

    // ── skill install ──────────────────────────────────────────────────────

    @Command(name = "install", description = "Install a skill from local path or GitHub URL")
    static class InstallCmd implements Runnable {

        @Inject
        SkillRegistry registry;

        @Parameters(index = "0", description = "Local path or GitHub URL")
        String source;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            try {
                Skill skill = registry.install(source);
                printer.success("Installed skill: " + skill.name());
            } catch (Exception e) {
                printer.error("Install failed: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // ── skill remove ───────────────────────────────────────────────────────

    @Command(name = "remove", description = "Remove an installed skill", aliases = {"rm", "uninstall"})
    static class RemoveCmd implements Runnable {

        @Inject
        SkillRegistry registry;

        @Parameters(index = "0", description = "Skill name")
        String skillName;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            try {
                registry.remove(skillName);
                printer.success("Removed skill: " + skillName);
            } catch (Exception e) {
                printer.error("Remove failed: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // ── skill validate ─────────────────────────────────────────────────────

    @Command(name = "validate", description = "Validate a skill directory against the spec")
    static class ValidateCmd implements Runnable {

        @Inject
        SkillValidator validator;

        @Parameters(index = "0", description = "Path to skill directory")
        Path skillPath;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            printer.sectionHeader("Validating: " + skillPath);
            List<String> errors = validator.validate(skillPath);
            if (errors.isEmpty()) {
                printer.success("Skill is valid ✓");
            } else {
                errors.forEach(e -> printer.error("  ✗ " + e));
                System.exit(1);
            }
        }
    }

    // ── skill run ──────────────────────────────────────────────────────────

    @Command(name = "run", description = "Directly invoke a skill (without LLM routing)")
    static class RunCmd implements Runnable {

        @Inject
        SkillRegistry registry;

        @Parameters(index = "0", description = "Skill name")
        String skillName;

        @Option(names = {"--args"}, description = "Skill arguments as key=value pairs",
                paramLabel = "KEY=VALUE", split = ",")
        java.util.Map<String, String> args;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            Skill skill = registry.find(skillName).orElse(null);
            if (skill == null) {
                printer.error("Skill not found: " + skillName);
                System.exit(1);
            }
            try {
                String result = skill.invoke(args != null ? args : java.util.Map.of());
                printer.println(result);
            } catch (Exception e) {
                printer.error("Skill execution failed: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // ── skill verify ───────────────────────────────────────────────────────

    @Command(name = "verify", description = "Verify skill integrity (supply-chain security)")
    static class VerifyCmd implements Runnable {
        @Inject tech.kayys.gamelan.skill.SkillRegistry registry;
        @Inject SkillVerifier verifier;
        @Inject tech.kayys.gamelan.config.GamelanConfig config;

        @Parameters(index = "0", arity = "0..1",
                    description = "Skill name to verify (omit to verify all)")
        String skillName;

        @Override
        public void run() {
            tech.kayys.gamelan.util.AnsiPrinter p =
                    new tech.kayys.gamelan.util.AnsiPrinter(config.color());

            var skills = skillName != null
                    ? registry.find(skillName).map(java.util.List::of).orElse(java.util.List.of())
                    : registry.listAll();

            if (skills.isEmpty()) {
                p.warn(skillName != null
                        ? "Skill not found: " + skillName
                        : "No skills installed.");
                return;
            }

            p.sectionHeader("Skill Verification (" + skills.size() + ")");
            int blocked = 0;
            for (tech.kayys.gamelan.skill.Skill skill : skills) {
                var result = verifier.verify(skill);
                String icon = switch (result.tier()) {
                    case TRUSTED   -> "✓";
                    case UNVERIFIED -> "?";
                    case TAMPERED  -> "⚠";
                    case BLOCKED   -> "✗";
                };
                String label = icon + " [" + result.tier() + "] " + skill.name();
                if (result.passed()) {
                    p.info(label);
                } else {
                    p.error(label);
                    result.findings().forEach(f -> p.error("    " + f));
                    blocked++;
                }
            }
            if (blocked > 0) {
                p.error(blocked + " skill(s) failed verification — do not use them.");
                System.exit(1);
            } else {
                p.success("All " + skills.size() + " skill(s) passed verification.");
            }
        }
    }

}
