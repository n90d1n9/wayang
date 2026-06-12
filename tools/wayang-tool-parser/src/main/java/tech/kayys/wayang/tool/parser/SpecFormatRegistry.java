package tech.kayys.wayang.tool.parser;

/**
 * Deprecated parser-local alias for the canonical tool spec format registry.
 *
 * <p>Supported-format metadata belongs to the core tool registry layer so
 * parser modules can focus on concrete format parsing/generation.
 *
 * @deprecated Use {@link tech.kayys.wayang.tool.registry.SpecFormatRegistry}.
 */
@Deprecated(since = "2026-05-26", forRemoval = false)
public class SpecFormatRegistry extends tech.kayys.wayang.tool.registry.SpecFormatRegistry {
}
