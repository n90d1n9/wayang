package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Registry of model-to-A2UI surface renderers.
 */
public final class WayangA2uiSurfaceRegistry {

    public static final String RUN_STATUS = "wayang.run.status";
    public static final String RUN_INSPECTION = "wayang.run.inspection";
    public static final String RUN_EVENTS = "wayang.run.events";
    public static final String RUN_HISTORY = "wayang.run.history";
    public static final String ACTION_RESULT = "wayang.action.result";

    private final List<Renderer<?>> renderers;

    private WayangA2uiSurfaceRegistry(List<Renderer<?>> renderers) {
        this.renderers = RecordCollections.nonNullList(renderers);
    }

    public static WayangA2uiSurfaceRegistry readOnly() {
        return standard(WayangA2uiSurfaceOptions.readOnly());
    }

    public static WayangA2uiSurfaceRegistry fromPolicy(WayangA2uiActionPolicy policy) {
        return standard(WayangA2uiSurfaceOptions.fromPolicy(policy));
    }

    public static WayangA2uiSurfaceRegistry standard(WayangA2uiSurfaceOptions options) {
        return standardBuilder(options).build();
    }

    public static Builder standardBuilder(WayangA2uiSurfaceOptions options) {
        WayangA2uiSurfaceOptions resolved = options == null ? WayangA2uiSurfaceOptions.readOnly() : options;
        return builder()
                .register(RUN_STATUS, AgentRunStatus.class, status -> WayangA2uiSurfaces.runStatus(status, resolved))
                .register(
                        RUN_INSPECTION,
                        AgentRunInspection.class,
                        inspection -> WayangA2uiSurfaces.runInspection(inspection, resolved))
                .register(RUN_EVENTS, AgentRunEvents.class, events -> WayangA2uiSurfaces.runEvents(events, resolved))
                .register(RUN_HISTORY, AgentRunHistory.class, history -> WayangA2uiSurfaces.runHistory(history, resolved))
                .register(
                        ACTION_RESULT,
                        WayangA2uiActionFeedback.class,
                        feedback -> WayangA2uiResultSurfaces.actionResult(feedback.result(), feedback.sequence()))
                .register(
                        ACTION_RESULT,
                        WayangA2uiActionResult.class,
                        result -> WayangA2uiResultSurfaces.actionResult(result, 1));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().extend(this);
    }

    public Optional<String> kindOf(Object model) {
        return descriptorOf(model).map(WayangA2uiSurfaceDescriptor::kind);
    }

    public Optional<WayangA2uiSurfaceDescriptor> descriptorOf(Object model) {
        if (model == null) {
            return Optional.empty();
        }
        return renderers.stream()
                .filter(renderer -> renderer.supports(model))
                .map(Renderer::descriptor)
                .findFirst();
    }

    public Optional<String> kindForModelType(Class<?> modelType) {
        return descriptorForModelType(modelType).map(WayangA2uiSurfaceDescriptor::kind);
    }

    public Optional<WayangA2uiSurfaceDescriptor> descriptorForModelType(Class<?> modelType) {
        if (modelType == null) {
            return Optional.empty();
        }
        return renderers.stream()
                .filter(renderer -> renderer.supportsModelType(modelType))
                .map(Renderer::descriptor)
                .findFirst();
    }

    public boolean supports(Object model) {
        return kindOf(model).isPresent();
    }

    public boolean supportsModelType(Class<?> modelType) {
        return descriptorForModelType(modelType).isPresent();
    }

    public Optional<List<A2uiServerMessage>> render(Object model) {
        if (model == null) {
            return Optional.empty();
        }
        return renderers.stream()
                .filter(renderer -> renderer.supports(model))
                .findFirst()
                .map(renderer -> renderer.render(model));
    }

    public List<A2uiServerMessage> renderRequired(Object model) {
        return render(model).orElseThrow(() -> new IllegalArgumentException(
                "No Wayang A2UI surface renderer registered for "
                        + (model == null ? "null" : model.getClass().getName())));
    }

    public List<A2uiServerMessage> renderActionFeedback(WayangA2uiActionResult result, int sequence) {
        WayangA2uiActionFeedback feedback = WayangA2uiActionFeedback.of(result, sequence);
        return render(feedback).orElseGet(() -> WayangA2uiResultSurfaces.actionResult(result, feedback.sequence()));
    }

    public List<String> surfaceKinds() {
        return renderers.stream()
                .map(Renderer::kind)
                .distinct()
                .toList();
    }

    public List<WayangA2uiSurfaceDescriptor> surfaceDescriptors() {
        return renderers.stream()
                .map(Renderer::descriptor)
                .distinct()
                .toList();
    }

    public List<WayangA2uiSurfaceDescriptor> surfaceDescriptors(String kind) {
        String normalizedKind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
        return surfaceDescriptors().stream()
                .filter(descriptor -> descriptor.kind().equals(normalizedKind))
                .toList();
    }

    public List<WayangA2uiSurfaceDescriptor> surfaceDescriptorsForModelType(Class<?> modelType) {
        if (modelType == null) {
            return List.of();
        }
        return surfaceDescriptors().stream()
                .filter(descriptor -> descriptor.supportsModelType(modelType))
                .toList();
    }

    public WayangA2uiSurfaceCatalog surfaceCatalog() {
        return WayangA2uiSurfaceCatalog.from(this);
    }

    public static final class Builder {

        private final List<Renderer<?>> renderers = new ArrayList<>();

        private Builder() {
        }

        public Builder extend(WayangA2uiSurfaceRegistry registry) {
            if (registry != null) {
                renderers.addAll(registry.renderers);
            }
            return this;
        }

        public <T> Builder register(
                String kind,
                Class<T> modelType,
                Function<? super T, List<A2uiServerMessage>> renderer) {
            renderers.add(new Renderer<>(kind, modelType, renderer));
            return this;
        }

        public <T> Builder replace(
                String kind,
                Class<T> modelType,
                Function<? super T, List<A2uiServerMessage>> renderer) {
            String normalizedKind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
            Class<T> resolvedType = Objects.requireNonNull(modelType, "modelType");
            int replacementIndex = indexOfBinding(normalizedKind, resolvedType);
            renderers.removeIf(existing -> existing.matchesBinding(normalizedKind, resolvedType));
            Renderer<T> replacement = new Renderer<>(normalizedKind, resolvedType, renderer);
            if (replacementIndex >= 0 && replacementIndex <= renderers.size()) {
                renderers.add(replacementIndex, replacement);
            } else {
                renderers.add(replacement);
            }
            return this;
        }

        public <T> Builder replaceKind(
                String kind,
                Class<T> modelType,
                Function<? super T, List<A2uiServerMessage>> renderer) {
            String normalizedKind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
            Class<T> resolvedType = Objects.requireNonNull(modelType, "modelType");
            renderers.removeIf(existing -> existing.kind().equals(normalizedKind));
            renderers.add(new Renderer<>(normalizedKind, resolvedType, renderer));
            return this;
        }

        public <T> Builder replaceModelType(
                String kind,
                Class<T> modelType,
                Function<? super T, List<A2uiServerMessage>> renderer) {
            String normalizedKind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
            Class<T> resolvedType = Objects.requireNonNull(modelType, "modelType");
            renderers.removeIf(existing -> existing.modelType().equals(resolvedType));
            renderers.add(new Renderer<>(normalizedKind, resolvedType, renderer));
            return this;
        }

        public Builder withoutBinding(String kind, Class<?> modelType) {
            String normalizedKind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
            Class<?> resolvedType = Objects.requireNonNull(modelType, "modelType");
            renderers.removeIf(existing -> existing.matchesBinding(normalizedKind, resolvedType));
            return this;
        }

        public Builder withoutKind(String kind) {
            String normalizedKind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
            renderers.removeIf(existing -> existing.kind().equals(normalizedKind));
            return this;
        }

        public Builder withoutModelType(Class<?> modelType) {
            Class<?> resolvedType = Objects.requireNonNull(modelType, "modelType");
            renderers.removeIf(existing -> existing.modelType().equals(resolvedType));
            return this;
        }

        public WayangA2uiSurfaceRegistry build() {
            return new WayangA2uiSurfaceRegistry(renderers);
        }

        private int indexOfBinding(String kind, Class<?> modelType) {
            for (int index = 0; index < renderers.size(); index++) {
                if (renderers.get(index).matchesBinding(kind, modelType)) {
                    return index;
                }
            }
            return -1;
        }
    }

    private record Renderer<T>(
            String kind,
            Class<T> modelType,
            Function<? super T, List<A2uiServerMessage>> renderer) {

        private Renderer {
            kind = WayangA2uiSurfaceDescriptor.normalizeKind(kind);
            modelType = Objects.requireNonNull(modelType, "modelType");
            renderer = Objects.requireNonNull(renderer, "renderer");
        }

        private boolean supports(Object model) {
            return modelType.isInstance(model);
        }

        private boolean supportsModelType(Class<?> candidateType) {
            return candidateType != null && modelType.isAssignableFrom(candidateType);
        }

        private boolean matchesBinding(String otherKind, Class<?> otherModelType) {
            return kind.equals(otherKind) && modelType.equals(otherModelType);
        }

        private WayangA2uiSurfaceDescriptor descriptor() {
            return new WayangA2uiSurfaceDescriptor(kind, modelType);
        }

        private List<A2uiServerMessage> render(Object model) {
            List<A2uiServerMessage> messages = renderer.apply(modelType.cast(model));
            return RecordCollections.nonNullList(messages);
        }
    }
}
