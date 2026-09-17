---
name: 'Adversarial Review — Critter Plains Visualization & Interaction Controls'
type: architecture-review
reviews: '../ARCHITECTURE-SPINE.md'
method: 'Construct pairs of stories that each obey every AD to the letter, then show the concrete incompatible build each pair would produce.'
created: '2026-09-17'
---

# Adversarial Review — ARCHITECTURE-SPINE.md

Methodology: for each of the 5 ADs, two hypothetical stories/implementers were constructed, each targeting a
different FR the AD governs, each reading the spine's Rule text literally and complying with it. Findings below
are the cases where both builds are individually correct-by-the-letter yet incompatible when merged. Findings are
grounded in the actual current source (`SimulationController.kt`, `BufferedWorldView.kt`, `InfoPanel.kt`,
`InputHandler.kt`, `StateView.kt`, `Game.kt`, `Critters.kt`) as read on review date, not just the spine text.

## Summary

| # | Severity | AD(s) | One-line description |
|---|---|---|---|
| 1 | Critical | AD-5, AD-2, AD-1 (scope) | Live-refresh of `selectedCritter` has no owner; two compliant fixes diverge on whether they touch `:critters`, violating the spine's own scope line |
| 2 | Critical | AD-5 | Auto-clear-on-death trigger point is unnamed; a rendering-layer local skip can satisfy one FR's acceptance criteria without ever firing the shared clear |
| 3 | Critical | AD-1 | "Screen space" origin (window vs. map-viewport sub-rectangle, panel-offset in/out) is never pinned down for `Camera`'s transforms |
| 4 | High | AD-1, AD-2 | Zoom-application strategy for the world buffer (re-render-per-zoom vs. fixed-resolution-buffer-scaled-blit) is unconstrained and both are AD-1/AD-2-legal |
| 5 | High | AD-2 | `Drawer` state (fill/stroke/alpha) leakage between OverlayView's sub-passes — no per-sub-pass reset convention is mandated |
| 6 | High | AD-4 | Key-based zoom (`+`/`-`) is absent from AD-4's explicit discrete-vs-held classification list |
| 7 | Medium | AD-2 | Z-order among tint / indicator / badges inside `OverlayView` is unspecified |
| 8 | Low | AD-3 | Tier→display-label mapping (`"1"`/`"2"`/`"3"` vs. tier name) has no single owner, invites duplicated logic |

**3 Critical, 3 High, 1 Medium, 1 Low.**

---

## Finding 1 — Critical: `selectedCritter` refresh has no owner, and the two honest fixes disagree on scope

**AD(s):** AD-5 (binds FR-5, FR-7), AD-1, and the spine's own scope line ("Does not touch `:critters`")

**Grounding.** Current `SimulationController.kt`:
```kotlin
var selectedCritter: CritterStateView? = null
    private set

fun selectAt(pos: Vector2) {
    selectedCritter = mapPosition(pos)?.let { state.occupant(it) }
}

fun update(seconds: Double) {
    if (!paused && seconds - lastUpdateTime >= updateInterval) {
        game.tick()
        state = game.stateView()          // <-- state is refreshed every tick
        ...
    }
}
```
`selectedCritter` is a **snapshot** (`CritterStateView` is a `data class`), captured once at click time via `state.occupant(pos)` — a lookup **by Position**. `state` itself is reassigned every tick, but `selectedCritter` is never re-derived from the new `state`. `GameStateView` (in `StateView.kt`) exposes only `territory(pos)` and `occupant(pos)` — there is no lookup by critter identity/name.

**The two FRs that need this to work:**
- FR-5's own acceptance criterion: *"As the simulation ticks forward and the critter's memory grows, previously-tinted tiles that become remembered lose their tint on the next render."* This requires `selectedCritter.memory` to be live, not the memory as it stood at the moment of the click.
- FR-7's own acceptance criterion: *"The indicator updates every render as the critter moves... staying accurate to the critter's live position."* Same requirement, for position instead of memory.

Since the critter *moves* every tick, you cannot re-fetch it by its old Position — `state.occupant(oldPos)` after a move returns `null` or the wrong critter. There is no by-name lookup to use instead.

**The divergence.** Two independently-compliant fixes:
- **Fix A (visualization-only, AD-1/AD-5-compliant on its face):** `SimulationController` rescans the full state grid every tick (`for x in 0..mapWidth, for y in 0..mapHeight: state.occupant(Position(x,y))`) looking for a `CritterStateView` whose `name` matches the previously-selected name, refreshing `selectedCritter` from that. Entirely inside `:visualization`.
- **Fix B (the "obvious" fix from inside `:critters`):** Add `fun critterByName(name: CritterName): CritterStateView?` to `GameStateView`/`Game.stateView()` — a one-line, natural-looking addition to `StateView.kt` / `Game.kt`, both of which live in the `critters/` module. This directly violates the spine's stated scope: *"Scope: ... Does not touch `:critters`."*

Both fixes satisfy their own FR's letter (AD-5's "no feature implements its own is-my-target-still-alive check" is about *death detection*, not about *how* the live view gets refreshed — the spine never addresses the refresh problem at all). Nothing in AD-1, AD-2, or AD-5 tells either implementer which module the fix belongs in, so whichever story lands first fixes it one way, and the other story either duplicates work differently or silently reaches across the `:critters`/`:visualization` boundary the spine claims is untouched.

**Suggested tightening.** Add an explicit rule (or a note under AD-5) naming the refresh mechanism and its owner: e.g. *"`SimulationController` re-derives `selectedCritter` from the fresh `state` every tick via [specific mechanism]; this may require extending `GameStateView` with a by-identity lookup — if so, that is an explicitly scoped exception to 'does not touch `:critters`', not an implementation detail left to whichever story hits it first."*

---

## Finding 2 — Critical: AD-5's auto-clear trigger point is unnamed

**AD:** AD-5 (binds FR-3, FR-4, FR-5, FR-6, FR-7)

**Grounding.** AD-5's Rule: *"Selection clearing is centralized: whatever code path detects the selected critter is no longer alive drives the exact same clear used by FR-4's manual deselect. No feature ... implements its own 'is my target still alive' check — they all simply stop rendering when `SimulationController.selectedCritter` becomes `null`."* This names the *consumer* contract (stop rendering on `null`) but never names *which code path* is responsible for performing the transition to `null`, nor when it runs.

**The divergence.** `InfoPanel.render()` today has a bare `// todo: critter death` with no guard at all — it will happily print a dead critter's stale name/hunger forever unless `selectedCritter` is actually nulled upstream.

- **Story A (FR-5, memory tint in `OverlayView`)** satisfies its own rendering requirement ("stop rendering when dead") with a *local* guard: `if (controller.selectedCritter?.isAlive != true) return` inside its own tint sub-pass. This technically obeys AD-5's letter — OverlayView stops rendering the tint — without ever calling the shared clear.
- **Story B (FR-6, recenter via `Camera`)** is the one that happens to touch `selectedCritter` on a `C` keypress, and is a natural place to put the actual "is it still alive, if not clear it" check, since a dead-critter recenter is the most visibly broken case.

Because Story A's local skip fully satisfies *its own* acceptance criteria in isolation, there is no compile-time or test-time pressure forcing it to call the shared clear. Result: after a selected critter dies, `OverlayView` immediately stops drawing the tint/indicator (looks correct), but `SimulationController.selectedCritter` is still non-null and `InfoPanel` keeps showing the dead critter's name and stale hunger — until the user happens to press `C` (triggering Story B's check) or clicks elsewhere. This is exactly the "stale reference across four features" bug AD-5's own "Prevents" clause claims to close, reproduced by two individually-compliant builds.

**Suggested tightening.** Name the owner and cadence explicitly: e.g. *"`SimulationController.update()` re-derives `selectedCritter` immediately after `game.tick()` each tick and nulls it there if `isAlive == false`; this is the only site permitted to write `selectedCritter = null` for a death; `OverlayView`/`InfoPanel` may early-return on `null` but must never implement their own liveness branch, only a null-check."*

---

## Finding 3 — Critical: "screen space" origin is never pinned down for `Camera`

**AD:** AD-1 (binds FR-1, FR-2, FR-6, FR-7)

**Grounding.** Current `SimulationController.mapPosition()`:
```kotlin
fun mapPosition(pos: Vector2): Position? {
    val cellX = (pos.x / cellSize).toInt() + viewportX
    val cellY = (pos.y / cellSize).toInt() + viewportY
    ...
}
```
This takes the raw OPENRNDR mouse `Vector2` with **no panel-offset subtraction** — it works today only because the map occupies the window from `x = 0`, and `InfoPanel` is drawn starting at a separate `xOffset` to its right. That convention ("world viewport's screen origin is the window's (0,0), not shifted by the panel") is implicit in the current code and is never written down as a contract anywhere in the spine.

AD-1's Rule states Camera is "the only owner ... every world↔screen conversion anywhere in `:visualization` ... calls into this one `Camera`," and gives one concrete signature: `zoomAt(anchorScreenPos: Vector2, factor: Double)`. It does not say what coordinate space `anchorScreenPos` — or any other screen-space value Camera consumes/produces — is defined in: full OPENRNDR window space (`program.window.size`, including the panel's pixel real estate) vs. the map's own sub-rectangle (`0,0` to `viewportWidthPx, viewportHeightPx`, panel excluded).

**The divergence.**
- **FR-2 (zoom)** implementer wires scroll-wheel zoom to `camera.zoomAt(event.position, factor)`, passing the raw `mouse.scrolled` event position directly — i.e., *window*-space, matching today's `mapPosition` pattern, with the implicit assumption "the map always starts at window (0,0)."
- **FR-7 (selection indicator)** implementer, needing to test "is the critter's tile within the current viewport" and "where's the edge of the viewport" to place the off-screen arrow, defines the viewport rectangle as `(0,0)` to `(viewportWidthPx, viewportHeightPx)` — the *map sub-region* only, deliberately excluding the panel, since drawing an edge-arrow at the literal window edge would put it on top of `InfoPanel`.

Both readings are individually consistent with AD-1's text ("calls into Camera," "one mechanism, two callers"). But if `Camera` internally represents "screen" as one of these and a consumer assumes the other, cursor-anchored zoom will anchor on the wrong tile whenever the cursor is near the panel boundary, and/or the FR-7 edge-arrow will be positioned using different viewport bounds than whatever `Camera.zoomAt` used for its own anchor math — two `worldToScreen`/`screenToWorld` call sites built against different implicit origins, exactly the "cellSize baked into three separate classes" failure mode AD-1 claims to close, just recreated one level up as a coordinate-space rather than a scale mismatch.

**Suggested tightening.** Add to AD-1's Rule: name the exact screen-space convention Camera uses (e.g. "all `Vector2` screen coordinates Camera accepts/returns are relative to the map viewport's own origin at `(0,0)`, sized `viewportWidthPx × viewportHeightPx`, explicitly excluding `InfoPanel`'s width — callers passing raw OPENRNDR window/mouse coordinates outside that rectangle must clamp/reject before calling Camera").

---

## Finding 4 — High: world-buffer zoom strategy is unconstrained

**AD(s):** AD-1, AD-2 (interaction)

**Grounding.** `BufferedWorldView.kt` renders the **entire map** (`for (col in 0 until world.width) for (row in 0 until world.height)`) into an off-screen `RenderTarget` at a fixed, constructor-injected `cellSize: Int` pixel size — not just the visible viewport. The Structural Seed table explicitly says `BufferedWorldView.kt` is "UNCHANGED contract — pure World renderer," and AD-2's Rule reasserts "no selection, no Camera awareness added to its signature." Yet AD-1 makes `Camera` the sole owner of zoom scale, and zoom must visibly change tile size on screen somehow.

**The divergence.** Two zoom strategies are equally legal under the letter of AD-1 + AD-2 (neither AD says which):
- **Strategy A:** `BufferedWorldView` is re-constructed or re-rendered with a new `cellSize` whenever `Camera`'s zoom changes (buffer content is always pixel-accurate to current zoom; contradicts "unchanged contract" since the constructor/call site now depends on live Camera state, but doesn't touch the *signature* of `render()` itself).
- **Strategy B:** `BufferedWorldView`'s buffer stays at a fixed base resolution/cellSize, and zoom is applied purely as a scale transform on the **blit** (`drawer.scale(...)` around `drawer.image(buffer)`), leaving `BufferedWorldView` genuinely untouched.

`OverlayView` (FR-5's tint, FR-7's indicator, FR-10/11's badges) computes its own tile-to-pixel placement via `Camera` per AD-2's Rule ("reading live state from Camera ... each frame"). Its math will only line up with the actually-rendered tiles if it assumes the same strategy the FR-2/zoom implementer picked. If FR-2's story picks Strategy A and OverlayView's story (built independently, taking "unchanged contract" at face value) assumes Strategy B, tint rectangles and the selection indicator will drift out of alignment with the visible tiles as soon as zoom is used — worse the further from 1.0× zoom.

**Suggested tightening.** AD-1 or AD-2 should state explicitly which strategy `Camera`/`BufferedWorldView` use for zoom (recommend Strategy B — scaled blit — since it's the only one that actually keeps `BufferedWorldView`'s contract unchanged as claimed), and that `OverlayView`'s Camera-driven transforms must be derived from the *same* base-resolution-plus-scale math, not a re-rendered-buffer assumption.

---

## Finding 5 — High: `Drawer` state leakage across `OverlayView`'s sub-passes

**AD:** AD-2 (binds FR-5, FR-7, FR-10, FR-11)

**Grounding.** OPENRNDR's `Drawer` is stateful (`fill`, `stroke`, alpha persist across calls until changed). The existing codebase's convention for this is visible in both `BufferedWorldView.render()` (`stroke = null` set once at top) and `InfoPanel.render()` (`drawer.stroke = null; drawer.fill = background` set at top) — **each top-level view resets the Drawer state it needs at the start of its own `render()` call.** This convention is informal (never written down as a rule) and, critically, was only ever exercised with *one* render pass per view class.

AD-2 merges four previously-separate concerns — memory tint (FR-5), selection indicator (FR-7), pause badge (FR-10), tier badge (FR-11) — into **one** `OverlayView` class drawn as multiple sub-passes within a single frame's render call. Nothing in AD-2's Rule or the Consistency Conventions table requires each internal sub-pass to reset Drawer state at its own entry point the way the existing top-level views do.

**The divergence.** FR-5's implementer writes the tint pass to set `drawer.fill = ColorRGBa.BLACK.opacify(0.5)` for a loop of rectangles, and — since it's the *first* sub-pass written and there's nothing after it in FR-5's own story to worry about — never resets `fill`/`alpha` afterward. FR-10/FR-11's implementer, building the badge text-draw sub-pass independently (and possibly merged into `OverlayView.render()` after the tint call, by whichever order the stories land), writes `drawer.fill = textColor; drawer.text(...)` assuming `fill` starts from a sane default — inheriting the tint pass's 50%-opacity black instead, producing a dim/wrong-colored "PAUSED" badge, without either story's code being individually wrong by its own AD-2 reading.

**Suggested tightening.** Add to AD-2: *"Each of `OverlayView`'s internal render sub-passes (tint, indicator, badges) must reset every `Drawer` property it uses (`fill`, `stroke`, `strokeWeight`) at its own entry, matching the existing `BufferedWorldView`/`InfoPanel` convention — no sub-pass may rely on the Drawer state left by whichever sub-pass ran before it, since draw order is not otherwise specified (see Finding 7)."*

---

## Finding 6 — High: key-based zoom (`+`/`-`) is unclassified under AD-4

**AD:** AD-4 (binds FR-1 directly; FR-2's key-zoom sits at the boundary)

**Grounding.** AD-4's Rule enumerates the actions that stay event-driven/discrete explicitly: *"select click, `Escape` deselect, spacebar pause, `1`/`2`/`3` tier switch, `C` recenter, scroll-wheel zoom (`mouse.scrolled`...)."* Key-based zoom (PRD FR-2's `+`/`-`, "equivalently to the scroll wheel") is **conspicuously absent** from that list — the list covers every other keyboard/mouse action in the epic except this one.

**The divergence.** AD-4's own "Prevents" clause frames the held-key `Set` mechanism as the pattern for *"any future hold-driven input feature."* 
- **FR-1's implementer**, building that generic held-key-polling mechanism and reading the "Prevents" clause as license to reuse it, may reasonably route `+`/`-` through the same `heldKeys: Set` + per-frame poll, giving continuous zoom-while-held (UX-consistent with hold-to-pan).
- **FR-2's implementer**, reading AD-4's explicit list literally and noting every *other* single-key action (`C`, `1`/`2`/`3`, `Escape`) is discrete `keyDown`-driven, may treat the unlisted `+`/`-` as analogous to those — implementing it as a one-shot `keyDown` handler calling `camera.zoomAt(...)` once per press.

Both are literally consistent with AD-4's text, since it never classifies this key. If merged, the two `InputHandler` designs conflict on whether `+`/`-` belongs in the held-key `Set` (continuous poll path) or the discrete `keyDown` dispatch (event path) — and if both land (e.g. one story's `keyDown` case plus the other's poll loop both present), `+`/`-` could double-fire `zoomAt` per frame while held, on top of a discrete additional call at press-time.

**Suggested tightening.** Add `+`/`-` explicitly to AD-4's discrete list (or explicitly to the held-key set) — the PRD's own NFR language ("equivalently to the scroll wheel," which is inherently a discrete per-notch event) suggests discrete is the intended reading, but the spine should say so rather than leave it inferable.

---

## Finding 7 — Medium: z-order among `OverlayView`'s draws is unspecified

**AD:** AD-2 (binds FR-5, FR-7, FR-10, FR-11)

**Grounding.** AD-2's Rule says memory tint, selection indicator, and pause/tier badges all render in `OverlayView`, "drawn directly to screen *after* the world buffer is blitted" — this fixes their order relative to the world buffer, but not relative to **each other**.

**The divergence.** FR-7's implementer assumes the selection indicator always renders on top (an arrow is useless if occluded). FR-10/FR-11's implementer assumes the same for badges (UI chrome should never be obscured). FR-5's memory tint, if implemented as a straightforward per-tile translucent overlay drawn in whatever order its sub-pass happens to be called in `OverlayView.render()`, could end up drawn *after* the indicator/badges if that story's code is appended last to the shared method (an accident of which story's PR lands second) — dimming or partially occluding the arrow/badges with the tint's semi-transparent fill. Nothing in AD-2 states a required stacking order (e.g. "world overlays first, then indicator, then UI chrome/badges last").

**Suggested tightening.** Add an explicit draw order to AD-2 or the Structural Seed: e.g. "tint → selection indicator → pause/tier badges," with badges always drawn last since they are fixed UI chrome, not world-space annotation.

---

## Finding 8 — Low: Speed Tier display-label has no single owner

**AD:** AD-3 (binds FR-9, FR-11)

**Grounding.** AD-3 fixes the enum (`SLOW(0.5)`, `NORMAL(1.0)`, `FAST(2.0)`) and says "`FR-11`'s indicator reads `controller.speedTier` directly — no comparison against a float," but never states what text/glyph the badge shows, nor where that mapping (`SpeedTier` → `"1"`/`"2"`/`"3"` or → tier name) lives.

**The divergence.** FR-9's implementer, wiring `1`/`2`/`3` keys to tiers, may add a convenience `SpeedTier.displayLabel` property (`SLOW -> "1"`, etc.) since it's a natural place to keep the key↔tier mapping symmetric. FR-11's implementer, built independently and only told to "read `controller.speedTier`," may instead write its own inline `when` mapping inside `OverlayView`. Not a functional break (`SpeedTier`'s declared order already matches 1/2/3), but duplicated, driftable logic with no single source of truth — e.g. a later rename of the enum's declaration order (unlikely but not forbidden by any Rule) would silently desync one of the two mappings.

**Suggested tightening.** Minor: state in AD-3 or the Consistency Conventions table that the tier↔display-text mapping lives once, either as a `SpeedTier` property or a single function `OverlayView` calls, not duplicated.
