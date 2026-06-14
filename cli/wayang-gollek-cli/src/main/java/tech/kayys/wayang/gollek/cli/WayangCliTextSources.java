package tech.kayys.wayang.gollek.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class WayangCliTextSources {

    private WayangCliTextSources() {
    }

    static String required(
            String label,
            String inlineText,
            String filePath,
            boolean readStdin,
            InputStream in) {
        return required(label, inlineText, filePath, readStdin, in, "");
    }

    static String required(
            String label,
            String inlineText,
            String filePath,
            boolean readStdin,
            InputStream in,
            String fallbackText) {
        String resolved = resolve(label, inlineText, filePath, readStdin, in, true, fallbackText);
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(label + " content is empty.");
        }
        return resolved;
    }

    static String optional(String label, String inlineText, String filePath, String fallbackText) {
        return resolve(label, inlineText, filePath, false, InputStream.nullInputStream(), false, fallbackText);
    }

    private static String resolve(
            String label,
            String inlineText,
            String filePath,
            boolean readStdin,
            InputStream in,
            boolean required,
            String fallbackText) {
        boolean hasInline = inlineText != null && !inlineText.isBlank();
        boolean hasFile = filePath != null && !filePath.isBlank();
        int sources = (hasInline ? 1 : 0) + (hasFile ? 1 : 0) + (readStdin ? 1 : 0);
        if (sources == 0) {
            String fallback = fallbackText == null ? "" : fallbackText;
            if (!fallback.isBlank()) {
                return fallback;
            }
            if (required) {
                throw new IllegalArgumentException(label + " is required. Provide inline text, --prompt-file, or --stdin.");
            }
            return "";
        }
        if (sources > 1) {
            throw new IllegalArgumentException(label + " must come from only one source.");
        }
        if (hasFile) {
            return readFile(label, filePath);
        }
        if (readStdin) {
            return readStdin(label, in);
        }
        return inlineText;
    }

    private static String readFile(String label, String filePath) {
        try {
            return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read " + label.toLowerCase() + " file: " + filePath, e);
        }
    }

    private static String readStdin(String label, InputStream in) {
        try {
            InputStream source = in == null ? InputStream.nullInputStream() : in;
            return new String(source.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read " + label.toLowerCase() + " from stdin.", e);
        }
    }
}
