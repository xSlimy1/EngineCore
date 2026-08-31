# EngineCore

Modular Forge 1.20.1 performance mod. Four independently toggleable modules, all controlled through
one hot-reloadable server config (`config/enginecore-server.toml` after first launch).

## Modules

| Module | Where | What it does |
|---|---|---|
| Entity AI & Dynamic Tick Throttling | `mixin/MixinMob`, `mixin/MixinGoalSelector`, `mixin/MixinEntity`, `core/TickThrottleManager` | Distance-banded throttling of goal re-evaluation and, at extreme range, the whole AI step. Hard-exempts bosses, named entities, and minecart/boat passengers, and self-corrects if a throttled mob's ground support changes mid-throttle. |
| Memory Allocation & GC Optimizer | `core/MutablePosPool`, `core/AabbScratchCache`, `core/GroundStabilityCheck`, `mixin/MixinEntity`, `mixin/MixinServerLevel` | Thread-local pooled `BlockPos.MutableBlockPos` and a per-tick AABB scratch cache, actively wired into `Entity#checkSupportingBlock` (runs every tick, every entity) and the redstone-batching neighbor scan - not just standalone utility classes. |
| Block & Redstone Tick Batching | `core/RedstoneUpdateBatcher`, `core/RedstoneCriticalBlocks`, `mixin/MixinLevel` | Coalesces repeated `Level#updateNeighborsAt` calls to the same position within one tick into a single end-of-tick flush. **Disabled by default** - see "Redstone safety" below. |
| Modular Config System | `config/EngineCoreConfig` | `ForgeConfigSpec` with every value mirrored into a `volatile` static field on load/reload, so hot-path code never pays the synchronized `ConfigValue#get()` cost. |

## Redstone safety (please read before enabling `enableRedstoneBatching`)

Delaying `updateNeighborsAt` to end-of-tick is **not** a free optimization: vanilla redstone
mechanics - 0-tick pistons, observer pulse chains, comparator/repeater latching, quasi-connectivity -
depend on neighbor updates being dispatched immediately, in order, once per actual state change.
Coalescing repeats breaks that assumption for genuine redstone circuits.

Because of this:

- `enableRedstoneBatching` **defaults to `false`**.
- Even when enabled, `RedstoneUpdateBatcher` checks `RedstoneCriticalBlocks.isCritical(...)` against
  the source block, the target position's current block, **and all six of that position's
  neighbors** before ever queuing anything. If any of those is a redstone/mechanical component
  (wire, repeater, comparator, piston, observer, lever, button, pressure plate, tripwire, daylight
  sensor, target block, redstone lamp/torch), the update is dispatched immediately through vanilla
  behavior, unbatched, regardless of the config toggle.
- This means the module only ever batches bulk, non-mechanical update floods - explosions, leaf/sand
  decay cascades, mass block breaks - not redstone circuits themselves. That's a deliberate,
  significant narrowing of scope in exchange for not risking circuit desync.

This is a best-effort defense-in-depth measure, not a formal proof of safety - if you enable it,
test against your own world's redstone before relying on it in production.

## Build

Requires a JDK 17 toolchain (`sourceCompatibility`/`targetCompatibility` and the toolchain are both
set explicitly in `build.gradle`) and network access to Forge's and Sponge's Maven repositories.

```
./gradlew build
```

Produces `build/libs/enginecore-<version>.jar`.

## Mapping verification

The following members were checked against a live 1.20.1 Mojmap javadoc mirror before this mixin
set was written (not just recalled from memory):

- `Mob#serverAiStep()` - confirmed `protected final void serverAiStep()`.
- `Mob#goalSelector` / `Mob#targetSelector` - confirmed `final GoalSelector` fields (hence the
  `@Shadow @Final` annotations in `MixinMob`, rather than plain `@Shadow`).
- `GoalSelector#tick()` and `GoalSelector#getRunningGoals()` - confirmed `public void tick()` and
  `public Stream<WrappedGoal> getRunningGoals()`, and that `GoalSelector extends Object` (no
  superclass, so no `extends` clause is needed in the mixin).
- `ServerLevel#tick(BooleanSupplier)` - confirmed `public void tick(BooleanSupplier)`.
- `Level#updateNeighborsAt(BlockPos, Block)` - confirmed concretely declared on `Level` itself (not
  abstract, not overridden per-side), which is why `MixinLevel` targets `Level` and not
  `ServerLevel`.
- `Level.isClientSide` - confirmed to exist as **both** a public final field and a
  `public boolean isClientSide()` method (via the `LevelReader` interface); this codebase
  consistently uses the method-call form since it also resolves correctly on `LevelReader`-typed
  references, not just concrete `Level` ones.
- `Entity#checkSupportingBlock(boolean, Vec3)` - confirmed to exist with this name and signature;
  `MixinEntity` only relies on the method existing and running every tick, never on its internal
  logic, since the injection is a non-cancelling `HEAD` observer.

Members that were **not** individually re-verified this way (lower risk, since a wrong name simply
fails the Mixin annotation processor at build time rather than misbehaving at runtime): the
`RedstoneCriticalBlocks` class-name list (`DiodeBlock`, `PistonBaseBlock`, `ObserverBlock`,
`RedStoneWireBlock`, etc.), and `BlockState#getCollisionShape(BlockGetter, BlockPos)`. Run
`./gradlew build` once locally - if any of these have shifted, the Mixin AP will fail fast with a
clear "could not find target member" error.
