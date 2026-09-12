# AnarchyAddon

A [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) addon for Fabric
**1.21.11**, built for the 6b6t anarchy server. Modules and HUD elements live under their
own **"Meteor++"** category/HUD group, separate from Meteor's built-in stuff.

- **Mod ID:** `anarchyaddon`
- **Author:** GamerGuy11
- **Target:** Minecraft 1.21.11, Fabric Loader, Meteor Client (snapshot build for 1.21.11)
- **Icon:** `src/main/resources/assets/anarchyaddon/icon.png`, referenced in `fabric.mod.json`

## Credits

- **Efly** — the elytra-flight logic is [Volizray](https://github.com/Volizray)'s own
  **VolytraFly**: <https://github.com/Volizray/VolytraFly-Addon>. It's repackaged and
  renamed only for this addon (package, class name, module id `efly`, and category
  changed) — the actual flight logic is untouched. Full credit for Efly's design and
  implementation goes to Volizray.
- **Shulker View** — ported from [cattyngmd/shulker-view](https://github.com/cattyngmd/shulker-view)
  (MIT licensed). Adapted to run as a normal Meteor module (own settings, own category)
  instead of a separate mod with its own config screen, and extended in this addon with
  fully customizable background color and on-screen position (see below).
- **Whisper Logger** — adapted from [Plumbiller](https://github.com/Plumbiller)'s
  [PlumbillerPublic](https://github.com/Plumbiller/PlumbillerPublic) addon. Repackaged into
  this addon's structure (package, category, config folder) and simplified to target only
  this project's supported Minecraft version, with the rest of the logic unchanged. Full
  credit for the original module and its Discord-style HTML log design goes to Plumbiller.
- Everything else (Ez, Inventory Sorter, `.dub` command + HUD, Anti-Drop, Auto TP Accept,
  Player Tracker HUD) was written from scratch for this addon.

## License

MIT — see [LICENSE](LICENSE). You're free to use, modify, and redistribute this addon
as long as the original copyright notice and the credits above are kept.

## Requirements

| Component      | Version                                     |
|-----------------|---------------------------------------------|
| Minecraft       | 1.21.11 (obfuscated — needs Yarn mappings)   |
| Yarn mappings   | 1.21.11+build.4 (check [fabricmc.net/develop](https://fabricmc.net/develop) for anything newer) |
| Fabric Loader   | 0.18.3+                                      |
| Fabric Loom     | 1.14-SNAPSHOT (legacy `fabric-loom` plugin id — see note below) |
| JDK             | 21                                            |
| Meteor Client   | current 1.21.11 snapshot — check [maven.meteordev.org/snapshots](https://maven.meteordev.org/snapshots/meteordevelopment/meteor-client/) |

> **Note:** `net.fabricmc.fabric-loom` is now reserved for *unobfuscated* Minecraft
> (26.1+). 1.21.11 is still obfuscated, so `build.gradle.kts` uses the legacy short
> plugin id `fabric-loom` instead — don't "fix" this back to the long id.

## Building

```
./gradlew build
```

The output jar lands in `build/libs/`, versioned as `anarchyaddon-<major>.<minor>.<patch>.jar`
(the version auto-increments on every build — see the comment block at the top of
`build.gradle.kts` if you want to reset or change that). Drop the built jar into your
`mods` folder alongside a matching Meteor Client build.

`gradle/libs.versions.toml` is the single place to bump Minecraft/Yarn/Loader/Loom/Meteor
versions if any of them move.

## Modules ("Meteor++" category)

### Efly
Elytra-flight/movement module tuned for 6b6t. See **Credits** above — this is Volizray's
VolytraFly, repackaged.

### Ez
Sends a message you write yourself when a nearby player dies or pops a totem. Ships with
**no built-in messages** — both the Kill Messages and Pop Messages lists start empty, so
nothing is sent until you fill them in yourself. Use `<NAME>` in a message to have it
replaced with the other player's name.

- **Kill attribution:** only fires when the game's own death message credits *you*
  specifically (it matches your exact name after "by" in the vanilla death message, e.g.
  "SomePlayer was slain by YourName"). This means your own deaths are correctly ignored.
  If your server uses non-English or custom death-message phrasing that doesn't follow
  the "... by \<killer\>" pattern, attribution won't catch it.
- **Pop** (totem) is separate and not kill-attributed — it fires for any nearby
  non-friend player who pops a totem, regardless of who caused it. Off by default.
- Meteor auto-generates the display title from the module's id, so this module shows up
  as "Ez" (not "EZ") in the module list.

### Inventory Sorter
Save your current inventory layout (armor + main inventory + hotbar, slot-for-slot) as a
named preset, then have it continuously re-sorted back into that exact layout — including
while items shift around from pickups, deaths, or crafting. This is driven entirely
through the `.invsorter` command below, **not** by toggling the module in the click-GUI
(toggling it by hand just re-applies whichever inventory was last active).

Settings:
- `chat-notify` — chat message when an inventory is saved or finishes sorting.
- `tick-rate` — ticks between each slot move (higher = slower but safer against anti-cheat).
- `auto-disable` — turns the module off by itself once a sort finishes.

Saved layouts persist to `.minecraft/config/inventory-sorter/inventories.json`.

### Dub Counter
Not a module - it's the `.dub` chat command. Counts double chests, either across every
loaded chunk or only within a given render-distance radius.

Usage:
- `.dub` — counts double chests across every loaded chunk.
- `.dub rendered` — counts only within an 8-chunk radius of you.
- `.dub rendered <radius>` — counts within a custom chunk radius (1-32).

Each run prints the result in chat and updates the **Dub Counter HUD** element, so you can
also just glance at the HUD instead of re-running the command.

### Anti-Drop
Stops you from dropping certain items.

Settings:
- `all-items` — blocks dropping completely.
- `items` — the specific item list to block (ignored if `all-items` is on).
- `check-shulkers` — also checks the contents of a shulker box you're trying to drop.

### Auto TP Accept
Automatically runs `/tpy <player>` when a teleport request comes in, since 6b6t requires
that manually.

Settings:
- `mode` — `Friends` (Meteor's real friends list, via `Friends.get()`), `Enemies` (the
  `enemy-names` list below - addon-managed, same idea as Player Tracker HUD's list, since
  Meteor has no built-in enemy concept), or `Everyone`.
- `enemy-names` — player names to auto-accept when `mode` is `Enemies`. Not case sensitive.
- `request-pattern` — the regex used to detect a teleport request and pull the requester's
  name out of it (capture group 1). Default matches 6b6t's actual `/tpa` notification
  (`"<name> wants to teleport to you."`) - only needs changing on a different server.
- `chat-feedback` — prints who got auto-accepted (or skipped) in chat.

### Whisper Logger
Keeps a running, Discord-styled HTML archive of your whisper conversations, one file per
person you've messaged. Adapted from Plumbiller's addon; see **Credits**.

- Every whisper you send or receive gets appended to a local `.html` file, styled to look
  like a Discord DM thread rather than plain chat text.
- Files live under `.minecraft/config/anarchyaddon/WhisperLogs/`, named after the other
  person in the conversation.
- The two format settings (`receive-format`/`send-format`) tell it how to recognize a whisper
  in chat, so you can adjust them to match your server's `/msg` or `/tell` wording if you're
  not on 6b6t.

Settings:
- `receive-format` — pattern for incoming whispers (`{player}`/`{message}` placeholders).
- `send-format` — pattern for whispers you send.
- `time-format` — timestamp style shown next to each logged message.

### Shulker View
Shows shulker box contents in a live preview overlay while your inventory is open — no
need to actually open each shulker box. Ported from cattyngmd/shulker-view; see
**Credits**.

**General**
- `compact` — merges stacks of the same item and hides empty slots.
- `both-sides` — once previews fill one side of the screen, continues them on the other.
- `tooltips` — shows the normal item tooltip when hovering an item in a preview.
- `scale` — preview size, in tenths (10 = normal size).

**Background** *(customizable)*
- `background-color` — full RGBA color picker for the preview background. Set alpha to 0
  for no background at all.

**Position** *(customizable)*
- `anchor-right` — starts drawing previews from the right edge of the screen instead of
  the left. With `both-sides` on, overflow spills to whichever edge you *didn't* anchor to.
- `offset-x` — extra horizontal offset in pixels, measured inward from whichever edge
  previews are anchored to.
- `offset-y` — extra vertical offset in pixels, measured down from the top of the screen.

Click a preview to pick up that shulker box (same as vanilla slot-click behavior); scroll
to pan through previews that overflow the screen height.

> Shulker View is a plain module, not a HUD element, so it can't be dragged around in
> Meteor's HUD editor — `offset-x`/`offset-y`/`anchor-right` are how you reposition it
> instead.

### Printer
Auto-builds whatever schematic is currently loaded in **[Litematica](https://github.com/maruohon/litematica)**, one block per tick, in a cube around you. Requires Litematica to be installed alongside this addon — checked at startup, and the module disables itself with a chat error if Litematica isn't found. It also only acts inside the bounding box of your currently enabled schematic placement(s) — if that can't be determined, it disables itself instead of touching anything, so it never breaks terrain outside the actual build.

> This is a from-scratch reimplementation of the same idea as another (Kotlin-based, non-Meteor)
> client's "Printer" module. That module reads the schematic the exact same way this one does —
> through Litematica's own `SchematicWorldHandler.getSchematicWorld()` — but everything *around*
> that call in the original runs on a large, client-specific async task/build-simulation engine
> (queued "build results", rotation prediction, its own config framework, etc.) that has no
> equivalent anywhere in Meteor Client, so that part genuinely can't be "dropped in" as-is. This
> version instead reads the schematic through reflection (see
> `printer/LitematicaBridge.java` — no Litematica Gradle dependency needed) and drives placement
> with Meteor's own normal module tools (`BlockUtils`, `InvUtils`, a plain tick loop): one
> place/break action per tick, always picking the closest mismatch first. It's simpler than the
> original — no async batching or build-order optimization beyond "closest first" — but
> self-contained and easy to follow/extend.

Settings:
- `range` — cube radius (in blocks) around you to scan for mismatches (1–6).
- `reach` — max distance from your eyes a block can be to act on it.
- `delay` — ticks to wait between each place/break action.
- `place-missing` — places blocks the schematic wants that aren't there yet.
- `break-mismatched` — breaks blocks that don't match the schematic, including extra blocks where the schematic wants air.
- `ignore-fluids` — never breaks a fluid source/flow, even if the schematic wants air there.
- `rotate` — visually turns your view to face the block while placing; off places silently without moving your camera.
- `notify-missing-items` — throttled chat warning when a needed block isn't in your hotbar.

## HUD elements ("Meteor++" group)

### Player Tracker
Lists every player currently loaded (within render/simulation distance), color-coded as
friend / enemy / everyone else, with distance in meters. Fully draggable and configurable
like a built-in HUD element.

- **General:** `limit` (max players shown), `show-distance`, `shadow`, `alignment`, `border`.
- **Colors:** separate colors for `friend-color`, `enemy-color`, `other-color`, and
  `distance-color`; `enemy-names` is a manually maintained list (Meteor has no built-in
  enemy list, so this addon keeps its own).
- **Grid Snapping:** `snap-to-grid` + `grid-size` — snaps the element to a pixel grid while
  dragging it in the HUD editor, instead of free placement.
- **Scale:** optional `custom-scale` independent of the global HUD text scale.
- **Background:** toggleable background with its own color.

### Dub Counter HUD
Small text element mirroring the `.dub` command's last result (reads the command's stored
state directly rather than re-scanning). `show-mode` toggles whether it also shows Loaded
vs. Rendered; `shadow` and `color` control text appearance.

## Commands

`.invsorter` and `.dub` (use whatever command prefix your Meteor build is set to, not the dot):

| Subcommand                     | Effect                                            |
|---------------------------------|----------------------------------------------------|
| `.invsorter save <name>`       | Snapshot your current inventory as `<name>`        |
| `.invsorter load <name>`       | Enable the sorter and start sorting to `<name>`    |
| `.invsorter delete <name>`     | Remove a saved inventory                           |
| `.invsorter clear`             | Remove every saved inventory                       |
| `.invsorter list`              | List saved inventory names                         |
| `.dub`                          | Count double chests across every loaded chunk      |
| `.dub rendered`                 | Count double chests within an 8-chunk radius       |
| `.dub rendered <radius>`        | Count double chests within a custom chunk radius   |

## Project layout

```
src/main/java/gamerguy11/anarchyaddon/
├── AnarchyAddon.java              # main addon entrypoint, registers everything below
├── commands/
│   ├── DubCounterCommand.java
│   └── InventoryCommand.java
├── hud/
│   ├── DubCounterHud.java
│   └── PlayerTrackerHud.java
├── mixin/shulkerview/             # hooks used only by Shulker View
├── modules/
│   ├── Efly.java
│   ├── Ez.java
│   ├── InventorySorterModule.java
│   ├── utility/
│   │   ├── AntiDrop.java
│   │   ├── AutoTpAccept.java
│   │   ├── ShulkerView.java
│   │   └── WhisperLogger.java
│   └── world/
│       └── Printer.java
├── printer/
│   └── LitematicaBridge.java      # reflection-only Litematica lookup, no compile dependency
└── shulkerview/                   # Shulker View's rendering/update/data classes
```

## Known caveats

- Double-check the exact Minecraft/Yarn/Loader/Loom/Meteor snapshot strings in
  `gradle/libs.versions.toml` before building — these move fast and couldn't all be
  verified from the environment this addon was written in.
- `Efly` keeps the type names its original source already used; `PlayerTrackerHud` and
  `AnarchyAddon` are written against the current official meteor-client API. If your
  project's mappings don't line up with one or the other, it should just be a handful of
  import fixes, not a logic rewrite.
- `Printer`'s in-schematic check (`LitematicaBridge.isInSchematic()`) mirrors the real
  `DataManager.getSchematicPlacementManager().getAllPlacementsTouchingChunk(pos)` API (verified
  against Lambda's actual source), resolving the returned wrapper object's methods dynamically
  at runtime rather than hardcoding its class name. If Printer errors out on activate with
  "Couldn't reach Litematica's placement manager", check the reported reason against your
  installed Litematica version.
