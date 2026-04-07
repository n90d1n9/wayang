package tech.kayys.wayang.prompt.store;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.prompt.core.PromptRegistry;
import tech.kayys.wayang.prompt.core.PromptTemplate;
import tech.kayys.wayang.prompt.core.PromptVersion;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class PromptRegistryRepositoryAdapter implements PromptRegistry.PromptTemplateRepository {

    @Inject
    PromptTemplateRepository delegate;

    @Override
    public Uni<PromptTemplate> save(PromptTemplate template) {
        return delegate.save(template);
    }

    @Override
    public Uni<PromptTemplate> findLatestPublished(String templateId, String tenantId) {
        return delegate.findById(templateId, tenantId)
                .map(this::latestPublished);
    }

    @Override
    public Uni<PromptTemplate> findByIdAndVersion(String templateId, String version, String tenantId) {
        return delegate.findById(templateId, tenantId)
                .map(template -> hasVersion(template, version) ? template : null);
    }

    @Override
    public Uni<List<PromptTemplate>> findAllVersions(String templateId, String tenantId) {
        return delegate.findById(templateId, tenantId)
                .map(template -> template == null ? List.of() : List.of(template));
    }

    @Override
    public Uni<List<PromptTemplate>> search(String keyword, String tenantId) {
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        return delegate.findByTenant(tenantId, 0, 200)
                .map(items -> items.stream()
                        .filter(template -> matchesKeyword(template, normalized))
                        .toList());
    }

    @Override
    public Uni<Void> delete(String templateId, String version, String tenantId) {
        return delegate.delete(templateId, tenantId);
    }

    private PromptTemplate latestPublished(PromptTemplate template) {
        if (template == null) {
            return null;
        }
        return template.getVersions().stream()
                .filter(version -> version.getStatus() == PromptVersion.VersionStatus.PUBLISHED)
                .max(Comparator.comparing(PromptVersion::getCreatedAt))
                .map(v -> template)
                .orElse(null);
    }

    private boolean hasVersion(PromptTemplate template, String version) {
        if (template == null || version == null) {
            return false;
        }
        return template.getVersions().stream()
                .map(PromptVersion::getVersion)
                .anyMatch(version::equals);
    }

    private boolean matchesKeyword(PromptTemplate template, String keyword) {
        if (template == null || keyword.isBlank()) {
            return true;
        }
        return contains(template.getTemplateId(), keyword)
                || contains(template.getName(), keyword)
                || contains(template.getDescription(), keyword);
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
