---
stepsCompleted: [1, 2, 3]
inputDocuments: [
  '_bmad-output/planning-artifacts/prds/prd-critter-plains-2026-09-17/prd.md',
  '_bmad-output/planning-artifacts/prds/prd-critter-plains-2026-09-17/addendum.md',
  '_bmad-output/planning-artifacts/architecture/architecture-critter-plains-visualization-controls-2026-09-17/ARCHITECTURE-SPINE.md'
]
---

# Critter Plains - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for Critter Plains, decomposing the requirements from the PRD ("Visualization & Interaction Controls") and its Architecture Spine into implementable stories. No UX design contract exists for this epic (not run — the PRD and spine cover the necessary interaction detail directly).

## Requirements Inventory

### Functional Requirements

FR1: Builder can pan the viewport by holding `W`/`A`/`S`/`D` or the arrow keys; panning continues smoothly for as long as the key is held, stops cleanly at the map boundary, and holding two perpendicular keys pans diagonally.
FR2: Builder can zoom the map in and out using either the mouse scroll wheel or dedicated keys, anchored at the cursor (scroll) or viewport center (keys), bounded so it can't zoom out past the full map or in past an unreasonable tile size.
FR3: Builder can click a critter to select it; its state displays in the right-hand panel. *(Already implemented — locked in as an acceptance criterion of this epic.)*
FR4: Builder can clear the current selection by clicking an empty tile, clicking a different critter (switches selection directly), or pressing `Escape`.
FR5: While a critter is selected, tiles it does not remember are visually tinted; remembered tiles render unchanged; the tint updates live as the critter's memory grows.
FR6: Builder can press `C` to snap/recenter the viewport on the selected critter, respecting the same map-boundary clamping as ordinary panning.
FR7: While a critter is selected, an arrow marks its location relative to the viewport at all times — pointing down at it when in view, pointing toward it from the viewport edge when off-screen — updating live as the critter moves or the viewport changes.
FR8: Builder can pause and resume the simulation with the spacebar. *(Already implemented — locked in as an acceptance criterion of this epic.)*
FR9: Builder can select one of three Speed Tiers with keys `1`/`2`/`3` (multipliers of the default tick rate: 0.5x/1x/2x); Tier 2 is the default on startup.
FR10: While the simulation is paused, a visible on-screen indicator shows that it's paused.
FR11: The currently active Speed Tier is always visible on screen, updating immediately when the tier changes.

### NonFunctional Requirements

NFR1: Panning and zooming must stay within the render loop's existing per-frame budget during continuous input — no visible frame drops. *(Exact frame-time budget deferred to implementation, per PRD/Architecture.)*

### Additional Requirements

From the Architecture Spine (`ARCHITECTURE-SPINE.md`) — brownfield project, no starter template applies:

- **AD-1 (Camera ownership):** A new `Camera` class (package `simulation`) becomes the sole owner of viewport position and zoom scale — `SimulationController` composes it instead of holding `viewportX`/`viewportY`/`cellSize` itself. Every world↔screen conversion in `:visualization` reads from this one `Camera`. Screen-space for `Camera` is the map-viewport rectangle only (`0,0` to `viewportWidthPx × viewportHeightPx`), explicitly excluding the info panel. `BufferedWorldView` redraws at `Camera`'s live zoom scale every frame into a fixed-size `RenderTarget` (no scaled-blit approach).
- **AD-2 (Overlay pass):** A new `OverlayView` class (package `view`) renders memory tint, the selection indicator, and the pause/tier badges as a screen-space pass *after* the world buffer is blitted — fixed draw order (tint → indicator → badges), each sub-pass resetting its own `Drawer` state (`fill`/`stroke`/`strokeWeight`). `BufferedWorldView` keeps rendering only sim ground truth (plus the narrow live-`cellSize` exception from AD-1).
- **AD-3 (Speed Tier):** A `SpeedTier` enum (`SLOW(0.5)`, `NORMAL(1.0)`, `FAST(2.0)`) becomes first-class state on `SimulationController`; tick interval is derived from it (`baseInterval / tier.multiplier`), never the reverse. The tier↔display-text mapping lives once, as a `SpeedTier` property.
- **AD-4 (Input model):** `InputHandler` uses OPENRNDR's built-in `org.openrndr.KeyTracker(keyEvents)` and its `pressedKeys: Set<String>`, polled once per frame, to drive continuous hold-to-pan — not a hand-rolled key-state `Set`, not GLFW `keyRepeat`. All other actions (select, deselect, pause, tier switch, recenter, scroll-wheel zoom, **and key-based `+`/`-` zoom**) stay discrete/event-driven.
- **AD-5 (Live selection):** `GameStateView` (`:critters`) gains one narrow, additive, read-only lookup — `fun critter(name: CritterName): CritterStateView?` — the epic's one explicit, scoped exception to "does not touch `:critters`." `SimulationController.update()` re-resolves `selectedCritter` from this lookup immediately after every `game.tick()`, clearing selection when the lookup returns `null` or a dead critter. This is the *only* place any liveness/staleness check happens — every other view is a pure reader of `selectedCritter`.
- **Structural Seed — files touched:** `critters/StateView.kt` (MODIFIED), `visualization/Visualization.kt` (MODIFIED — wires `Camera`/`OverlayView`, scroll listener, per-frame held-key poll), `simulation/Camera.kt` (NEW), `simulation/SimulationController.kt` (MODIFIED), `input/InputHandler.kt` (MODIFIED), `view/BufferedWorldView.kt` (MODIFIED), `view/OverlayView.kt` (NEW), `view/HudView.kt` (unchanged), `view/InfoPanel.kt` (MODIFIED).
- **Dependency direction:** `:critters` must retain zero outgoing dependency on `:visualization` — the one new `GameStateView.critter(name)` lookup is read-only and additive, not a boundary violation.
- **Deferred to implementation (no cross-unit divergence risk, single-owner):** exact zoom min/max bounds and step size; exact key-based zoom key pair (`+`/`-` assumed).

### UX Design Requirements

None — no UX design contract exists for this epic.

### FR Coverage Map

| Requirement | Epic.Story |
| --- | --- |
| FR1 | 1.1 |
| FR2 | 1.2 |
| FR3 | 2.1 *(verification only — already implemented)* |
| FR4 | 2.2 |
| FR5 | 2.3 |
| FR6 | 2.4 |
| FR7 | 2.5 |
| FR8 | 3.1 *(verification only — already implemented)* |
| FR9 | 3.2 |
| FR10 | 3.3 |
| FR11 | 3.4 |
| NFR1 | 1.x (Camera/zoom stories) |

FR1: Epic 1 - Hold-to-pan
FR2: Epic 1 - Zoom in/out
FR3: Epic 1 - Select a critter (verification only, already implemented)
FR4: Epic 1 - Deselect a critter
FR5: Epic 1 - Memory highlight
FR6: Epic 1 - Recenter on selection
FR7: Epic 1 - Selection indicator
FR8: Epic 1 - Pause/resume (verification only, already implemented)
FR9: Epic 1 - Speed Tiers
FR10: Epic 1 - Pause indicator
FR11: Epic 1 - Speed Tier indicator

## Epic List

### Epic 1: Visualization & Interaction Controls
Builder can navigate the map, inspect and track any critter, and control simulation pace — all from the viewer, no code edits needed.
**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR7, FR8, FR9, FR10, FR11

**Rationale for a single epic:** the Architecture Spine is fully validated (5 ADs, reviewed twice) with low direction-change risk between stories, and nearly every FR touches the same small file cluster (`Camera`, `SimulationController`, `OverlayView`) — splitting into multiple epics would only churn those same files across epic boundaries without adding a meaningful standalone-value checkpoint. The PRD itself already scopes this as one epic.

## Epic 1: Visualization & Interaction Controls

Builder can navigate the map, inspect and track any critter, and control simulation pace — all from the viewer, no code edits needed.

### Story 1.1: Camera & Hold-to-Pan

As the builder,
I want to pan the viewport by holding `W`/`A`/`S`/`D` or the arrow keys,
So that I can move across the map continuously without repeated taps.

**Acceptance Criteria:**

**Given** the simulation is running
**When** I hold `W`/`A`/`S`/`D` (or the corresponding arrow key)
**Then** the viewport pans continuously in that direction for as long as the key is held, driven by OPENRNDR's `KeyTracker.pressedKeys` polled once per frame — not `keyDown`/`keyRepeat`
**And** panning stops immediately on key release

**Given** I hold two perpendicular direction keys (e.g. `W`+`D`)
**When** both are held
**Then** the viewport pans diagonally

**Given** the viewport reaches a map edge while panning
**When** I continue holding the key
**Then** the viewport stops at the boundary with no overshoot

**Given** viewport position is now owned by a new `Camera` class (AD-1)
**When** `SimulationController` needs viewport bounds elsewhere (click mapping, `visibleCols`/`visibleRows`)
**Then** it reads through `Camera` rather than its own `viewportX`/`viewportY` fields
**And** panning introduces no visible stutter at the existing render rate (NFR1)

### Story 1.2: Zoom In/Out

As the builder,
I want to zoom the map in and out with the scroll wheel or `+`/`-` keys,
So that I can move between an overview and close detail.

**Acceptance Criteria:**

**Given** the simulation is running
**When** I scroll the mouse wheel while the cursor is over the map viewport
**Then** the map zooms in/out anchored at the cursor position via `Camera.zoomAt(cursorPos, factor)`

**Given** the cursor is over the info panel, not the map viewport
**When** I scroll
**Then** no zoom occurs — the panel is outside `Camera`'s screen-space domain (AD-1)

**Given** the simulation is running
**When** I press `+` or `-`
**Then** the map zooms by one discrete step anchored at the viewport center — a single `keyDown`-driven step, not continuous even if held (AD-4)

**Given** zoom changes the live `cellSize`
**When** `BufferedWorldView` renders the next frame
**Then** tiles are redrawn at the new size into the same fixed-size buffer — no stale or scaled-blit artifacts (AD-1)
**And** zoom stops cleanly at its configured min/max bounds without erroring
**And** zooming introduces no visible stutter at the existing render rate (NFR1)

### Story 1.3: Live Selection Tracking, Deselection & Auto-Clear on Death

As the builder,
I want my selected critter's state to stay live and clear itself sensibly,
So that I never look at stale or dead-critter data.

**Acceptance Criteria:**

**Given** a critter is selected
**When** the simulation ticks forward
**Then** `SimulationController` re-resolves `selectedCritter` via the new `GameStateView.critter(name)` lookup immediately after `game.tick()`, so the panel's displayed hunger/state is always current (fixes today's frozen-snapshot gap)

**Given** a critter is selected
**When** I click an empty tile
**Then** the selection clears and the panel returns to its unselected state

**Given** a critter is selected
**When** I click a different critter
**Then** selection switches directly to the newly-clicked critter

**Given** a critter is selected
**When** I press `Escape`
**Then** the selection clears regardless of mouse position

**Given** a critter is selected
**When** that critter dies or is otherwise removed on a later tick
**Then** the selection clears automatically, exactly as manual deselection does, with no input required

**Given** `GameStateView` gains the `critter(name)` lookup
**When** checked against the architecture spine
**Then** `:critters` gains no dependency on `:visualization` and no new mutation surface (AD-5's scoped exception)

### Story 1.4: Memory (Fog-of-War) Highlight

As the builder,
I want unremembered tiles dimmed while a critter is selected,
So that I can see exactly what it has and hasn't explored.

**Acceptance Criteria:**

**Given** a critter is selected
**When** the map renders
**Then** every visible tile outside that critter's `memory.area()` is tinted, drawn by a new `OverlayView` after the world buffer is blitted (AD-2)

**Given** a critter is selected
**When** a tile is inside that critter's remembered area
**Then** it renders with no tint — identical to the no-selection appearance

**Given** the simulation ticks forward
**When** the selected critter's memory grows
**Then** previously-tinted tiles that become remembered lose their tint on the next render

**Given** the selection clears (Story 1.3)
**When** the next frame renders
**Then** the tint disappears from the whole map immediately

**Given** `OverlayView`'s tint sub-pass draws
**When** it finishes
**Then** it resets the `Drawer` fill/stroke state it used, establishing the per-sub-pass hygiene pattern (AD-2) for later stories

### Story 1.5: Recenter Viewport on Selected Critter

As the builder,
I want to press `C` to snap the view back to my selected critter,
So that I don't lose it after panning or zooming away.

**Acceptance Criteria:**

**Given** a critter is selected
**When** I press `C`
**Then** the viewport recenters so the critter's tile sits at, or as close as boundary clamping allows to, the center of the visible area, via `Camera`

**Given** no critter is selected
**When** I press `C`
**Then** nothing happens

**Given** recentering would push the viewport past a map edge
**When** `C` is pressed near that edge
**Then** the same boundary clamping as ordinary panning (Story 1.1) applies — no overshoot

### Story 1.6: Selection Indicator Arrow

As the builder,
I want an arrow that always points to my selected critter,
So that I never lose track of it on or off screen.

**Acceptance Criteria:**

**Given** a critter is selected and its tile is within the current viewport
**When** the map renders
**Then** an arrow renders directly above that tile, pointing down at it, in `OverlayView`

**Given** a critter is selected and its tile is outside the current viewport
**When** the map renders
**Then** an arrow renders at the viewport edge nearest the critter, pointing toward its actual position, using screen-space bounds that exclude the info panel (AD-1)

**Given** the critter moves or the viewport pans/zooms
**When** the next frame renders
**Then** the indicator's position/orientation updates to stay accurate

**Given** the selection clears (Story 1.3)
**When** the next frame renders
**Then** the indicator disappears immediately

**Given** `OverlayView` now has two sub-passes (tint, indicator)
**When** both render in the same frame
**Then** they draw in the fixed order tint → indicator (AD-2), and the indicator sub-pass resets its own `Drawer` state rather than relying on the tint pass's

### Story 1.7: Speed Tiers

As the builder,
I want to switch between three simulation speeds,
So that I can study behavior at whatever pace suits the moment.

**Acceptance Criteria:**

**Given** the simulation is running
**When** I press `1`, `2`, or `3`
**Then** `SimulationController`'s `SpeedTier` is set to `SLOW` (0.5x), `NORMAL` (1x), or `FAST` (2x) respectively, and `updateInterval` is derived from `baseInterval / tier.multiplier` (AD-3)

**Given** the app starts
**When** it first renders
**Then** `SpeedTier` defaults to `NORMAL`, matching today's existing tick rate exactly — no key press needed

**Given** the simulation is paused
**When** I switch tiers
**Then** the tier changes but the simulation does not resume on its own

**Given** spacebar pause/resume already works today
**When** this story is complete
**Then** that behavior remains unchanged (FR8 regression coverage)

### Story 1.8: Pause & Speed Tier Status Badges

As the builder,
I want on-screen badges showing pause state and active speed tier,
So that I always know the simulation's current pace at a glance.

**Acceptance Criteria:**

**Given** the simulation is paused
**When** the map renders
**Then** a visible "PAUSED" badge appears on screen, in `OverlayView`
**And** the badge disappears immediately once resumed

**Given** any Speed Tier is active
**When** the map renders
**Then** a badge shows which of `1`/`2`/`3` is active, reading the tier's own display-label property (AD-3) — no duplicated mapping in `OverlayView`
**And** the badge updates immediately when the tier changes (Story 1.7)

**Given** `OverlayView` now has three sub-passes (tint, indicator, badges)
**When** all render in the same frame
**Then** they draw in the fixed order tint → indicator → badges (AD-2), badges always topmost, each sub-pass resetting its own `Drawer` state
