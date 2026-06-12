package tech.kayys.wayang.agent.core.skills.loader;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Runtime boundary for executing a loaded skill.
 */
interface SkillRuntime {

    SkillProcessRunner.ProcessResult execute(String skillName, Map<String, Object> parameters)
            throws IOException, InterruptedException, TimeoutException;
}
