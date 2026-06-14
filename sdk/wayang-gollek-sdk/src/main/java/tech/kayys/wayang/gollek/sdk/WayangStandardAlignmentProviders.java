package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Discovery and composition helpers for standard-alignment providers.
 */
public final class WayangStandardAlignmentProviders {

    private WayangStandardAlignmentProviders() {
    }

    public static List<WayangStandardAlignmentProvider> load() {
        return loadProviders().providers();
    }

    public static WayangStandardAlignmentProviderDiscovery discover() {
        LoadedProviders loaded = loadProviders();
        return discover(loaded.providers(), loaded.issues());
    }

    public static WayangStandardAlignmentPortfolio loadPortfolio() {
        return discover().portfolio();
    }

    public static WayangStandardAlignmentPortfolio portfolio(
            List<? extends WayangStandardAlignmentProvider> providers) {
        return discover(providers).portfolio();
    }

    public static WayangStandardAlignmentProviderDiscovery discover(
            List<? extends WayangStandardAlignmentProvider> providers) {
        return discover(providers, List.of());
    }

    private static WayangStandardAlignmentProviderDiscovery discover(
            List<? extends WayangStandardAlignmentProvider> providers,
            List<WayangStandardAlignmentProviderIssue> initialIssues) {
        WayangStandardAlignmentPortfolio.Builder builder = WayangStandardAlignmentPortfolio.builder();
        List<String> providerIds = new ArrayList<>();
        List<WayangStandardAlignmentProviderSummary> providerSummaries = new ArrayList<>();
        List<WayangStandardAlignmentProviderIssue> issues = new ArrayList<>(initialIssues);
        if (providers != null) {
            providerSlots(providers, issues).stream()
                    .sorted(WayangStandardAlignmentProviders::compare)
                    .forEach(slot -> appendProvider(builder, providerIds, providerSummaries, issues, slot));
        }
        return new WayangStandardAlignmentProviderDiscovery(providerIds, providerSummaries, builder.build(), issues);
    }

    private static LoadedProviders loadProviders() {
        List<WayangStandardAlignmentProvider> providers = new ArrayList<>();
        List<WayangStandardAlignmentProviderIssue> issues = new ArrayList<>();
        Iterator<ServiceLoader.Provider<WayangStandardAlignmentProvider>> iterator =
                ServiceLoader.load(WayangStandardAlignmentProvider.class).stream().iterator();
        while (true) {
            ServiceLoader.Provider<WayangStandardAlignmentProvider> provider;
            try {
                if (!iterator.hasNext()) {
                    return new LoadedProviders(providers, issues);
                }
                provider = iterator.next();
            } catch (ServiceConfigurationError e) {
                issues.add(WayangStandardAlignmentProviderIssue.from("unknown", "", e));
                continue;
            }
            try {
                providers.add(provider.get());
            } catch (RuntimeException | ServiceConfigurationError e) {
                issues.add(WayangStandardAlignmentProviderIssue.from("unknown", provider.type().getName(), e));
            }
        }
    }

    private static List<ProviderSlot> providerSlots(
            List<? extends WayangStandardAlignmentProvider> providers,
            List<WayangStandardAlignmentProviderIssue> issues) {
        List<ProviderSlot> slots = new ArrayList<>();
        providers.stream()
                .filter(provider -> provider != null)
                .forEach(provider -> {
                    try {
                        slots.add(new ProviderSlot(
                                provider,
                                providerId(provider),
                                provider.getClass().getName(),
                                provider.priority()));
                    } catch (RuntimeException e) {
                        issues.add(WayangStandardAlignmentProviderIssue.from(
                                "unknown",
                                provider.getClass().getName(),
                                e));
                    }
                });
        return slots;
    }

    private static void appendProvider(
            WayangStandardAlignmentPortfolio.Builder builder,
            List<String> providerIds,
            List<WayangStandardAlignmentProviderSummary> providerSummaries,
            List<WayangStandardAlignmentProviderIssue> issues,
            ProviderSlot slot) {
        try {
            WayangStandardAlignmentPortfolio portfolio = slot.provider().portfolio();
            builder.portfolio(portfolio);
            providerIds.add(slot.providerId());
            providerSummaries.add(WayangStandardAlignmentProviderSummary.from(
                    slot.providerId(),
                    slot.providerClass(),
                    slot.priority(),
                    portfolio));
        } catch (RuntimeException e) {
            issues.add(WayangStandardAlignmentProviderIssue.from(
                    slot.providerId(),
                    slot.providerClass(),
                    e));
        }
    }

    private static int compare(ProviderSlot first, ProviderSlot next) {
        int priority = Integer.compare(first.priority(), next.priority());
        if (priority != 0) {
            return priority;
        }
        return first.providerId().compareTo(next.providerId());
    }

    private static String providerId(WayangStandardAlignmentProvider provider) {
        return SdkText.trimToDefault(provider.providerId(), provider.getClass().getName());
    }

    private record LoadedProviders(
            List<WayangStandardAlignmentProvider> providers,
            List<WayangStandardAlignmentProviderIssue> issues) {
    }

    private record ProviderSlot(
            WayangStandardAlignmentProvider provider,
            String providerId,
            String providerClass,
            int priority) {
    }
}
