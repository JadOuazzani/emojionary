# 🎮 Emojionary

A multiplayer emoji-based word guessing game built with Scala.js for the CS-214 Unguided Lab.

## 📖 Game Overview

Emojionary is a turn-based party game where one player (the **Describer**) must make others guess a secret word using only emojis. Think of it as Pictionary, but with emojis instead of drawings!

### How to Play

1. **Start**: One player becomes the Describer and chooses a secret word
2. **Describe**: The Describer selects emojis (up to 5) to represent the word
3. **Guess**: Other players try to guess the word within the time limit
4. **Score**: Fewer hints = more points! (5 pts for 1 hint, 1 pt for 5 hints)
5. **Rotate**: The winner becomes the next Describer

### Game Rules

| Rule | Value |
|------|-------|
| Max hints per round | 5 |
| Time per hint | 20 seconds |
| Points formula | `MaxHints - hintCount + 1` |
| Min players | 2 |

## 🏗️ Architecture

```
apps/
├── js/src/main/scala/apps/
│   └── emojionary/
│       └── MainJS.scala      # Frontend UI (Scala.js)
├── jvm/src/main/scala/apps/
│   └── emojionary/
│       ├── Types.scala       # State, View, Event, Phase definitions
│       ├── Wire.scala        # JSON serialization/deserialization
│       └── Logic.scala       # Game state machine & business logic
└── jvm/src/test/scala/apps/
    └── emojionary/
        ├── StateTest.scala   # State invariants & require tests
        ├── WireTest.scala    # Serialization round-trip tests
        └── LogicTest.scala   # Game logic & transitions tests
```

## 🧩 Components

### Types (`Types.scala`)

Defines the core data structures with Design by Contract validation:

- **`Phase`**: Game phases (`WaitingToStart`, `WaitingForHint`, `Guessing`, `GameEnd`)
- **`Event`**: User actions (`StartGame`, `SendFirstHint`, `SendHint`, `SubmitGuess`)
- **`State`**: Complete game state with invariants enforced via `require`
- **`View`**: Player-specific projection of the state
- **`GuessEntry`**: Record of a guess attempt

### Wire (`Wire.scala`)

Handles JSON encoding/decoding for client-server communication:

- `eventFormat`: Encodes/decodes player events
- `viewFormat`: Encodes/decodes game views
- Includes validation with `require` and `ensuring` clauses

### Logic (`Logic.scala`)

Implements the `ClockDrivenStateMachine` interface:

- **`init`**: Initializes game state for given players
- **`project`**: Creates player-specific views (hides secret word from guessers)
- **`transition`**: Handles events and timer ticks
- **`handleTick`**: Manages timer expiration and round transitions

Key features:
- Automatic round rotation when timer expires
- Winner becomes next Describer
- Score calculation based on hint count
- 500+ available emojis across 8 categories

### Frontend (`MainJS.scala`)

Modern, animated UI built with Laminar:

- Custom color palette (purple/turquoise gradient)
- Google Fonts (Fredoka + Nunito)
- Animated emoji hints with drop-in effects
- Visual timer with progress bar
- Responsive design for mobile
- Categorized emoji picker for Describer

## 🧪 Testing

Run all tests:
```bash
sbt test
```

### Test Coverage

| Test Suite | Coverage |
|------------|----------|
| `StateTest` | 30 tests - State invariants, require validations |
| `WireTest` | 30 tests - JSON round-trips, edge cases, unicode |
| `LogicTest` | 10 tests - Game flow, transitions, scoring |

### Design by Contract

The codebase uses Scala's `require` (preconditions) and `ensuring` (postconditions):

```scala
// Example from State
require(playerOrder.nonEmpty, "Player order cannot be empty")
require(hints.distinct == hints, "Hints must not contain duplicate emojis")

// Example from Logic
val newState = state.copy(phase = Phase.Guessing, ...)
  .ensuring(s => s.phase == Phase.Guessing && s.hints.size == 1)
```

## 🚀 Running the Game

1. Start the server:
```bash
sbt run
```

2. Open in browser:
```
http://localhost:8080
```

3. Create a game room and share the link with friends!

## 👥 Team

| Member | Contributions |
|--------|---------------|
| **Riad** | Frontend development |
| **Sara** | Frontend development (UI/UX design focus), `StateTest` |
| **Lilya** | `Types`, `Wire`, `WireTest` |
| **Jad** | `Types`, `Logic`, `LogicTest` |

## 📝 Technical Notes

### State Invariants

The `State` class enforces these invariants via `require`:

- `playerOrder` must be non-empty with no duplicates
- `describerIndex` must be within bounds
- `scores` must match `playerOrder` exactly
- All scores must be non-negative
- `hints` cannot exceed `MaxHints` and must be unique
- `secretWord` must be trimmed, lowercase, and non-empty
- `currentHintGuessers` must be a subset of players
- Phase-specific constraints (e.g., `Guessing` requires `secretWord`)

### Game Flow

```
WaitingToStart ──StartGame──> WaitingForHint ──SendFirstHint──> Guessing
                                    ↑                              │
                                    │                              │
                              (timer expires                 (correct guess)
                               & hints < max)                      │
                                    │                              ↓
                                    └────────────────────────── GameEnd
                                         (auto-restart)
```

## 📄 License

CS-214 Unguided Lab Project - EPFL 2025