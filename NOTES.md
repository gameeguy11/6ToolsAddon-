# AnarchyAddon — notes

## What's here
A full Gradle project (Kotlin DSL, matching Meteor's own official addon template) —
open the folder in IntelliJ IDEA as a Gradle project and it should sync.

- `AnarchyAddon.java` — main addon class. Registers the `Meteor++` category (module list),
  a `Meteor++` HUD group, `Efly`, and the player-tracker HUD.
- `modules/Efly.java` — your VolytraFly source, repackaged and renamed only. Package,
  class name, module id (`"efly"`) and category changed; every line of actual flight logic
  is untouched.
- `hud/PlayerTrackerHud.java` — new HUD element, written from scratch against Meteor
  Client's real current source.
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`,
  `gradlew`/`gradlew.bat` + wrapper jar — pulled from Meteor's own official addon-template
  repo, with the module/package names swapped to yours.

## EZ / AutoEz
`modules/Ez.java` is a clean rebuild - none of the original's built-in message banks
are in it (that content is what I declined to include). Both message lists (Kill Messages,
Pop Messages) start **empty** - the module sends nothing until you write your own lines in
its settings. `<NAME>` in a message gets replaced with the other player's name.

**Kill attribution**: rather than firing on "some enemy died near you" (what the original
did), it only fires when the game's own death message credits *you* specifically -
Minecraft's death messages always read like "SomePlayer was slain by YourName", so it
regex-matches your exact name after "by" in that message. This also means your own deaths
are correctly ignored (your name is the victim there, not the one after "by"). This relies
on:
- `ClientboundPlayerCombatKillPacket` having `playerId()` and `message()` accessors - I
  confirmed both are real by grepping meteor-client's own source (`Surround.java` and
  `Freecam.java` already use `packet.playerId()` the same way), so that part's solid.
- Standard English death-message phrasing ("X was slain/shot/etc. by Y"). If your server
  uses a different locale or custom death messages that don't follow that "by <killer>"
  pattern, the attribution check won't catch it - let me know and I can adjust the pattern.

**Pop** (totem) is separate and NOT kill-attributed, since a totem pop isn't credited to a
specific attacker the way a death message is - it just fires for any nearby non-friend
player who pops one. Off by default.

Cosmetic note: Meteor auto-generates a module's display title from its kebab-case name, so
`"ez"` shows up in the module list as "Ez" rather than "EZ" - let me know if you want that
changed.

## Things to double check before building
- **Minecraft/meteor/loader/loom/jdk versions** in `gradle/libs.versions.toml` — I set
  `minecraft = "1.21.11"` since that's what you told me, but I can't verify the exact
  matching `fabric-loader`/`loom`/`meteor` snapshot strings from here (these move fast and
  meteor-client's own template currently points at a different MC version entirely).
  Check these against whatever your [[nextclient-mod]] project already uses - they should
  probably match, since both target 1.21.11.
- **Color**: `meteor-client:color` is `225,25,25` (Meteor's standard addon-template red) -
  swap it if your last addon used something else.
- **Mappings**: `Efly` keeps its original type names (`PlayerEntity`, `ClientWorld`, etc.)
  since that's what your source already used and I didn't want to guess-convert it.
  `PlayerTrackerHud` and `AnarchyAddon` are written against the current official
  meteor-client API (`Player`, `mc.level`, etc.). If your project's mappings don't match
  one or the other, the fix is a handful of import lines, not the logic.

## Build-fix pass (round 2)
`./gradlew build` failed instantly with just the bare exception message `26.0.2` -
that's Gradle's own launcher choking, not your build script. Your machine's default
`java` is JDK 26; the Gradle 8.14 I pinned in round 1 only got Java 24 support and can't
even boot on JDK 26 (Java 26 support wasn't added to Gradle until 9.4). This has nothing
to do with the `java.toolchain { languageVersion.set(21) }` block in `build.gradle.kts` -
that only controls what JDK your *mod code* compiles against; it doesn't change what JDK
*Gradle itself* runs on to read the build scripts.

Fix: bumped the wrapper back up to Gradle **9.7.1** (current latest stable, released Aug
19 2026), which officially supports running on JVM 17 through 26. Your original file
actually had this closer to right at 9.6.1 - the 8.14 downgrade in round 1 was my own
mistake, prioritizing "Loom's documented Gradle range" over "can this even start on your
JDK." If Loom's SNAPSHOT build complains about a Gradle version mismatch now, that'll show
up as a much clearer, specific message than the bare `26.0.2` crash - send it over.

## Build-fix pass (round 1)
That wall of IntelliJ errors wasn't ~100 separate bugs - it was the Kotlin/Gradle script
model failing to resolve at all, which makes IntelliJ spray "cannot access java.lang.Object"
noise on nearly every line. Once the plugin itself can't apply, everything downstream shows
red. Root causes fixed this round:

1. **Wrong Loom plugin id for 1.21.11.** `net.fabricmc.fabric-loom` is now reserved for
   *unobfuscated* Minecraft (26.1+). 1.21.11 is still obfuscated, so it needs the legacy
   short id `fabric-loom` (confirmed against a real, currently-maintained 1.21.11 project).
   This alone likely explains "plugins {} block must not be used here" and "Unresolved
   reference 'fabric' on receiver of type Nothing" - the plugin never applied.
2. **Loom version far too old.** Was pinned to `1.9-SNAPSHOT`; Fabric's own 1.21.11
   announcement calls for Loom 1.14. Bumped `versions.loom`.
3. **Fabric Loader far too old.** Was `0.16.10`; bumped to `0.18.3` to match current
   1.21.11 projects.
4. **Yarn mappings were missing entirely.** There was no `mappings(...)` line in
   `build.gradle.kts` and no yarn entry in the version catalog at all. Without it, Loom has
   no named classes to compile `PlayerEntity`, `ClientWorld`, etc. against. Added
   `versions.yarn-mappings`, a `yarn` library entry, and `mappings(variantOf(libs.yarn) {
   classifier("v2") })`.
5. **`fabric-loader` / `meteor-client` used `implementation` instead of
   `modImplementation`.** Without `modImplementation`, Loom doesn't remap those jars to your
   Yarn mappings, so their classes stay in intermediary names and nothing lines up. Switched
   both.
6. **`version`/`group` were set inside the `base { }` block.** Those are `Project`
   properties, not members of `BasePluginExtension` - `archivesName` belongs in `base { }`,
   but `version`/`group` need to be set at the top level (matches the official template).
7. **Gradle wrapper pinned to 9.6.1**, which is newer than what Loom's own release notes
   declare support for (8.14 and 9.0). Rolled back to `8.14` to match a version Loom
   explicitly supports.

Still can't fully verify from here (flagged inline where relevant):
- The exact current Meteor Client snapshot build string for 1.21.11 - check
  `https://maven.meteordev.org/snapshots/meteordevelopment/meteor-client/` for the latest
  folder if the one in `libs.versions.toml` 404s.
- Whether `yarn-mappings = "1.21.11+build.4"` is still the newest build by the time you read
  this - check `https://fabricmc.net/develop`.

If you still get real (non-noise) errors after a Gradle sync with these changes, send them
over and I'll keep going.

## Grid snapping
`PlayerTrackerHud` overrides `move()` (what the HUD editor calls while you drag an
element) and rounds its position to the nearest multiple of `grid-size` pixels whenever
`snap-to-grid` is on. Off by default.

## Printer (Litematica auto-builder)
Ported the *idea* of another (Kotlin, non-Meteor) client's "Printer" module - not its actual
code, since that one is built on that client's own async task/build-simulation engine, which
doesn't exist in Meteor Client. What's actually shared between the two: reading the schematic
via Litematica's own `SchematicWorldHandler.getSchematicWorld()`.

`printer/LitematicaBridge.java` gets that one static call via reflection instead of a compile-time
Gradle dependency on Litematica, because I couldn't verify correct Maven coordinates/a matching
1.21.11 build for Litematica from this environment (no network access to Litematica's maven).
Once we have the schematic world back, everything else is plain `net.minecraft.world.World` API
(it's a real World subclass under the hood) - no further reflection needed. If you'd rather have
a normal compile dependency (better autocomplete/type safety), add something like:

```kotlin
repositories {
    maven { url = uri("https://masa.dy.fi/maven") } // unverified from here - double check
}
dependencies {
    modCompileOnly("fi.dy.masa.litematica:litematica-fabric-1.21.11:<version>") // unverified version string
}
```
then swap `LitematicaBridge` for direct imports. Couldn't verify that coordinate/version string
compiles against your exact 1.21.11 build from here, so left it as reflection-only instead of
guessing and risking a broken build.

`modules/world/Printer.java` itself is new, plain Meteor module code (settings + a `TickEvent.Post`
handler), not a port of the original's internals - see README.md for what it does and its settings.

## Auto TP Accept
New `modules/utility/AutoTpAccept.java`. Listens on `ReceiveMessageEvent` (the standard
Meteor Client chat-receive event - every other Meteor addon that reacts to incoming chat
uses this same event, but I couldn't compile-check it against your exact meteor-client
version from here), regex-matches each line against the `request-pattern` setting, and runs
`/tpy <name>` via `ChatUtils.sendPlayerMsg(...)` (same call Ez.java already uses to send
messages, so that part's verified against this project's own code) if the requester passes
the `Friends`/`Enemies`/`Everyone` mode check.

**The default `request-pattern` regex is confirmed against a real 6b6t chat screenshot**:
`"<name> wants to teleport to you."` (followed by a second line explaining `/tpy`/`/tpn`,
which the regex correctly ignores since it only matches the first line). No longer a guess.

Friends mode originally guessed a `Friends.isFriend(String)` overload that doesn't exist -
confirmed by a real compile error, which also told us the two overloads that DO exist:
`isFriend(PlayerEntity)` and `isFriend(PlayerListEntry)`. Since the whisper/tp-request sender
often isn't a loaded entity, `isFriend(String)` now looks up a `PlayerListEntry` via
`mc.player.networkHandler.getPlayerListEntry(name)` (standard vanilla tab-list lookup) and
passes that to the confirmed `isFriend(PlayerListEntry)` overload.

Enemies mode uses its own `enemy-names` setting, intentionally separate from
PlayerTrackerHud's `enemy-names` list (same idea, same convention, different instance). If
you'd rather they share one list, that's a quick follow-up - pull the shared bit into a small
`EnemyList` utility both features read from.

## Whisper Logger (ported from Plumbiller/PlumbillerPublic)
`modules/utility/WhisperLogger.java` is adapted from Plumbiller's original module (see
Credits in README.md). Kept the actual whisper-detection/HTML-logging logic as-is; changed:
- Package/category: `com.Plumbiller.publicaddon` -> `gamerguy11.anarchyaddon`, `Main.CATEGORY`
  -> `AnarchyAddon.CATEGORY`.
- Log folder: the original used its own `FileManager.getAddonFolder()` (not present in this
  project). Replaced with `FabricLoader.getInstance().getConfigDir().resolve("anarchyaddon")`,
  the same convention `InventorySorterModule` already uses for its own save file, so logs land
  in `.minecraft/config/anarchyaddon/WhisperLogs/`.
- Player name lookup: the original used its own `MultiVersionCompat.getProfileName(...)`
  helper (also not present here, since that project apparently supports multiple Minecraft
  versions and this addon doesn't). First replaced with `getGameProfile().getName()`, which
  turned out not to exist on this build's `GameProfile` (confirmed by a real compile error) -
  switched to `mc.player.getName().getString()` instead (`Entity#getName()`, the standard
  vanilla display-name accessor, unambiguous across versions).
- Swallowed the original's empty `catch (Exception e) {}` around the format-matching regex
  and its `e.printStackTrace()` in the file-write catch with this project's `error(...)` chat
  method instead, for consistency with how the rest of this addon reports failures.

Not verified against a real build from this environment - the regex-from-format-string
approach is copied over unchanged from a real, presumably-working addon, so it's on stronger
footing than the guesses elsewhere in this project, but worth a quick sanity check with your
actual whisper format on first use anyway.

## Dub Counter: module -> command
`DubCounter` module removed; its scan logic moved into `commands/DubCounterCommand.java`
as the `.dub` command (`.dub`, `.dub rendered`, `.dub rendered <radius>`), and
`DubCounterHud` now reads `DubCounterCommand`'s static `lastDubs`/`lastNormalChests`/`lastMode`
fields instead of looking up the module. Chat feedback moved from `Module#info()` (which
Efly/Ez/InventorySorterModule/etc. use, and which prefixes messages with the module's title)
to a direct `ChatUtils.info(...)` call, since a `Command` has no `info()`/`error()` helpers of
its own. Couldn't verify from here whether `ChatUtils.info` supports the same
`(highlight)...(default)` inline color-formatting syntax the module methods do, or adds any
chat prefix at all - if the `.dub` output looks unformatted or the placeholders show up
literally in chat, that's the thing to check first.

## Printer bug fix - breaking terrain around the player
Root cause: the tick loop scanned a plain cube around the player and compared every position
against `schematicWorld.getBlockState(pos)`, with no check for whether that position was
actually part of the loaded schematic's footprint. Outside the real build, the schematic world
just reports air, and with `break-mismatched` on by default the module "corrected" that by
breaking whatever real block was there - ground included. That's what looked like random
breaking under the player.

First fix attempt used a guessed reflection API (`getAllSchematicsPlacements`, `getOrigin`,
`getEnclosingSize`) that doesn't actually exist on Litematica's classes - it always failed and
the module correctly refused to run, but with the wrong root cause identified.

Real fix, based on Lambda's actual `PlayerBuildLayerUtils.inSchematic(pos)` (from the original
client's source): Litematica's `DataManager.getSchematicPlacementManager()` exposes
`getAllPlacementsTouchingChunk(pos)`, a per-position, chunk-scoped lookup - not a global list of
placements. Each returned entry has a `.placement` (check `isEnabled`) and a `.bb` bounding box
with `containsPos(pos)`. `LitematicaBridge.isInSchematic(pos)` mirrors that exact check via
reflection, resolving the wrapper object's methods dynamically off its runtime class (so we
don't have to hardcode Litematica's internal wrapper class name). `Printer` now calls this once
per scanned position and skips anything not inside an enabled placement, and refuses to run at
all if `getSchematicPlacementManager`/`getAllPlacementsTouchingChunk` can't be reached.
