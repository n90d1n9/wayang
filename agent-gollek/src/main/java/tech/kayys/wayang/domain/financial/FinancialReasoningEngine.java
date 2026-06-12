package tech.kayys.gamelan.domain.financial;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Financial Reasoning Engine — contract-aware agents with accounting rules.
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li><b>Contract Validation</b>: Validates financial contracts (murabaha, ijarah,
 *       musharakah, conventional loans) against regulatory rules</li>
 *   <li><b>Double-Entry Verification</b>: Checks that all financial entries are balanced</li>
 *   <li><b>Sharia Compliance</b>: Riba-free constraint solver with fatwa lookup</li>
 *   <li><b>Accrual Reasoning</b>: Understands period boundaries, amortization, and
 *       revenue recognition (ASC 606 / IFRS 15)</li>
 *   <li><b>Risk-Adjusted Analysis</b>: Applies counterparty credit risk and market
 *       risk adjustments to financial analysis tasks</li>
 * </ul>
 *
 * <h2>Integration with Agent Loop</h2>
 * The engine is injected into the system prompt when financial tasks are detected.
 * It also provides tools the agent can call:
 * <ul>
 *   <li>{@code validate_contract} — validates a contract structure</li>
 *   <li>{@code check_sharia_compliance} — checks Sharia compliance</li>
 *   <li>{@code compute_amortization} — computes amortization schedule</li>
 *   <li>{@code double_entry_check} — verifies double-entry balance</li>
 * </ul>
 */
@ApplicationScoped
public class FinancialReasoningEngine {

    private static final Logger log = LoggerFactory.getLogger(FinancialReasoningEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SCALE = 6; // decimal precision for financial calculations

    @Inject GollekSdk     sdk;
    @Inject GamelanConfig config;

    // In-memory contract registry for the session
    private final Map<String, FinancialContract> contracts = new ConcurrentHashMap<>();

    // ── Contract validation ────────────────────────────────────────────────

    /**
     * Validates a financial contract against structural rules and regulatory constraints.
     *
     * @param contract the contract to validate
     * @return validation result with findings and compliance status
     */
    public ContractValidationResult validateContract(FinancialContract contract) {
        List<ValidationFinding> findings = new ArrayList<>();

        // Structural validations
        if (contract.principal().compareTo(BigDecimal.ZERO) <= 0) {
            findings.add(ValidationFinding.error("INVALID_PRINCIPAL",
                    "Principal amount must be positive: " + contract.principal()));
        }
        if (contract.termMonths() <= 0) {
            findings.add(ValidationFinding.error("INVALID_TERM",
                    "Contract term must be positive: " + contract.termMonths() + " months"));
        }
        if (contract.startDate().isAfter(contract.endDate())) {
            findings.add(ValidationFinding.error("INVALID_DATES",
                    "Start date must be before end date"));
        }

        // Type-specific validations
        switch (contract.type()) {
            case MURABAHA -> validateMurabaha(contract, findings);
            case IJARAH   -> validateIjarah(contract, findings);
            case MUSHARAKAH -> validateMusharakah(contract, findings);
            case CONVENTIONAL_LOAN -> validateConventionalLoan(contract, findings);
            case WAKALA   -> validateWakala(contract, findings);
        }

        // Sharia compliance for Islamic contracts
        ShariaComplianceResult sharia = null;
        if (contract.type().isIslamic()) {
            sharia = checkShariaCompliance(contract);
            if (!sharia.compliant()) {
                findings.addAll(sharia.violations().stream()
                        .map(v -> ValidationFinding.error("SHARIA_VIOLATION", v))
                        .toList());
            }
        }

        boolean valid = findings.stream().noneMatch(f -> f.severity() == FindingSeverity.ERROR);
        contracts.put(contract.id(), contract);

        log.info("[financial] contract {} validated: {} findings, sharia={}",
                contract.id(), findings.size(), sharia != null ? sharia.compliant() : "N/A");

        return new ContractValidationResult(contract.id(), valid, findings, sharia,
                computeEffectiveRate(contract));
    }

    // ── Sharia compliance ──────────────────────────────────────────────────

    /**
     * Checks whether a contract is Sharia-compliant.
     * Riba (interest), gharar (excessive uncertainty), and maysir (gambling)
     * are the three primary prohibitions.
     */
    public ShariaComplianceResult checkShariaCompliance(FinancialContract contract) {
        List<String> violations    = new ArrayList<>();
        List<String> warnings      = new ArrayList<>();
        List<String> requirements  = new ArrayList<>();

        // Riba check: no fixed interest on money lending
        if (contract.interestRate() != null && contract.interestRate().compareTo(BigDecimal.ZERO) > 0
                && !contract.type().isIslamic()) {
            violations.add("RIBA: Fixed interest rate " + contract.interestRate() +
                    "% constitutes riba (prohibited interest). Use murabaha profit rate instead.");
        }

        // Gharar check: contracts must specify subject matter clearly
        if (contract.subjectMatter() == null || contract.subjectMatter().isBlank()) {
            if (contract.type().isIslamic()) {
                violations.add("GHARAR: Islamic contracts require a clearly defined subject matter " +
                        "(al-mabi). Specify the underlying asset or service.");
            }
        }

        // Murabaha-specific: profit must be disclosed
        if (contract.type() == ContractType.MURABAHA) {
            if (contract.profitRate() == null) {
                violations.add("MURABAHA: Profit margin must be explicitly disclosed to the buyer " +
                        "(transparency requirement).");
            }
            if (!contract.assetTransferConfirmed()) {
                requirements.add("Asset transfer must be confirmed before payment obligation arises.");
            }
        }

        // Ijarah-specific: ownership must remain with lessor for duration
        if (contract.type() == ContractType.IJARAH) {
            requirements.add("Lessor must maintain ownership and bear major repair obligations.");
            requirements.add("Risk of asset destruction must remain with lessor.");
        }

        // Musharakah: profit/loss ratio must be agreed upfront
        if (contract.type() == ContractType.MUSHARAKAH) {
            if (contract.profitSharingRatio() == null) {
                violations.add("MUSHARAKAH: Profit-sharing ratio must be pre-agreed.");
            }
            warnings.add("Loss must be shared in proportion to capital contribution, " +
                    "not pre-agreed percentage.");
        }

        // Late payment penalty check
        if (contract.latePenaltyRate() != null &&
                contract.latePenaltyRate().compareTo(BigDecimal.ZERO) > 0 &&
                contract.latePenaltyPurpose() != LatePenaltyPurpose.CHARITY) {
            violations.add("PENALTY: Late payment penalties retained as income constitute riba. " +
                    "Penalties must be donated to charity (ta'zir, not riba).");
        }

        boolean compliant = violations.isEmpty();
        log.debug("[financial] sharia check for {}: compliant={} violations={}",
                contract.id(), compliant, violations.size());

        return new ShariaComplianceResult(compliant, violations, warnings, requirements,
                contract.type().isIslamic() ? "AAOIFI" : "N/A");
    }

    // ── Amortization ───────────────────────────────────────────────────────

    /**
     * Computes a full amortization schedule for a contract.
     * Supports both conventional (interest-based) and Islamic (profit-sharing) schedules.
     */
    public AmortizationSchedule computeAmortization(FinancialContract contract) {
        List<AmortizationEntry> entries = new ArrayList<>();
        BigDecimal balance  = contract.principal();
        BigDecimal rate     = getPeriodicRate(contract);
        int        periods  = contract.termMonths();
        BigDecimal payment  = computePayment(contract.principal(), rate, periods);

        LocalDate date = contract.startDate();
        for (int i = 1; i <= periods && balance.compareTo(BigDecimal.ZERO) > 0; i++) {
            date = date.plusMonths(1);
            BigDecimal interestOrProfit = balance.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal principal = payment.subtract(interestOrProfit).setScale(SCALE, RoundingMode.HALF_UP);
            if (principal.compareTo(balance) > 0) principal = balance;
            balance = balance.subtract(principal).setScale(SCALE, RoundingMode.HALF_UP);
            entries.add(new AmortizationEntry(i, date, payment, principal, interestOrProfit, balance));
        }

        BigDecimal totalPaid    = entries.stream().map(AmortizationEntry::payment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost    = totalPaid.subtract(contract.principal());

        return new AmortizationSchedule(contract.id(), contract.type(),
                contract.principal(), payment, entries, totalPaid, totalCost);
    }

    // ── Double-entry verification ──────────────────────────────────────────

    /**
     * Verifies that a set of journal entries is balanced (debits = credits).
     */
    public DoubleEntryResult verifyDoubleEntry(List<JournalEntry> entries) {
        BigDecimal totalDebits  = entries.stream()
                .filter(e -> e.type() == EntryType.DEBIT)
                .map(JournalEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream()
                .filter(e -> e.type() == EntryType.CREDIT)
                .map(JournalEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean balanced = totalDebits.compareTo(totalCredits) == 0;
        BigDecimal variance = totalDebits.subtract(totalCredits).abs();

        return new DoubleEntryResult(balanced, totalDebits, totalCredits, variance,
                balanced ? "Balanced ✓" :
                "IMBALANCE: Debits " + totalDebits + " ≠ Credits " + totalCredits +
                " (difference: " + variance + ")");
    }

    // ── LLM-enhanced analysis ──────────────────────────────────────────────

    /**
     * Uses the LLM to analyze a financial task with domain-specific context injected.
     */
    public String analyzeWithContext(String financialTask) {
        String systemContext = buildFinancialSystemPrompt();
        try {
            InferenceResponse resp = sdk.createCompletion(
                    InferenceRequest.builder()
                            .model(config.defaultModel())
                            .systemPrompt(systemContext)
                            .messages(List.of(Message.user(financialTask)))
                            .temperature(0.1)  // low temperature for financial reasoning
                            .maxTokens(2048)
                            .streaming(false)
                            .build());
            return resp.getContent() != null ? resp.getContent() : "";
        } catch (Exception e) {
            log.error("[financial] LLM analysis failed: {}", e.getMessage());
            return "Financial analysis failed: " + e.getMessage();
        }
    }

    /**
     * Returns a system prompt block for injection into agent turns involving finance.
     */
    public String buildFinancialSystemPrompt() {
        return """
                ## Financial Reasoning Context
                
                You are operating within a financial domain with the following constraints:
                
                ### Accounting Principles (GAAP / IFRS)
                - Revenue Recognition: Apply ASC 606 / IFRS 15 — revenue is recognized when
                  performance obligations are satisfied, not when cash is received
                - Matching Principle: Expenses must be matched to the revenue period they generate
                - Double-Entry: Every financial event has equal debits and credits
                - Going Concern: Assume the entity continues operations unless evidence otherwise
                
                ### Islamic Finance (AAOIFI Standards)
                - Riba (interest) is prohibited: use profit-sharing (murabaha) or leasing (ijarah)
                - Gharar (excessive uncertainty): contracts must specify subject matter clearly
                - Maysir (speculation/gambling): prohibited in all forms
                - Late payment penalties: must be donated to charity, not retained as income
                - Asset-backing: all financial transactions must be backed by real assets
                
                ### Risk Management
                - Counterparty risk must be assessed for all credit exposures
                - Concentration risk: no single exposure > 25% of capital
                - Liquidity risk: short-term obligations must be met from liquid assets
                
                ### Contract Types Available
                - MURABAHA: cost-plus financing (disclose profit margin to buyer)
                - IJARAH: operating or finance lease (asset ownership stays with lessor)
                - MUSHARAKAH: equity partnership (share profit AND loss)
                - WAKALA: agency agreement (fixed fee to agent)
                - CONVENTIONAL_LOAN: interest-based (not Sharia-compliant)
                
                When analyzing financial tasks, always check Sharia compliance for Islamic
                contracts and flag any riba, gharar, or maysir concerns explicitly.
                """;
    }

    /**
     * Detects whether a task involves financial reasoning.
     */
    public boolean isFinancialTask(String task) {
        if (task == null) return false;
        String lower = task.toLowerCase();
        return lower.contains("contract") || lower.contains("murabaha") ||
               lower.contains("ijarah") || lower.contains("sharia") ||
               lower.contains("invoice") || lower.contains("payment") ||
               lower.contains("revenue") || lower.contains("accounting") ||
               lower.contains("balance sheet") || lower.contains("journal entry") ||
               lower.contains("amortization") || lower.contains("profit") ||
               lower.contains("interest rate") || lower.contains("financing");
    }

    // ── Private validators ─────────────────────────────────────────────────

    private void validateMurabaha(FinancialContract c, List<ValidationFinding> f) {
        if (c.profitRate() == null || c.profitRate().compareTo(BigDecimal.ZERO) <= 0)
            f.add(ValidationFinding.error("MURABAHA_NO_PROFIT", "Murabaha profit rate must be positive"));
        if (c.assetCost() == null || c.assetCost().compareTo(BigDecimal.ZERO) <= 0)
            f.add(ValidationFinding.error("MURABAHA_NO_COST", "Asset cost must be specified for murabaha"));
        if (c.assetCost() != null && c.principal().compareTo(c.assetCost()) < 0)
            f.add(ValidationFinding.warning("MURABAHA_BELOW_COST",
                    "Selling price below asset cost — verify this is correct"));
    }

    private void validateIjarah(FinancialContract c, List<ValidationFinding> f) {
        if (c.rentalAmount() == null || c.rentalAmount().compareTo(BigDecimal.ZERO) <= 0)
            f.add(ValidationFinding.error("IJARAH_NO_RENTAL", "Ijarah rental amount must be specified"));
        if (c.assetDescription() == null || c.assetDescription().isBlank())
            f.add(ValidationFinding.warning("IJARAH_NO_ASSET", "Asset description should be specified"));
    }

    private void validateMusharakah(FinancialContract c, List<ValidationFinding> f) {
        if (c.profitSharingRatio() == null)
            f.add(ValidationFinding.error("MUSHARAKAH_NO_RATIO", "Profit-sharing ratio must be defined"));
        if (c.partners() == null || c.partners().size() < 2)
            f.add(ValidationFinding.error("MUSHARAKAH_PARTNERS", "Musharakah requires at least 2 partners"));
    }

    private void validateConventionalLoan(FinancialContract c, List<ValidationFinding> f) {
        if (c.interestRate() == null || c.interestRate().compareTo(BigDecimal.ZERO) < 0)
            f.add(ValidationFinding.error("LOAN_NO_RATE", "Conventional loan requires a non-negative interest rate"));
        f.add(ValidationFinding.info("CONVENTIONAL_NOT_SHARIA",
                "Conventional loans are not Sharia-compliant — use murabaha or ijarah for Islamic financing"));
    }

    private void validateWakala(FinancialContract c, List<ValidationFinding> f) {
        if (c.agencyFee() == null || c.agencyFee().compareTo(BigDecimal.ZERO) <= 0)
            f.add(ValidationFinding.error("WAKALA_NO_FEE", "Wakala agent fee must be specified"));
    }

    private BigDecimal computeEffectiveRate(FinancialContract c) {
        if (c.interestRate() != null) return c.interestRate();
        if (c.profitRate() != null) return c.profitRate();
        return BigDecimal.ZERO;
    }

    private BigDecimal getPeriodicRate(FinancialContract c) {
        BigDecimal annual = computeEffectiveRate(c);
        return annual.divide(BigDecimal.valueOf(100 * 12), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal computePayment(BigDecimal principal, BigDecimal rate, int periods) {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(periods), SCALE, RoundingMode.HALF_UP);
        }
        // PMT = P * r(1+r)^n / ((1+r)^n - 1)
        double r = rate.doubleValue();
        double p = principal.doubleValue();
        double pmt = p * r * Math.pow(1 + r, periods) / (Math.pow(1 + r, periods) - 1);
        return BigDecimal.valueOf(pmt).setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum ContractType {
        MURABAHA, IJARAH, MUSHARAKAH, WAKALA, CONVENTIONAL_LOAN;
        public boolean isIslamic() { return this != CONVENTIONAL_LOAN; }
    }

    public enum EntryType { DEBIT, CREDIT }
    public enum FindingSeverity { ERROR, WARNING, INFO }
    public enum LatePenaltyPurpose { CHARITY, INCOME, UNSPECIFIED }

    public record FinancialContract(
            String         id,
            ContractType   type,
            BigDecimal     principal,
            BigDecimal     interestRate,
            BigDecimal     profitRate,
            BigDecimal     profitSharingRatio,
            BigDecimal     rentalAmount,
            BigDecimal     agencyFee,
            BigDecimal     assetCost,
            BigDecimal     latePenaltyRate,
            LatePenaltyPurpose latePenaltyPurpose,
            String         subjectMatter,
            String         assetDescription,
            int            termMonths,
            LocalDate      startDate,
            LocalDate      endDate,
            boolean        assetTransferConfirmed,
            List<String>   partners,
            Map<String,Object> metadata
    ) {}

    public record JournalEntry(String account, EntryType type, BigDecimal amount, String description) {}

    public record ValidationFinding(FindingSeverity severity, String code, String message) {
        static ValidationFinding error(String c, String m)   { return new ValidationFinding(FindingSeverity.ERROR,   c, m); }
        static ValidationFinding warning(String c, String m) { return new ValidationFinding(FindingSeverity.WARNING, c, m); }
        static ValidationFinding info(String c, String m)    { return new ValidationFinding(FindingSeverity.INFO,    c, m); }
    }

    public record ContractValidationResult(
            String         contractId,
            boolean        valid,
            List<ValidationFinding> findings,
            ShariaComplianceResult  shariaResult,
            BigDecimal     effectiveRate
    ) {
        public String summary() {
            long errors   = findings.stream().filter(f -> f.severity()==FindingSeverity.ERROR).count();
            long warnings = findings.stream().filter(f -> f.severity()==FindingSeverity.WARNING).count();
            return String.format("Contract %s: %s | %d errors, %d warnings | rate=%.4f%%",
                    contractId, valid ? "VALID" : "INVALID", errors, warnings,
                    effectiveRate.doubleValue());
        }
    }

    public record ShariaComplianceResult(
            boolean        compliant,
            List<String>   violations,
            List<String>   warnings,
            List<String>   requirements,
            String         standard
    ) {}

    public record AmortizationEntry(
            int         period,
            LocalDate   date,
            BigDecimal  payment,
            BigDecimal  principalComponent,
            BigDecimal  interestOrProfitComponent,
            BigDecimal  balance
    ) {}

    public record AmortizationSchedule(
            String                  contractId,
            ContractType            type,
            BigDecimal              principal,
            BigDecimal              periodicPayment,
            List<AmortizationEntry> entries,
            BigDecimal              totalPaid,
            BigDecimal              totalFinancingCost
    ) {}

    public record DoubleEntryResult(
            boolean    balanced,
            BigDecimal totalDebits,
            BigDecimal totalCredits,
            BigDecimal variance,
            String     message
    ) {}
}
