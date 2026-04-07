package tech.kayys.wayang.rag.slo;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@ApplicationScoped
public class RagSloAlertService {
    private static final long DEFAULT_SNOOZE_MS = 900000L;
    private static final String SCOPE_ALL = "all";
    private static final String SCOPE_GUARDRAIL = "guardrail";

    private final RagRuntimeConfig config;
    private final RagSloAdminService sloService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AtomicReference<AlertMarker> marker = new AtomicReference<>(new AlertMarker(null, null));
    private final AtomicReference<SnoozeMarker> snoozeMarker = new AtomicReference<>(SnoozeMarker.inactive());
    private volatile boolean initialized;
    private volatile Path snoozePath;

    @Inject
    public RagSloAlertService(
            RagRuntimeConfig config,
            RagSloAdminService sloService,
            Instance<ObjectMapper> objectMapperInstance) {
        this(
                config,
                sloService,
                Clock.systemUTC(),
                objectMapperInstance != null && objectMapperInstance.isResolvable() ? objectMapperInstance.get()
                        : null);
    }

    RagSloAlertService(RagRuntimeConfig config, RagSloAdminService sloService, Clock clock) {
        this(config, sloService, clock, null);
    }

    RagSloAlertService(RagRuntimeConfig config, RagSloAdminService sloService, Clock clock, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config, "config");
        this.sloService = Objects.requireNonNull(sloService, "sloService");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.objectMapper = objectMapper == null ? new ObjectMapper().findAndRegisterModules() : objectMapper;
    }

    public synchronized RagSloAlertState evaluate() {
        initializeIfRequired();
        Instant now = clock.instant();
        RagSloStatus status = sloService.status();
        AlertMarker currentMarker = marker.get();
        SnoozeMarker activeSnooze = resolveActiveSnooze(now);

        if (!config.isSloAlertEnabled()) {
            return new RagSloAlertState(
                    false,
                    false,
                    activeSnooze.active(),
                    activeSnooze.scope(),
                    activeSnooze.expiresAt(),
                    null,
                    0,
                    Math.max(0L, config.getSloAlertCooldownMs()),
                    0,
                    "",
                    "alerting_disabled",
                    now,
                    currentMarker.lastAlertAt(),
                    List.of());
        }

        String minSeverity = normalizeSeverity(config.getSloAlertMinSeverity());
        List<RagSloBreach> activeBreaches = status.breaches().stream()
                .filter(breach -> severityRank(breach.severity()) >= severityRank(minSeverity))
                .sorted(Comparator.comparing(RagSloBreach::metric).thenComparing(RagSloBreach::severity))
                .toList();

        if (activeBreaches.isEmpty()) {
            marker.set(new AlertMarker(currentMarker.lastAlertAt(), null));
            return new RagSloAlertState(
                    true,
                    false,
                    activeSnooze.active(),
                    activeSnooze.scope(),
                    activeSnooze.expiresAt(),
                    null,
                    0,
                    Math.max(0L, config.getSloAlertCooldownMs()),
                    0,
                    "",
                    "no_qualifying_breaches",
                    now,
                    currentMarker.lastAlertAt(),
                    List.of());
        }

        String fingerprint = buildFingerprint(activeBreaches);
        long cooldownMs = Math.max(0L, config.getSloAlertCooldownMs());
        long cooldownRemainingMs = remainingCooldownMs(currentMarker.lastAlertAt(), cooldownMs, now);
        boolean suppressedBySnooze = isSuppressedBySnooze(activeSnooze, activeBreaches, fingerprint);
        boolean sameFingerprint = fingerprint.equals(currentMarker.fingerprint());
        boolean suppressedByCooldown = sameFingerprint && cooldownRemainingMs > 0;
        boolean shouldAlert = !suppressedBySnooze && !suppressedByCooldown;

        Instant lastAlertAt = currentMarker.lastAlertAt();
        if (shouldAlert) {
            lastAlertAt = now;
            marker.set(new AlertMarker(lastAlertAt, fingerprint));
            cooldownRemainingMs = 0L;
        }

        return new RagSloAlertState(
                true,
                shouldAlert,
                suppressedBySnooze,
                activeSnooze.scope(),
                activeSnooze.expiresAt(),
                highestSeverity(activeBreaches),
                activeBreaches.size(),
                cooldownMs,
                cooldownRemainingMs,
                fingerprint,
                shouldAlert ? "alert_ready" : (suppressedBySnooze ? "suppressed_by_snooze" : "suppressed_by_cooldown"),
                now,
                lastAlertAt,
                List.copyOf(activeBreaches));
    }

    public synchronized RagSloAlertSnoozeStatus snooze(RagSloAlertSnoozeRequest request) {
        initializeIfRequired();
        Instant now = clock.instant();
        String scope = normalizeScope(request == null ? null : request.scope());
        long durationMs = normalizeDurationMs(request == null ? null : request.durationMs());
        List<RagSloBreach> activeBreaches = qualifyingBreaches(sloService.status());
        if (activeBreaches.isEmpty()) {
            return new RagSloAlertSnoozeStatus(false, scope, "", null, null, "no_qualifying_breaches", now);
        }
        List<RagSloBreach> scopedBreaches = scopedBreaches(activeBreaches, scope);
        if (scopedBreaches.isEmpty()) {
            return new RagSloAlertSnoozeStatus(false, scope, "", null, null, "no_breaches_for_scope", now);
        }
        String fingerprint = buildFingerprint(scopedBreaches);
        SnoozeMarker marker = new SnoozeMarker(scope, fingerprint, now, now.plusMillis(durationMs), true);
        snoozeMarker.set(marker);
        persistSnoozeState();
        return new RagSloAlertSnoozeStatus(
                true,
                scope,
                fingerprint,
                marker.snoozedAt(),
                marker.expiresAt(),
                "snoozed",
                now);
    }

    public synchronized RagSloAlertSnoozeStatus clearSnooze() {
        initializeIfRequired();
        Instant now = clock.instant();
        snoozeMarker.set(SnoozeMarker.inactive());
        persistSnoozeState();
        return new RagSloAlertSnoozeStatus(false, SCOPE_ALL, "", null, null, "cleared", now);
    }

    public synchronized RagSloAlertSnoozeStatus snoozeStatus() {
        initializeIfRequired();
        Instant now = clock.instant();
        SnoozeMarker marker = resolveActiveSnooze(now);
        return new RagSloAlertSnoozeStatus(
                marker.active(),
                marker.scope(),
                marker.fingerprint(),
                marker.snoozedAt(),
                marker.expiresAt(),
                marker.active() ? "active" : "inactive",
                now);
    }

    private List<RagSloBreach> qualifyingBreaches(RagSloStatus status) {
        if (status == null || status.breaches() == null) {
            return List.of();
        }
        String minSeverity = normalizeSeverity(config.getSloAlertMinSeverity());
        return status.breaches().stream()
                .filter(breach -> severityRank(breach.severity()) >= severityRank(minSeverity))
                .sorted(Comparator.comparing(RagSloBreach::metric).thenComparing(RagSloBreach::severity))
                .toList();
    }

    private static List<RagSloBreach> scopedBreaches(List<RagSloBreach> activeBreaches, String scope) {
        if (SCOPE_GUARDRAIL.equals(scope)) {
            return activeBreaches.stream().filter(RagSloAlertService::isGuardrailBreach).toList();
        }
        return activeBreaches;
    }

    private static boolean isGuardrailBreach(RagSloBreach breach) {
        return breach.metric() != null && breach.metric().startsWith("eval_guardrail_");
    }

    private boolean isSuppressedBySnooze(SnoozeMarker snooze, List<RagSloBreach> activeBreaches,
            String fullFingerprint) {
        if (!snooze.active()) {
            return false;
        }
        if (SCOPE_GUARDRAIL.equals(snooze.scope())) {
            List<RagSloBreach> guardrail = activeBreaches.stream().filter(RagSloAlertService::isGuardrailBreach)
                    .toList();
            List<RagSloBreach> nonGuardrail = activeBreaches.stream().filter(b -> !isGuardrailBreach(b)).toList();
            if (!nonGuardrail.isEmpty() || guardrail.isEmpty()) {
                return false;
            }
            return buildFingerprint(guardrail).equals(snooze.fingerprint());
        }
        return fullFingerprint.equals(snooze.fingerprint());
    }

    private SnoozeMarker resolveActiveSnooze(Instant now) {
        SnoozeMarker marker = snoozeMarker.get();
        if (!marker.active()) {
            return marker;
        }
        if (marker.expiresAt() == null || now.isAfter(marker.expiresAt())) {
            SnoozeMarker inactive = SnoozeMarker.inactive();
            snoozeMarker.set(inactive);
            persistSnoozeState();
            return inactive;
        }
        return marker;
    }

    private void initializeIfRequired() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            String configuredPath = config.getSloAlertSnoozePath();
            if (configuredPath != null && !configuredPath.isBlank()) {
                snoozePath = Path.of(configuredPath.trim());
                loadSnoozeState();
            }
            initialized = true;
        }
    }

    private void loadSnoozeState() {
        if (snoozePath == null || !Files.exists(snoozePath)) {
            return;
        }
        try {
            String payload = Files.readString(snoozePath).trim();
            if (payload.isEmpty()) {
                return;
            }
            RagSloAlertSnoozeSnapshot snapshot = objectMapper.readValue(payload, RagSloAlertSnoozeSnapshot.class);
            snoozeMarker.set(new SnoozeMarker(
                    normalizeScope(snapshot.scope()),
                    snapshot.fingerprint() == null ? "" : snapshot.fingerprint(),
                    snapshot.snoozedAt(),
                    snapshot.expiresAt(),
                    snapshot.active()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load SLO alert snooze state", e);
        }
    }

    private void persistSnoozeState() {
        if (snoozePath == null) {
            return;
        }
        try {
            Path parent = snoozePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            SnoozeMarker marker = snoozeMarker.get();
            RagSloAlertSnoozeSnapshot snapshot = new RagSloAlertSnoozeSnapshot(
                    marker.scope(),
                    marker.fingerprint(),
                    marker.snoozedAt(),
                    marker.expiresAt(),
                    marker.active());
            String payload = objectMapper.writeValueAsString(snapshot);
            Files.writeString(
                    snoozePath,
                    payload,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist SLO alert snooze state", e);
        }
    }

    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return SCOPE_ALL;
        }
        String normalized = scope.trim().toLowerCase();
        if (SCOPE_GUARDRAIL.equals(normalized)) {
            return SCOPE_GUARDRAIL;
        }
        return SCOPE_ALL;
    }

    private static long normalizeDurationMs(Long requested) {
        if (requested == null) {
            return DEFAULT_SNOOZE_MS;
        }
        return Math.max(1000L, Math.min(requested, 7L * 24L * 60L * 60L * 1000L));
    }

    private static long remainingCooldownMs(Instant lastAlertAt, long cooldownMs, Instant now) {
        if (lastAlertAt == null || cooldownMs <= 0L) {
            return 0L;
        }
        long elapsed = Math.max(0L, now.toEpochMilli() - lastAlertAt.toEpochMilli());
        return Math.max(0L, cooldownMs - elapsed);
    }

    private static String highestSeverity(List<RagSloBreach> breaches) {
        return breaches.stream()
                .map(RagSloBreach::severity)
                .max(Comparator.comparingInt(RagSloAlertService::severityRank))
                .orElse("warning");
    }

    private static String buildFingerprint(List<RagSloBreach> breaches) {
        return breaches.stream()
                .map(b -> b.metric() + ":" + normalizeSeverity(b.severity()))
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private static int severityRank(String severity) {
        String normalized = normalizeSeverity(severity);
        if ("critical".equals(normalized)) {
            return 2;
        }
        return 1;
    }

    private static String normalizeSeverity(String severity) {
        if (severity == null) {
            return "warning";
        }
        String normalized = severity.trim().toLowerCase();
        if ("critical".equals(normalized)) {
            return "critical";
        }
        return "warning";
    }

    private record AlertMarker(Instant lastAlertAt, String fingerprint) {
    }

    private record SnoozeMarker(
            String scope,
            String fingerprint,
            Instant snoozedAt,
            Instant expiresAt,
            boolean active) {
        static SnoozeMarker inactive() {
            return new SnoozeMarker(SCOPE_ALL, "", null, null, false);
        }
    }
}
