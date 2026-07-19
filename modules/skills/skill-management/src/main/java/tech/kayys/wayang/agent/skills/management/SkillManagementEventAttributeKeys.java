package tech.kayys.wayang.agent.skills.management;

/**
 * Shared vocabulary for skill-management event attribute maps.
 */
final class SkillManagementEventAttributeKeys {

    static final String ARTIFACT_CHANGED = "artifactChanged";
    static final String ARTIFACT_CHANGES = "artifactChanges";
    static final String ARTIFACT_CONFLICTS = "artifactConflicts";
    static final String CHANGED = "changed";
    static final String CONFLICTS = "conflicts";
    static final String CONSISTENT = "consistent";
    static final String CONTENT_TYPE = "contentType";
    static final String COPIED = "copied";
    static final String CREATED = "created";
    static final String DEFINITION_CHANGED = "definitionChanged";
    static final String DEFINITION_CHANGES = "definitionChanges";
    static final String DELETED = "deleted";
    static final String DRY_RUN = "dryRun";
    static final String ERROR = "error";
    static final String ERROR_TYPE = "errorType";
    static final String EVENT_PRUNE_CHANGED = "eventPruneChanged";
    static final String EVENT_PRUNE_ENABLED = "eventPruneEnabled";
    static final String EVENT_PRUNE_SKIPPED = "eventPruneSkipped";
    static final String EVENT_PRUNED = "eventPruned";
    static final String KIND = "kind";
    static final String LIFECYCLE_CONSISTENT = "lifecycleConsistent";
    static final String LIFECYCLE_CREATED = "lifecycleCreated";
    static final String LIFECYCLE_REMOVED = "lifecycleRemoved";
    static final String MISSING = "missing";
    static final String NAME = "name";
    static final String OPERATION_ID = "operationId";
    static final String ORPHANED = "orphaned";
    static final String PARENT_OPERATION_ID = "parentOperationId";
    static final String PREFLIGHT = "preflight";
    static final String PREFLIGHT_CAPABILITY = "preflightCapability";
    static final String PREFLIGHT_CAPABILITY_ERRORS = "preflightCapabilityErrors";
    static final String PREFLIGHT_CONFIGURATION = "preflightConfiguration";
    static final String PREFLIGHT_CONFIGURATION_ERRORS = "preflightConfigurationErrors";
    static final String PREFLIGHT_DEPLOYABLE = "preflightDeployable";
    static final String PREFLIGHT_ERRORS = "preflightErrors";
    static final String PREFLIGHT_MESSAGE = "preflightMessage";
    static final String PREFLIGHT_READY = "preflightReady";
    static final String PREFLIGHT_SOURCE_STORE = "preflightSourceStore";
    static final String PREFLIGHT_SOURCE_STORE_ERRORS = "preflightSourceStoreErrors";
    static final String PREFLIGHT_TARGET_STORE = "preflightTargetStore";
    static final String PREFLIGHT_TARGET_STORE_ERRORS = "preflightTargetStoreErrors";
    static final String QUALIFIED_NAME = "qualifiedName";
    static final String READY = "ready";
    static final String REMOVED = "removed";
    static final String REVISION = "revision";
    static final String SIZE_BYTES = "sizeBytes";
    static final String STATUS = "status";
    static final String UNCHANGED = "unchanged";
    static final String UPDATED = "updated";
    static final String VERSION = "version";

    private SkillManagementEventAttributeKeys() {
    }
}
