# Review: ARCHITECTURE-SPINE.md — Critter Plains Visualization & Interaction Controls

**Reviewed:** 2026-09-17
**Reviewer method:** Read the spine, the driving PRD, and the addendum; verified claims about the brownfield codebase against the actual source (`SimulationController.kt`, `BufferedWorldView.kt`, `InputHandler.kt`, `Visualization.kt`, `World.kt`, `HudView.kt`, `InfoPanel.kt`, `StateView.kt`, `Game.kt`, `MutablePlacement.kt`, `Critters.kt`, `gradle/libs.versions.toml`), and independently confirmed the OPENRNDR keyboard-API claim against `guide.openrndr.org`.

## Overall Verdict

The spine correctly diagnoses the real brownfield divergence (the triplicated `cellSize`/viewport math) and gives it a clean single-owner fix, but it ships with two critical, concrete gaps — a self-contradiction between AD-1 and AD-2 over how zoom actually reaches the world-buffer draw loop, and an unaddressed mechanism for keeping the *selected* critter's position/state live tick-to-tick, which FR-5/FR-6/FR-7 and AD-5 all silently depend on — so it needs one more pass before stories can be cut from it without two reasonable implementers diverging.

## Per-AD Judgment

| AD | Does the Rule actually prevent its stated divergence? |
| --- | --- |
| **AD-1** — Camera owns viewport/zoom/transform math | Mostly yes — correctly identifies and closes the real triplication (verified: `cellSize` is a separate constructor-fixed field in both `SimulationController.kt` and `BufferedWorldView.kt`, plus implicitly sizes the fixed-pixel `renderTarget` in `Visualization.kt`). The single `zoomAt()` entry point cleanly resolves PRD Open Question 2. **But** the rule text names the "world-buffer draw loop" as a required Camera consumer, which directly contradicts AD-2's ban on Camera awareness in that same file (see Finding C1) — so as written, the rule cannot be satisfied together with AD-2. |
| **AD-2** — Overlay pass separate from world buffer | Good separation of concerns, and its claim about `HudView` being unaffected is verified accurate (it already draws post-blit in `Visualization.kt`'s `extend {}`). **But** it conflicts with AD-1 over whether `BufferedWorldView` needs a live `cellSize` to make zoom actually visible in the world buffer — see Finding C1. As written, unenforceable together with AD-1. |
| **AD-3** — Speed Tier first-class, interval derived | Yes. Directly implements the addendum's own recommended option (c) for PRD Open Question 4, sidesteps the `Int`-fps rounding problem, and is unambiguous about direction of derivation (`multiplier → interval`, never the reverse). Only a minor omission (Finding L1). |
| **AD-4** — Held-key polling, not GLFW repeat | Yes, and its cited technical claim is verified correct: OPENRNDR's `Keyboard` exposes only `keyDown`/`keyUp`/`keyRepeat`/`character` — no held-key query — confirmed against `guide.openrndr.org/interaction/mouseAndKeyboardEvents.html`. Sound and enforceable. |
| **AD-5** — Selection auto-clears on death | No, not as written. The rule ("whatever code path detects the selected critter is no longer alive...") never says which code path does the detecting, and more fundamentally presupposes a live, continuously-refreshed view of the selected critter that nothing in the spine actually provides — see Finding C2. `SimulationController.selectedCritter` today is a one-shot snapshot; nothing re-resolves it as the sim ticks. Without that, "detects... no longer alive" has no mechanism to run on. |

## Findings

### Critical

**C1 — AD-1 and AD-2 contradict each other over whether `BufferedWorldView` needs Camera-owned zoom state, and the Structural Seed silently sides with AD-2, breaking FR-2.**
- **Cites:** AD-1 ("world-buffer draw loop... calls into this one `Camera`"); AD-2 ("no selection, no Camera awareness added to its signature"); Structural Seed (`BufferedWorldView.kt # UNCHANGED contract — pure World renderer`).
- **Evidence:** `BufferedWorldView` (`visualization/src/main/kotlin/view/BufferedWorldView.kt`) takes `cellSize: Int` as a constructor-time `val`. `SimulationController.visibleCols`/`visibleRows` are live `get()` properties that shrink as `cellSize` grows — the design already depends on tile *count* changing live with zoom — but the tile *pixel size* baked into `BufferedWorldView.render()`'s `rectangle(...)` calls is fixed at construction and can never reflect a live zoom change if AD-2's ban is honored literally. Two implementers following AD-2 as written would leave the world buffer rendering at a stale scale while only overlay/click math "zooms" — a visibly broken, inconsistent FR-2.
- **Fix:** Pick one and state it: (a) carve a narrow, explicit exception into AD-2 for `BufferedWorldView` reading a plain `cellSize: Int` from `Camera` each `render()` call (distinct from selection/overlay awareness), and change the Structural Seed row to `BufferedWorldView.kt — MODIFIED: reads live cellSize from Camera`; or (b) keep `BufferedWorldView` truly frozen and instead implement zoom by scaling the final blit (`drawer.image(buffer.colorBuffer(0), ...)`) rather than varying `cellSize`, and say so explicitly in AD-1/AD-2.

**C2 — No mechanism is specified for keeping the selected critter's position/state live tick-to-tick; FR-5/FR-6/FR-7 and AD-5 all depend on it, and the natural fix crosses the spine's own `:critters` boundary.**
- **Cites:** AD-5; Capability Map rows FR-5, FR-6, FR-7; spine `scope` frontmatter ("Does not touch `:critters`").
- **Evidence:** `GameStateView` (`critters/src/main/kotlin/StateView.kt`, implemented in `Game.kt`) exposes only `occupant(pos: Position): CritterStateView?` — position-keyed, no identity lookup, no enumeration. `SimulationController.selectedCritter` (`visualization/src/main/kotlin/simulation/SimulationController.kt:32,49-51`) is set once in `selectAt()` and is **never reassigned** in `update()` when the tick advances — it's a frozen snapshot from click time. `Critter.view()` (`StateView.kt:34`) builds a disconnected copy each call, so there is no live reference anywhere for `:visualization` to hold. Yet FR-6 needs the critter's *current* tile, FR-7 needs the indicator to "update every render as the critter moves," and FR-5 needs the tint to shrink "as the simulation ticks forward and the critter's memory grows." AD-5 only covers clearing-on-death and implicitly assumes this live-refresh problem is solved elsewhere in the spine — it isn't, anywhere.
- **Fix:** Either (a) add one minimal identity-keyed lookup to `:critters`' `GameStateView` (e.g. `fun critter(name: CritterName): CritterStateView?`) and amend the spine's scope line to acknowledge this one narrow, additive exception to "does not touch `:critters`"; or (b) if the boundary must hold absolutely, specify explicitly (as a new or expanded AD) how `:visualization` re-resolves a moving selection using only position-keyed lookups — e.g. cache `lastKnownPosition` and, immediately after each `game.tick()`, search the 1-tile neighborhood for a critter with matching `name` (bounded, since critters move at most one tile per tick) — but this is a real design decision that belongs in the spine, not left for story-level implementers to each invent separately.

### High

**H1 — `Visualization.kt` is required to change for nearly every AD, but is absent from the Structural Seed entirely.**
- **Cites:** Structural Seed; AD-1 (Camera composition at construction), AD-2 (new `OverlayView` instantiation + render call), AD-4 (per-frame held-key poll needs a call site in the `extend {}` loop), FR-2 (scroll-wheel listener wiring).
- **Evidence:** `Visualization.kt` (`visualization/src/main/kotlin/Visualization.kt`) is the sole place `SimulationController`, `BufferedWorldView`, `HudView`, and `InfoPanel` are constructed and wired into the `extend {}` per-frame loop. None of AD-1 through AD-4 can be realized without editing it (new `Camera`/`OverlayView` wiring, new listener registration, new per-frame poll call), yet it's the one file in the whole call chain missing from the Structural Seed's file list — a whole integration dimension left silent.
- **Fix:** Add `Visualization.kt — MODIFIED` to the Structural Seed with a one-line note on what changes (constructs `Camera`, adds `OverlayView.render()` post-blit, wires the scroll listener, invokes the per-frame held-key poll).

### Medium

**M1 — AD-5 doesn't say which class performs the "detects... no longer alive" check, inviting exactly the per-feature divergence it claims to prevent.**
- **Cites:** AD-5.
- **Fix:** State explicitly that the liveness check happens once, in `SimulationController` (e.g. at the top of `update()`, right after `game.tick()`), so every other view is a pure reader of `selectedCritter == null`. This fix is naturally bundled with resolving C2, since both need the same live-refresh call site.

### Low

**L1 — AD-3 doesn't state how the new `baseInterval` is seeded from the existing `initialFps: Int` constructor parameter.** Low risk (there's really only one sensible mapping, `baseInterval = 1.0 / initialFps`), but a half-sentence would remove even that ambiguity.

**L2 — AD-5 doesn't cite PRD Open Question 5 as the open question it resolves**, the way AD-1 explicitly cites OQ2 and AD-3 cites OQ4. Traceability nit only, no functional risk.

## Checklist Coverage Notes (for completeness, not separate findings)
- **FR-1 through FR-11:** all present in the Capability → Architecture Map; no PRD capability is missing.
- **Deferred section:** correctly single-owns the zoom bounds/step and key-pair items (no cross-unit divergence risk there); deployment/packaging/environment and version pins are both explicitly deferred with stated reasons, satisfying the "whole dimension left silent" check for those two dimensions specifically. The dimensions actually left silent are the two called out above (C1's zoom-to-buffer mechanism, C2's live-selection mechanism) — neither is deferred, decided, or listed as an open question, which is exactly what makes them findings rather than acceptable scope cuts.
- **Named tech:** OPENRNDR 0.4.5 confirmed against `gradle/libs.versions.toml`; the AD-4 "no held-key query" claim confirmed against current OPENRNDR guide docs.
- **Brownfield ratification:** all other factual claims about the existing codebase checked out against source (triplicated `cellSize`, `MemoryStateView.area()`, `CritterStateView.isAlive`, `HudView` post-blit draw order, `fps` private-set field, `InputHandler`'s current `keyDown`-only listener).
