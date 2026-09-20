package gamerguy11.sixtoolsaddon.printer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LitematicaBridge {
    private static final String SCHEMATIC_WORLD_HANDLER_CLASS = "fi.dy.masa.litematica.world.SchematicWorldHandler";
    private static final String GET_SCHEMATIC_WORLD_METHOD = "getSchematicWorld";

    private static final String DATA_MANAGER_CLASS = "fi.dy.masa.litematica.data.DataManager";
    private static final String GET_PLACEMENT_MANAGER_METHOD = "getSchematicPlacementManager";
    private static final String GET_ALL_PLACEMENTS_METHOD = "getAllSchematicsPlacements";

    private static final String SUB_REGION_PLACEMENT_CLASS = "fi.dy.masa.litematica.schematic.placement.SubRegionPlacement";
    private static final String REQUIRED_ENABLED_ENUM_CLASS = SUB_REGION_PLACEMENT_CLASS + "$RequiredEnabled";
    private static final String REQUIRED_ENABLED_CONSTANT = "RENDERING_ENABLED";

    private static final String MATCHES_REQUIREMENT_METHOD = "matchesRequirement";
    private static final String GET_SUB_REGION_BOXES_METHOD = "getSubRegionBoxes";

    private static final String GET_POS_1_METHOD = "getPos1";
    private static final String GET_POS_2_METHOD = "getPos2";

    private static Boolean available;
    private static Method getSchematicWorldMethod;

    private static Boolean placementLookupAvailable;
    private static Method getPlacementManagerMethod;
    private static Method getAllPlacementsMethod;
    private static Method matchesRequirementMethod;
    private static Method getSubRegionBoxesMethod;
    private static Method getPos1Method;
    private static Method getPos2Method;
    private static Object renderingEnabledConstant;
    private static String placementLookupFailureReason;

    private static int[][] cachedBoxes = new int[0][];
    private static boolean lastRefreshFailed;

    private static volatile boolean lastCallFailed;

    private LitematicaBridge() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            try {
                Class<?> handler = Class.forName(SCHEMATIC_WORLD_HANDLER_CLASS);
                getSchematicWorldMethod = handler.getMethod(GET_SCHEMATIC_WORLD_METHOD);
                available = true;
            } catch (ReflectiveOperationException | LinkageError e) {
                available = false;
            }
        }

        return available;
    }

    public static World getSchematicWorld() {
        if (!isAvailable()) return null;

        try {
            Object result = getSchematicWorldMethod.invoke(null);
            return result instanceof World world ? world : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static String getPlacementLookupFailureReason() {
        return placementLookupFailureReason;
    }

    public static boolean lastCallFailed() {
        return lastCallFailed;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean placementLookupAvailable() {
        if (placementLookupAvailable == null) {
            try {
                Class<?> dataManagerClass = Class.forName(DATA_MANAGER_CLASS);
                getPlacementManagerMethod = dataManagerClass.getMethod(GET_PLACEMENT_MANAGER_METHOD);

                Object placementManager = getPlacementManagerMethod.invoke(null);
                if (placementManager == null) {
                    placementLookupFailureReason = "getSchematicPlacementManager() returned null";
                    placementLookupAvailable = false;
                    return false;
                }

                getAllPlacementsMethod = placementManager.getClass().getMethod(GET_ALL_PLACEMENTS_METHOD);

                Class<?> requiredEnabledEnum = Class.forName(REQUIRED_ENABLED_ENUM_CLASS);
                renderingEnabledConstant = Enum.valueOf((Class<? extends Enum>) requiredEnabledEnum, REQUIRED_ENABLED_CONSTANT);

            } catch (ReflectiveOperationException | LinkageError e) {
                placementLookupFailureReason = e.getClass().getSimpleName() + ": " + e.getMessage();
                placementLookupAvailable = false;
                return false;
            }

            placementLookupAvailable = true;
        }

        return placementLookupAvailable;
    }

    public static void refreshPlacements() {
        lastRefreshFailed = false;
        if (!placementLookupAvailable()) {
            cachedBoxes = new int[0][];
            return;
        }

        try {
            Object placementManager = getPlacementManagerMethod.invoke(null);
            if (placementManager == null) {
                cachedBoxes = new int[0][];
                return;
            }

            Object all = getAllPlacementsMethod.invoke(placementManager);
            if (!(all instanceof Iterable<?> iterable)) {
                cachedBoxes = new int[0][];
                return;
            }

            List<int[]> boxes = new ArrayList<>();

            for (Object placement : iterable) {
                if (placement == null) continue;

                resolvePlacementAccessorsIfNeeded(placement.getClass());

                Object enabledObj = matchesRequirementMethod.invoke(placement, renderingEnabledConstant);
                if (!(enabledObj instanceof Boolean enabled) || !enabled) continue;

                Object boxesMapObj = getSubRegionBoxesMethod.invoke(placement, renderingEnabledConstant);
                if (!(boxesMapObj instanceof Map<?, ?> boxesMap)) continue;

                for (Object box : boxesMap.values()) {
                    if (box == null) continue;
                    resolveBoxAccessorsIfNeeded(box.getClass());

                    Object pos1Obj = getPos1Method.invoke(box);
                    Object pos2Obj = getPos2Method.invoke(box);
                    if (!(pos1Obj instanceof BlockPos pos1) || !(pos2Obj instanceof BlockPos pos2)) continue;

                    int minX = Math.min(pos1.getX(), pos2.getX());
                    int minY = Math.min(pos1.getY(), pos2.getY());
                    int minZ = Math.min(pos1.getZ(), pos2.getZ());
                    int maxX = Math.max(pos1.getX(), pos2.getX());
                    int maxY = Math.max(pos1.getY(), pos2.getY());
                    int maxZ = Math.max(pos1.getZ(), pos2.getZ());

                    boxes.add(new int[]{minX, minY, minZ, maxX, maxY, maxZ});
                }
            }

            cachedBoxes = boxes.toArray(new int[0][]);
        } catch (ReflectiveOperationException | RuntimeException e) {
            placementLookupFailureReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            lastRefreshFailed = true;
            cachedBoxes = new int[0][];
        }
    }

    private static void resolvePlacementAccessorsIfNeeded(Class<?> placementClass) throws NoSuchMethodException {
        if (matchesRequirementMethod != null && matchesRequirementMethod.getDeclaringClass().isAssignableFrom(placementClass)) {
            return;
        }

        Class<?> requiredEnabledEnum = renderingEnabledConstant.getClass();
        matchesRequirementMethod = placementClass.getMethod(MATCHES_REQUIREMENT_METHOD, requiredEnabledEnum);
        getSubRegionBoxesMethod = placementClass.getMethod(GET_SUB_REGION_BOXES_METHOD, requiredEnabledEnum);
    }

    private static void resolveBoxAccessorsIfNeeded(Class<?> boxClass) throws NoSuchMethodException {
        if (getPos1Method != null && getPos1Method.getDeclaringClass().isAssignableFrom(boxClass)) return;

        getPos1Method = boxClass.getMethod(GET_POS_1_METHOD);
        getPos2Method = boxClass.getMethod(GET_POS_2_METHOD);
    }

    public static boolean isInSchematic(BlockPos pos) {
        lastCallFailed = lastRefreshFailed;
        if (lastRefreshFailed) return false;

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (int[] box : cachedBoxes) {
            if (x >= box[0] && x <= box[3]
                && y >= box[1] && y <= box[4]
                && z >= box[2] && z <= box[5]) {
                return true;
            }
        }
        return false;
    }
}
