package gamerguy11.sixtoolsaddon.hud;

import gamerguy11.sixtoolsaddon.SixToolsAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.Stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class StatsHud extends HudElement {
    public static final HudElementInfo<StatsHud> INFO = new HudElementInfo<>(
        SixToolsAddon.HUD_GROUP,
        "stats",
        "Displays player statistics, dragged and positioned like any other HUD element.",
        StatsHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");
    private final SettingGroup sgGrid = settings.createGroup("Grid Snapping");
    private final SettingGroup sgSync = settings.createGroup("Sync");
    private final SettingGroup sgOrder = settings.createGroup("Order and Formatting");
    private final SettingGroup sgStats = settings.createGroup("Stats");

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders a shadow behind the text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal text alignment.")
        .defaultValue(Alignment.Left)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the HUD text.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("Padding around the element.")
        .defaultValue(2)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this HUD element instead of the global HUD text scale.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .defaultValue(1)
        .min(0.1)
        .sliderRange(0.1, 10)
        .visible(customScale::get)
        .build()
    );

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a background behind the stats.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .defaultValue(new SettingColor(25, 25, 25, 100))
        .visible(background::get)
        .build()
    );

    private final Setting<Boolean> snapToGrid = sgGrid.add(new BoolSetting.Builder()
        .name("snap-to-grid")
        .description("While dragging this element in the HUD editor, snaps it to a pixel grid instead of free placement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> gridSize = sgGrid.add(new IntSetting.Builder()
        .name("grid-size")
        .description("Size, in pixels, of one grid cell.")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 50)
        .visible(snapToGrid::get)
        .build()
    );

    private final Setting<Boolean> autoSync = sgSync.add(new BoolSetting.Builder()
        .name("auto-sync")
        .description("Automatically requests fresh stats from the server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> syncDelay = sgSync.add(new IntSetting.Builder()
        .name("sync-delay")
        .description("Delay between sync packets (seconds).")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 300)
        .visible(autoSync::get)
        .build()
    );

    private final Setting<Integer> updateInterval = sgSync.add(new IntSetting.Builder()
        .name("update-interval")
        .description("Recomputes displayed stats every X ticks (20 ticks = 1 second).")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 200)
        .build()
    );

    private static final String KEY_PLAYTIME = "playtime";
    private static final String KEY_DISTANCE = "distancetravelled";
    private static final String KEY_DISTANCE_WALKED = "distancewalked";
    private static final String KEY_DISTANCE_SPRINTED = "distancesprinted";
    private static final String KEY_DISTANCE_FLOWN = "distanceflown";
    private static final String KEY_DISTANCE_SWUM = "distanceswum";
    private static final String KEY_BLOCKS = "blocksbroken";
    private static final String KEY_MOBS = "mobskilled";
    private static final String KEY_PVP = "playerkills";
    private static final String KEY_ITEMSCRAFTED = "itemscrafted";
    private static final String KEY_ITEMSUSED = "itemsused";
    private static final String KEY_ITEMSPICKED = "itemspickedup";
    private static final String KEY_DEATHS = "deaths";
    private static final String KEY_TIMESINCEDEATH = "timesincedeath";
    private static final String KEY_TIMESINCESLEEP = "timesincesleep";

    private final Setting<List<String>> statOrder = sgOrder.add(new StringListSetting.Builder()
        .name("stat-order")
        .description("Order stats appear in the HUD. Default: playtime, distancetravelled, distancewalked, distancesprinted, distanceflown, distanceswum, blocksbroken, mobskilled, playerkills, itemscrafted, itemsused, itemspickedup, deaths, timesincedeath, timesincesleep")
        .defaultValue(List.of(
            KEY_PLAYTIME, KEY_DISTANCE, KEY_DISTANCE_WALKED, KEY_DISTANCE_SPRINTED, KEY_DISTANCE_FLOWN, KEY_DISTANCE_SWUM,
            KEY_BLOCKS, KEY_MOBS, KEY_PVP, KEY_ITEMSCRAFTED, KEY_ITEMSUSED, KEY_ITEMSPICKED,
            KEY_DEATHS, KEY_TIMESINCEDEATH, KEY_TIMESINCESLEEP
        ))
        .build()
    );

    private final Setting<Boolean> showRates = sgOrder.add(new BoolSetting.Builder()
        .name("hourly-rates")
        .description("Show hourly rates for stats. Based on total play time.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showPlayTime = sgStats.add(new BoolSetting.Builder()
        .name("play-time")
        .description("Show total play time.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showDistance = sgStats.add(new BoolSetting.Builder()
        .name("distance-travelled")
        .description("Show total distance travelled, across every movement type.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showDistanceWalked = sgStats.add(new BoolSetting.Builder()
        .name("distance-walked")
        .description("Show distance walked.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showDistanceSprinted = sgStats.add(new BoolSetting.Builder()
        .name("distance-sprinted")
        .description("Show distance sprinted.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showDistanceFlown = sgStats.add(new BoolSetting.Builder()
        .name("distance-flown")
        .description("Show distance flown (creative flight + elytra gliding).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showDistanceSwum = sgStats.add(new BoolSetting.Builder()
        .name("distance-swum")
        .description("Show distance swum (surface + underwater).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> showBlocks = sgStats.add(new BoolSetting.Builder()
        .name("blocks-broken")
        .description("Show blocks broken count.")
        .defaultValue(true)
        .build()
    );

    private enum BlockMode { Count, DoNotCount }

    private final Setting<BlockMode> blocksCountMode = sgStats.add(new EnumSetting.Builder<BlockMode>()
        .name("blocks-broken-count-mode")
        .description("The mode for counting Blocks.")
        .defaultValue(BlockMode.DoNotCount)
        .visible(showBlocks::get)
        .build()
    );

    private final Setting<List<Block>> blocks = sgStats.add(new BlockListSetting.Builder()
        .name("blocks")
        .visible(showBlocks::get)
        .build()
    );

    private final Setting<Boolean> showMobs = sgStats.add(new BoolSetting.Builder()
        .name("mobs-killed")
        .description("Show total mobs killed.")
        .defaultValue(true)
        .build()
    );

    private enum EntityMode { Count, DoNotCount }

    private final Setting<EntityMode> mobsCountMode = sgStats.add(new EnumSetting.Builder<EntityMode>()
        .name("mobs-killed-count-mode")
        .description("The mode for counting Mobs.")
        .defaultValue(EntityMode.DoNotCount)
        .visible(showMobs::get)
        .build()
    );

    private final Setting<Set<EntityType<?>>> mobs = sgStats.add(new EntityTypeListSetting.Builder()
        .name("mobs")
        .visible(showMobs::get)
        .build()
    );

    private final Setting<Boolean> showPvpKills = sgStats.add(new BoolSetting.Builder()
        .name("players-killed")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showItemsCrafted = sgStats.add(new BoolSetting.Builder()
        .name("items-crafted")
        .description("Show items crafted count.")
        .defaultValue(true)
        .build()
    );

    private enum CraftMode { Count, DoNotCount }

    private final Setting<CraftMode> craftedCountMode = sgStats.add(new EnumSetting.Builder<CraftMode>()
        .name("items-crafted-count-mode")
        .description("The mode for counting crafted Items.")
        .defaultValue(CraftMode.DoNotCount)
        .visible(showItemsCrafted::get)
        .build()
    );

    private final Setting<List<Item>> craftedItems = sgStats.add(new ItemListSetting.Builder()
        .name("crafted-items")
        .visible(showItemsCrafted::get)
        .build()
    );

    private final Setting<Boolean> showItemsUsed = sgStats.add(new BoolSetting.Builder()
        .name("items-used")
        .description("Show items used count.")
        .defaultValue(true)
        .build()
    );

    private enum UseMode { Count, DoNotCount }

    private final Setting<UseMode> usedCountMode = sgStats.add(new EnumSetting.Builder<UseMode>()
        .name("items-used-count-mode")
        .description("The mode for counting used Items.")
        .defaultValue(UseMode.DoNotCount)
        .visible(showItemsUsed::get)
        .build()
    );

    private final Setting<List<Item>> usedItems = sgStats.add(new ItemListSetting.Builder()
        .name("used-items")
        .visible(showItemsUsed::get)
        .build()
    );

    private final Setting<Boolean> showItemsPicked = sgStats.add(new BoolSetting.Builder()
        .name("items-picked-up")
        .description("Show items picked up count.")
        .defaultValue(true)
        .build()
    );

    private enum PickupMode { Count, DoNotCount }

    private final Setting<PickupMode> itemsCountMode = sgStats.add(new EnumSetting.Builder<PickupMode>()
        .name("items-picked-up-count-mode")
        .description("The mode for counting Items.")
        .defaultValue(PickupMode.DoNotCount)
        .visible(showItemsPicked::get)
        .build()
    );

    private final Setting<List<Item>> items = sgStats.add(new ItemListSetting.Builder()
        .name("picked-up-items")
        .visible(showItemsPicked::get)
        .build()
    );

    private final Setting<Boolean> showDeaths = sgStats.add(new BoolSetting.Builder()
        .name("deaths")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showTimeSinceDeath = sgStats.add(new BoolSetting.Builder()
        .name("time-since-death")
        .description("Show time since last death.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showTimeSinceSleep = sgStats.add(new BoolSetting.Builder()
        .name("time-since-sleep")
        .description("Show time since last sleep.")
        .defaultValue(true)
        .build()
    );

    private List<Block> allBlocks;
    private List<Item> allItems;
    private List<EntityType<?>> allEntities;

    private final Map<String, String> cachedStatLines = new ConcurrentHashMap<>();
    private boolean computedOnce = false;
    private long lastComputeMs = 0;
    private long lastSyncMs = 0;

    public StatsHud() {
        super(INFO);
    }

    private void ensureRegistriesLoaded() {
        if (allBlocks == null) allBlocks = Registries.BLOCK.stream().toList();
        if (allItems == null) allItems = Registries.ITEM.stream().toList();
        if (allEntities == null) allEntities = Registries.ENTITY_TYPE.stream().toList();
    }

    private void updateStats() {
        if (mc.player == null || mc.player.getStatHandler() == null) {
            computedOnce = false;
            cachedStatLines.clear();
            return;
        }

        ensureRegistriesLoaded();
        long now = System.currentTimeMillis();

        if (!computedOnce) {
            computeAllStats();
            computedOnce = true;
            lastComputeMs = now;

            if (autoSync.get() && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
                lastSyncMs = now;
            }

            return;
        }

        if (autoSync.get() && mc.getNetworkHandler() != null && now - lastSyncMs >= syncDelay.get() * 1000L) {
            mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
            lastSyncMs = now;
        }

        if (now - lastComputeMs >= updateInterval.get() * 50L) {
            computeAllStats();
            lastComputeMs = now;
        }
    }

    private double getHoursPlayed(StatHandler statHandler) {
        int playTime = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
        return playTime / 72000.0;
    }

    private void computeAllStats() {
        if (mc.player == null) return;
        StatHandler statHandler = mc.player.getStatHandler();
        if (statHandler == null) return;

        Map<String, String> newStats = new LinkedHashMap<>();
        double hours = getHoursPlayed(statHandler);

        if (showPlayTime.get()) {
            int playTime = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            long hrs = playTime / 72000L;
            long mins = (playTime % 72000) / 1200L;
            newStats.put(KEY_PLAYTIME, String.format("Play Time: %dh %dm", hrs, mins));
        }

        int walkCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM));
        int sprintCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM));
        int crouchCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM));
        int flyCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM));
        int aviateCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.AVIATE_ONE_CM));
        int swimCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SWIM_ONE_CM));
        int walkUnderWaterCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_UNDER_WATER_ONE_CM));
        int walkOnWaterCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ON_WATER_ONE_CM));
        int minecartCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.MINECART_ONE_CM));
        int boatCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.BOAT_ONE_CM));
        int pigCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PIG_ONE_CM));
        int horseCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.HORSE_ONE_CM));
        int striderCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.STRIDER_ONE_CM));
        int happyGhastCm = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.HAPPY_GHAST_ONE_CM));

        if (showDistance.get()) {
            long totalCm = (long) walkCm + sprintCm + crouchCm + flyCm + aviateCm + swimCm + walkUnderWaterCm
                + walkOnWaterCm + minecartCm + boatCm + pigCm + horseCm + striderCm + happyGhastCm;
            double km = totalCm / 100000.0;
            String text = String.format("Distance travelled: %.1fkm", km);
            if (showRates.get() && hours > 0) text += String.format(" (%.1f/h)", km / hours);
            newStats.put(KEY_DISTANCE, text);
        }

        if (showDistanceWalked.get()) {
            double km = walkCm / 100000.0;
            String text = String.format("Distance walked: %.1fkm", km);
            if (showRates.get() && hours > 0) text += String.format(" (%.1f/h)", km / hours);
            newStats.put(KEY_DISTANCE_WALKED, text);
        }

        if (showDistanceSprinted.get()) {
            double km = sprintCm / 100000.0;
            String text = String.format("Distance sprinted: %.1fkm", km);
            if (showRates.get() && hours > 0) text += String.format(" (%.1f/h)", km / hours);
            newStats.put(KEY_DISTANCE_SPRINTED, text);
        }

        if (showDistanceFlown.get()) {
            double km = (flyCm + aviateCm) / 100000.0;
            String text = String.format("Distance flown: %.1fkm", km);
            if (showRates.get() && hours > 0) text += String.format(" (%.1f/h)", km / hours);
            newStats.put(KEY_DISTANCE_FLOWN, text);
        }

        if (showDistanceSwum.get()) {
            double km = (swimCm + walkUnderWaterCm) / 100000.0;
            String text = String.format("Distance swum: %.1fkm", km);
            if (showRates.get() && hours > 0) text += String.format(" (%.1f/h)", km / hours);
            newStats.put(KEY_DISTANCE_SWUM, text);
        }

        if (showBlocks.get()) {
            int totalBlocksBroken = 0;
            List<Block> blockList = blocks.get();
            BlockMode blockMode = blocksCountMode.get();

            Block singleBlock = (blockMode == BlockMode.Count && !blockList.isEmpty()) ? blockList.get(0) : null;

            for (Block block : allBlocks) {
                boolean shouldCount = blockMode == BlockMode.Count
                    ? (blockList.isEmpty() || blockList.contains(block))
                    : !blockList.contains(block);

                if (shouldCount) {
                    Stat<Block> stat = Stats.MINED.getOrCreateStat(block);
                    totalBlocksBroken += statHandler.getStat(stat);
                }
            }

            String blocksText = singleBlock != null
                ? String.format("%s broken: %d", singleBlock.getName().getString(), totalBlocksBroken)
                : String.format("Blocks broken: %d", totalBlocksBroken);
            if (showRates.get() && hours > 0) blocksText += String.format(" (%.0f/h)", totalBlocksBroken / hours);
            newStats.put(KEY_BLOCKS, blocksText);
        }

        if (showMobs.get()) {
            int totalMobsKilled = 0;
            List<EntityType<?>> mobList = new ArrayList<>(mobs.get());
            EntityMode mobMode = mobsCountMode.get();

            EntityType<?> singleMob = (mobMode == EntityMode.Count && !mobList.isEmpty()) ? mobList.get(0) : null;

            for (EntityType<?> entityType : allEntities) {
                boolean shouldCount = mobMode == EntityMode.Count
                    ? (mobList.isEmpty() || mobList.contains(entityType))
                    : !mobList.contains(entityType);

                if (shouldCount) {
                    Stat<EntityType<?>> stat = Stats.KILLED.getOrCreateStat(entityType);
                    totalMobsKilled += statHandler.getStat(stat);
                }
            }

            String mobsText = singleMob != null
                ? String.format("%s killed: %d", singleMob.getName().getString(), totalMobsKilled)
                : String.format("Mobs killed: %d", totalMobsKilled);
            if (showRates.get() && hours > 0) mobsText += String.format(" (%.0f/h)", totalMobsKilled / hours);
            newStats.put(KEY_MOBS, mobsText);
        }

        if (showPvpKills.get()) {
            int pvpKills = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAYER_KILLS));
            String text = String.format("Players killed: %d", pvpKills);
            if (showRates.get() && hours > 0) text += String.format(" (%.1f/h)", pvpKills / hours);
            newStats.put(KEY_PVP, text);
        }

        if (showItemsCrafted.get()) {
            int totalItemsCrafted = 0;
            List<Item> craftedItemList = new ArrayList<>(craftedItems.get());
            CraftMode craftedMode = craftedCountMode.get();

            for (Item item : allItems) {
                boolean shouldCount = craftedMode == CraftMode.Count
                    ? (craftedItemList.isEmpty() || craftedItemList.contains(item))
                    : !craftedItemList.contains(item);

                if (shouldCount) totalItemsCrafted += statHandler.getStat(Stats.CRAFTED.getOrCreateStat(item));
            }

            String craftedText;
            if (craftedMode == CraftMode.Count && craftedItemList.size() == 1) {
                Item singleItem = craftedItemList.get(0);
                craftedText = String.format("%s crafted: %d", singleItem.getName(singleItem.getDefaultStack()).getString(), totalItemsCrafted);
            } else {
                craftedText = String.format("Items crafted: %d", totalItemsCrafted);
            }
            if (showRates.get() && hours > 0) craftedText += String.format(" (%.0f/h)", totalItemsCrafted / hours);
            newStats.put(KEY_ITEMSCRAFTED, craftedText);
        }

        if (showItemsUsed.get()) {
            int totalItemsUsed = 0;
            List<Item> usedItemList = new ArrayList<>(usedItems.get());
            UseMode usedMode = usedCountMode.get();

            for (Item item : allItems) {
                boolean shouldCount = usedMode == UseMode.Count
                    ? (usedItemList.isEmpty() || usedItemList.contains(item))
                    : !usedItemList.contains(item);

                if (shouldCount) totalItemsUsed += statHandler.getStat(Stats.USED.getOrCreateStat(item));
            }

            String usedText;
            if (usedMode == UseMode.Count && usedItemList.size() == 1) {
                Item singleItem = usedItemList.get(0);
                usedText = String.format("%s used: %d", singleItem.getName(singleItem.getDefaultStack()).getString(), totalItemsUsed);
            } else {
                usedText = String.format("Items used: %d", totalItemsUsed);
            }
            if (showRates.get() && hours > 0) usedText += String.format(" (%.0f/h)", totalItemsUsed / hours);
            newStats.put(KEY_ITEMSUSED, usedText);
        }

        if (showItemsPicked.get()) {
            int totalItemsPicked = 0;
            List<Item> itemList = new ArrayList<>(items.get());
            PickupMode itemMode = itemsCountMode.get();

            for (Item item : allItems) {
                boolean shouldCount = itemMode == PickupMode.Count
                    ? (itemList.isEmpty() || itemList.contains(item))
                    : !itemList.contains(item);

                if (shouldCount) totalItemsPicked += statHandler.getStat(Stats.PICKED_UP.getOrCreateStat(item));
            }

            String itemsText;
            if (itemMode == PickupMode.Count && itemList.size() == 1) {
                Item singleItem = itemList.get(0);
                itemsText = String.format("%s picked up: %d", singleItem.getName(singleItem.getDefaultStack()).getString(), totalItemsPicked);
            } else {
                itemsText = String.format("Items picked up: %d", totalItemsPicked);
            }
            if (showRates.get() && hours > 0) itemsText += String.format(" (%.0f/h)", totalItemsPicked / hours);
            newStats.put(KEY_ITEMSPICKED, itemsText);
        }

        int deaths = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS));
        if (showDeaths.get()) {
            String text = String.format("Deaths: %d", deaths);
            if (showRates.get() && hours > 0) text += String.format(" (%.1f/h)", deaths / hours);
            newStats.put(KEY_DEATHS, text);
        }

        if (showTimeSinceDeath.get() && deaths >= 1) {
            int ticksSinceDeath = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH));
            newStats.put(KEY_TIMESINCEDEATH, "Since death: " + formatTicksAsTime(ticksSinceDeath));
        }

        if (showTimeSinceSleep.get()) {
            int ticksSinceSleep = statHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
            newStats.put(KEY_TIMESINCESLEEP, "Since last sleep: " + formatTicksAsTime(ticksSinceSleep));
        }

        cachedStatLines.clear();
        cachedStatLines.putAll(newStats);
    }

    private String formatTicksAsTime(int ticks) {
        long totalSeconds = ticks / 20L;
        long hrs = totalSeconds / 3600;
        long mins = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        return hrs > 0
            ? String.format("%dh %02dm %02ds", hrs, mins, secs)
            : String.format("%dm %02ds", mins, secs);
    }

    private List<String> getLines() {
        return statOrder.get().stream()
            .filter(cachedStatLines::containsKey)
            .map(cachedStatLines::get)
            .toList();
    }

    @Override
    public void move(int deltaX, int deltaY) {
        super.move(deltaX, deltaY);

        if (snapToGrid.get()) {
            int size = Math.max(1, gridSize.get());

            box.x = Math.round((float) box.x / size) * size;
            box.y = Math.round((float) box.y / size) * size;

            updatePos();
        }
    }

    @Override
    public void tick(HudRenderer renderer) {
        updateStats();
        List<String> lines = getLines();

        double width;
        double height;

        if (lines.isEmpty()) {
            width = renderer.textWidth("Stats", shadow.get(), getScale());
            height = renderer.textHeight(shadow.get(), getScale());
        } else {
            width = 0;
            height = 0;

            for (String line : lines) {
                width = Math.max(width, renderer.textWidth(line, shadow.get(), getScale()));
                height += renderer.textHeight(shadow.get(), getScale());
            }

            height += (lines.size() - 1) * 2;
        }

        setSize(width, height);
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    @Override
    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth() - border.get() * 2, width, alignment);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
        }

        List<String> lines = getLines();
        double y = this.y + border.get();

        if (lines.isEmpty()) {
            String placeholder = "Stats";
            renderer.text(placeholder, x + border.get() + alignX(renderer.textWidth(placeholder, shadow.get(), getScale()), alignment.get()), y, textColor.get(), shadow.get(), getScale());
            return;
        }

        boolean first = true;
        for (String line : lines) {
            if (!first) y += renderer.textHeight(shadow.get(), getScale()) + 2;
            first = false;

            double x = this.x + border.get() + alignX(renderer.textWidth(line, shadow.get(), getScale()), alignment.get());
            renderer.text(line, x, y, textColor.get(), shadow.get(), getScale());
        }
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }
}
