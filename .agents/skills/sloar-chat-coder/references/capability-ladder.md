# Capability ladder

Always use the lowest sufficient level. Convenience is not a reason to escalate.

## L0 — Sandbox native

The sandbox already has the required source and capability. Use normal repository tools directly.

Examples: git, node, python, compiler, browser, test runner already present.

## L1 — Sandbox acquisition

The sandbox can faithfully run the capability after installing or downloading ordinary disposable dependencies itself.

Escalate only after inspecting the acquisition failure rather than assuming the sandbox cannot support the tool.

## L2 — Connected repository transport

Use authorized repository APIs/connectors for exact reads/writes, Git objects, branches, PRs, logs, artifacts, or other bounded repository operations when ordinary sandbox network transport is unavailable or inefficient.

Prefer native Git object operations for large exact multi-file publication when available and reliable.

## L3 — Supply mission

A bounded remote runner acquires or packages bytes the sandbox cannot obtain but can use faithfully.

Examples:
- exact source bundle;
- browser/runtime distribution;
- SDK/compiler cache;
- binary dependency;
- generated artifact from a trusted declared build input.

Return the verified artifact to the sandbox, then continue the normal engineering loop there.

## L4 — Bounded remote execution

Use only when the sandbox cannot faithfully execute the required capability even after inputs are supplied, or the sandbox itself cannot sustain the task.

Each mission must be non-interactive, bounded, identity-gated, and terminal.

## L5 — Blocked

Use when no authorized exact path can complete the required operation without inventing state, weakening verification, or violating cost/security constraints.

Report the concrete missing capability rather than pretending completion.
