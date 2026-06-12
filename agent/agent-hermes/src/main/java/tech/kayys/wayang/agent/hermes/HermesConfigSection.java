package tech.kayys.wayang.agent.hermes;

/**
 * Applies one cohesive Hermes config domain to an agent mode builder.
 */
@FunctionalInterface
interface HermesConfigSection {

    void apply(HermesConfigValues scoped, HermesAgentModeConfig.Builder builder);
}
