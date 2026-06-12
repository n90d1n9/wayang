package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimePortCatalogTest {

    @Test
    void exposesStableRuntimePortGroups() {
        assertThat(HermesRuntimePortCatalog.REQUEST_DIRECTIVE_PORTS)
                .containsExactly(
                        "execution",
                        "gateway",
                        "automation",
                        "delegation",
                        "provider-routing",
                        "memory-reflection",
                        "trajectory-export",
                        "skill-lineage");
        assertThat(HermesRuntimePortCatalog.ADAPTER_BUNDLE_PORTS)
                .containsExactly(
                        "execution",
                        "gateway",
                        "automation",
                        "delegation",
                        "provider-routing",
                        "memory-reflection",
                        "trajectory-export",
                        "skill-persistence",
                        "runtime-journal",
                        "learning-audit",
                        "skill-lineage");
        assertThat(HermesRuntimePortCatalog.SUPPORT_PORTS)
                .containsExactly(
                        "skill-persistence",
                        "runtime-journal",
                        "learning-audit",
                        "runtime-diagnostics",
                        "skill-lineage");
        assertThat(HermesRuntimePortCatalog.ALL_PORTS)
                .contains("runtime-diagnostics")
                .doesNotHaveDuplicates();
    }

    @Test
    void adapterBundleCatalogMatchesRuntimePortDescriptors() {
        assertThat(HermesRuntimePorts.noop().descriptors())
                .extracting(HermesRuntimePortDescriptor::port)
                .containsExactlyElementsOf(HermesRuntimePortCatalog.ADAPTER_BUNDLE_PORTS);
    }

    @Test
    void detectsKnownPortIds() {
        assertThat(HermesRuntimePortCatalog.contains("execution")).isTrue();
        assertThat(HermesRuntimePortCatalog.contains(" runtime-diagnostics ")).isTrue();
        assertThat(HermesRuntimePortCatalog.contains("unknown-port")).isFalse();
    }
}
