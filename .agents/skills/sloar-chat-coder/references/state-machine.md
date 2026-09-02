# State machine

Sloar models repository engineering as explicit states. The states are procedural guardrails, not a requirement to narrate every step.

Forge health/capability is an orthogonal status overlay. A task can remain in IMPLEMENT or VERIFY with `LOCAL_READY + REMOTE_PARTIAL` or `LOCAL_READY + REMOTE_DEGRADED`, then enter a successful PUBLISH only when the required remote path can be proven. See [forge-resilience.md](forge-resilience.md).

## RECOVER

Enter when prior work may exist or state continuity is uncertain.

Must establish:
- surviving sandbox workspace status;
- known durable branch/PR/commit/artifact state;
- ownership of unfamiliar temporary resources before deleting them;
- whether any previous interruption was local-state loss, hosted-forge degradation, or a permission/policy capability block.

Exit when the candidate task state is known well enough to identify the intended source.

## IDENTIFY

Resolve mutable names to immutable identity.

Minimum evidence:
- repository identity;
- intended branch/PR/ref;
- resolved commit SHA;
- resolved tree SHA when available.

Exit when the exact source target is immutable.

## MATERIALIZE

Create a complete working tree for the identified source.

Must verify:
- checked-out HEAD equals expected commit SHA;
- tree identity equals expected tree when known;
- unexpected dirty state is absent or explicitly preserved and understood.

A collection of individually fetched source snippets is not a complete materialized working tree for repository-wide engineering.

## BRANCH

Use the target repository's branch policy. When isolated task branches are required, create from the verified immutable base, not from a stale mutable name.

Exit when the write target and base identity are known.

## IMPLEMENT

Make the smallest coherent change that satisfies the task. Preserve unrelated work. Use repository-native formatting, architecture, and dependency rules.

When the task creates, modifies, builds, signs, packages, or distributes an Android app/module, read [android-engineering.md](android-engineering.md) before Android implementation. If local execution exists, run `scripts/android-preflight.py <repo> --json` first. An empty/non-Android repository is a bootstrap state, not permission to guess package identity, toolchain compatibility, permissions, or release policy.

A hosted forge outage or one missing remote permission does not by itself prevent IMPLEMENT when exact source is already materialized and local execution remains valid.

## VERIFY

Choose checks based on change risk and repository declarations. Verification failures must be classified before source is changed.

For Android work, keep compile/test/artifact evidence separate from UI, device runtime, performance, thermal, and power evidence. CI-green APK generation does not prove real-device heat, battery, sensor, touch, OEM, or rendering behavior. Use [android-engineering.md](android-engineering.md) for the Android evidence contract and report unavailable physical-device scopes as unverified rather than inferred.

Local checks may continue during `REMOTE_PARTIAL` or `REMOTE_DEGRADED`, but record which required checks are still remote-only. Local green evidence supports `LOCAL_READY`; it does not imply REMOTE_VERIFY success.

Exit when required local checks pass or a concrete blocker is recorded in the evidence ledger.

## PUBLISH

Before writing remote state:
1. re-resolve mutable base/head refs;
2. compare them to expected identities;
3. stop if reconciliation is required;
4. confirm the required operation capability is available and publication is not `PUBLICATION_BLOCKED`;
5. publish exact verified bytes/objects;
6. verify the resulting remote head/diff.

If the forge is `REMOTE_DEGRADED`, preserve a checkpoint and defer publication rather than retrying indefinitely.

If it is `REMOTE_PARTIAL`, identify the exact missing permission/policy/approval. Preserve the verified tree and change the authorized operation path instead of retrying the same forbidden write or mutating correct product code.

## REMOTE_VERIFY

Run or inspect relevant CI, deployment, integration, production, or remote checks required by the repository/task. Do not treat a green unrelated check as evidence for the changed behavior.

After a forge outage, permission change, approval, or delayed publication, re-resolve remote identity before using the newly available path; remote state may have moved while the task was constrained.

## CLEANUP

Remove only task-owned temporary resources after terminal state is known. Do not delete unfamiliar workflows, branches, artifacts, or files merely because their names look temporary.

If publication was deferred, keep the minimal exact recovery artifact/checkpoint needed to resume safely rather than cleaning away the only durable copy of verified work.
