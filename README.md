**___EngineCore (v1.0.0)___**

EngineCore is a modular, high-efficiency performance mod for Minecraft Forge 1.20.1. It targets server tick times (MSPT), memory churn, and block update cascades without altering vanilla mechanics or breaking redstone contraptions.

It operates primarily on the server tick loops but includes a full-featured in-game configuration GUI. Clients do not need the mod installed to join dedicated servers running EngineCore.

---

**__Features__**

**Dynamic Mob AI Throttling:** 
Gradually reduces pathfinding and goal evaluation frequencies for distant and idle entities. Mobs automatically return to full tick rates when a player approaches, during combat, when named with a nametag, or when riding vehicles (boats/minecarts).

**Configurable Entity Blacklist:** 
Allows specific entity registry IDs (e.g. `minecraft:warden`, `minecraft:wither`, or custom mod bosses) to be fully exempted from AI throttling.

**Farm Animal & Breeding Optimizer:** 
Throttles ambient mate-searching and passive plant checks for livestock in dense pens unless actively in breeding mode or targeted by players.

**Ground Item Merging Optimizer:** 
Coalesces ground item merge scans to eliminate tick lag caused by mass drop events like quarrying, explosions, or mob farms.

**Memory Allocation & GC Optimizer:** 
Minimizes Garbage Collection overhead by pooling mutable BlockPos instances and collision scratch caches across heavy movement loops.

**Safe Redstone Update Batching:** 
 Coalesces non-mechanical neighbor update storms (falling sand, mass block breaks) while passing sensitive components (pistons, observers, comparators, redstone dust) immediately to maintain 0-tick mechanics.

**In-Game Configuration GUI:** 
Features dedicated module toggles, distance sliders, and one-click presets (**Safe**, **Norm**, **Max**) directly in the pause/mod menu.

---

**__In-Game GUI & Presets__**

**Safe:** Conservative settings preserving maximum mod compatibility.'
**Norm:** Balanced performance profile recommended for standard playthroughs and mid-sized modpacks.
**Max:** Aggressive optimization designed for dense servers and heavy modpacks.

---

### Configuration File

All parameters can also be configured manually in:
`config/enginecore-common.toml`

`toml
[general]
enableAiThrottle = true
enableGcOptimizer = true
enableRedstoneBatching = false
enableItemMergeOptimizer = true
enableAnimalOptimizer = true
enableLightDampener = trueSupport & Development`

`[aiThrottle]
nearDistance = 16.0
midDistance = 32.0
farDistance = 48.0
midSkipInterval = 2
farSkipInterval = 4
veryFarSkipInterval = 8
exemptCustomNamed = true
exemptVehiclePassengers = true
blacklist = ["minecraft:warden", "minecraft:wither"]`

`[redstone]
maxBatchedUpdates = 512`

`[memory]
mutablePosPoolSize = 8`

---

**__Support & Development__**
Support ongoing updates and development on Patreon:
https://patreon.com/Da_Slimy

---

**__License__**
EngineCore is released under the Apache License 2.0. Free for use in any private or public modpack.