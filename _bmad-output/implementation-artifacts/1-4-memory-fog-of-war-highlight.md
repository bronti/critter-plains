---
title: 'Memory (Fog-of-War) Highlight'
type: 'feature'
created: '2026-09-18'
status: 'in-progress'
route: 'dispatch'
review_loop_iteration: 0
context: ['{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md']
baseline_commit: '74f067775e7af8ca09ec45727d0f068897e1bca0'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The viewport has no way to see what a selected critter has and hasn't explored — every tile renders identically regardless of whether it's inside that critter's fog-of-war memory.

**Approach:** Add a new screen-space `OverlayView` (per epic Technical Decision) that, only while a critter is selected, tints every visible tile the critter hasn't remembered (`memory.area()` doesn't contain it) and leaves remembered tiles unchanged. It reads `SimulationController.selectedCritter` (Story 1.3's live, auto-clearing selection) each frame — nothing to build there, only to read from — and draws after the world buffer is blitted to screen, per the epic's fixed tint → indicator → badges draw order (this story implements only the tint sub-pass; `OverlayView` will gain the other two in later stories).

## Boundaries & Constraints

**Always:** `OverlayView` reads only live state each frame (`selectedCritter`, its `memory.area()`, `cellSize`, `viewportX`/`viewportY`) and never mutates world/selection state. Tint covers only the currently visible tiles (viewport-culled, same range convention `BufferedWorldView` already uses), never the whole map. Tint disappears the same frame `selectedCritter` becomes null, whether from Escape, click-elsewhere, or Story 1.3's death auto-clear. `OverlayView.render()` resets its own `Drawer` fill/stroke/strokeWeight at its top rather than relying on whatever `BufferedWorldView`'s blit left behind — this is the house convention Stories 1.6 and 1.8 must also follow when they add their own sub-passes to this same file.

**Never:** Do not implement the selection indicator arrow or status badges — Stories 1.6/1.8, even though they'll share this file later. Do not touch `BufferedWorldView` — it stays sim-ground-truth-only and selection-unaware. Do not touch Story 1.3's selection re-resolve/auto-clear mechanism in `SimulationController`. Do not add `mapWidth`/`mapHeight` accessors to `SimulationController` for this story — a tint tile computed slightly past the map edge at the viewport boundary is harmless (nothing else renders there either) and not worth new public surface.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Select a critter | `selectedCritter != null` | visible tiles outside `memory.area()` render tinted; tiles inside render unchanged | N/A |
| Memory grows over ticks | critter explores, `area()` gains a previously-fogged tile | that tile's tint disappears the same frame, with no code change needed beyond reading live state | N/A |
| Deselect (Escape / click-elsewhere / death) | `selectedCritter` becomes null | no tint anywhere, same frame | N/A |
| Nothing ever selected | `selectedCritter == null` from the start | `OverlayView.render()` draws nothing | N/A |
| Pan or zoom while selected | `viewportX`/`viewportY`/`cellSize` change | tint recalculates for the new visible tile range every frame — no stale positions from a prior viewport | N/A |

</frozen-after-approval>

## Code Map

- `visualization/src/main/kotlin/view/OverlayView.kt` (new) -- `class OverlayView(private val drawer: Drawer)` with `fun render(selectedCritter: CritterStateView?, cellSize: Double, viewportX: Double, viewportY: Double, viewportWidthPx: Int, viewportHeightPx: Int)`. No-ops entirely when `selectedCritter == null`. Otherwise mirrors `BufferedWorldView.kt`'s tile-iteration math (lines 20-28: `firstCol`/`firstRow` via `floor(viewportX/Y)`, `fracX`/`fracY`, `colCount`/`rowCount` via `viewportWidthPx/heightPx / cellSize + 1`) but computes each tile's world `Position` directly as `Position(firstCol + col, firstRow + row)` (no `Vector2`/`World` lookup needed, since only membership in `memory.area(): Set<Position>` matters, not terrain) and draws a tint `rectangle(screenX, screenY, cellSize, cellSize)` wherever `shouldTint(pos, selectedCritter.memory.area())` is true.
- top-level `internal fun shouldTint(pos: Position, memoryArea: Set<Position>?): Boolean = memoryArea != null && pos !in memoryArea` in the same file -- pure, unit-testable without an OPENRNDR window, mirroring `isOverMapViewport`/`panDelta`'s extraction pattern in `InputHandler.kt`.
- `visualization/src/main/kotlin/Visualization.kt` (`extend { }` block, lines 37-53) -- construct `val overlayView = OverlayView(drawer)` alongside `worldView`/`hudView` (near line 30-32); call `overlayView.render(controller.selectedCritter, controller.cellSize, controller.viewportX, controller.viewportY, controller.viewportWidthPx, controller.viewportHeightPx)` immediately after the buffer blit (line 44: `drawer.image(buffer.colorBuffer(0), ...)`), before `hudView`/`infoPanel` (lines 46-52).
- `visualization/src/test/kotlin/view/OverlayViewTest.kt` (new; no `visualization/src/test/kotlin/view/` directory exists yet) -- unit tests for `shouldTint` only (the pure, testable core): position inside `memoryArea` -> false; position outside -> true; `memoryArea == null` -> false regardless of position.
- `critters/src/main/kotlin/StateView.kt` (`CritterStateView.memory: MemoryStateView`, `MemoryStateView.area(): Set<Position>` at lines 15-32) -- read-only, no changes; confirmed `area()` already returns the set of *remembered* tiles.

## Tasks & Acceptance

**Execution:**
- [ ] `visualization/src/main/kotlin/view/OverlayView.kt` -- new `OverlayView` class with `render(...)` and the pure `shouldTint(...)` helper -- implements the tint sub-pass per epic Technical Decision
- [ ] `visualization/src/main/kotlin/Visualization.kt` -- construct `overlayView` and call `render(...)` right after the buffer blit -- wires the tint pass into the live render loop at the correct point in the fixed draw order
- [ ] `visualization/src/test/kotlin/view/OverlayViewTest.kt` -- three cases for `shouldTint` -- the only pure/testable logic this story adds; the tile-iteration/draw math stays untested, consistent with `BufferedWorldView`'s own already-accepted untested-render-math precedent (see `deferred-work.md`)

**Acceptance Criteria:**
- Given a critter is selected, when the frame renders, then every visible tile outside its `memory.area()` shows the tint and every tile inside does not.
- Given the selected critter's memory grows across ticks, when the frame renders, then a tile that just became remembered no longer shows the tint, with no stale carry-over from the previous frame.
- Given the critter is deselected by any means (Escape, click-elsewhere, or auto-clear on death), when the next frame renders, then no tint appears anywhere.
- Given nothing has ever been selected, when the frame renders, then `OverlayView` draws nothing.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Design Notes

Tint color/opacity is this story's one free implementation choice (the epic doesn't specify it, and the builder can retune it later without anyone else noticing) — a semi-transparent dark fill (e.g. `ColorRGBa(0.0, 0.0, 0.0, alpha)` at a middling alpha) reads as "fog" over the terrain colors underneath without fully hiding them. Verify `ColorRGBa`'s alpha actually blends as expected against the installed OPENRNDR 0.4.5 jar before hardcoding a value — same verification discipline Story 1.2 used for the scroll-event API, don't guess.

`shouldTint`'s signature takes `memoryArea: Set<Position>?` (nullable) rather than requiring the caller to branch on `selectedCritter` first, so the "nothing selected" case is just one more input to the same pure function instead of a separate code path — one fewer thing to keep in sync as this file grows two more sub-passes in later stories.

## Verification

**Commands:**
- `.\scripts\compile-check.ps1` -- expected: no compile errors.
- `.\scripts\build-quiet.ps1 test` -- expected: `BUILD SUCCESSFUL`, new `OverlayViewTest` passing.

**Manual checks (if no CLI):**
- Run the app, select a critter, confirm unexplored tiles look tinted and explored ones don't; let it move and confirm the tint shrinks live; deselect and confirm the tint vanishes immediately.
