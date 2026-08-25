# Approval Flow Engine

This project implements the core approval-chain engine for a purchase-approval study case. The goal is not to build a full enterprise workflow application, but to answer one question deterministically: given a request, a rule snapshot, an organization snapshot, and a leave/delegation calendar, who should approve it and in what order?

## Scope

This repository intentionally keeps the solution focused on the decision engine:

- Pure domain model
- Deterministic approval-chain generation
- Versioned rule snapshots
- Leave/delegation resolution
- In-memory H2-backed data loading for sample scenarios
- Test coverage for the six study-case scenarios

Out of scope:

- UI / web interface
- Authentication / authorization
- Database persistence beyond the in-memory sample setup
- Full workflow orchestration and timeout escalation engine
- Email / notification delivery

## Repository structure

- `com.example.approval.model` — domain entities such as `Request`, `RuleSet`, `Organization`, `LeaveCalendar`, and `ApprovalStep`
- `com.example.approval.engine` — the pure approval engine that computes the chain
- `com.example.approval.repository` — H2-backed repository that loads immutable snapshots from SQL fixtures
- `com.example.approval.cli` — small console utility to print H2 tables

## Rules and snapshots

The engine is built around one important design principle: a request must be evaluated against the rule snapshot and organization snapshot that existed when the request started.

This ensures:

- no live mutation changes an already started approval flow
- historical cases remain auditable
- new requests can use the latest rule version without rewriting previous decisions

## H2 sample data model

The in-memory H2 database seeds the core input data:

- `employees`: organization hierarchy and manager relationships
- `cost_center_owners`: cost center to owner mapping
- `rule_sets`: versioned thresholds and mandatory approvers
- `approval_scenarios`: the six study-case requests and their rule snapshot versions
- `leave_delegations`: date ranges and delegate names for leave coverage

The SQL scripts are located in:

- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`

## Approval engine behavior

The engine builds an ordered chain by evaluating:

1. request amount thresholds
2. category-specific mandatory approvers
3. requester self-approval exclusion
4. leave/delegation resolution
5. duplicate approver deduplication while keeping role history

The core method is:

```java
ApprovalEngine.buildChain(request, ruleSet, organization, leaveCalendar)
```

It is intentionally deterministic: the same inputs always produce the same chain.

## Requirement coverage

The implementation covers the core study-case requirements as follows.

### R1 — Rules are data-driven
Implemented in `ApprovalEngine.amountChain(...)` and `ApprovalEngine.addCategoryRequirements(...)`.

### R3 — Rule snapshot must be fixed for the request lifecycle
Implemented by loading `rule_version` with each request in `H2ApprovalRepository.loadScenario(...)` and by using immutable model snapshots.

### R5 — Delegation when approver is on leave
Implemented in `LeaveCalendar.resolve(...)` and related helpers.

### R6 — Repeated approver with multiple roles
Handled by `ApprovalStep` storing a list of role labels for each approver, while `ApprovalEngine` still deduplicates by approver.

### R7 — Requester cannot approve own request
Handled in `ApprovalEngine.buildChain(...)` with the self-approval guard.

### R9 — Correcting organization history later must not affect already started requests
Handled by `H2ApprovalRepository.updateManager(...)` and verified by the scenario-6 regression test.

### Partial / design-level support

- R2: Rule changes are modeled as versioned `RuleSet` snapshots in data, but hot-reload / runtime editing of live rules is outside the current scope.
- R4: Escalation timeout logic is intentionally not implemented because this repository focuses on the pure decision engine, not workflow scheduling.
- R8: Re-submission is modeled as a new request with a new date and rule snapshot, rather than a full workflow-state machine.
- R10: Auditability is supported through versioned request metadata and underlying tables, but a dedicated reporting layer is outside the current scope.

## Tests

The project uses JUnit 5.

Run all tests with:

```bash
mvn test
```

Current coverage includes:

- the six study-case scenarios
- delegation recursion and cycle detection
- manager corrections affecting only newly loaded snapshots
- ASCII table rendering for H2 verification

## Example usage

To print a full table from the H2 in-memory database:

```bash
mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.example.approval.cli.H2TableViewer"
```

To print a specific table:

```bash
mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.example.approval.cli.H2TableViewer" "-Dexec.args=employees"
```

## Design assumptions

The most important assumptions made in this implementation are:

- the engine is a pure function, not a workflow service
- rule versions are immutable once used by a request
- approval chain generation occurs at request evaluation time, not later
- escalation and workflow timers belong to a separate orchestration layer
- the repository layer exists only to feed the engine with realistic test data

## Summary

This project delivers the decision-core of the approval flow engine: deterministic request-to-approval-chain calculation, snapshot-based rule integrity, delegation support, and testable logic that matches the case scenarios without expanding into unrelated application concerns.
