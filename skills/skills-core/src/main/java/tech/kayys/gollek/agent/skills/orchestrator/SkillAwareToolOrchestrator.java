package tech.kayys.gollek.agent.skills.orchestrator;

public class SkillAwareToolOrchestrator {

    // Skills loaded once per session
    private final SkillContext skills;
    private final ToolRegistry tools;

    public Response process(Request request) {
        // 1. Load relevant skills (one-time per domain)
        List<String> relevantSkills = skills.selectByDomain(request.getDomain());
        skills.load(relevantSkills); // Skills.md content loaded here

        // 2. AI plans using skills
        List<ToolCall> plan = ai.planWithSkills(request, skills);

        // 3. Execute tools (many calls)
        for (ToolCall call : plan) {
            // Tool may reference skills, but doesn't contain them
            tools.execute(call);
        }

        // 4. Skills validate output
        return skills.validate(plan);
    }
}