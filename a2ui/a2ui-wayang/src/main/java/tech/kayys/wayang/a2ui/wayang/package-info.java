/**
 * Wayang integration layer for A2UI contracts.
 *
 * <p>The package is intentionally held behind a source-compatible public
 * facade while the implementation is prepared for a concern-based split:
 *
 * <ul>
 *   <li>{@code WayangA2ui}, {@code WayangA2uiBridge},
 *       {@code WayangA2uiActions}, {@code WayangA2uiSurfaces}, and other
 *       public model records remain the branded API surface.</li>
 *   <li>{@code WayangA2uiModuleBoundary} names the future package skeleton:
 *       action, bridge, http, projection, session, spec, surface, transport,
 *       and support.</li>
 *   <li>Implementation helpers use compact role names such as
 *       {@code TransportMaps}, {@code HttpIssueMaps}, and
 *       {@code SessionProjection}; public contracts keep {@code WayangA2ui*}
 *       names so callers can identify the integration boundary. Helpers moved
 *       into target subpackages may be public inside that boundary while
 *       still-rooted compact helpers remain package-private.</li>
 *   <li>Projection helpers own ordered map shapes. Public records should
 *       delegate to those helpers instead of rebuilding transport JSON in
 *       constructors or facades.</li>
 *   <li>HTTP diagnostics, smoke probes, readiness probes, and scenario harness
 *       behavior stay behind HTTP-focused models and helpers so transport,
 *       surface, and session code do not absorb protocol-specific details.</li>
 * </ul>
 *
 * <p>When adding behavior, prefer the smallest matching boundary: action logic
 * in action helpers, session defaults in session models, transport envelope
 * mechanics in transport helpers, ordered JSON shape in projection helpers, and
 * protocol-specific diagnostics in HTTP helpers.
 */
package tech.kayys.wayang.a2ui.wayang;
