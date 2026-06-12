/**
 * Skill lifecycle management for Wayang's agent runtime.
 *
 * <p>The package is intentionally organized around a few stable boundaries:
 *
 * <ul>
 *   <li>{@code SkillManagementService} and {@code SkillManagementServiceFactory}
 *       are source-compatible public facades. They should delegate behavior
 *       instead of owning store, workflow, or projection details.</li>
 *   <li>{@code SkillManagementModuleBoundary} names the future package split.
 *       Marker subpackages reserve config, contracts, preflight, runtime,
 *       store, workflow, events, admin, and support destinations while the
 *       source-compatible facade remains here.</li>
 *   <li>{@code SkillManagementServiceRuntime} and the {@code SkillManagementRuntime*}
 *       records assemble normalized dependencies for the service facade.</li>
 *   <li>Runners, readers, and workflows own behavior: definition mutations,
 *       lifecycle transitions, catalog reads, artifact reads/mutations/sync,
 *       maintenance, inspections, preflight checks, and operation traces.</li>
 *   <li>Store interfaces define persistence boundaries. In-memory, filesystem,
 *       JDBC, object-storage, hybrid, and mirrored implementations stay behind
 *       store factories and config records.</li>
 *   <li>{@code SkillManagementServiceProfiles} owns named runtime persistence
 *       profiles and expands them into the same service config records used by
 *       store factories.</li>
 *   <li>{@code SkillPersistenceContractMatrix} derives provider, durability,
 *       capability, and hybrid/mirrored fallback contracts from config before
 *       stores are constructed.</li>
 *   <li>{@code SkillPersistenceStrategySummary} turns those contract rows into
 *       an operational persistence stance for ephemeral, local filesystem,
 *       object-storage, database, hybrid fallback, mirrored, and custom
 *       deployments.</li>
 *   <li>{@code SkillManagementPreflightMatrix} joins validation buckets with
 *       persistence contract rows for target stores, maintenance sources, and
 *       event-pruning capability checks.</li>
 *   <li>Event sinks/readers/history and typed event-attribute helpers form the
 *       observability boundary for operation correlation and admin diagnostics.</li>
 *   <li>Admin DTOs plus focused {@code *Views} mappers own operator-facing
 *       projection, including store status, deployment readiness, event
 *       history, operation traces, and persistence strategy summaries.
 *       {@code SkillManagementAdminViews} remains a compatibility facade and
 *       should delegate to focused mapper classes.</li>
 * </ul>
 *
 * <p>When adding behavior, prefer placing it at the smallest matching boundary:
 * store mechanics in stores/factories, workflow decisions in runners or
 * workflows, read-only queries in readers, and API/CLI shaping in admin DTOs or
 * focused admin mappers.
 */
package tech.kayys.wayang.agent.skills.management;
