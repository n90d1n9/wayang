package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementFileStoreSupportTest {

    @Test
    void listsSortedMatchingRegularFiles(@TempDir Path tempDir) throws Exception {
        Path nested = Files.createDirectory(tempDir.resolve("nested"));
        Path second = Files.writeString(tempDir.resolve("b.properties"), "b", StandardCharsets.UTF_8);
        Path first = Files.writeString(tempDir.resolve("a.properties"), "a", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("ignore.txt"), "x", StandardCharsets.UTF_8);

        assertThat(SkillManagementFileStoreSupport.regularFiles(
                tempDir,
                path -> path.getFileName().toString().endsWith(".properties"),
                "test files"))
                .containsExactly(first, second)
                .doesNotContain(nested);
        assertThat(SkillManagementFileStoreSupport.regularFiles(
                tempDir.resolve("missing"),
                path -> true,
                "missing files"))
                .isEmpty();
    }

    @Test
    void listsSortedMatchingRegularFilesRecursively(@TempDir Path tempDir) throws Exception {
        Path nested = Files.createDirectories(tempDir.resolve("nested/deeper"));
        Path second = Files.writeString(tempDir.resolve("b.properties"), "b", StandardCharsets.UTF_8);
        Path first = Files.writeString(nested.resolve("a.properties"), "a", StandardCharsets.UTF_8);
        Files.writeString(nested.resolve("ignore.txt"), "x", StandardCharsets.UTF_8);

        assertThat(SkillManagementFileStoreSupport.regularFilesRecursively(
                tempDir,
                path -> path.getFileName().toString().endsWith(".properties"),
                "recursive test files"))
                .containsExactly(second, first);
        assertThat(SkillManagementFileStoreSupport.regularFilesRecursively(
                tempDir.resolve("missing"),
                path -> true,
                "missing recursive files"))
                .isEmpty();
    }

    @Test
    void writesReadsPropertiesAndDeletesFiles(@TempDir Path tempDir) {
        Path directory = tempDir.resolve("state");
        Path path = directory.resolve("sample.properties");
        Properties properties = new Properties();
        properties.setProperty("skillId", "planner");

        SkillManagementFileStoreSupport.ensureDirectory(directory, "create state directory");
        SkillManagementFileStoreSupport.writeUtf8Properties(path, properties, "test", "write properties");

        assertThat(SkillManagementFileStoreSupport.readUtf8Properties(path, "read properties"))
                .containsEntry("skillId", "planner");
        assertThat(SkillManagementFileStoreSupport.readAllBytes(path, "read bytes")).isNotEmpty();
        assertThat(SkillManagementFileStoreSupport.deleteIfExists(path, "delete properties")).isTrue();
        assertThat(SkillManagementFileStoreSupport.deleteIfExists(path, "delete properties")).isFalse();
    }
}
