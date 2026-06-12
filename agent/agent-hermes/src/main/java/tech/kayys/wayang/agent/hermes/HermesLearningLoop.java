package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.Objects;

/**
 * Built-in learning loop for Hermes mode. It persists procedural skills through
 * SkillManagementService, so the backing store can be database, files, object
 * storage, or a hybrid configured elsewhere.
 */
public final class HermesLearningLoop {

    private final HermesLearningSignalFactory signalFactory;
    private final HermesLearningPlanner planner;
    private final HermesLearningPlanExecutor planExecutor;

    public HermesLearningLoop(SkillManagementService skillManagementService) {
        this(skillManagementService, new HermesSkillDistiller(), HermesAgentModeConfig.defaults());
    }

    public HermesLearningLoop(
            SkillManagementService skillManagementService,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config) {
        this(skillManagementService, distiller, config, new HermesLearningSignalFactory());
    }

    public HermesLearningLoop(
            SkillManagementService skillManagementService,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesLearningSignalFactory signalFactory) {
        this(skillManagementService, distiller, config, signalFactory, new HermesSkillReusePolicy());
    }

    public HermesLearningLoop(
            SkillManagementService skillManagementService,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesLearningSignalFactory signalFactory,
            HermesSkillReusePolicy reusePolicy) {
        this(
                new HermesLearnedSkillRepository(
                        Objects.requireNonNull(skillManagementService, "skillManagementService"),
                        new HermesSkillMarkdownRenderer(),
                        (config == null ? HermesAgentModeConfig.defaults() : config)
                                .skillPersistenceStrategy()
                                .routePlan()
                                .targetPlan()),
                distiller,
                config,
                signalFactory,
                reusePolicy);
    }

    public HermesLearningLoop(
            HermesLearnedSkillRepository learnedSkills,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config) {
        this(learnedSkills, distiller, config, new HermesLearningSignalFactory(), new HermesSkillReusePolicy());
    }

    public HermesLearningLoop(
            HermesLearnedSkillRepository learnedSkills,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesLearningSignalFactory signalFactory,
            HermesSkillReusePolicy reusePolicy) {
        this(
                learnedSkills,
                distiller,
                config,
                signalFactory,
                reusePolicy,
                HermesLearningPromotionReceiptLedgerResolver.resolve(config));
    }

    public HermesLearningLoop(
            HermesLearnedSkillRepository learnedSkills,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesLearningSignalFactory signalFactory,
            HermesSkillReusePolicy reusePolicy,
            HermesLearningPromotionReceiptLedger receiptLedger) {
        this(learnedSkills, distiller, config, signalFactory, reusePolicy, receiptLedger, null);
    }

    public HermesLearningLoop(
            HermesLearnedSkillRepository learnedSkills,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesLearningSignalFactory signalFactory,
            HermesSkillReusePolicy reusePolicy,
            HermesLearningPromotionReceiptLedger receiptLedger,
            HermesLearnedSkillIndexer learnedSkillIndexer) {
        this.signalFactory = signalFactory == null ? new HermesLearningSignalFactory() : signalFactory;
        HermesLearnedSkillRepository resolvedLearnedSkills =
                Objects.requireNonNull(learnedSkills, "learnedSkills");
        this.planner = new HermesLearningPlanner(
                resolvedLearnedSkills,
                distiller,
                config,
                reusePolicy);
        this.planExecutor = new HermesLearningPlanExecutor(
                resolvedLearnedSkills,
                new HermesLearningPromotionRecorder(resolvedLearnedSkills, receiptLedger),
                learnedSkillIndexer);
    }

    public Uni<HermesLearningResult> learn(AgentRequest request, AgentResponse response) {
        HermesLearningSignal signal = signalFactory.from(request, response);
        return planner.plan(signal)
                .flatMap(plan -> planExecutor.execute(plan, signal));
    }
}
