package tech.kayys.wayang.agent.skills.management;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Shared filesystem operations for file-backed skill-management stores.
 */
final class SkillManagementFileStoreSupport {

    private SkillManagementFileStoreSupport() {
    }

    static void ensureDirectory(Path directory, String failureMessage) {
        try {
            Files.createDirectories(Objects.requireNonNull(directory, "directory"));
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage + ": " + directory, error);
        }
    }

    static List<Path> regularFiles(Path directory, Predicate<Path> include, String description) {
        Objects.requireNonNull(directory, "directory");
        Predicate<Path> resolvedInclude = include == null ? path -> true : include;
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(resolvedInclude)
                    .sorted()
                    .toList();
        } catch (IOException error) {
            throw new IllegalStateException("Failed to list " + description + " under " + directory, error);
        }
    }

    static List<Path> regularFilesRecursively(Path directory, Predicate<Path> include, String description) {
        Objects.requireNonNull(directory, "directory");
        Predicate<Path> resolvedInclude = include == null ? path -> true : include;
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(resolvedInclude)
                    .sorted()
                    .toList();
        } catch (IOException error) {
            throw new IllegalStateException("Failed to list " + description + " under " + directory, error);
        }
    }

    static boolean isRegularFile(Path path) {
        return Files.isRegularFile(Objects.requireNonNull(path, "path"));
    }

    static byte[] readAllBytes(Path path, String failureMessage) {
        try {
            return Files.readAllBytes(Objects.requireNonNull(path, "path"));
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage + ": " + path, error);
        }
    }

    static void writeBytes(Path path, byte[] content, String failureMessage) {
        try {
            Files.write(Objects.requireNonNull(path, "path"), Objects.requireNonNull(content, "content"));
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage + ": " + path, error);
        }
    }

    static Properties readUtf8Properties(Path path, String failureMessage) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Objects.requireNonNull(path, "path"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage + ": " + path, error);
        }
        return properties;
    }

    static void writeUtf8Properties(
            Path path,
            Properties properties,
            String comments,
            String failureMessage) {
        try (Writer writer = Files.newBufferedWriter(Objects.requireNonNull(path, "path"), StandardCharsets.UTF_8)) {
            Objects.requireNonNull(properties, "properties").store(writer, comments);
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage + ": " + path, error);
        }
    }

    static boolean deleteIfExists(Path path, String failureMessage) {
        try {
            return Files.deleteIfExists(Objects.requireNonNull(path, "path"));
        } catch (IOException error) {
            throw new IllegalStateException(failureMessage + ": " + path, error);
        }
    }
}
