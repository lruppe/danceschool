# Reference

## Finding Categories

### 1. Delete — Dead code and unused abstractions

Code that survived a refactoring but is no longer called. Unused DTOs, mapper methods, old creation paths, commented-out code. These add cognitive load and mislead the AI developer into thinking they're load-bearing.

**Action:** Delete. No replacement needed. Run tests to confirm nothing breaks.

### 2. Fix — Architecture rule violations and bugs

Violations of the rules in CLAUDE.md files: business logic in controllers, missing validations at system boundaries, resource leaks, silent failure patterns.

**Action:** Fix to match the documented rules. If a rule is wrong, flag it for discussion — don't silently ignore it.

### 3. Simplify — Over-engineered code

Abstractions that serve a single caller. Fragmented state management. Multiple code paths that do the same thing differently. Code that's harder to read than it needs to be.

**Action:** Inline, consolidate, or reduce. The goal is fewer concepts, not fewer lines.

### 4. Standardize — Inconsistent patterns

Two endpoints that handle the same concern differently. Mixed state paradigms in one component. Different validation strategies for similar forms.

**Action:** Pick the better pattern and apply it consistently. Document the pattern in CLAUDE.md if it's not already there.

## Effort Sizing

| Size | Summary                                                                                                 |
|------|---------------------------------------------------------------------------------------------------------|
| **S** | Delete unused method, inline a single-caller helper, add missing unsubscribe                            |
| **M** | Consolidate duplicate upload handlers, standardize endpoint patterns, fix state management              |
| **L** | Restructure component into sub-components, migrate data model, extract shared service |
