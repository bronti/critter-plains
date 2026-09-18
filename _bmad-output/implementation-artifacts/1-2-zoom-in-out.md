---
title: 'Zoom In/Out'
type: 'feature'
created: '2026-09-17'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/implementation-artifacts/epic-1-context.md']
baseline_commit: 'aa6c2d9c455b6133ec6dfb7c40f2e4ed612374a3'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The map renders at a fixed `cellSize` with no way to zoom in for detail or out for an overview. Separately, investigation for this story found that `BufferedWorldView` never offsets tile drawing by `viewportX`/`viewportY`, so Story 1.1's panning has no visible effect on screen today — only on click-to-tile mapping.

**Approach:** `Camera` gains a live, bounded zoom scale and a single `zoomAt(anchorScreenPos, factor)` entry point (per AD-1); scroll wheel and `+`/`-` keys both drive it. Fixing `BufferedWorldView`'s missing viewport offset is folded into this story since it already needs to start reading a live `cellSize` from `Camera` for zoom to render at all — the user approved this scope addition during investigation.

## Boundaries & Constraints

**Always:** All zoom math (bounds, scale, cursor/center anchoring) lives only in `Camera`, mirroring AD-1 — `SimulationController`/`InputHandler` only call through `camera.zoomAt`/`controller.zoom`. `BufferedWorldView` reads live `cellSize` and viewport offset every frame, never a stored/stale copy. Scroll-wheel zoom does nothing when the cursor is over the info panel (right of `controller.viewportWidthPx` in window coordinates). Zoom clamps cleanly at its min/max bounds, no overshoot or error. `SimulationController.visibleCols`/`visibleRows` must read through `camera` (today they're computed independently from a fixed `cellSize`, which would silently diverge once zoom makes `cellSize` live).

**Never:** Do not implement recenter, selection indicator, or memory highlight — Stories 1.5/1.6/1.4. Do not touch `:critters`, `HudView`, or `InfoPanel`. No animated/tweened zoom transitions — instantaneous per input, per epic UX notes. Key-based zoom (`+`/`-`) is one discrete step per keypress — never add it to the per-frame `KeyTracker` poll.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Scroll up over map | wheel up, cursor in map viewport | `cellSize` increases one factor step, anchored at cursor | N/A |
| Scroll down over map | wheel down, cursor in map viewport | `cellSize` decreases one factor step, anchored at cursor | N/A |
| Scroll over info panel | wheel event, cursor x ≥ `viewportWidthPx` | no zoom occurs | N/A |
| Press `+` / `-` | keyDown | one discrete zoom step, anchored at viewport center | N/A |
| Zoom in repeatedly past max | many zoom-in inputs | `cellSize` clamps at its max bound, no overshoot | N/A |
| Zoom out repeatedly past min | many zoom-out inputs | `cellSize` clamps at its min bound; if the now-larger `visibleCols`/`visibleRows` exceed map size, pan clamps to show the whole map | N/A |
| Pan then zoom | viewport panned off-origin, then zoom in/out | the world point under the anchor stays visually fixed; result still clamps to map bounds | N/A |
| Render after zoom change | `cellSize` changed this frame | `BufferedWorldView` draws tiles at the new size and correct viewport offset — no stale or scaled-blit artifacts | N/A |

</frozen-after-approval>

## Code Map

- `visualization/src/main/kotlin/simulation/Camera.kt` — currently: `mapWidth/mapHeight/visibleCols/visibleRows/cellSize` all `Int`, fixed at construction; `viewportX/viewportY: Int private set`; `pan(dx, dy)`; `mapPosition(pos): Position?`. Rework: constructor takes `viewportWidthPx`/`viewportHeightPx` instead of precomputed `visibleCols`/`visibleRows`; `cellSize` becomes `Double private set` with companion `MIN_CELL_SIZE`/`MAX_CELL_SIZE` bounds (owned solely here, per AD-1); `viewportX`/`viewportY` become `Double private set` (fractional, needed for pixel-exact zoom anchoring — `pan()` still steps by whole tiles); `visibleCols`/`visibleRows` become computed properties off the live `cellSize`; add `zoomAt(anchorScreenPos: Vector2, factor: Double)`.
- `visualization/src/main/kotlin/simulation/SimulationController.kt` — `camera` field (private), `scroll`/`mapPosition` one-line delegates, and independent `visibleCols`/`visibleRows` getters computed from the controller's own fixed `cellSize` (must switch to delegate through `camera.visibleCols`/`camera.visibleRows`, or they'll silently diverge once zoom changes `Camera`'s `cellSize` but not this one). Add a `zoom(anchorScreenPos, factor)` delegate and live `cellSize`/`viewportX`/`viewportY` getters for the renderer.
- `visualization/src/main/kotlin/input/InputHandler.kt` — the existing `keyboard.keyDown.listen` when-block (currently only `KEY_SPACEBAR`) is where `+`/`-` cases go, calling `controller.zoom(viewportCenter, factor)`. No mouse-scroll listener exists anywhere in the codebase yet — add one in `setup()` alongside the existing `mouse.moved`/`mouse.buttonDown` listeners, gated on `event.position.x < controller.viewportWidthPx`. Verify the exact OPENRNDR scroll-event API/property names against the installed jar before hardcoding — same verification discipline Story 1.1 used for `KeyTracker` key names, don't guess.
- `visualization/src/main/kotlin/Visualization.kt` — constructs `BufferedWorldView(drawer, controller.world, cellSize, buffer)` with a static `Int` and calls `worldView.render()` with no args each frame. Both change to pass live `controller.cellSize`/`controller.viewportX`/`controller.viewportY` into `render(...)` every frame instead of a construction-time constant.
- `visualization/src/main/kotlin/view/BufferedWorldView.kt` — ctor stores a static `cellSize: Int`; `render()` loops the *entire* `world.width x world.height` grid with `screenX = col * cellSizeD` — no viewport offset anywhere (this is the Story 1.1 pan-rendering bug). Rework: drop the stored `cellSize` field, take live `cellSize`/`viewportX`/`viewportY` as `render(...)` parameters, offset `screenX`/`screenY` by the viewport position, and only iterate tiles actually visible in the buffer instead of the whole map.
- `visualization/src/test/kotlin/simulation/CameraTest.kt` — JUnit 4, backtick test names, `newCamera()` fixture helper. Extend with zoom tests: anchor-preserving zoom in/out, min/max clamp, zoom while already panned.
- `visualization/src/test/kotlin/input/InputHandlerTest.kt` — tests the pure `panDelta` helper directly, no live window needed. If a pure zoom-factor/direction helper is extracted for the `+`/`-` handling (mirroring `panDelta`), test it the same way.

## Tasks & Acceptance

**Execution:**
- [x] `visualization/src/main/kotlin/simulation/Camera.kt` — rework constructor to accept `viewportWidthPx`/`viewportHeightPx`; add live `Double` `cellSize` (companion min/max bounds) and fractional `viewportX`/`viewportY`; add `zoomAt(anchorScreenPos, factor)` preserving the anchored world point; derive `visibleCols`/`visibleRows` from live `cellSize`.
- [x] `visualization/src/main/kotlin/simulation/SimulationController.kt` — construct `Camera` with the new signature; delegate `visibleCols`/`visibleRows` through `camera` instead of computing independently; add `zoom(anchorScreenPos, factor)` delegate; expose live `cellSize`/`viewportX`/`viewportY` getters.
- [x] `visualization/src/main/kotlin/input/InputHandler.kt` — verify OPENRNDR's mouse-scroll event API against the installed jar; add a scroll listener in `setup()` that calls `controller.zoom(...)` only when the cursor is left of `controller.viewportWidthPx`; add `+`/`-` cases to the existing `keyDown` when-block calling `controller.zoom(viewportCenter, factor)`.
- [x] `visualization/src/main/kotlin/Visualization.kt` — pass `controller.cellSize`/`controller.viewportX`/`controller.viewportY` into `worldView.render(...)` each frame instead of construction-time constants.
- [x] `visualization/src/main/kotlin/view/BufferedWorldView.kt` — accept live `cellSize`/`viewportX`/`viewportY` in `render(...)`; offset tile screen position by the viewport and only draw tiles actually visible in the buffer — fixes the missing-offset bug from Story 1.1 as part of this story's approved scope.
- [x] `visualization/src/test/kotlin/simulation/CameraTest.kt` — add zoom tests: anchored zoom-in/out preserves the world point under the anchor, clamps at min/max with no overshoot, interacts correctly with an already-panned viewport.

**Acceptance Criteria:**
- Given the simulation is running, when I pan or zoom, then the visibly rendered map — not just click-to-tile mapping — reflects the current viewport position and zoom level.
- Given `SimulationController`'s `visibleCols`/`visibleRows`, when zoom changes `cellSize`, then both values are read live through `Camera` and never diverge from its bookkeeping.
- Given continuous pan and discrete zoom both active across frames, when rendering, then no visible stutter occurs at the existing render rate (NFR1).

## Implementation Notes

- `Camera`'s bounds: `MIN_CELL_SIZE = 5.0`, `MAX_CELL_SIZE = 45.0` (companion constants) — chosen so the real app's 150×150 map fully fits in the 900×750px viewport at min zoom, and max zoom stays a reasonable detail level. `zoomAt` reuses the exact anchor-preserving formula from Design Notes, then re-clamps `viewportX`/`viewportY` to the new bound (verified by a dedicated test where the naive anchor formula and the clamp disagree).
- Verified OPENRNDR 0.4.5's mouse/key event API via `javap` against the installed jar (no sources jar available): `MouseEvents.scrolled: Event<MouseEvent>` exists, with `MouseEvent.rotation.y` as the scroll-delta axis. `KeysKt` has no named `Int` constant for printable/symbol keys — only special keys like `KEY_SPACEBAR` do — so discrete `+`/`-` zoom matches on `KeyEvent.name` instead (same convention `KeyTracker` uses for WASD): `"="`/`"+"` zoom in (main key and numpad), `"-"` zooms out.
- Extracted `zoomFactorForKey`/`zoomFactorForScroll` as pure top-level functions mirroring `panDelta`, unit-tested without a window. `+`/`-` zoom stays in the `keyDown` listener (one discrete step per press), never the per-frame `KeyTracker` poll, per the "never continuous" boundary.
- `BufferedWorldView.render()` now offsets tile screen position by the fractional viewport (`(col - fracX) * cellSize`) and reuses `SimulationController.mapPosition` (via the `simulation.World` wrapper) for each visible tile's world lookup — since that lookup re-divides by the same live `cellSize` used to build the lookup position, the two cancel algebraically and resolve to the correct absolute tile, so no separate offset math was duplicated in the renderer. Only tiles actually visible in the buffer are iterated (buffer size ÷ cellSize + 1), rather than the whole map — this also fixes the Story 1.1 bug where panning had no visual effect.
- **Matrix-audit follow-up:** the info-panel scroll boundary (`position.x < controller.viewportWidthPx`) was initially left as an untested inline check in `onMouseScrolled`, inconsistent with the pure/tested pattern used for the rest of the story's input logic. Extracted to `internal fun isOverMapViewport(x, viewportWidthPx)` and covered with 3 tests (inside viewport, over panel, exact boundary — boundary itself belongs to the panel, not the map).
- **Not run in this session:** the manual checks below (live scroll direction, `+`/`-` keypress, clamping, pan+zoom together) — no windowed display available to either implementing or orchestrating agent. Scroll-direction sign and the `+`/`-`/`=` key-name reading are a best-effort decompiled-bytecode interpretation, not confirmed against a live scroll wheel/keyboard.
- Worth watching (not blocking): tile count at `MIN_CELL_SIZE` is ~27k rectangle+circle draws/frame on the real viewport — noticeably more than the old fixed-`cellSize` case. Acceptable given the "only draw visible tiles" requirement, but no automated way to verify actual frame timing (NFR1) from here.

## Spec Change Log

## Review Triage Log

- **low** — `BufferedWorldView.render()`'s `colCount`/`rowCount` already add `+1` for the trailing partial tile, but the loops use inclusive ranges (`0..colCount`), drawing 2 extra tiles per axis instead of 1 (blind-hunter, verification-gap, edge-case-hunter — all three independently). Verified: confirmed at lines 27-31. Harmless (clipped by the RenderTarget), but wasteful every frame. → **patch**.
- **low** — `isOverMapViewport(x, viewportWidthPx)` only checks the x-axis despite the name implying full 2D containment (blind-hunter). Verified: correct today only because the window has no vertical panel (`height = viewportHeight` in Visualization.kt, full-height map region) — not a live bug, but the name overpromises. → **patch** (clarifying comment, not a speculative y-check for a panel that doesn't exist).
- **low** — README's Controls section documents pan/spacebar/hover/click but not the new zoom controls (blind-hunter). Verified: real gap, but `README.md` isn't part of this story's diff — it's mid-edit in a different, uncommitted session sharing this working tree. Not caused by this change. → **defer**.
- **low** — `BufferedWorldView.render(cellSize: Double, viewportX: Double, viewportY: Double)` takes 3 same-typed positional `Double`s at its only call site, a silent-transposition foot-gun (blind-hunter). Verified real. → **patch** (named arguments at the call site).
- **low, rejected** — zoom key bindings (`"="`/`"+"`/`"-"`) are hardcoded to US-layout glyphs; other layouts would lose keyboard zoom (blind-hunter). Verified real but: unlikely to be hit by this solo-hobby project's own builder, and a correct fix (scancode-based, layout-independent handling) is nontrivial — fails the reject-low bar on both counts.
- **low** — `World.width`/`World.height` (the `simulation.World` interface members) are no longer read anywhere after `BufferedWorldView` switched to deriving its own tile counts from buffer size + live `cellSize` (blind-hunter). Verified via repo-wide grep: no remaining consumer. → **patch** (delete the two now-dead members).
- **low** — no `CameraTest` covers a non-square viewport for `zoomAt`, so asymmetric X/Y clamping is unverified (blind-hunter). Verified: every zoom test uses a 150×150 viewport. → **patch** (test-only addition).
- **maybe-false** — fractional-viewport tile math in `BufferedWorldView.render()` (the `worldLookupPos`/`mapPosition` round-trip) has no test exercising it directly; a sign flip or wrong-position bug there wouldn't be caught (verification-gap, blind-hunter's floating-point round-trip note). Filed pre-verified by the verification-gap layer. What would settle it: extracting the pure tile-position math out of the OPENRNDR-coupled `render()` for unit testing, mirroring `panDelta`/`zoomFactorForKey`. → **defer** (per the layer's filed disposition — real refactor, not a proven defect, not blocking).
- **medium** — `Camera.mapPosition()`'s fractional-offset path (`viewportX`/`viewportY` now `Double` post-zoom) has no test with a non-integer offset; the only existing offset test happens to land on whole numbers (verification-gap). Filed pre-verified, with a concrete demonstration of how a regression here would go uncaught. → **patch** (add a `CameraTest` case with a fractional `viewportX`/`viewportY`).
- **rejected** — claim that `InputHandler.kt`'s `+`/`-` handling isn't literally inside the `keyDown` `when`-block as the Task text describes (edge-case-hunter). Verified true, but the only possible fix is correcting the spec's own Task wording — automatically rejected per "reject any finding whose fix is to edit this build's spec."

## Design Notes

`zoomAt` keeps the world point under the anchor pixel-fixed across a zoom step:

```
worldAnchor = viewportX + anchor.x / cellSize
cellSize' = (cellSize * factor).coerceIn(MIN_CELL_SIZE, MAX_CELL_SIZE)
viewportX' = (worldAnchor - anchor.x / cellSize')
                .coerceIn(0.0, max(0.0, mapWidth - viewportWidthPx / cellSize'))
-- same for Y
```

`viewportX`/`viewportY` move from `Int` to `Double` so the anchor point stays pixel-exact under the cursor through a zoom step; `pan()` still moves by whole tiles, so keyboard panning stays crisp and unaffected.

## Verification

**Commands:**
- `.\scripts\compile-check.ps1` — expected: no compile errors.
- `.\scripts\build-quiet.ps1 test` — expected: `BUILD SUCCESSFUL`, new `Camera` zoom tests passing.

**Manual checks (if no CLI):**
- Run the app; scroll over the map to zoom in/out anchored at the cursor; scroll over the info panel and confirm no zoom occurs; press `+`/`-` and confirm one discrete step per press anchored at viewport center; zoom to both extremes and confirm clean clamping; pan to a non-origin position, then zoom, and confirm the map visibly shifts and rescales — this also confirms the Story 1.1 pan-rendering bug is fixed.
