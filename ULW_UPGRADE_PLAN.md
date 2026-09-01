# Drive ULW upgrade checkpoint

Temporary checkpoint for the 2026-09-02 production upgrade pass.

Planned non-destructive upgrades:

- Android toolchain: AGP 9.4.0 / Gradle 9.6.0 / Kotlin 2.4.10 / Compose BOM 2026.08.00 / compileSdk 37.
- Preserve current applicationId (`com.example`) during this pass to avoid breaking update continuity for already-installed preview APKs.
- Harden activity lifecycle so sensor/network work cannot remain active after the controller UI leaves the foreground.
- Reduce hot-path allocation and scheduler jitter where it can be done without changing the 36-byte UDP protocol.
- Make preview release provenance point at the exact current source commit while preserving the stable download URL.
- Add device/performance diagnostics that expose packet interval/jitter and runtime health without forcing UI recomposition at transport rate.
- Explore receiver discovery as a protocol-compatible additive feature; do not break manual IP connection.

Completion requires CI green and a verified APK/release. Remove this checkpoint after the pass if no longer useful.
