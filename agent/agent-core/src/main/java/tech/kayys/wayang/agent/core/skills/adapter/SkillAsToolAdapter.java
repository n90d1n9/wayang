package tech.kayys.wayang.agent.core.skills.adapter;

import tech.kayys.wayang.agent.core.skills.loader.SkillExecutor;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

/**
 * @deprecated Use {@link tech.kayys.wayang.agent.core.skills.adapters.ManifestSkillToolAdapter}.
 */
@Deprecated(since = "2026-05-26", forRemoval = false)
public class SkillAsToolAdapter extends tech.kayys.wayang.agent.core.skills.adapters.ManifestSkillToolAdapter {

    public SkillAsToolAdapter(SkillManifest metadata, SkillExecutor executor) {
        super(metadata, executor);
    }
}
