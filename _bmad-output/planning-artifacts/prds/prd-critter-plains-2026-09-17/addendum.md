# Addendum: Critter Plains — Visualization & Interaction Controls

Technical/mechanism notes grounded in the existing codebase. Not part of the PRD's capability narrative — for `bmad-architecture` to pick up.

## Current implementation baseline

- **Panning (FR-1)** — `InputHandler.setup()` currently listens on `keyboard.keyDown` only (single-fire per press), mapped to `WASD` via GLFW key codes. `SimulationController.scroll(dx, dy)` already clamps to map bounds. Hold-to-pan needs either: (a) tracking key-down/key-up state and polling it once per render frame in the `extend {}` loop, or (b) OPENRNDR's built-in pressed-key query if available. Arrow key codes need adding alongside the existing `KEY_W/A/S/D` constants.

- **Zoom (FR-2)** — `cellSize: Int` is currently a constructor parameter baked into three places: `SimulationController` (drives `visibleCols`/`visibleRows`/`mapPosition`), `BufferedWorldView` (drives tile draw size), and implicitly the `renderTarget` buffer size in `Visualization.kt`. Making zoom work requires `cellSize` (or an equivalent scale factor) to become a shared mutable value all three read from, and a decision on whether the off-screen buffer needs to be resized/recreated on zoom or whether draw calls just scale within a fixed buffer.

- **Memory highlight (FR-5)** — `MemoryStateView.area(): Set<Position>` already exposes exactly the set of remembered tiles for a critter (see `critters/src/main/kotlin/StateView.kt` and `Memory.area()` in `Critters.kt`). Rendering the "unremembered → tinted" requirement is: for each visible tile, if `!selectedCritter.memory.area().contains(pos)`, draw a tint overlay after the normal terrain fill in `BufferedWorldView.render()`. No new sim-side data needed.

- **Deselect (FR-4)** — `SimulationController.selectAt()` already reassigns `selectedCritter`; switching selection by clicking another critter is free. Empty-tile click already resolves to `null` via `state.occupant(it)`. Escape key needs a new binding in `InputHandler`, calling a new `controller.deselect()` (or `selectedCritter = null` exposed via a method, since the setter is currently private).

- **Speed tiers (FR-7)** — `SimulationController.fps: Int` already has a `private set`, so it's mutable internally — just needs a public method (e.g. `setSpeedTier(tier: Int)`) rather than a full refactor. Open question from PRD §8.4: `fps` is typed `Int`, but a 0.5× multiplier of an odd default won't be a whole number. Options: (a) round to nearest int ≥ 1, (b) change `fps`/`updateInterval` to work in `Double` throughout, (c) redefine Speed Tier as a multiplier applied directly to `updateInterval` rather than to `fps`, sidestepping integer rounding entirely. Option (c) is probably cleanest since `updateInterval` is already a `Double`.

- **Pause (FR-8)** — no changes needed; already correct via `controller.togglePause()`.

- **Recenter (FR-6)** — trivial once `SimulationController` exposes a `recenterOn(pos: Position)` (or reuses `scroll` with a computed delta): `viewportX = pos.x - visibleCols / 2`, clamped exactly as `scroll()` already does.

- **Selection indicator (FR-7)** — needs a small vector calc each frame: convert the selected critter's `Position` to screen space via the existing `viewportX/Y` + `cellSize` math (already done for tile rendering); if within `[0, viewportWidthPx] × [0, viewportHeightPx]`, draw a down-pointing arrow above that screen point; if outside, clamp the point to the viewport rectangle edge and draw an arrow rotated to point from that clamped edge position toward the true (unclamped) direction. This is a HUD-layer draw (like `HudView`/`InfoPanel`), not part of the off-screen `BufferedWorldView` buffer, since it must track live cursor-independent state every frame regardless of mouse position.

- **Pause / speed tier indicators (FR-10, FR-11)** — simplest as small additions to the existing `HudView` or a new lightweight `StatusView`, drawn every frame from `controller.paused` and whatever exposes the active tier (e.g. `controller.speedTier`). No new sim-side data needed — both are already tracked in `SimulationController`.
