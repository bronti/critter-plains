# Repository Overview

## Project Description

**Critter Plains** is a creature simulation written in Kotlin. A population of critters inhabits a procedurally generated tile map, each maintaining its own fog-of-war memory and making decisions based on hunger. The simulation runs in real time and is rendered via an OPENRNDR desktop window with interactive controls.

Key technologies:
- **Kotlin 2.0 / JVM 17**
- **OPENRNDR** — OpenGL-backed rendering framework (+ ORX extension library)
- **Gradle** multi-project build with Shadow JAR packaging
- **kotlinx.coroutines / serialization**, **kotlin-logging / Log4j2**

## Architecture Overview

The project has a strict three-layer separation:

```
Main.kt  ──creates──►  Game  ──passed to──►  runVisualization()
              │                                       │
         [:critters]                        [:visualization]
         Pure sim logic                     OPENRNDR UI only
         No UI dependency                   Depends on :critters
```

### Simulation loop (`:critters`)

```
WorldState.tick()
  ├── critter.experience(world, pos)   # update fog-of-war Memory
  ├── critter.tick()                   # increment hunger; die at 40
  └── act(critter.intent(), ...)       # translate Intention → world mutation
        ├── EXPLORE  → step toward unexplored tile (naive step-toward)
        └── EAT      → step toward / consume FOOD terrain
```

### Render loop (`:visualization`)

```
OPENRNDR extend {}
  ├── SimulationController.update(seconds)   # throttled tick at configured FPS
  ├── BufferedWorldView.render()             # tiles + critters → off-screen buffer
  ├── drawer.image(buffer)                   # blit to screen
  ├── HudView.render(hoveredCritter)         # tooltip on mouse hover
  └── InfoPanel.render(selectedCritter)      # right-side details panel
```

`SimulationController` is the bridge: it converts OPENRNDR screen coordinates (`Vector2`) to simulation grid `Position`, owns pause/speed state, and gates `game.tick()` to the configured frame rate.

## Directory Structure

```
critter-plains/
├── src/main/kotlin/
│   └── Main.kt                        # Entry point — creates Game, calls runVisualization()
│
├── critters/src/main/kotlin/          # :critters subproject — pure simulation, no UI
│   ├── Game.kt                        # Public facade: tick(), state(), mapWidth/Height
│   ├── critter/
│   │   ├── Critters.kt                # Critter, Memory, Intention, CritterNameGenerator
│   │   └── Actor.kt                   # act() — maps Intention to World.Interactive calls
│   └── world/
│       ├── World.kt                   # WorldState (tick loop), World.Observable/Interactive interfaces
│       ├── Map.kt                     # Territory hierarchy, Terrain, Position, MutableGameMap
│       ├── MapGeneration.kt           # Blob-based procedural generation via GameMapBuilder
│       └── MutablePlacement.kt        # Critter ↔ Position mapping; rip() removes dead critters
│
├── visualization/src/main/kotlin/     # :visualization subproject — OPENRNDR rendering
│   ├── Visualization.kt               # runVisualization() — wires OPENRNDR program
│   ├── simulation/
│   │   ├── SimulationController.kt    # Tick throttle, pause, speed, critter selection
│   │   └── WorldData.kt              # Interface for screen-space world queries
│   ├── view/
│   │   ├── BufferedWorldView.kt       # Renders tiles + critters to off-screen RenderTarget
│   │   ├── HudView.kt                 # Mouse-hover tooltip
│   │   └── InfoPanel.kt              # Right-side selected-critter details panel
│   └── input/
│       └── InputHandler.kt            # Space = pause/resume; click = select critter
│
├── data/
│   ├── fonts/default.otf              # Font loaded at runtime
│   └── images/                        # Static image assets
│
├── scripts/                           # PowerShell developer utilities
│   ├── compile-check.ps1              # Fast (~3-4 s) error-only compile check — use after edits
│   ├── build-quiet.ps1                # Full build with filtered output
│   ├── _log.ps1                       # Logs commands and file reads to cmd.log
│   └── analyze-log.ps1                # Updates patterns.md and AI_STATUS.md from cmd.log
│
├── .continue/rules/
│   ├── AI_ARCH.md                     # Stable architecture reference (auto-injected by Continue)
│   ├── AI_STATUS.md                   # Current TODOs and session state (auto-injected)
│   ├── patterns.md                    # Observed shell mistakes / corrections (auto-injected)
│   └── project.md                     # AI workflow rules (auto-injected)
│
├── build.gradle.kts                   # Root build — app plugin, Shadow JAR, dependency updates
├── settings.gradle.kts                # Declares subprojects: critters, visualization
└── gradle.properties                  # OPENRNDR tasks enabled; Kotlin official code style
```

### Key constants (Main.kt)

| Constant | Value | Meaning |
|---|---|---|
| `gridSize` | 50 | World dimensions (50×50 tiles) |
| `cellPixelSize` | 15 | Pixels per tile |
| `fps` | 5 | Simulation ticks per second |

## Development Workflow

### Build & Run

```powershell
# Run the application (IDE or Gradle)
./gradlew run

# Build a fat/shadow JAR
./gradlew shadowJar

# Run the built JAR
java -jar build/libs/critter-plains-1.0.0-all.jar
```

### Preferred scripts (use these instead of raw gradlew)

```powershell
# Fast compile check after edits — errors only, ~3-4 s
.\scripts\compile-check.ps1

# Full build with filtered output
.\scripts\build-quiet.ps1

# Analyze session log; updates patterns.md and AI_STATUS.md
.\scripts\analyze-log.ps1
```

> **Never run `gradlew.bat` directly** — output is too verbose. Always use the scripts above.

### Testing

- JUnit is on the test classpath in both subprojects (`testImplementation(libs.junit)`).
- No test source files exist yet; tests should go in `critters/src/test/kotlin/` or `visualization/src/test/kotlin/`.

### Dependency updates

```powershell
./gradlew dependencyUpdates   # reports outdated dependencies; ignores alpha/beta/rc versions
```

### Logging

Full Log4j2 logging is active (`Logging.FULL` in `visualization/build.gradle.kts`). Logs are written to `application.log`. After a session, run `analyze-log.ps1` to update the AI context files.

### Code style

- Kotlin 2.0 idiomatic style (`kotlin.code.style=official` in `gradle.properties`).
- No unnecessary blank lines; no trailing comments on closing braces.
- Match surrounding code style when editing existing files.

### Platform note

OPENRNDR native dependencies are resolved automatically for Windows, macOS (x64/arm64), and Linux (x64/arm64). Cross-compilation is possible via `-PtargetPlatform=<platform>`.
