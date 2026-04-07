---
-- Human Tasks Table
CREATE TABLE IF NOT EXISTS human_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) UNIQUE NOT NULL,
    workflow_run_id VARCHAR(100) NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    priority INTEGER NOT NULL DEFAULT 3,
    status VARCHAR(20) NOT NULL,
    assignee_type VARCHAR(20),
    assignee_identifier VARCHAR(200),
    assigned_by VARCHAR(200),
    assigned_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    claimed_at TIMESTAMP,
    completed_at TIMESTAMP,
    due_date TIMESTAMP,
    outcome VARCHAR(20),
    completed_by VARCHAR(200),
    comments TEXT,
    context_data TEXT,
    form_data TEXT,
    completion_data TEXT,
    escalated BOOLEAN DEFAULT FALSE,
    escalation_reason VARCHAR(50),
    escalated_to VARCHAR(200),
    escalated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP
);

-- Indexes for human_tasks
CREATE INDEX idx_ht_tenant_status ON human_tasks(tenant_id, status);
CREATE INDEX idx_ht_assignee ON human_tasks(assignee_identifier, status);
CREATE INDEX idx_ht_workflow ON human_tasks(workflow_run_id);
CREATE INDEX idx_ht_due_date ON human_tasks(due_date, status);
CREATE INDEX idx_ht_created_at ON human_tasks(created_at);

-- Task Assignment History Table
CREATE TABLE IF NOT EXISTS human_task_assignments (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    assignee_type VARCHAR(20) NOT NULL,
    assignee_identifier VARCHAR(200) NOT NULL,
    assigned_by VARCHAR(200),
    assigned_at TIMESTAMP NOT NULL,
    delegation_reason TEXT,
    sequence_number INTEGER NOT NULL,
    FOREIGN KEY (task_id) REFERENCES human_tasks(task_id) ON DELETE CASCADE
);

CREATE INDEX idx_hta_task ON human_task_assignments(task_id);

-- Task Audit Trail Table
CREATE TABLE IF NOT EXISTS human_task_audit (
    id BIGSERIAL PRIMARY KEY,
    entry_id VARCHAR(100) NOT NULL,
    task_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    performed_by VARCHAR(200),
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (task_id) REFERENCES human_tasks(task_id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_task ON human_task_audit(task_id, timestamp);