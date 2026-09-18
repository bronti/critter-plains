# Tech Verification Review — ARCHITECTURE-SPINE.md

**Target:** `_bmad-output/planning-artifacts/architecture/architecture-critter-plains-visualization-controls-2026-09-17/ARCHITECTURE-SPINE.md`
**Reviewed:** 2026-09-17
**Scope:** Every committed technical claim in the spine — library/framework versions, technology existence/fit, and specific API assertions — checked against primary sources (guide.openrndr.org, github.com/openrndr/openrndr, GitHub API) and, where the claim was about the existing codebase, against the actual repo.

## Verdict: Issues found

Not a factual-error verdict — both explicitly "web-verified" claims in the spine check out against the primary source. The issue is a **verification completeness gap**: the research behind AD-4 stopped one layer too early and missed an existing framework convenience that materially changes the recommendation. There's also an unflagged version-freshness gap worth surfacing.

---

## Findings

### 1. [MEDIUM] AD-4's held-key claim is narrowly true but the research stopped short — OPENRNDR already ships the exact convenience class AD-4 tells `InputHandler` to hand-roll

**Claim in spine (AD-4):** `InputHandler` maintains its own `Set` of held key codes via `keyDown`/`keyUp`, tagged `[web-verified: OPENRNDR's Keyboard exposes only discrete keyDown/keyUp/keyRepeat/character events — no built-in held-key query — guide.openrndr.org/interaction/mouseAndKeyboardEvents.html]`.

**What I checked:**
- `guide.openrndr.org/interaction/mouseAndKeyboardEvents.html` — fetched directly. Confirms the `Keyboard` class exposes exactly `keyDown`, `keyUp`, `keyRepeat`, `character` and nothing else. **This part of the claim is accurate.**
- Source, tag `v0.4.5` (the project's pinned version), `openrndr-application/src/commonMain/kotlin/org/openrndr/Keys.kt` — fetched via raw GitHub content at the exact pinned tag. Confirms `Keyboard` (implements `KeyEvents`) exposes only those four events and nothing that queries held state.

**What the claim misses:** The same file, same module, same `org.openrndr` package, **same v0.4.5 tag**, also defines a `KeyTracker` class:
- Constructor takes a `KeyEvents` instance (i.e. wraps any `Keyboard`).
- Internally subscribes to `keyDown`/`keyUp` and maintains a private mutable set.
- Exposes `pressedKeys: Set<String>` — a read-only, continuously-updated view of currently-held keys.

Confirmed in use in the official demo `openrndr-demos/src/main/kotlin/DemoKeyTracker01.kt` (same v0.4.5 tag):
```kotlin
val keyTracker = KeyTracker(keyboard)
extend {
    for (key in keyTracker.pressedKeys) { ... }
}
```

This is **the exact mechanism** AD-4 instructs `InputHandler` to build from scratch — subscribe to `keyDown`/`keyUp`, maintain a set, expose held keys — already shipped in core OPENRNDR, importable as `org.openrndr.KeyTracker`, no extra dependency (it's in `openrndr-application`, already on the classpath).

**Why this matters:** AD-4's own stated rationale is "prevents ... any future hold-driven input feature inventing its own tracking mechanism." As written, AD-4 has `InputHandler` invent a tracking mechanism that OPENRNDR already provides. This isn't a factual error in the spine's citation — the citation is correct as far as it goes — but the underlying research question should have been "does OPENRNDR expose a way to query held keys" (broader), not "does the `Keyboard` class expose one" (narrower). The narrower question was answered correctly; the broader, actually-relevant one wasn't asked.

**Recommendation:** AD-4 should either (a) reuse `KeyTracker` directly instead of hand-rolling the set, or (b) explicitly justify why hand-rolling is preferred over `KeyTracker` (e.g. if `pressedKeys: Set<String>` — keyed by name, not key code — is a worse fit than a custom `Set<KeyCode>`/`Int` set for `Camera.pan()`'s call site). Either way this should be a conscious decision recorded in the spine, not an unexamined gap.

**Sources checked:**
- https://guide.openrndr.org/interaction/mouseAndKeyboardEvents.html
- https://raw.githubusercontent.com/openrndr/openrndr/v0.4.5/openrndr-application/src/commonMain/kotlin/org/openrndr/Keys.kt
- https://raw.githubusercontent.com/openrndr/openrndr/v0.4.5/openrndr-demos/src/main/kotlin/DemoKeyTracker01.kt

---

### 2. [VERIFIED CLEAN] `mouse.scrolled` / `rotation` claim

**Claim in spine (AD-4):** scroll-wheel zoom uses `mouse.scrolled`, which "carries `rotation` as delta."

**What I checked:** Source, tag `v0.4.5`, `openrndr-application/src/commonMain/kotlin/org/openrndr/Mouse.kt`:
- `ApplicationMouse.scrolled` is declared as `Event<MouseEvent>("mouse-scrolled", postpone = true)`.
- `MouseEvent` carries a `rotation: Vector2` field, documented as "the rotational change of the mouse event, typically used for scroll or gesture detection."

**Result:** Accurate. `rotation` exists, is populated on the `scrolled` event, and is a reasonable source for a zoom delta (spine correctly leaves which component/scale to use as an implementation detail owned by `Camera`).

**Source:** https://raw.githubusercontent.com/openrndr/openrndr/v0.4.5/openrndr-application/src/commonMain/kotlin/org/openrndr/Mouse.kt

---

### 3. [LOW] OPENRNDR 0.4.5 is not stale, but a newer minor version exists and deprecates the current backend — spine doesn't flag this even implicitly

**Claim in spine (Deferred section):** "OPENRNDR/Kotlin/JVM version pins — unchanged, already fixed project-wide (OPENRNDR 0.4.5, Kotlin 2.0, JVM 17); this epic introduces no new dependency." No currency claim is explicitly made, but the spine builds AD-4's API claims against 0.4.5 without noting version context.

**What I checked (GitHub API, authoritative — not the AI-summarized release notes, which I cross-checked and found consistent):**
```
v0.4.5 published_at: 2025-09-06T11:05:56Z
v0.5.0 published_at: 2026-09-16T08:57:22Z   (one day before this review)
```
0.5.0's release notes: adds an SDL3-based backend; **"the GLFW backend is still available but has been deprecated."** Also bumps to Kotlin 2.4.20, Gradle 9.7.1, LWJGL 3.4.3.

**Assessment:** 0.4.5 is roughly a year old and is now one minor version behind a release that deprecates (but doesn't remove) the backend 0.4.5 uses. This is **not "obviously stale"** — 0.4.5 is still on Maven Central, still functional, GLFW still works in 0.5.0 — but it's no longer current, and the gap is more than cosmetic (backend deprecation, not just a patch bump). Since the spine explicitly puts version pins out of scope for this epic, this doesn't invalidate anything in the document. Flagging it so the decision to stay on 0.4.5 is a known tradeoff rather than an unexamined one, especially since AD-4/AD-1's input-handling code is exactly the kind of code a GLFW→SDL3 backend migration would eventually touch.

**Sources:**
- https://api.github.com/repos/openrndr/openrndr/releases/tags/v0.4.5
- https://api.github.com/repos/openrndr/openrndr/releases/tags/v0.5.0
- https://github.com/openrndr/openrndr/releases/tag/v0.5.0

---

### 4. [VERIFIED CLEAN] Internal codebase claims (not external, but asserted as current fact — reality-checked against the repo)

Cross-checked against `D:\brontozyablik\critter-plains` at the current commit:

| Claim | Checked against | Result |
| --- | --- | --- |
| "Today's `cellSize` is a `val` baked into three separate classes" (AD-1) | `Visualization.kt` (`cellSize` param), `SimulationController.kt`, `BufferedWorldView.kt` | Confirmed — `cellSize` appears independently in exactly these three files. |
| "`updateInterval` (already `Double`)" (AD-3) | `visualization/src/main/kotlin/simulation/SimulationController.kt:46` — `private val updateInterval: Double get() = 1.0 / fps` | Confirmed. |
| "`BufferedWorldView` today takes only `World`" (AD-2) | `visualization/src/main/kotlin/view/BufferedWorldView.kt:10-12` — constructor takes `world: World` only | Confirmed. |
| "`HudView`'s ... tooltip already draws to screen post-blit" (AD-2) | `Visualization.kt` `extend {}` block — blit at line 42, `hudView.render(...)` at line 47 | Confirmed, in that order. |

No discrepancies found.

---

## Other technical claims in the document

No other named-technology or specific-API claims requiring independent verification were found beyond the two AD-4 items and the version pin. `Kotlin 2.0` / `JVM 17` are pre-existing, already-fixed project facts (per `AGENTS.md`) restated by the spine, not new claims introduced by it — not in scope for re-verification here. The proposed new types (`Camera`, `OverlayView`, `SpeedTier`, `zoomAt(...)`) are original design, not assertions about external reality, so "web-research" doesn't apply to them.

## Summary

| Severity | Count |
| --- | --- |
| Critical | 0 |
| High | 0 |
| Medium | 1 |
| Low | 1 |
| Verified clean | 6 (guide.openrndr.org Keyboard API, Mouse.scrolled/rotation, and 4 internal-codebase claims) |

The two explicitly `[web-verified]` claims in AD-4 are both accurate as stated. The real gap is scope: the research answered "does `Keyboard` have a held-key query" (no) but not "does OPENRNDR have a held-key query anywhere" (yes — `KeyTracker`), which weakens AD-4's design rationale. Recommend the spine either adopt `KeyTracker` or explicitly justify not using it before this moves to implementation.
