# CustomClient 1.2 (Fabric, Minecraft 1.21.11, Yarn)

Setup: JDK 21, `gradle wrapper --gradle-version 9.2.1`, then `./gradlew build` / `./gradlew runClient`.
Pinned: Loom 1.14-SNAPSHOT, Loader 0.18.4, Yarn 1.21.11+build.4, Fabric API 0.141.1+1.21.11.

## New in 1.2 (enabled automatically on first world join)
- **FpsBoost** (performance): decreased particles, no entity shadows, entity-distance scale, adaptive view distance
  (+-1 chunk per 5 s with hysteresis, never above your own setting), FPS cap while the window is unfocused. Fully reversible.
- **AppleSkin**: saturation bar, predicted hunger restore + saturation for the held food, exact numbers.
- **ArmorHud**: armor + held tool icons, durability bar, colored %, optional raw numbers, one-shot low-durability warning.
- **InfoHud**: FPS, coordinates, facing, ping.
- HUD hook moved to `HudElementRegistry` (HudRenderCallback is deprecated); the render event object is reused (no per-frame allocation);
  module list is cached; all HUD numbers are computed on the tick thread, never per frame.

Keys: R TriggerBot, O LungeSwap (V = lunge). HUD/perf modules have no keys; toggle with `module.setEnabled(...)`.

## Be realistic about FPS
FpsBoost trims options. It cannot match mods that replace the renderer or game logic. For large gains install
(all have Fabric builds for 1.21.11 as of this writing): **Sodium** (renderer), **Lithium** (game logic), **FerriteCore** (RAM),
**ModernFix**, **ImmediatelyFast** (HUD/text/immediate-mode), **EntityCulling** + **More Culling**, **Krypton** (network),
optionally **C2ME** (chunk threads), **Dynamic FPS** (background), **Iris** (shaders), **Zoomify**, **Mod Menu**.
If you run Dynamic FPS, turn off our ThrottleBackground; if you set view distance in Sodium Extra, turn off AdaptiveViewDistance.

## Unverified API points (no jars/network here) - check first on build/launch errors
`HudElementRegistry.attachElementBefore` / `VanillaHudElements.CHAT` (shape taken from the 1.21.11 Fabric docs),
`DrawContext#drawStackOverlay`, `FoodComponent#nutrition()/saturation()/canAlwaysEat()`, `GameOptions#getParticles()/getEntityShadows()/
getEntityDistanceScaling()/getMaxFps()`, `MinecraftClient#getCurrentFps()/isWindowFocused()`, `Enchantments.LUNGE`,
`doAttack` still returning boolean. The mixin config uses `defaultRequire: 1`, so a drifted mixin target fails loudly at launch.
AppleSkin shows no exhaustion bar (needs an accessor on HungerManager whose field name I could not confirm).

## Use responsibly
Combat modules (TriggerBot, MaceStun, BreachSwap, ShieldBreaker, LungeSwap) are banned on most public servers; LungeSwap relies on
vanilla bug MC-28289. The HUD/perf modules are harmless on any server.
