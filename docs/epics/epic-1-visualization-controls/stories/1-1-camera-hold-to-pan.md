---
title: 'Camera & Hold-to-Pan'
type: 'feature'
created: '2026-09-17'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context: ['_bmad-output/implementation-artifacts/epic-1-context.md']
baseline_commit: '7af2f3f567e37ab0b864a7dc5485d5b84f8a66f5'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `InputHandler` reacts to `keyDown` for `W`/`A`/`S`/`D`, so holding a key pans only once per press, not continuously; there's no arrow-key support; and viewport position lives directly on `SimulationController`, which later stories (zoom, recenter, the selection indicator) need a single shared owner for instead.

**Approach:** Introduce a new `Camera` class owning viewport position (zoom joins it in Story 1.2); `SimulationController` composes it instead of holding `viewportX`/`viewportY` itself. `InputHandler` switches the held-direction path to OPENRNDR's built-in `KeyTracker`, polled once per frame from the render loop, driving continuous pan for both `WASD` and arrow keys.

## Boundaries & Constraints

**Always:** Viewport-position math lives only on `Camera` — `SimulationController` must not keep parallel `viewportX`/`viewportY` fields. Panning clamps to map bounds exactly as today's `scroll()` does, no overshoot. The held-direction path polls `KeyTracker.pressedKeys` once per frame in `extend {}` — never `keyDown`/`keyRepeat` for that path.

**Never:** Do not implement zoom, recenter, or the selection indicator here — those are Stories 1.2/1.5/1.6, only `Camera`'s position/pan capability belongs in this story. Do not touch `BufferedWorldView`, `HudView`, `InfoPanel`, or `:critters`. Do not special-case diagonal panning — it must fall out of independent X/Y deltas. Existing discrete actions (spacebar pause, click-to-select) stay event-driven, unchanged.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Hold W | `W` held across N frames | viewport y decreases each frame until released or top edge reached | N/A |
| Hold two perpendicular keys | `W`+`D` held together | viewport pans diagonally (both axes move) | N/A |
| Hold toward an edge already reached | viewport at `y=0`, `W` held | viewport stays clamped at `y=0`, no overshoot | N/A |
| Release all direction keys | none held | panning stops on the next frame | N/A |
| Hold an arrow key | Up arrow held | identical behavior to holding `W` | N/A |
| Click to select while panning | click during a held pan | selection still resolves normally (FR3 regression) | N/A |

</frozen-after-approval>

## Code Map

- `visualization/src/main/kotlin/simulation/SimulationController.kt` — owns `viewportX`/`viewportY: Int` (private set), `scroll(dx, dy)` clamps via `mapWidth`/`mapHeight`/`visibleCols`/`visibleRows`, `mapPosition(pos: Vector2): Position?` converts screen→world using `cellSize` + viewport. Extract position/clamping into `Camera`; keep `mapPosition`/`scroll`'s existing signatures so `InputHandler.onMouseClicked` and `selectAt` need no changes.
- `visualization/src/main/kotlin/input/InputHandler.kt` — `KEY_W/A/S/D` are `Int` codes compared in `keyboard.keyDown.listen`, one-shot per press; `SCROLL_SPEED = 3`. Add a `KeyTracker(program.keyboard)`-backed method polled once per frame (new, called from `Visualization.kt`), checking held key names for `WASD` + arrow keys. Verify exact arrow-key name strings against the installed OPENRNDR source/jar (`org.openrndr.Keys`) before hardcoding — don't guess. Spacebar `keyDown` handling is untouched.
- `visualization/src/main/kotlin/Visualization.kt` — constructs `InputHandler`, calls `inputHandler.setup()` once outside `extend {}`. Add a call to the new per-frame poll method inside the existing `extend { ... }` block.

## Tasks & Acceptance

**Execution:**
- [x] `visualization/src/main/kotlin/simulation/Camera.kt` — create `Camera`: viewport position (tiles) + `pan(dx, dy)` clamped to map bounds + the `mapPosition`-equivalent screen→world conversion — establishes the single-owner pattern Stories 1.2/1.5/1.6 extend.
- [x] `visualization/src/main/kotlin/simulation/SimulationController.kt` — compose `Camera`; delegate `scroll`/`mapPosition` to it, dropping the local `viewportX`/`viewportY` fields.
- [x] `visualization/src/main/kotlin/input/InputHandler.kt` — add the `KeyTracker`-based per-frame poll for `WASD` + arrows, calling `Camera.pan()`; leave `keyDown` spacebar/click handling as-is.
- [x] `visualization/src/main/kotlin/Visualization.kt` — call the new poll method inside `extend { }`.

**Acceptance Criteria:**
- Given the simulation is running, when I hold `W`/`A`/`S`/`D` or an arrow key, then the viewport pans continuously in that direction until released or a map edge is reached.
- Given I hold two perpendicular direction keys, when both are held, then the viewport pans diagonally.
- Given the viewport is already at a map edge, when I keep holding the key toward it, then it stays clamped with no overshoot.
- Given viewport position now lives on `Camera`, when `SimulationController` needs bounds for click-mapping or `visibleCols`/`visibleRows`, then it reads through `Camera`.
- Given panning is active, when frames render, then no visible stutter occurs at the existing render rate.

## Implementation Notes

- New `Camera` (`visualization/src/main/kotlin/simulation/Camera.kt`) owns `viewportX`/`viewportY: Int` (public, private-set), `pan(dx, dy)` (renamed from `scroll` at this layer, clamped exactly as the old `SimulationController.scroll` was) and `mapPosition(pos: Vector2): Position?`. Constructed once in `SimulationController` with `mapWidth`, `mapHeight`, `visibleCols`, `visibleRows`, `cellSize` — all of which are already-stable values at construction time (map size and pixel dimensions never change post-construction), so capturing them once is equivalent to the old per-call `get()` computation.
- `SimulationController.scroll`/`mapPosition` keep their exact old signatures and now one-line delegate to `camera.pan`/`camera.mapPosition`, so `InputHandler.onMouseClicked`/`selectAt` and `World.kt`'s extension needed no changes, as the spec required.
- Verified the exact `KeyTracker` key-name strings by decompiling the pinned OPENRNDR `0.4.5` jars (`openrndr-application-jvm`, `openrndr-gl3-jvm`) rather than guessing: `org.openrndr.KeyTracker` wraps a `Keyboard`'s `keyDown`/`keyUp` events into a `Set<String>` called `pressedKeys`. The GLFW key-code → name mapping (in `ApplicationGLFWGL3.loop$lambda$44`) hardcodes `"arrow-up"`/`"arrow-down"`/`"arrow-left"`/`"arrow-right"` for the arrow keys (GLFW codes 265/264/263/262) and falls back to `GLFW.glfwGetKeyName(key, scancode)` for ordinary printable keys — which returns lowercase single-character names (`"w"`, `"a"`, `"s"`, `"d"`) for the WASD keys on a standard layout. This confirms `org.openrndr.KeyTracker` (not a hand-rolled tracker) is the correct, already-available mechanism, matching the architecture review's recommendation in `review-tech-verification.md`.
- `InputHandler` now owns a `KeyTracker(program.keyboard)` instance (constructed once, not per-frame, since it accumulates state across events) and a new `pollHeldKeys()` method, computing independent `dx`/`dy` deltas from whichever of `WASD`/arrow-key names are currently held (so diagonal panning falls out naturally, per the "never special-case diagonal" constraint) and calling `controller.scroll(dx, dy)` once if either is non-zero.
- Removed the old `keyDown`-based `KEY_W/A/S/D` branches (single-press, non-continuous) from `InputHandler.setup()` — spacebar pause and mouse click/move handling are untouched, per the spec's "discrete actions stay event-driven" boundary.
- `Visualization.kt` calls `inputHandler.pollHeldKeys()` once per frame, first thing inside the existing `extend { }` block, before `controller.update(seconds)`.
- **Review round 1 patch:** `SCROLL_SPEED` reduced from `3` to `1` tile/frame — the original value was tuned for a one-shot per-keypress jump, not a per-rendered-frame step, and at ~60fps produced an effectively instant snap to the map edge on a brief tap. Now a small per-frame magnitude, documented in a comment at the constant's declaration.
- **Review round 1 patch:** extracted the key-name→delta mapping out of `pollHeldKeys()` into a pure top-level `internal fun panDelta(heldKeys: Set<String>, speed: Int): Pair<Int, Int>`, unit-tested directly (12 tests in `visualization/src/test/kotlin/input/InputHandlerTest.kt`: each WASD/arrow key, opposing-pair cancellation, perpendicular-pair diagonal combination, no keys held, and speed scaling) without needing an OPENRNDR window. `pollHeldKeys()` itself is now a two-line call to `panDelta` + `controller.scroll`.

## Spec Change Log

## Review Triage Log

- **medium** — `InputHandler.pollHeldKeys()` applies the full per-press `SCROLL_SPEED = 3` every rendered frame instead of once per keypress (blind-hunter, verification-gap). Verified: at ~60fps this is up to 180 tiles/sec against a 50×50 map — a brief tap-and-hold snaps the viewport near an edge in well under a second, and speed varies with monitor refresh rate. Real, caused by this change. → **patch**.
- **false** — "Diagonal panning isn't normalized, ~41% faster than axial" (blind-hunter). Disproven: frozen Boundaries explicitly requires "Do not special-case diagonal panning — it must fall out of independent X/Y deltas," so unnormalized diagonal magnitude is spec-mandated, not a defect.
- **low, rejected** — "Held-key state can get stuck if the window loses focus (Alt-Tab) while a key is held" (blind-hunter, edge-case-hunter). Verified real: decompiled `ApplicationGLFWGL3`'s `glfwSetWindowFocusCallback` handler — it only fires a `WindowEvent`, it never clears `KeyTracker`/`pressedKeys` state. Rejected: uncommon trigger (must hold a pan key while switching window focus) and the fix isn't trivial (needs new focus-tracking state), and the matrix never scoped focus-loss.
- **medium** — `InputHandler.pollHeldKeys()`'s key-name→delta mapping (which strings map to which axis, opposing-key cancellation) has no test, automated or manual (verification-gap, blind-hunter). Verified: `grep` confirms no test file references `InputHandler`/`KeyTracker`/`pollHeldKeys`; `CameraTest` only exercises `Camera` with hand-supplied deltas. Real, caused by this change — this is the story's primary behavior. → **patch**.
- **low** — AGENTS.md's Directory Structure/Key types table and Testing section weren't updated for the new `Camera.kt`/`CameraTest.kt` (blind-hunter). Verified real. → **defer** (fix edits agent-context files).
- **false** — "`Camera` snapshots `mapWidth`/`mapHeight`/`visibleCols`/`visibleRows` once at construction instead of live `get()`, a latent trap if the map/viewport ever becomes resizable" (blind-hunter, edge-case-hunter). Disproven: all of those inputs are read-only for the object's lifetime in the current codebase — no resize feature exists in this story or its declared successors (1.2/1.5/1.6), so the values cannot currently diverge.
- **false** — "`SimulationController.visibleCols`/`visibleRows` are still computed locally instead of reading through `Camera`, so the two can diverge" (edge-case-hunter, claim). Disproven: both are derived from the same immutable inputs (`viewportWidthPx`/`viewportHeightPx`/`cellSize`), so they cannot diverge given current code — no caller is shown to actually hit a bad outcome.
- **false** — "Whether panning works while the simulation is paused is untested/unintentional" (blind-hunter). Disproven: unchanged from pre-existing behavior — the old `keyDown`-based WASD handlers didn't check `paused` either, so this isn't new or caused by this story.
- **false** — "`Camera(cellSize = 0)` would throw `ArithmeticException` in `mapPosition`" (edge-case-hunter). Disproven: unreachable — every current caller passes the fixed constant `cellPixelSize = 15`; no code path constructs `Camera` with `cellSize = 0`.
- **false** — "Task description says `InputHandler` calls `Camera.pan()` directly; it actually calls `controller.scroll()`, which delegates" (edge-case-hunter, low confidence, self-flagged as no functional gap). Disproven: the actual call path (`InputHandler` → `controller.scroll` → `camera.pan`) is functionally identical to the described one; no bad outcome occurs.

## Verification

**Commands:**
- `.\scripts\compile-check.ps1` — re-ran after the review-round-1 patch, output ended `COMPILE OK` (both `:critters:compileKotlin` and `:visualization:compileKotlin` succeeded). Note: the script's `_log.ps1`/`Write-CmdLog` sourcing failed with a "not recognized" error because `scripts/_log.ps1` doesn't exist in the working tree despite being referenced — pre-existing repo/environment gap, unrelated to this story's code and not fixed here.
- `.\scripts\build-quiet.ps1 test` — re-ran after the review-round-1 patch, `BUILD SUCCESSFUL`. `visualization/src/test/kotlin/simulation/CameraTest.kt` (6 tests: hold-one-direction, hold-two-perpendicular/diagonal, hold-toward-an-already-reached-edge/clamp, clamp at the far edge, `mapPosition` screen→world conversion in/out of bounds) and `visualization/src/test/kotlin/input/InputHandlerTest.kt` (12 tests on the extracted `panDelta`: each WASD/arrow key, opposing-pair cancellation, perpendicular-pair diagonal, no keys held, speed scaling) both confirmed via their `TEST-*.xml` — `tests="6"`/`tests="12"`, `failures="0" errors="0"` in both.

**Manual checks (if no CLI):**
- The remaining matrix rows depend on live OPENRNDR runtime input (`KeyTracker` held-key state, actual arrow-key name strings, mouse click while panning) and cannot be exercised without an interactive display — not run in this session (none available to either implementing or orchestrating agent). Still outstanding: run the app; hold each of `W`/`A`/`S`/`D` and all four arrow keys — confirm continuous smooth panning, diagonal panning on two perpendicular keys, and clean clamping at each of the four map edges. Click a critter while panning to confirm selection still works. Also worth eyeballing pan speed (see Implementation Notes) since it now runs continuously per-frame instead of once per keypress.
