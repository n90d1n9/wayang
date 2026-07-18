package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.List;
import java.util.Objects;

/**
 * Result of validating a CLI skill definition.
 */
record SkillsDefinitionValidationReport(SkillValidation validation) {

    SkillsDefinitionValidationReport {
        validation = Objects.requireNonNull(validation, "validation");
    }

    boolean valid() {
        return validation.valid();
    }

    List<String> errors() {
        return validation.errors();
    }
}
