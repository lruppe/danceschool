# Codebase Health Check

Post-implementation review: find and fix what's messy, dead, or drifting from our architecture rules. Prioritize simplicity, cleanliness, and maintainability — not new abstractions.

## Guiding Principles

1. **Clean over clever.** Delete dead code. Remove unused abstractions. Simplify what's over-engineered.
2. **Respect the architecture rules.** CLAUDE.md files define the architecture. Violations of those rules are findings. Disagreements with those rules are not.
3. **Prefer duplication over coupling.** If reuse requires coupling modules that shouldn't know about each other, keep the duplication.
4. **Don't add abstractions.** No new interfaces, wrappers, or helpers unless they remove existing complexity. Three similar lines of code is better than a premature abstraction.
5. **Fix what's actually wrong.** Standard framework patterns (JPA value objects, thin CRUD services, Spring layering within a slice) are not problems — even if they look "shallow."
6. **Optimize for the AI developer.** The primary developer is Claude (AI agent). Findings should prioritize: consistent patterns (so the agent can apply the same approach everywhere), locality (so understanding a feature doesn't require reading 10 files), and explicit contracts (so the agent doesn't have to guess behavior).

## Process

### 1. Read the architecture rules

Read all CLAUDE.md files (root, backend, frontend) to understand the established architecture. These are the baseline — not suggestions, not defaults. Findings are measured against these rules.

### 2. Explore the codebase

Use the Agent tool with subagent_type=Explore to navigate the code affected by the parent PRD. Look for:

**Dead weight:**
- Unused code (methods, classes, imports, DTOs, routes) that survived refactoring
- Abstractions that serve a single caller and add indirection without value
- Commented-out code, TODOs that are already done, stale documentation

**Architecture rule violations:**
- Cross-slice entity leakage (entities visible outside their slice)
- Circular or unexpected dependencies between slices
- Business logic in controllers (should be in services or use-cases)
- Inconsistent patterns (e.g., POST uses a use-case class but PUT doesn't)
- Missing validations at system boundaries
- Hardcoded values that should use tokens (frontend)

**Cleanliness issues:**
- Duplicated logic that could be a private method within the same class (not a shared util)
- Fragmented state management (mixed paradigms for the same concern)
- Resource leaks (unclosed clients, missing unsubscribe)
- Silent failure patterns (catch-all exception handlers that swallow errors)

**What is NOT a finding:**
- Standard framework patterns (JPA entities, thin repos, mapper interfaces) — even if "shallow"
- Pragmatic cross-slice JPA references (entity FKs require references; that's JPA, not a bug)
- Anemic services for simple CRUD — not every service needs complex logic
- Missing client-side validation when the backend validates (nice-to-have, not architecture)

### 3. Present findings

Present a categorized list. For each finding:

- **What:** One-sentence description
- **Where:** File path(s) and line numbers
- **Why it matters:** Impact on maintainability, correctness, or developer (AI) efficiency
- **Suggested fix:** Concrete action (delete X, move Y, inline Z) — not "consider refactoring"
- **Effort:** S (< 30 min), M (30 min – 2 hours), L (> 2 hours)

Group findings into:
1. **Delete** — dead code, unused abstractions
2. **Fix** — rule violations, bugs, resource leaks
3. **Simplify** — reduce complexity without changing behavior
4. **Standardize** — make inconsistent patterns consistent

Ask the user: "Which of these should I turn into issues? Or should I just fix them directly?"

### 4. Act on user decision

Based on user's choice, either:
- **Create GitHub issues** using `gh issue create` for findings the user wants tracked
- **Fix directly** in the current session for quick wins the user approves
- **Skip** findings the user disagrees with

For GitHub issues, use a simple format:

```
## What
[One-sentence description]

## Where
[File paths and line numbers]

## Why
[Impact on maintainability / correctness]

## Fix
[Concrete steps — what to delete, move, change]
```

$ARGUMENTS
