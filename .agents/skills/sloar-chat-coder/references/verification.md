# Change-aware verification

The target repository defines actual commands. Sloar defines how to select and interpret evidence.

For consequential, repeated-regression, real-device, responsive, persistence, caching, deployment, or first-frame work, read [ownership-evidence-closure.md](ownership-evidence-closure.md) before treating existing green checks as sufficient. Verification begins from the acceptance claim and authoritative semantic owner, not from the list of tests that happen to exist.

## Ownership preflight

Before changing source after a non-trivial failure, identify the semantic decision being changed and its authoritative owner. Inspect producers, writers/overrides, derived consumers, renderer/presentation owner, tests/gates, and package/deploy/cache layers to the depth relevant to the task.

Classify unresolved structure as:

- `OWNER_UNKNOWN`: symptom is visible but authoritative owner is not established;
- `OWNERSHIP_SPLIT`: two or more layers independently decide the same semantic fact.

Do not solve either state by stacking specificity, duplicate state, another renderer, or a downstream synchronizer without differentiated evidence. If the same failure fingerprint survives the normal one-correction cycle, rediscover ownership before another implementation attempt.

## Risk matrix

### Logic/runtime code
Typically require syntax/type/build checks plus focused behavioral tests. If multiple consumers answer the same product question, verify they share the intended canonical model rather than independently re-deriving from raw state.

### UI/CSS/interaction
Typically require relevant desktop/mobile viewport checks, console/page-error inspection, overflow/layout checks, and screenshots for interaction states that are visually meaningful.

When input or motion matters, distinguish modality and phase. Mouse input does not prove real-touch behavior when gesture arbitration can differ. Final-state screenshots do not prove direct tracking, interruption, release inertia, or cancellation.

### Persistence/state migration
Test fresh state, write, same-session read, reload/restart, compatibility/migration, and invalid/stale stored state where applicable.

### Network/data flow
Inspect request count, duplicate requests, cancellation/error paths, loading/fallback behavior, cache freshness/provenance, and real integration contracts when required. Do not make unrelated UI tests repeatedly depend on the same unstable live upstream when a dedicated live integration gate plus deterministic fixture gives stronger separation of evidence.

### Performance-sensitive code
Inspect newly introduced polling, observers, mutation churn, animation loops, timers, repeated layout reads/writes, or request amplification. Measure repository-specific budgets when they exist.

### Deployment/configuration
Require relevant CI plus the runtime stages that can diverge for the task:

```text
SOURCE -> VERIFIED -> PACKAGED -> DEPLOYED -> SERVED -> CACHED -> FIRST_FRAME
```

A successful build or deploy is not necessarily successful production behavior. If deployment manifests, service workers, CDN/browser caches, or delayed enhancement can serve a different contract, collect evidence at those stages instead of inferring convergence.

## Claim -> evidence matrix

For important acceptance claims, record the dimensions that must be exercised:

```text
target identity
input modality
interaction phase
temporal phase
responsive/device class
persistence/state phase
production/runtime phase
```

Then bind each claim only to checks that actually cover those dimensions. Evidence from an older commit/runtime anchor is stale unless equivalence is explicitly established.

## Feature lifecycle and gate relevance

Before changing active product code because a gate is RED, verify that the gate still protects an active contract. Useful lifecycle states are `active`, `experimental`, `dormant`, and `retired`.

A failing gate for a dormant/retired feature outside the current change boundary is `STALE_GATE_SUSPECTED`. Inspect whether the correct action is to retain, scope, fixture, quarantine, or retire the gate. Do not automatically delete the gate, and do not automatically rewrite active product source.

## Failure classification

Before editing source after a failed verification, classify the failure as one of:

- product regression;
- ownership unknown/split;
- stale/incorrect test;
- stale feature-lifecycle gate;
- environment/capability failure;
- external dependency/integration failure;
- transport/publication corruption;
- package/deploy/convergence failure;
- permission/auth failure;
- concurrency/stale-base failure;
- unknown.

For `unknown`, inspect more evidence before source changes.

## Retry rule

A single bounded retry can be justified for a clearly transient failure only after logs show a transient mechanism and the retry does not risk duplicate destructive effects. Otherwise change strategy or report the blocker.

For one unchanged product failure fingerprint, use the normal bounded cycle: diagnosis -> at most one corrective edit -> one affected revalidation. If the same fingerprint remains, another symptom patch is not justified; return to ownership discovery or terminalize under the turn-terminalization contract.
