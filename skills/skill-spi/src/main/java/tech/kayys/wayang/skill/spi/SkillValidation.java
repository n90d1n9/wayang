package tech.kayys.wayang.skill.spi;

import java.util.Collections;
import java.util.List;

/**
 * Result of a skill parameter or state validation.
 */
public record SkillValidation(boolean valid, List<String> errors) {
    
    public static SkillValidation success() {
        return new SkillValidation(true, Collections.emptyList());
    }

    public static SkillValidation error(String error) {
        return new SkillValidation(false, Collections.singletonList(error));
    }

    public static SkillValidation errors(List<String> errors) {
        return new SkillValidation(false, List.copyOf(errors));
    }
}
