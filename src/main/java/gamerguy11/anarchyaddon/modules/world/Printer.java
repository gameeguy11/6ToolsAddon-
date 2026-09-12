package gamerguy11.anarchyaddon.modules.world;

import gamerguy11.anarchyaddon.AnarchyAddon;
import gamerguy11.anarchyaddon.printer.LitematicaBridge;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Printer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgActions = settings.createGroup("Actions");
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Cube radius around you (in blocks) to scan for mismatches against the schematic.")
        .defaultValue(5)
        .range(1, 8)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
        .name("reach")
        .description("Max distance (from your eyes) a block can be to place or break it.")
        .defaultValue(4.5)
        .range(1, 6)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<BuildOrder> buildOrder = sgGeneral.add(new EnumSetting.Builder<BuildOrder>()
        .name("build-order")
        .description("How mismatches are prioritized within reach each tick.")
        .defaultValue(BuildOrder.NEAREST)
        .build()
    );

    private final Setting<Boolean> placeMissing = sgActions.add(new BoolSetting.Builder()
        .name("place-missing")
        .description("Places blocks the schematic wants that aren't there yet.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakMismatched = sgActions.add(new BoolSetting.Builder()
        .name("break-mismatched")
        .description("Breaks blocks that don't match the schematic - including extra blocks where the schematic wants air.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> prioritizeBreaking = sgActions.add(new BoolSetting.Builder()
        .name("prioritize-breaking")
        .description("Within a tick's action budget, clear wrong blocks before placing new ones. Off interleaves by distance instead.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreFluids = sgActions.add(new BoolSetting.Builder()
        .name("ignore-fluids")
        .description("Never breaks a fluid source/flow, even if the schematic wants air there.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgActions.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Visually turns your view to face the block while placing. Off places silently, without moving your camera.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> actionsPerTick = sgSpeed.add(new IntSetting.Builder()
        .name("actions-per-tick")
        .description("How many blocks to place/break in a single tick. 1 = old behaviour, higher = much faster building.")
        .defaultValue(3)
        .range(1, 10)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Integer> delay = sgSpeed.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait after each action-batch before acting again. 0 with actions-per-tick > 1 is the fastest safe combo.")
        .defaultValue(0)
        .range(0, 20)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> searchFullInventory = sgInventory.add(new BoolSetting.Builder()
        .name("search-full-inventory")
        .description("Also look in your main inventory (not just hotbar) and swap materials into the hotbar as needed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notifyMissingItems = sgInventory.add(new BoolSetting.Builder()
        .name("notify-missing-items")
        .description("Sends a chat message (throttled) when a block the schematic needs isn't available.")
        .defaultValue(true)
        .build()
    );

    public enum BuildOrder {
        NEAREST,
        BOTTOM_UP,
        TOP_DOWN
    }

    private record Mismatch(BlockPos pos, BlockState schematicState, BlockState worldState, double distSq) {
    }

    private final List<Mismatch> scratch = new ArrayList<>();

    private int cooldown;
    private int warnCooldown;
    private int diagCooldown;

    public Printer() {
        super(AnarchyAddon.CATEGORY, "printer", "Auto-builds the schematic currently loaded in Litematica.");
    }

    @Override
    public void onActivate() {
        cooldown = 0;
        warnCooldown = 0;
        diagCooldown = 0;

        if (!LitematicaBridge.isAvailable()) {
            error("Litematica is not installed!");
            toggle();
            return;
        }

        if (!LitematicaBridge.placementLookupAvailable()) {
            error("Couldn't reach Litematica's placement manager (%s) - refusing to run to avoid breaking terrain outside the build.", LitematicaBridge.getPlacementLookupFailureReason());
            toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (warnCooldown > 0) warnCooldown--;
        if (diagCooldown > 0) diagCooldown--;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        World schematicWorld = LitematicaBridge.getSchematicWorld();
        if (schematicWorld == null) {
            warnDiag("No schematic world from Litematica - is a schematic actually loaded/selected?");
            return;
        }

        LitematicaBridge.refreshPlacements();

        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get();

        scratch.clear();
        int checked = 0;
        int insideSchematic = 0;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    checked++;

                    if (!LitematicaBridge.isInSchematic(pos)) continue;
                    insideSchematic++;

                    BlockState schematicState = schematicWorld.getBlockState(pos);
                    BlockState worldState = mc.world.getBlockState(pos);
                    if (schematicState.equals(worldState)) continue;

                    scratch.add(new Mismatch(pos, schematicState, worldState, pos.getSquaredDistance(playerPos)));
                }
            }
        }

        if (LitematicaBridge.lastCallFailed()) {
            warnDiag("In-schematic check is failing (%s) - not touching anything until this is fixed.", LitematicaBridge.getPlacementLookupFailureReason());
            return;
        }

        if (insideSchematic == 0) {
            warnDiag("None of the %d scanned blocks are inside an enabled schematic placement - move closer, or check the placement is enabled in Litematica.", checked);
            return;
        }

        if (scratch.isEmpty()) {
            warnDiag("%d of %d scanned blocks are in the schematic, but none differ from what's already placed - nothing to do here.", insideSchematic, checked);
            return;
        }

        sortScratch(playerPos);

        Vec3d eyePos = mc.player.getEyePos();
        double reachSq = reach.get() * reach.get();

        int done = 0;
        int budget = actionsPerTick.get();

        for (Mismatch mismatch : scratch) {
            if (done >= budget) break;
            if (eyePos.squaredDistanceTo(Vec3d.ofCenter(mismatch.pos())) > reachSq) continue;

            if (act(mismatch)) done++;
        }

        if (done > 0) cooldown = delay.get();
    }

    private void sortScratch(BlockPos playerPos) {
        Comparator<Mismatch> byDistance = Comparator.comparingDouble(Mismatch::distSq);

        Comparator<Mismatch> comparator = switch (buildOrder.get()) {
            case NEAREST -> byDistance;
            case BOTTOM_UP -> Comparator.comparingInt((Mismatch m) -> m.pos().getY()).thenComparing(byDistance);
            case TOP_DOWN -> Comparator.comparingInt((Mismatch m) -> -m.pos().getY()).thenComparing(byDistance);
        };

        if (prioritizeBreaking.get()) {

            comparator = Comparator.comparing((Mismatch m) -> !needsBreakFirst(m)).thenComparing(comparator);
        }

        scratch.sort(comparator);
    }

    private boolean needsBreakFirst(Mismatch m) {
        BlockState schematicState = m.schematicState();
        BlockState worldState = m.worldState();

        if (schematicState.isAir()) return !worldState.isAir();
        return !worldState.isAir() && !worldState.isReplaceable();
    }

    private boolean act(Mismatch mismatch) {
        BlockPos pos = mismatch.pos();
        BlockState schematicState = mismatch.schematicState();
        BlockState worldState = mismatch.worldState();

        if (schematicState.isAir()) {
            if (!breakMismatched.get()) return false;
            if (worldState.isAir()) return false;
            if (ignoreFluids.get() && !worldState.getFluidState().isEmpty()) return false;

            return BlockUtils.breakBlock(pos, true);
        }

        if (!worldState.isAir() && !worldState.isReplaceable()) {
            if (!breakMismatched.get()) return false;
            if (ignoreFluids.get() && !worldState.getFluidState().isEmpty()) return false;

            return BlockUtils.breakBlock(pos, true);
        }

        if (!placeMissing.get()) return false;

        Block wanted = schematicState.getBlock();
        Item item = wanted.asItem();
        if (item == Items.AIR) return false;

        FindItemResult found = findItem(item);
        if (!found.found()) {
            warnMissingItem(wanted);
            return false;
        }

        return BlockUtils.place(pos, found, rotate.get(), 4);
    }

    private FindItemResult findItem(Item item) {
        FindItemResult hotbar = InvUtils.findInHotbar(item);
        if (hotbar.found() || !searchFullInventory.get()) return hotbar;

        return InvUtils.find(item);
    }

    private void warnDiag(String message, Object... args) {
        if (diagCooldown > 0) return;
        diagCooldown = 60;
        error(message, args);
    }

    private void warnMissingItem(Block wanted) {
        if (!notifyMissingItems.get() || warnCooldown > 0) return;

        warnCooldown = 60;
        error("Need (highlight)%s(default) to keep printing.", wanted.getName().getString());
    }
}
