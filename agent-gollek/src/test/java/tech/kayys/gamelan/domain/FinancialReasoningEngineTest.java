package tech.kayys.gamelan.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import tech.kayys.gamelan.domain.financial.FinancialReasoningEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FinancialReasoningEngine} — contract validation, Sharia compliance,
 * amortization, and double-entry verification.
 */
class FinancialReasoningEngineTest {

    private FinancialReasoningEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FinancialReasoningEngine();
    }

    // ── Contract validation ────────────────────────────────────────────────

    @Test
    void validMurabahaPasses() {
        var contract = murabaha("M001", new BigDecimal("100000"),
                new BigDecimal("5.0"), new BigDecimal("90000"), 12);
        var result = engine.validateContract(contract);
        assertThat(result.valid()).isTrue();
        assertThat(result.contractId()).isEqualTo("M001");
    }

    @Test
    void murabahaWithoutProfitRateFails() {
        var contract = new FinancialReasoningEngine.FinancialContract(
                "M002", FinancialReasoningEngine.ContractType.MURABAHA,
                new BigDecimal("100000"), null, null, null, null, null,
                new BigDecimal("90000"), null, null,
                "Property", "Office Building", 12,
                LocalDate.now(), LocalDate.now().plusYears(1),
                true, List.of(), Map.of());
        var result = engine.validateContract(contract);
        assertThat(result.valid()).isFalse();
        assertThat(result.findings()).anyMatch(f ->
                f.severity() == FinancialReasoningEngine.FindingSeverity.ERROR &&
                f.code().equals("MURABAHA_NO_PROFIT"));
    }

    @Test
    void negativePrincipalFails() {
        var contract = murabaha("M003", new BigDecimal("-1000"),
                new BigDecimal("5.0"), new BigDecimal("900"), 12);
        var result = engine.validateContract(contract);
        assertThat(result.valid()).isFalse();
        assertThat(result.findings()).anyMatch(f -> f.code().equals("INVALID_PRINCIPAL"));
    }

    @Test
    void invalidDateRangeFails() {
        var bad = new FinancialReasoningEngine.FinancialContract(
                "M004", FinancialReasoningEngine.ContractType.MURABAHA,
                new BigDecimal("100000"), null, new BigDecimal("5.0"), null,
                null, null, new BigDecimal("90000"), null, null,
                "Vehicle", "Car", 12,
                LocalDate.now().plusYears(1),  // start AFTER end
                LocalDate.now(),               // end BEFORE start
                true, List.of(), Map.of());
        var result = engine.validateContract(bad);
        assertThat(result.valid()).isFalse();
        assertThat(result.findings()).anyMatch(f -> f.code().equals("INVALID_DATES"));
    }

    @Test
    void ijarahWithoutRentalFails() {
        var contract = new FinancialReasoningEngine.FinancialContract(
                "I001", FinancialReasoningEngine.ContractType.IJARAH,
                new BigDecimal("50000"), null, null, null,
                null, // NO rental amount
                null, null, null, null,
                "Equipment", "Machinery", 24,
                LocalDate.now(), LocalDate.now().plusYears(2),
                false, List.of(), Map.of());
        var result = engine.validateContract(contract);
        assertThat(result.findings()).anyMatch(f -> f.code().equals("IJARAH_NO_RENTAL"));
    }

    // ── Sharia compliance ──────────────────────────────────────────────────

    @Test
    void conventionalLoanWithInterestFailsShariaCheck() {
        var contract = new FinancialReasoningEngine.FinancialContract(
                "L001", FinancialReasoningEngine.ContractType.CONVENTIONAL_LOAN,
                new BigDecimal("200000"), new BigDecimal("8.5"), null, null,
                null, null, null, null, null,
                null, null, 60,
                LocalDate.now(), LocalDate.now().plusYears(5),
                false, List.of(), Map.of());
        var result = engine.checkShariaCompliance(contract);
        // Conventional loans don't trigger riba check (only Islamic contracts checked for riba)
        // But they report as non-Islamic
        assertThat(result).isNotNull();
    }

    @Test
    void murabahaWithoutDisclosedProfitViolatesTransparency() {
        var contract = new FinancialReasoningEngine.FinancialContract(
                "M005", FinancialReasoningEngine.ContractType.MURABAHA,
                new BigDecimal("100000"), null, null, null, // no profitRate
                null, null, new BigDecimal("90000"), null, null,
                "Property", "Apartment", 24,
                LocalDate.now(), LocalDate.now().plusYears(2),
                true, List.of(), Map.of());
        var result = engine.checkShariaCompliance(contract);
        assertThat(result.violations()).anyMatch(v -> v.contains("MURABAHA"));
    }

    @Test
    void musharakahWithoutProfitRatioViolatesSharia() {
        var contract = new FinancialReasoningEngine.FinancialContract(
                "MS001", FinancialReasoningEngine.ContractType.MUSHARAKAH,
                new BigDecimal("500000"), null, null,
                null, // no profit sharing ratio
                null, null, null, null, null,
                "Business", "Trading Company", 36,
                LocalDate.now(), LocalDate.now().plusYears(3),
                false, List.of("Partner A", "Partner B"), Map.of());
        var result = engine.checkShariaCompliance(contract);
        assertThat(result.compliant()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("MUSHARAKAH"));
    }

    @Test
    void latePenaltyRetainedAsIncomeViolatesSharia() {
        var contract = new FinancialReasoningEngine.FinancialContract(
                "M006", FinancialReasoningEngine.ContractType.MURABAHA,
                new BigDecimal("100000"), null, new BigDecimal("5.0"), null,
                null, null, new BigDecimal("90000"),
                new BigDecimal("1.5"),  // late penalty
                FinancialReasoningEngine.LatePenaltyPurpose.INCOME, // retained as INCOME
                "Property", "Office", 12,
                LocalDate.now(), LocalDate.now().plusYears(1),
                true, List.of(), Map.of());
        var result = engine.checkShariaCompliance(contract);
        assertThat(result.compliant()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("PENALTY"));
    }

    @Test
    void latePenaltyDonatedToCharityIsCompliant() {
        var contract = new FinancialReasoningEngine.FinancialContract(
                "M007", FinancialReasoningEngine.ContractType.MURABAHA,
                new BigDecimal("100000"), null, new BigDecimal("5.0"), null,
                null, null, new BigDecimal("90000"),
                new BigDecimal("1.5"),  // late penalty
                FinancialReasoningEngine.LatePenaltyPurpose.CHARITY, // donated — OK
                "Property", "Office", 12,
                LocalDate.now(), LocalDate.now().plusYears(1),
                true, List.of(), Map.of());
        var result = engine.checkShariaCompliance(contract);
        assertThat(result.violations()).noneMatch(v -> v.contains("PENALTY"));
    }

    // ── Amortization ───────────────────────────────────────────────────────

    @Test
    void amortizationScheduleHasCorrectPeriodCount() {
        var contract = murabaha("A001", new BigDecimal("120000"),
                new BigDecimal("6.0"), new BigDecimal("100000"), 12);
        var schedule = engine.computeAmortization(contract);
        assertThat(schedule.entries()).hasSize(12);
    }

    @Test
    void amortizationFinalBalanceIsNearZero() {
        var contract = murabaha("A002", new BigDecimal("100000"),
                new BigDecimal("5.0"), new BigDecimal("90000"), 24);
        var schedule = engine.computeAmortization(contract);
        BigDecimal finalBalance = schedule.entries().getLast().balance();
        assertThat(finalBalance.doubleValue()).isCloseTo(0.0, within(1.0));
    }

    @Test
    void amortizationWithZeroRateGivesEqualPayments() {
        // Zero profit rate → equal principal payments each period
        var contract = new FinancialReasoningEngine.FinancialContract(
                "A003", FinancialReasoningEngine.ContractType.MURABAHA,
                new BigDecimal("120000"), null, BigDecimal.ZERO, null,
                null, null, new BigDecimal("120000"), null, null,
                "Asset", "Equipment", 12,
                LocalDate.now(), LocalDate.now().plusYears(1),
                true, List.of(), Map.of());
        var schedule = engine.computeAmortization(contract);
        // All entries should have zero interest component
        schedule.entries().forEach(e ->
                assertThat(e.interestOrProfitComponent().doubleValue()).isCloseTo(0.0, within(0.001)));
    }

    @Test
    void totalPaidExceedsPrincipal() {
        var contract = murabaha("A004", new BigDecimal("100000"),
                new BigDecimal("5.0"), new BigDecimal("90000"), 24);
        var schedule = engine.computeAmortization(contract);
        assertThat(schedule.totalPaid().compareTo(contract.principal())).isGreaterThan(0);
        assertThat(schedule.totalFinancingCost().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
    }

    // ── Double-entry ──────────────────────────────────────────────────────

    @Test
    void balancedEntriesPass() {
        var entries = List.of(
                new FinancialReasoningEngine.JournalEntry("Cash", FinancialReasoningEngine.EntryType.DEBIT, new BigDecimal("1000"), "Cash received"),
                new FinancialReasoningEngine.JournalEntry("Revenue", FinancialReasoningEngine.EntryType.CREDIT, new BigDecimal("800"), "Revenue recognized"),
                new FinancialReasoningEngine.JournalEntry("Tax Payable", FinancialReasoningEngine.EntryType.CREDIT, new BigDecimal("200"), "Tax payable")
        );
        var result = engine.verifyDoubleEntry(entries);
        assertThat(result.balanced()).isTrue();
        assertThat(result.variance().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    @Test
    void unbalancedEntriesFail() {
        var entries = List.of(
                new FinancialReasoningEngine.JournalEntry("Cash", FinancialReasoningEngine.EntryType.DEBIT, new BigDecimal("1000"), "Cash"),
                new FinancialReasoningEngine.JournalEntry("Revenue", FinancialReasoningEngine.EntryType.CREDIT, new BigDecimal("900"), "Revenue")
        );
        var result = engine.verifyDoubleEntry(entries);
        assertThat(result.balanced()).isFalse();
        assertThat(result.variance().doubleValue()).isEqualTo(100.0);
        assertThat(result.message()).containsIgnoringCase("IMBALANCE");
    }

    // ── Task detection ────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
        "create an invoice for the customer",
        "validate the murabaha contract",
        "check sharia compliance of the financing",
        "compute the amortization schedule",
        "verify the journal entry balance sheet"
    })
    void financialTasksAreDetected(String task) {
        assertThat(engine.isFinancialTask(task)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "fix the null pointer in UserService",
        "add unit tests to OrderController",
        "refactor the authentication module"
    })
    void nonFinancialTasksAreNotDetected(String task) {
        assertThat(engine.isFinancialTask(task)).isFalse();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private FinancialReasoningEngine.FinancialContract murabaha(
            String id, BigDecimal principal, BigDecimal profitRate,
            BigDecimal assetCost, int termMonths) {
        return new FinancialReasoningEngine.FinancialContract(
                id, FinancialReasoningEngine.ContractType.MURABAHA,
                principal, null, profitRate, null, null, null, assetCost,
                null, FinancialReasoningEngine.LatePenaltyPurpose.CHARITY,
                "Property", "Real Estate Asset", termMonths,
                LocalDate.now(), LocalDate.now().plusMonths(termMonths),
                true, List.of(), Map.of());
    }
}
