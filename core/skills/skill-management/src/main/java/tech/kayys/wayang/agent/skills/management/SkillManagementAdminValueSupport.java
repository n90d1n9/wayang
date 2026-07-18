package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Shared normalization helpers for stable admin-facing DTOs.
 */
final class SkillManagementAdminValueSupport {

    private static final String UNKNOWN = "UNKNOWN";

    private SkillManagementAdminValueSupport() {
    }

    static String text(String value) {
        return SkillManagementValueSupport.text(value);
    }

    static String identifier(String value) {
        return SkillManagementValueSupport.identifier(value);
    }

    static String blankToEmpty(String value) {
        return SkillManagementValueSupport.blankToEmpty(value);
    }

    static String unknownIfBlank(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value;
    }

    static String action(String action) {
        return unknownIfBlank(action);
    }

    static boolean changedForSyncAction(String action, boolean fallback) {
        return switch (action) {
            case "COPIED", "UPDATED", "DELETED" -> true;
            case "UNCHANGED", "CONFLICT" -> false;
            default -> fallback;
        };
    }

    static int nonNegative(int value) {
        return SkillManagementValueSupport.nonNegative(value);
    }

    static long nonNegative(long value) {
        return SkillManagementValueSupport.nonNegative(value);
    }

    static int atLeast(int value, int minimum) {
        return SkillManagementValueSupport.atLeast(value, minimum);
    }

    static Map<String, Integer> nonNegativeCounts(Map<String, Integer> values) {
        return SkillManagementValueSupport.nonNegativeCounts(values);
    }

    static boolean booleanAttribute(Map<String, String> attributes, String name) {
        return SkillManagementValueSupport.booleanAttribute(attributes, name);
    }

    static int nonNegativeIntAttribute(Map<String, String> attributes, String name) {
        return SkillManagementValueSupport.nonNegativeIntAttribute(attributes, name);
    }

    static boolean hasAttributePrefix(Map<String, String> attributes, String prefix) {
        return SkillManagementValueSupport.hasAttributePrefix(attributes, prefix);
    }

    static List<String> compactStrings(List<String> values) {
        return SkillManagementValueSupport.compactStrings(values);
    }

    static List<String> sortedDistinctStrings(List<String> values) {
        return SkillManagementValueSupport.sortedDistinctStrings(values);
    }

    static <T> List<T> nonNullList(List<T> values) {
        return SkillManagementValueSupport.nonNullList(values);
    }

    static String joinedMessage(List<String> values) {
        return SkillManagementValueSupport.joinedMessage(values);
    }

    static List<String> validationErrors(SkillManagementAdminValidationReport... reports) {
        return Stream.of(reports)
                .flatMap(report -> report.errors().stream())
                .toList();
    }

    static <T> long countAction(List<T> values, Function<T, String> actionExtractor, String action) {
        return SkillManagementValueSupport.countBy(values, actionExtractor, action);
    }

    static <T> int countMatching(List<T> values, Predicate<T> predicate) {
        return (int) SkillManagementValueSupport.countMatching(values, predicate);
    }
}
