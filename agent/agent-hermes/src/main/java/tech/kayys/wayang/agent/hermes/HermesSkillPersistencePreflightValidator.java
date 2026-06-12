package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates Hermes learned-skill persistence routes before store creation.
 */
final class HermesSkillPersistencePreflightValidator {

    private HermesSkillPersistencePreflightValidator() {
    }

    static List<HermesSkillPersistenceValidationIssue> validate(
            HermesSkillPersistenceTargetPlan targetPlan,
            boolean dataSourceRequired,
            boolean dataSourceAvailable,
            boolean objectStorageRequired,
            boolean objectStorageAvailable) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        List<HermesSkillPersistenceValidationIssue> issues = new ArrayList<>();
        validateTarget(effectivePlan.definitions(), issues);
        validateTarget(effectivePlan.artifacts(), issues);
        if (dataSourceRequired && !dataSourceAvailable) {
            issues.add(HermesSkillPersistenceValidationIssue.error(
                    "runtime-resource",
                    "database",
                    "database",
                    "Database learned-skill persistence requires a DataSource",
                    "Provide a DataSource bean or switch the Hermes definitions/artifacts store to file-system or skill-management"));
        }
        if (objectStorageRequired && !objectStorageAvailable) {
            issues.add(HermesSkillPersistenceValidationIssue.error(
                    "runtime-resource",
                    "object-storage",
                    "object-storage",
                    "Object-storage learned-skill persistence requires an ObjectStorageService",
                    "Provide an ObjectStorageService bean for S3/RustFS-compatible storage or switch artifacts to file-system"));
        }
        return List.copyOf(issues);
    }

    private static void validateTarget(
            HermesSkillPersistenceTarget target,
            List<HermesSkillPersistenceValidationIssue> issues) {
        if (target == null) {
            issues.add(HermesSkillPersistenceValidationIssue.error(
                    "custom",
                    "unknown",
                    "none",
                    "Learned-skill persistence target is missing",
                    "Configure supported Hermes definitions and artifacts stores"));
            return;
        }
        if (!target.ready()) {
            issues.add(HermesSkillPersistenceValidationIssue.error(
                    target.purpose(),
                    "none",
                    "none",
                    "No compatible learned-skill persistence backend selected for " + target.purpose(),
                    "Configure " + target.purpose() + " to use skill-management, file-system, database, S3, RustFS, MinIO, GCS, or Azure Blob"));
        }
        target.activeProfiles().forEach(profile -> validateProfile(target.purpose(), profile, issues));
    }

    private static void validateProfile(
            String purpose,
            HermesSkillPersistenceBackendProfile profile,
            List<HermesSkillPersistenceValidationIssue> issues) {
        if (profile == null) {
            return;
        }
        String family = profile.storageFamily();
        if ("custom".equals(family)) {
            issues.add(HermesSkillPersistenceValidationIssue.error(
                    purpose,
                    profile.store(),
                    profile.storeType(),
                    "Unsupported learned-skill persistence store '" + profile.store() + "' for " + purpose,
                    "Use a supported store alias or register a concrete persistence adapter before enabling this store"));
        } else if ("hybrid".equals(family)) {
            issues.add(HermesSkillPersistenceValidationIssue.error(
                    purpose,
                    profile.store(),
                    profile.storeType(),
                    "Ambiguous learned-skill persistence store '" + profile.store() + "' for " + purpose,
                    "Configure concrete definitions, artifacts, cloudStores, and fallback stores instead of using a generic hybrid store"));
        } else if ("none".equals(family) && !profile.fallback()) {
            issues.add(HermesSkillPersistenceValidationIssue.error(
                    purpose,
                    profile.store(),
                    profile.storeType(),
                    "Learned-skill persistence store is disabled for " + purpose,
                    "Configure " + purpose + " to use a supported durable store or skill-management"));
        }
    }
}
