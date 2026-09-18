---
title: "Critter Plains — Visualization & Interaction Controls"
status: final
created: 2026-09-17
updated: 2026-09-17
---

# PRD: Critter Plains — Visualization & Interaction Controls
*Working title — confirm.*

## 0. Document Purpose

This PRD scopes a single epic: giving the Critter Plains viewer richer navigation and inspection tools — panning, zooming, critter selection with a detail panel, memory/fog-of-war highlighting, pause, and simulation speed control. It's written for the project owner (also the sole builder) and feeds directly into `bmad-architecture` and `bmad-create-epics-and-stories`. Technical grounding in the existing codebase (what's already implemented vs. net-new) lives in `addendum.md` alongside this document — this PRD states capabilities only.

## 1. Vision

Critter Plains renders a live 50×50 tile simulation, but the viewer currently offers only fixed-scale viewing, single-tap panning, and a bare-bones critter panel (name + hunger). This epic turns the viewer into a proper observation tool: the map can be panned smoothly by holding a direction, zoomed in and out to move between overview and detail, and a critter can be selected to see both its current state and the exact area it has explored, rendered as its fog-of-war memory. Simulation pace becomes controllable — paused instantly, or sped up/slowed down — so behavior can be studied at whatever tempo suits the moment.

## 2. Target User

### 2.1 Jobs To Be Done
- As the builder, I want to freely navigate the full map and zoom to whatever level of detail I need, so I can watch emergent behavior across the whole population or focus on one region.
- As the builder, I want to select a critter and see its internal state (including what it remembers of the world), so I can understand *why* it's acting the way it is.
- As the builder, I want to control simulation pace (pause / slow / fast), so I can catch fast-moving events or skip through uneventful stretches.

### 2.2 Key User Journeys
*Single-sentence form — hobby/solo project, single operator (the builder).*

- **UJ-1.** The builder, watching the simulation run, holds `D` to sweep the viewport east across the map, scrolls to zoom in on a cluster of critters, clicks one to select it, and watches its fog-of-war area (dimmed elsewhere) grow tile by tile as it explores — then hits `1` to slow the simulation down for a closer look.

## 3. Glossary

- **Viewport** — the visible window into the 50×50 tile map; has a position (top-left tile offset) and a size in tiles, currently derived from a fixed pixel size divided by tile size.
- **Zoom Level** — the current tile-to-pixel scale; changing it changes how many tiles fit in the viewport.
- **Selected Critter** — the single critter (if any) currently chosen for inspection in the right-hand panel.
- **Memory (fog-of-war)** — the set of tile positions a given critter has observed and still remembers; distinct from the ground-truth map.
- **Speed Tier** — one of three simulation-speed presets (keys `1`/`2`/`3`), expressed as a multiplier of the default tick rate.
- **Tick** — one simulation step (`WorldState.tick()`), advanced at a rate governed by the current Speed Tier.

## 4. Features

### 4.1 Map Navigation
**Description:** The viewer can pan continuously by holding a direction key, and zoom in/out to change tile scale. Realizes UJ-1.

#### FR-1: Hold-to-pan
Builder can pan the viewport by holding `W`/`A`/`S`/`D` **or** the arrow keys, and panning continues smoothly for as long as the key is held, not just on a single tap.

**Consequences (testable):**
- Holding a direction key for a sustained period moves the viewport continuously in that direction until released or the map edge is reached.
- Arrow keys produce the same panning behavior as the corresponding `WASD` key.
- Holding two perpendicular direction keys pans diagonally. `[ASSUMPTION: falls out naturally from independent X/Y handling — no special-cased diagonal logic needed.]`
- Panning stops cleanly at the map boundary (no overshoot, no wraparound).

#### FR-2: Zoom in/out
Builder can zoom the map in and out using **either** the mouse scroll wheel **or** dedicated keys.

**Consequences (testable):**
- Scroll wheel up/forward zooms in; scroll down/back zooms out.
- A dedicated key pair (e.g. `+`/`-`) zooms in/out equivalently to the scroll wheel. `[ASSUMPTION: exact key pair TBD at implementation — "+"/"-" assumed.]`
- Zoom is bounded — it cannot zoom out past showing the full map or in past a tile becoming unreasonably large. `[ASSUMPTION: exact min/max bounds deferred to architecture/implementation — see Open Questions.]`
- Zoom anchor point (what stays fixed on screen while zooming) is defined per input method. `[ASSUMPTION: scroll-wheel zoom anchors at the cursor; key-based zoom anchors at the viewport center — unconfirmed, see Open Questions.]`

**Feature-specific NFRs:**
- Panning and zooming must stay within the render loop's existing per-frame budget during continuous input. `[ASSUMPTION: exact frame-time budget deferred to architecture, same as the zoom bounds in FR-2 — see Open Questions §8.1.]`

### 4.2 Critter Selection & Inspection
**Description:** Selecting a critter shows its state in the existing right-hand panel, highlights what it remembers of the map, and stays trackable — recenterable on demand and pointed to by an on-screen indicator even as it moves or the viewport changes. Realizes UJ-1.

#### FR-3: Select a critter
Builder can click a critter to select it; its state displays in the right-hand panel. *(Already implemented — included here to lock it in as an acceptance criterion of this epic, not just incidentally preserved.)*

**Consequences (testable):**
- Clicking an occupied tile selects that critter and shows its name and hunger in the panel.
- Clicking a different occupied tile switches selection to the newly-clicked critter.

#### FR-4: Deselect a critter
Builder can clear the current selection three ways: clicking an empty tile, clicking a different critter (switches directly, per FR-3), or pressing `Escape`.

**Consequences (testable):**
- Clicking a tile with no critter clears the current selection and the panel returns to its unselected state.
- Pressing `Escape` clears the current selection regardless of mouse position.

#### FR-5: Highlight selected critter's memory
While a critter is selected, tiles the critter does **not** remember are visually dimmed/tinted on the map; tiles it does remember render exactly as they normally would (unchanged).

**Consequences (testable):**
- With a critter selected, every visible tile outside that critter's remembered area is tinted.
- Tiles inside the critter's remembered area show no tint — identical to the no-selection rendering.
- Clearing the selection (FR-4) removes the tint from the whole map immediately.
- As the simulation ticks forward and the critter's memory grows, previously-tinted tiles that become remembered lose their tint on the next render.

#### FR-6: Recenter viewport on selected critter
Builder can press `C` to snap the viewport so the selected critter is centered on screen.

**Consequences (testable):**
- Pressing `C` while a critter is selected moves the viewport so the critter's tile sits at (or as close to, given map-edge clamping) the center of the visible area.
- Pressing `C` with no critter selected has no effect.
- Recentering respects the same map-boundary clamping as ordinary panning (FR-1) — no overshoot past map edges.

#### FR-7: Selection indicator
While a critter is selected, an arrow marks its location relative to the viewport at all times, so it's never lost track of.

**Consequences (testable):**
- If the selected critter's tile is within the current viewport, an arrow renders directly above that tile, pointing down at it.
- If the selected critter's tile is outside the current viewport, an arrow renders at the edge of the viewport, pointing toward the critter's actual position.
- The indicator updates every render as the critter moves or the viewport pans/zooms, staying accurate to the critter's live position.
- Clearing the selection (FR-4) removes the indicator immediately.

### 4.3 Simulation Speed Control
**Description:** Builder can pause the simulation instantly, choose between three speed presets, and see both states reflected on screen at all times. Realizes UJ-1.

#### FR-8: Pause / resume
Builder can pause and resume the simulation with the spacebar. *(Already implemented — included here to lock it in as an acceptance criterion of this epic.)*

**Consequences (testable):**
- Pressing spacebar while running pauses the simulation; the map stops advancing.
- Pressing spacebar while paused resumes at the current Speed Tier.

#### FR-9: Speed Tiers
Builder can select one of three Speed Tiers with keys `1`, `2`, `3`. Tier `2` is the default on startup. Tiers are multipliers of the app's default tick rate: `1` = 0.5×, `2` = 1× (default), `3` = 2×.

**Consequences (testable):**
- Pressing `1` sets the simulation to half its default tick rate.
- Pressing `2` returns the simulation to its default tick rate.
- Pressing `3` sets the simulation to double its default tick rate.
- On startup, the simulation runs at Tier `2` (its existing default rate) without any key needing to be pressed.
- Switching tiers while paused changes the tier but does not itself resume the simulation.

#### FR-10: Pause indicator
While the simulation is paused, a visible on-screen indicator shows that it's paused (not just running slowly).

**Consequences (testable):**
- When paused, an on-screen element (e.g. a "PAUSED" badge) is visible.
- When resumed, that element disappears immediately.

#### FR-11: Speed Tier indicator
The currently active Speed Tier is always visible on screen.

**Consequences (testable):**
- An on-screen element shows which of the three tiers (`1`/`2`/`3`) is currently active.
- The indicator updates immediately when the tier changes (FR-9).
- On startup, the indicator shows Tier `2` without any key needing to be pressed.

## 5. Non-Goals (Explicit)
- No changes to critter AI, behavior, or decision-making (`Intention`, hunger logic, etc.) — this epic is viewer-only.
- No mouse-drag ("click and drag") panning — keyboard/scroll only, per the brain dump.
- No multi-critter selection or comparison view.
- No persistence of camera position, zoom level, or selection across app restarts.
- No touch, gamepad, or remappable-keybinding support.
- No changes to the `Chronicler` narration feature.

## 6. MVP Scope

### 6.1 In Scope
- FR-1 through FR-11, as specified above.

### 6.2 Out of Scope for MVP
- Minimap or overview thumbnail.
- Animated/tweened camera transitions for pan or zoom (instantaneous per-input movement only).
- Camera bookmarks / saved views.
- Any keybinding configuration UI.

## 7. Success Metrics

**Primary**
- **SM-1**: The builder can reach any tile on the 50×50 map, inspect any critter's full remembered area, and never lose track of a selected critter regardless of pan/zoom. Validates FR-1, FR-2, FR-5, FR-6, FR-7.
- **SM-2**: The builder can run a full session switching between all three speed tiers and pause/resume without friction, always able to tell at a glance whether the sim is paused and which tier is active. Validates FR-8, FR-9, FR-10, FR-11.

*Hobby-scale — no quantitative targets beyond "it's actually pleasant to use during my own sessions."*

## 8. Open Questions
1. Exact zoom bounds (min/max tile-pixel size) and step size per scroll/key event — needs a concrete number before implementation.
2. Zoom anchor: should scroll-wheel and key-based zoom anchor differently (cursor vs. center), or should both anchor the same way for consistency?
3. Exact key pair for key-based zoom (`+`/`-` assumed) — confirm no conflict with existing bindings.
4. Speed Tier rounding: when 0.5× of the default tick rate isn't a whole number, how should it round? (Addendum flags this as a representation question for architecture — `fps` is currently an `Int`.)
5. What happens to the selection (panel, memory highlight, recenter, indicator) when the selected critter dies mid-session? `InfoPanel` already has an unresolved `// todo: critter death` gap — this epic doesn't fix that gap but its new features (FR-6, FR-7) will visibly interact with it.

## 9. Assumptions Index
- §4.1 FR-1 — diagonal panning falls out of independent X/Y handling, no special logic assumed needed.
- §4.1 FR-2 — zoom key pair assumed to be `+`/`-`.
- §4.1 FR-2 — zoom min/max bounds not yet specified, deferred to architecture.
- §4.1 FR-2 — zoom anchor differs by input method (cursor for scroll, center for keys) — unconfirmed.
- §4.1 FR-2 NFR — panning/zooming frame-time budget deferred to architecture, same gap as zoom bounds above.
