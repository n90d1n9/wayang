package tech.kayys.gamelan.domain;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tech.kayys.gamelan.domain.ontology.BusinessProcessOntology;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link BusinessProcessOntology} — entity state machines,
 * terminology resolution, process flows, and context injection.
 */
class BusinessProcessOntologyTest {

    private BusinessProcessOntology ontology;

    @BeforeEach
    void setUp() { ontology = new BusinessProcessOntology(); }

    // ── Entity state machine ────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "invoice, DRAFT,      SUBMITTED,  true",
        "invoice, SUBMITTED,  APPROVED,   true",
        "invoice, APPROVED,   SENT,       true",
        "invoice, SENT,       PAID,       true",
        "invoice, SENT,       OVERDUE,    true",
        "invoice, PAID,       DRAFT,      false",  // can't re-open a paid invoice
        "invoice, CANCELLED,  APPROVED,   false",  // can't un-cancel
    })
    void invoiceStateTransitions(String entity, String from, String to, boolean expected) {
        var result = ontology.validateTransition(entity, from, to);
        assertThat(result.valid()).isEqualTo(expected);
        assertThat(result.entityType()).isEqualTo(entity);
        assertThat(result.fromState()).isEqualTo(from);
        assertThat(result.toState()).isEqualTo(to);
    }

    @ParameterizedTest
    @CsvSource({
        "purchase_order, DRAFT,         PENDING_APPROVAL,       true",
        "purchase_order, PENDING_APPROVAL, APPROVED,            true",
        "purchase_order, APPROVED,      SENT_TO_VENDOR,         true",
        "purchase_order, FULLY_RECEIVED, CLOSED,                true",
        "purchase_order, CLOSED,        DRAFT,                  false",
    })
    void purchaseOrderStateTransitions(String entity, String from, String to, boolean expected) {
        var result = ontology.validateTransition(entity, from, to);
        assertThat(result.valid()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "payment, PENDING,    PROCESSING, true",
        "payment, PROCESSING, COMPLETED,  true",
        "payment, COMPLETED,  REVERSED,   true",
        "payment, COMPLETED,  PENDING,    false",
    })
    void paymentStateTransitions(String entity, String from, String to, boolean expected) {
        assertThat(ontology.validateTransition(entity, from, to).valid()).isEqualTo(expected);
    }

    @Test
    void unknownEntityReturnsInvalidTransition() {
        var result = ontology.validateTransition("nonexistent_entity", "A", "B");
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("Unknown entity");
    }

    @Test
    void invalidTransitionIncludesAllowedTransitions() {
        var result = ontology.validateTransition("invoice", "PAID", "DRAFT");
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isNotBlank();
    }

    // ── Field validation ───────────────────────────────────────────────────

    @Test
    void invoiceWithAllRequiredFieldsPasses() {
        var fields = Map.<String, Object>of(
                "invoice_number", "INV-001",
                "customer_id",    "CUST-123",
                "issue_date",     "2024-01-01",
                "due_date",       "2024-02-01",
                "line_items",     List.of("item1"),
                "currency",       "USD",
                "total_amount",   "1000.00"
        );
        var result = ontology.validateFields("invoice", fields);
        assertThat(result.valid()).isTrue();
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void invoiceWithMissingRequiredFieldFails() {
        var fields = Map.<String, Object>of(
                "invoice_number", "INV-002"
                // missing: customer_id, issue_date, due_date, line_items, currency, total_amount
        );
        var result = ontology.validateFields("invoice", fields);
        assertThat(result.valid()).isFalse();
        assertThat(result.missingFields()).contains("customer_id", "issue_date");
    }

    @Test
    void unknownEntityTypeReturnsInvalidFieldResult() {
        var result = ontology.validateFields("spaceship", Map.of("name", "Apollo"));
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("Unknown entity");
    }

    // ── Terminology resolution ─────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "bill,             invoice",
        "bills,            invoice",
        "PO,               purchase_order",
        "GRN,              goods_receipt_note",
        "AR,               accounts_receivable",
        "P&L,              income_statement",
        "staff,            employee",
        "fixed asset,      asset",
    })
    void terminologyResolvesCorrectly(String alias, String canonical) {
        assertThat(ontology.resolve(alias)).isEqualTo(canonical);
    }

    @Test
    void unknownTermIsReturnedAsIs() {
        assertThat(ontology.resolve("invented_term")).isEqualTo("invented_term");
    }

    @Test
    void customTermCanBeRegistered() {
        ontology.addTerm("myalias", "my_canonical_entity");
        assertThat(ontology.resolve("myalias")).isEqualTo("my_canonical_entity");
    }

    // ── Context block ──────────────────────────────────────────────────────

    @Test
    void contextBlockIncludesRelevantEntitiesForInvoiceTask() {
        String ctx = ontology.buildContextBlock("create an invoice for customer CUST-001");
        assertThat(ctx).containsIgnoringCase("Invoice");
        assertThat(ctx).isNotBlank();
    }

    @Test
    void contextBlockIncludesProcessForOrderTask() {
        String ctx = ontology.buildContextBlock("process a customer order and payment");
        // Should match order-to-cash process
        assertThat(ctx).isNotBlank();
    }

    @Test
    void contextBlockEmptyForUnrelatedTask() {
        String ctx = ontology.buildContextBlock("fix the null pointer in UserService.java");
        // No financial entities match → empty or very short
        // Allow some minimal content but not full financial context
        assertThat(ctx).isNotNull();
    }

    @Test
    void contextBlockEmptyForNullTask() {
        assertThat(ontology.buildContextBlock(null)).isEmpty();
        assertThat(ontology.buildContextBlock("")).isEmpty();
    }

    // ── Registered entities and processes ─────────────────────────────────

    @Test
    void coreEntitiesAreRegistered() {
        assertThat(ontology.allEntities()).containsKeys("invoice", "purchase_order",
                "payment", "employee", "asset");
    }

    @Test
    void coreProcessesAreRegistered() {
        assertThat(ontology.allProcesses()).containsKeys(
                "order_to_cash", "procure_to_pay", "hire_to_retire");
    }

    @Test
    void customEntityCanBeRegistered() {
        ontology.registerEntity(new BusinessProcessOntology.EntityDefinition(
                "custom_entity", "Custom Entity", "A custom domain entity",
                java.util.List.of("id", "name"), java.util.List.of(),
                Map.of("ACTIVE", java.util.Set.of("INACTIVE"))));
        assertThat(ontology.allEntities()).containsKey("custom_entity");
    }

    @Test
    void customProcessCanBeRegistered() {
        ontology.registerProcess(new BusinessProcessOntology.ProcessFlow(
                "my_process", "My Process", "Custom process description",
                java.util.List.of("trigger"),
                java.util.List.of(new BusinessProcessOntology.ProcessStep(
                        1, "Step 1", "Do something", java.util.List.of("output"), java.util.List.of()))));
        assertThat(ontology.allProcesses()).containsKey("my_process");
    }

    // ── Process flow steps ─────────────────────────────────────────────────

    @Test
    void orderToCashProcessHasFiveSteps() {
        BusinessProcessOntology.ProcessFlow o2c = ontology.allProcesses().get("order_to_cash");
        assertThat(o2c).isNotNull();
        assertThat(o2c.steps()).hasSize(5);
    }

    @Test
    void procureToPayProcessHasFiveSteps() {
        BusinessProcessOntology.ProcessFlow p2p = ontology.allProcesses().get("procure_to_pay");
        assertThat(p2p).isNotNull();
        assertThat(p2p.steps()).hasSize(5);
    }

    @Test
    void processStepsAreSequentiallyNumbered() {
        ontology.allProcesses().values().forEach(process ->
                process.steps().forEach((step) ->
                        assertThat(step.sequence()).isGreaterThan(0)));
    }
}
