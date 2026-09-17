# AnarchyAddon

A [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) addon for Fabric
**1.21.11**, built for the 6b6t anarchy server. Modules and HUD elements live under their
own "AnarchyAddon" category and HUD group, separate from Meteor's built-in stuff.

## Credits

- **Efly**, the elytra-flight logic, is [Volizray](https://github.com/Volizray)'s own
  **VolytraFly**: <https://github.com/Volizray/VolytraFly-Addon>. It's repackaged and
  renamed only for this addon (package, class name, module id `efly`, and category
  changed); the actual flight logic is untouched. Full credit for Efly's design and
  implementation goes to Volizray.
- **Shulker View** is ported from [cattyngmd/shulker-view](https://github.com/cattyngmd/shulker-view)
  (MIT licensed). Adapted to run as a normal Meteor module (own settings, own category)
  instead of a separate mod with its own config screen, and extended in this addon with
  fully customizable background color and on-screen position.
- **Whisper Logger** is adapted from [Plumbiller](https://github.com/Plumbiller)'s
  [PlumbillerPublic](https://github.com/Plumbiller/PlumbillerPublic) addon. Repackaged into
  this addon's structure (package, category, config folder) and simplified to target only
  this project's supported Minecraft version, with the rest of the logic unchanged. Full
  credit for the original module and its Discord-style HTML log design goes to Plumbiller.

## Requirements

| Component     | Version                    |
|----------------|------------------------------|
| Fabric Loader | 0.18.3+                     |
| Meteor Client | current 1.21.11 snapshot, check [maven.meteordev.org/snapshots](https://maven.meteordev.org/snapshots/meteordevelopment/meteor-client/) |
| JDK           | 21                           |

## Modules

### Efly
Elytra-flight and movement module tuned for 6b6t. This is Volizray's VolytraFly,
repackaged (see Credits above).

To use it, enable the module and start gliding on an elytra. Key setting groups:
- **Mapping mode** controls how the module reads terrain to steer around it, plus a
  `render-radius` for how far ahead it looks.
- **Building mode** slows the module down near unloaded or newly placed blocks so it
  doesn't fly into something that just appeared.
- **Player/hazard avoidance** steers around nearby players, wither skulls, arrows, and
  blocks within a configurable radius, with a `ignore-friends` toggle so it won't dodge
  people on your friends list.
- **Anti-slam** slows the module down before hitting terrain at speed.
- **Auto-pilot** automatically fires fireworks to maintain speed, with configurable delay
  and a slot to pull fireworks from.
- **Elytra/firework management**: `elytra-replace` and `chest-swap` swap in a fresh elytra
  from your inventory or a nearby chest when durability runs low; `replenish-fireworks`
  keeps a hotbar slot topped up.

Every one of these settings has its own in-game description in the module's settings
panel, so if a specific value's behavior isn't obvious, check there first.

### Ez
Sends a message you write yourself when a nearby player dies or pops a totem. Ships with
no built-in messages, both the Kill Messages and Pop Messages lists start empty, so
nothing is sent until you fill them in yourself.

To use it: enable the module, open its settings, and add lines to `kill-messages` and/or
`pop-messages`. Use `<n>` in a message to have it replaced with the other player's name.

- **Kill attribution**: only fires when the game's own death message credits you
  specifically (it matches your exact name after "by" in the vanilla death message, e.g.
  "SomePlayer was slain by YourName"). This means your own deaths are correctly ignored.
  If your server uses non-English or custom death-message phrasing that doesn't follow
  the "by killer" pattern, attribution won't catch it.
- **Pop** (totem) is separate and not kill-attributed, it fires for any nearby non-friend
  player who pops a totem, regardless of who caused it. Off by default (`enabled` under
  Pop Messages).
- `range` sets how far away a death or pop is still detected; `delay` throttles repeated
  triggers.

### Inventory Sorter
Save your current inventory layout (armor, main inventory, and hotbar, slot for slot) as a
named preset, then have it continuously re-sorted back into that exact layout, including
while items shift around from pickups, deaths, or crafting.

This is driven entirely through the `.invsorter` command below, not by toggling the
module in the click-GUI (toggling it by hand just re-applies whichever inventory was last
active).

Settings:
- `chat-notify`, chat message when an inventory is saved or finishes sorting.
- `tick-rate`, ticks between each slot move (higher is slower but safer against
  anti-cheat).
- `auto-disable`, turns the module off by itself once a sort finishes.

Saved layouts persist to `.minecraft/config/inventory-sorter/inventories.json`.

### Anti-Drop
Stops you from dropping certain items.

To use it, enable the module and, if you want to allow-list rather than block-list, add
items to `items`.

Settings:
- `all-items`, blocks dropping completely.
- `items`, the specific item list to block (ignored if `all-items` is on).
- `check-shulkers`, also checks the contents of a shulker box you're trying to drop.

### Auto TP Accept
Automatically runs `/tpy <player>` when a teleport request comes in, since 6b6t requires
that manually.

To use it, enable the module and pick a `mode`.

Settings:
- `mode`: `Friends` (Meteor's real friends list), `Enemies` (the `enemy-names` list
  below), or `Everyone`.
- `enemy-names`, player names to auto-accept when `mode` is `Enemies`. Not case sensitive.
- `request-pattern`, the regex used to detect a teleport request and pull the requester's
  name out of it (capture group 1). Default matches 6b6t's actual `/tpa` notification
  ("<n> wants to teleport to you."), only needs changing on a different server.
- `chat-feedback`, prints who got auto-accepted (or skipped) in chat.

### Whisper Logger
Keeps a running, Discord-styled HTML archive of your whisper conversations, one file per
person you've messaged. Adapted from Plumbiller's addon, see Credits.

Enable the module and it logs automatically, no further setup needed unless your server's
whisper wording differs from 6b6t's.

- Every whisper you send or receive gets appended to a local `.html` file, styled to look
  like a Discord DM thread rather than plain chat text.
- Files live under `.minecraft/config/anarchyaddon/WhisperLogs/`, named after the other
  person in the conversation.
- The two format settings (`receive-format`/`send-format`) tell it how to recognize a
  whisper in chat, so you can adjust them to match your server's `/msg` or `/tell` wording
  if you're not on 6b6t.

Settings:
- `receive-format`, pattern for incoming whispers (`{player}`/`{message}` placeholders).
- `send-format`, pattern for whispers you send.
- `time-format`, timestamp style shown next to each logged message.

### Shulker View
Shows shulker box contents in a live preview overlay while your inventory is open, no need
to actually open each shulker box. Ported from cattyngmd/shulker-view, see Credits.

Just enable the module and open your inventory, previews appear automatically next to any
shulker box shown in your inventory.

**General**
- `compact`, merges stacks of the same item and hides empty slots.
- `both-sides`, once previews fill one side of the screen, continues them on the other.
- `tooltips`, shows the normal item tooltip when hovering an item in a preview.
- `scale`, preview size, in tenths (10 = normal size).

**Background** (customizable)
- `background-color`, full RGBA color picker for the preview background. Set alpha to 0
  for no background at all.

**Position** (customizable)
- `anchor-right`, starts drawing previews from the right edge of the screen instead of the
  left. With `both-sides` on, overflow spills to whichever edge you didn't anchor to.
- `offset-x`, extra horizontal offset in pixels, measured inward from whichever edge
  previews are anchored to.
- `offset-y`, extra vertical offset in pixels, measured down from the top of the screen.

Click a preview to pick up that shulker box (same as vanilla slot-click behavior); scroll
to pan through previews that overflow the screen height.

Shulker View is a plain module, not a HUD element, so it can't be dragged around in
Meteor's HUD editor, `offset-x`/`offset-y`/`anchor-right` are how you reposition it
instead.

### Unblock Servers
Prevents known anarchy servers from being flagged as blocked by Mojang's server
blocklist, so you can actually connect to them.

Just enable it, there's nothing to configure. It ships with a default list of known
anarchy server domains (6b6t and its mirrors, 8b8t, 7b7t, and a few others) and also
fetches an up-to-date list from 6b6t's own API on activation, falling back to the built-in
defaults if that fails.

### Printer
**Under development, not included in this release.**

## HUD elements 

### Player Tracker
Lists every player currently loaded (within render/simulation distance), color-coded as
friend, enemy, or everyone else, with distance in meters. Fully draggable and configurable
like a built-in HUD element.

Drag it onto your screen from Meteor's HUD editor to use it.

- **General**: `limit` (max players shown), `show-distance`, `shadow`, `alignment`,
  `border`.
- **Colors**: separate colors for `friend-color`, `enemy-color`, `other-color`, and
  `distance-color`; `enemy-names` is a manually maintained list (Meteor has no built-in
  enemy list, so this addon keeps its own).
- **Grid Snapping**: `snap-to-grid` and `grid-size`, snaps the element to a pixel grid
  while dragging it in the HUD editor, instead of free placement.
- **Scale**: optional `custom-scale` independent of the global HUD text scale.
- **Background**: toggleable background with its own color.

### Dub Counter HUD
Small text element mirroring the `.dub` command's last result (reads the command's stored
state directly rather than re-scanning). Drag it onto your screen from the HUD editor,
then run `.dub` to populate it.

`show-mode` toggles whether it also shows Loaded vs. Rendered; `shadow` and `color`
control text appearance.

### Stats HUD
Displays your Minecraft statistics (play time, distance traveled, blocks broken, mobs
killed, and more) as a HUD element, dragged and positioned like any other HUD element.

Drag it onto your screen from the HUD editor. It works out of the box with sane defaults,
turn individual stats on or off in the **Stats** setting group.

- **General**: `shadow`, `alignment`, `text-color`, `border`.
- **Scale**: optional `custom-scale` independent of the global HUD text scale.
- **Background**: toggleable background with its own color.
- **Grid Snapping**: `snap-to-grid` and `grid-size`, same as Player Tracker's.
- **Sync**: `auto-sync` periodically requests fresh stats from the server (`sync-delay`
  controls how often); `update-interval` controls how often the displayed text
  recalculates from the last-known stats.
- **Order and Formatting**: `stat-order` controls which stats show and in what order;
  `hourly-rates` appends a per-hour rate next to applicable stats.
- **Stats**: toggle each stat individually (play time, distance traveled/walked/
  sprinted/flown/swum, blocks broken, mobs killed, players killed, items crafted/used/
  picked up, deaths, time since death, time since sleep). Blocks, mobs, and crafted/used/
  picked-up items can each be narrowed to a specific list via their own count-mode and
  list setting.

## Commands

`.invsorter` and `.dub` (use whatever command prefix your Meteor build is set to, not the
dot):

| Subcommand                | Effect                                            |
|-----------------------------|------------------------------------------------------|
| `.invsorter save <n>`    | Snapshot your current inventory as `<n>`         |
| `.invsorter load <n>`    | Enable the sorter and start sorting to `<n>`     |
| `.invsorter delete <n>`  | Remove a saved inventory                            |
| `.invsorter clear`          | Remove every saved inventory                        |
| `.invsorter list`           | List saved inventory names                          |
| `.dub`                       | Count double chests across every loaded chunk       |
| `.dub rendered`              | Count double chests within an 8-chunk radius        |
| `.dub rendered <radius>`     | Count double chests within a custom chunk radius    |

## Building

```
./gradlew build
```

The output jar lands in `build/libs/`, versioned as `anarchyaddon-<major>.<minor>.<patch>.jar`
(the version auto-increments on every build, see the top of `build.gradle.kts` if you want
to reset or change that). Drop the built jar into your `mods` folder alongside a matching
Meteor Client build.

`gradle/libs.versions.toml` is the single place to bump Minecraft/Yarn/Loader/Loom/Meteor
versions if any of them move.

## License

MIT, see [LICENSE](LICENSE). You're free to use, modify, and redistribute this addon as
long as the original copyright notice and the credits above are kept.

