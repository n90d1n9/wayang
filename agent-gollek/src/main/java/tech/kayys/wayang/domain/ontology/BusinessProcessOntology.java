package tech.kayys.gamelan.domain.ontology;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business Process Ontology — reusable domain models for ERP and enterprise systems.
 *
 * <h2>Why an Ontology</h2>
 * Without shared vocabulary, an agent asked to "create an invoice" generates
 * code that is structurally correct but semantically wrong for the domain —
 * missing required fields, incorrect process flows, wrong status transitions.
 *
 * <h2>Ontology Layers</h2>
 * <ol>
 *   <li><b>Entity definitions</b>: Invoice, Order, Payment, Employee, Asset …</li>
 *   <li><b>Process flows</b>: Order-to-Cash, Procure-to-Pay, Record-to-Report …</li>
 *   <li><b>State machines</b>: valid status transitions for each entity type</li>
 *   <li><b>Business rules</b>: validation constraints for domain operations</li>
 *   <li><b>Terminology mappings</b>: synonym resolution across ERP systems</li>
 * </ol>
 *
 * <h2>Integration</h2>
 * The ontology is injected into agent system prompts when domain-specific
 * tasks are detected. It acts as a "domain knowledge" layer that makes the
 * agent aware of correct terminology, required fields, and process steps.
 */
@ApplicationScoped
public class BusinessProcessOntology {

    private static final Logger log = LoggerFactory.getLogger(BusinessProcessOntology.class);

    // Entity registry: entityType → EntityDefinition
    private final Map<String, EntityDefinition>  entities    = new ConcurrentHashMap<>();
    // Process registry: processName → ProcessFlow
    private final Map<String, ProcessFlow>        processes   = new ConcurrentHashMap<>();
    // Terminology: alias → canonical name
    private final Map<String, String>             terminology = new ConcurrentHashMap<>();

    // ── Initialization ─────────────────────────────────────────────────────

    {
        registerCoreEntities();
        registerCoreProcesses();
        registerTerminology();
        log.info("[ontology] initialized: {} entities, {} processes, {} terms",
                entities.size(), processes.size(), terminology.size());
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns the ontology context block for injection into agent system prompts.
     * Content is task-aware: only entities and processes relevant to the task are included.
     */
    public String buildContextBlock(String task) {
        if (task == null || task.isBlank()) return "";

        String lower = task.toLowerCase();
        StringBuilder sb = new StringBuilder("## Business Domain Context\n\n");
        boolean anyMatch = false;

        for (Map.Entry<String, EntityDefinition> e : entities.entrySet()) {
            if (lower.contains(e.getKey().toLowerCase()) ||
                lower.contains(e.getValue().displayName().toLowerCase())) {
                sb.append(e.getValue().toPromptSnippet()).append("\n");
                anyMatch = true;
            }
        }

        for (Map.Entry<String, ProcessFlow> p : processes.entrySet()) {
            if (lower.contains(p.getKey().toLowerCase()) ||
                p.getValue().triggers().stream().anyMatch(t -> lower.contains(t.toLowerCase()))) {
                sb.append(p.getValue().toPromptSnippet()).append("\n");
                anyMatch = true;
            }
        }

        return anyMatch ? sb.toString() : "";
    }

    /**
     * Resolves a synonym or alias to its canonical entity/process name.
     * e.g., "bill" → "invoice", "PO" → "purchase_order"
     */
    public String resolve(String term) {
        return terminology.getOrDefault(term.toLowerCase(), term);
    }

    /**
     * Validates a state transition for an entity.
     * Returns a result indicating whether the transition is valid and why.
     */
    public StateTransitionResult validateTransition(String entityType,
                                                     String fromState, String toState) {
        EntityDefinition def = entities.get(entityType.toLowerCase());
        if (def == null) {
            return StateTransitionResult.unknown(entityType);
        }
        Set<String> allowed = def.transitions().getOrDefault(fromState.toUpperCase(), Set.of());
        boolean valid = allowed.contains(toState.toUpperCase());
        return new StateTransitionResult(valid, entityType, fromState, toState,
                valid ? null : "Transition " + fromState + " → " + toState +
                " is not allowed. Valid transitions from " + fromState + ": " + allowed);
    }

    /**
     * Validates required fields for a domain operation.
     */
    public FieldValidationResult validateFields(String entityType, Map<String, Object> fields) {
        EntityDefinition def = entities.get(entityType.toLowerCase());
        if (def == null) return FieldValidationResult.unknownEntity(entityType);

        List<String> missing = def.requiredFields().stream()
                .filter(f -> !fields.containsKey(f) || fields.get(f) == null ||
                        fields.get(f).toString().isBlank())
                .toList();

        return new FieldValidationResult(missing.isEmpty(), entityType, missing,
                missing.isEmpty() ? null : "Missing required fields: " + missing);
    }

    /** Registers a custom entity definition. */
    public void registerEntity(EntityDefinition entity) {
        entities.put(entity.name().toLowerCase(), entity);
        log.debug("[ontology] registered entity: {}", entity.name());
    }

    /** Registers a custom process flow. */
    public void registerProcess(ProcessFlow process) {
        processes.put(process.name().toLowerCase(), process);
        log.debug("[ontology] registered process: {}", process.name());
    }

    /** Adds a terminology mapping. */
    public void addTerm(String alias, String canonical) {
        terminology.put(alias.toLowerCase(), canonical.toLowerCase());
    }

    public Map<String, EntityDefinition> allEntities()  { return Collections.unmodifiableMap(entities); }
    public Map<String, ProcessFlow>       allProcesses() { return Collections.unmodifiableMap(processes); }

    // ── Core entity definitions ────────────────────────────────────────────

    private void registerCoreEntities() {

        // Invoice
        registerEntity(new EntityDefinition("invoice", "Invoice",
                "A bill issued to a customer for goods or services delivered",
                List.of("invoice_number","customer_id","issue_date","due_date",
                        "line_items","currency","total_amount"),
                List.of("po_number","payment_terms","tax_amount","discount"),
                Map.of(
                    "DRAFT",     Set.of("SUBMITTED","CANCELLED"),
                    "SUBMITTED", Set.of("APPROVED","REJECTED","CANCELLED"),
                    "APPROVED",  Set.of("SENT","CANCELLED"),
                    "SENT",      Set.of("PAID","OVERDUE","DISPUTED","CANCELLED"),
                    "OVERDUE",   Set.of("PAID","WRITTEN_OFF","DISPUTED"),
                    "DISPUTED",  Set.of("RESOLVED","CANCELLED"),
                    "PAID",      Set.of(),
                    "CANCELLED", Set.of()
                )));

        // Purchase Order
        registerEntity(new EntityDefinition("purchase_order", "Purchase Order",
                "A formal request to purchase goods or services from a vendor",
                List.of("po_number","vendor_id","order_date","delivery_date",
                        "line_items","currency","total_amount","requester_id"),
                List.of("budget_code","department","notes"),
                Map.of(
                    "DRAFT",    Set.of("PENDING_APPROVAL","CANCELLED"),
                    "PENDING_APPROVAL", Set.of("APPROVED","REJECTED","CANCELLED"),
                    "APPROVED", Set.of("SENT_TO_VENDOR","CANCELLED"),
                    "SENT_TO_VENDOR", Set.of("ACKNOWLEDGED","CANCELLED"),
                    "ACKNOWLEDGED", Set.of("PARTIALLY_RECEIVED","FULLY_RECEIVED","CANCELLED"),
                    "PARTIALLY_RECEIVED", Set.of("FULLY_RECEIVED","CANCELLED"),
                    "FULLY_RECEIVED", Set.of("CLOSED"),
                    "CLOSED", Set.of(),
                    "CANCELLED", Set.of()
                )));

        // Payment
        registerEntity(new EntityDefinition("payment", "Payment",
                "A transfer of funds to settle a financial obligation",
                List.of("payment_id","payer_id","payee_id","amount","currency",
                        "payment_date","payment_method"),
                List.of("reference_id","bank_reference","reconciled"),
                Map.of(
                    "PENDING",    Set.of("PROCESSING","CANCELLED","FAILED"),
                    "PROCESSING", Set.of("COMPLETED","FAILED","REVERSED"),
                    "COMPLETED",  Set.of("REVERSED","RECONCILED"),
                    "FAILED",     Set.of("PENDING"),
                    "REVERSED",   Set.of("RECONCILED"),
                    "RECONCILED", Set.of()
                )));

        // Employee
        registerEntity(new EntityDefinition("employee", "Employee",
                "A person employed by the organization",
                List.of("employee_id","first_name","last_name","department",
                        "position","hire_date","manager_id"),
                List.of("salary","currency","bank_account","tax_id","nationality"),
                Map.of(
                    "APPLICANT",   Set.of("ACTIVE","REJECTED"),
                    "ACTIVE",      Set.of("ON_LEAVE","SUSPENDED","TERMINATED"),
                    "ON_LEAVE",    Set.of("ACTIVE","TERMINATED"),
                    "SUSPENDED",   Set.of("ACTIVE","TERMINATED"),
                    "TERMINATED",  Set.of()
                )));

        // Asset
        registerEntity(new EntityDefinition("asset", "Fixed Asset",
                "A long-term tangible piece of property owned by the organization",
                List.of("asset_id","name","category","acquisition_date","cost",
                        "useful_life_months","depreciation_method"),
                List.of("location","serial_number","vendor_id","salvage_value"),
                Map.of(
                    "ACTIVE",    Set.of("DISPOSED","IMPAIRED","TRANSFERRED"),
                    "IMPAIRED",  Set.of("ACTIVE","DISPOSED"),
                    "TRANSFERRED", Set.of("ACTIVE"),
                    "DISPOSED",  Set.of()
                )));
    }

    // ── Core process flows ─────────────────────────────────────────────────

    private void registerCoreProcesses() {

        // Order-to-Cash
        registerProcess(new ProcessFlow("order_to_cash", "Order-to-Cash (O2C)",
                "End-to-end process from customer order to cash receipt",
                List.of("order","sale","customer","invoice","payment","revenue"),
                List.of(
                    new ProcessStep(1, "Order Management",
                        "Receive and validate customer order → create sales order",
                        List.of("sales_order"), List.of()),
                    new ProcessStep(2, "Fulfillment",
                        "Pick, pack, and ship goods or deliver services",
                        List.of("delivery_note"), List.of("sales_order")),
                    new ProcessStep(3, "Invoicing",
                        "Create and send invoice to customer → set payment terms",
                        List.of("invoice"), List.of("delivery_note")),
                    new ProcessStep(4, "Collections",
                        "Monitor payment due dates → send reminders → collect payment",
                        List.of("payment"), List.of("invoice")),
                    new ProcessStep(5, "Cash Application",
                        "Match payment to invoice → reconcile AR → close transaction",
                        List.of("reconciliation_entry"), List.of("payment","invoice"))
                )));

        // Procure-to-Pay
        registerProcess(new ProcessFlow("procure_to_pay", "Procure-to-Pay (P2P)",
                "End-to-end process from purchase requisition to vendor payment",
                List.of("purchase","vendor","procurement","payment","invoice","PO"),
                List.of(
                    new ProcessStep(1, "Requisition",
                        "Department creates purchase requisition → budget check",
                        List.of("purchase_requisition"), List.of()),
                    new ProcessStep(2, "Purchase Order",
                        "Procurement approves and issues PO to vendor",
                        List.of("purchase_order"), List.of("purchase_requisition")),
                    new ProcessStep(3, "Receipt",
                        "Goods received → 3-way match: PO + GRN + Invoice",
                        List.of("goods_receipt_note"), List.of("purchase_order")),
                    new ProcessStep(4, "Invoice Verification",
                        "Verify vendor invoice against PO and GRN",
                        List.of("verified_invoice"), List.of("goods_receipt_note","invoice")),
                    new ProcessStep(5, "Payment",
                        "Approve payment → execute via bank → reconcile AP",
                        List.of("payment"), List.of("verified_invoice"))
                )));

        // Hire-to-Retire
        registerProcess(new ProcessFlow("hire_to_retire", "Hire-to-Retire (H2R)",
                "End-to-end employee lifecycle management",
                List.of("employee","hire","payroll","HR","onboarding","termination"),
                List.of(
                    new ProcessStep(1, "Recruitment",     "Post job → screen → interview → offer",        List.of("job_offer"),       List.of()),
                    new ProcessStep(2, "Onboarding",      "Contracts → systems access → orientation",      List.of("employee_record"), List.of("job_offer")),
                    new ProcessStep(3, "Payroll",         "Calculate → approve → disburse salary",         List.of("payroll_run"),     List.of("employee_record")),
                    new ProcessStep(4, "Performance",     "Set goals → review → appraise → reward",        List.of("appraisal"),       List.of("employee_record")),
                    new ProcessStep(5, "Separation",      "Resignation/termination → clearance → final pay",List.of("separation_doc"), List.of("employee_record"))
                )));
    }

    // ── Terminology ────────────────────────────────────────────────────────

    private void registerTerminology() {
        Map<String, String> terms = Map.ofEntries(
            Map.entry("bill",                "invoice"),
            Map.entry("bills",               "invoice"),
            Map.entry("vendor invoice",      "invoice"),
            Map.entry("po",                  "purchase_order"),
            Map.entry("purchase order",      "purchase_order"),
            Map.entry("gr",                  "goods_receipt_note"),
            Map.entry("grn",                 "goods_receipt_note"),
            Map.entry("goods receipt",       "goods_receipt_note"),
            Map.entry("ar",                  "accounts_receivable"),
            Map.entry("ap",                  "accounts_payable"),
            Map.entry("gl",                  "general_ledger"),
            Map.entry("chart of accounts",   "coa"),
            Map.entry("p&l",                 "income_statement"),
            Map.entry("profit and loss",     "income_statement"),
            Map.entry("bs",                  "balance_sheet"),
            Map.entry("staff",               "employee"),
            Map.entry("personnel",           "employee"),
            Map.entry("fixed asset",         "asset"),
            Map.entry("ppe",                 "asset"),
            Map.entry("depreciation",        "asset_depreciation")
        );
        terminology.putAll(terms);
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record EntityDefinition(
            String                        name,
            String                        displayName,
            String                        description,
            List<String>                  requiredFields,
            List<String>                  optionalFields,
            Map<String, Set<String>>      transitions    // fromState → Set of allowedToStates
    ) {
        public String toPromptSnippet() {
            return "**" + displayName + "** (`" + name + "`): " + description +
                    "\nRequired: " + String.join(", ", requiredFields) +
                    (optionalFields.isEmpty() ? "" : "\nOptional: " + String.join(", ", optionalFields));
        }
    }

    public record ProcessFlow(
            String            name,
            String            displayName,
            String            description,
            List<String>      triggers,
            List<ProcessStep> steps
    ) {
        public String toPromptSnippet() {
            StringBuilder sb = new StringBuilder("**Process: " + displayName + "**: " + description + "\n");
            steps.forEach(s -> sb.append("  Step ").append(s.sequence()).append(": ")
                    .append(s.name()).append(" — ").append(s.description()).append("\n"));
            return sb.toString();
        }
    }

    public record ProcessStep(
            int          sequence,
            String       name,
            String       description,
            List<String> outputs,
            List<String> inputs
    ) {}

    public record StateTransitionResult(
            boolean valid,
            String  entityType,
            String  fromState,
            String  toState,
            String  reason
    ) {
        static StateTransitionResult unknown(String t) {
            return new StateTransitionResult(false, t, "?", "?", "Unknown entity type: " + t);
        }
    }

    public record FieldValidationResult(
            boolean      valid,
            String       entityType,
            List<String> missingFields,
            String       message
    ) {
        static FieldValidationResult unknownEntity(String t) {
            return new FieldValidationResult(false, t, List.of(), "Unknown entity type: " + t);
        }
    }
}
