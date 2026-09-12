package gamerguy11.anarchyaddon;

import com.mojang.logging.LogUtils;
import gamerguy11.anarchyaddon.commands.DubCounterCommand;
import gamerguy11.anarchyaddon.commands.InventoryCommand;
import gamerguy11.anarchyaddon.hud.DubCounterHud;
import gamerguy11.anarchyaddon.hud.PlayerTrackerHud;
import gamerguy11.anarchyaddon.hud.StatsHud;
import gamerguy11.anarchyaddon.modules.Efly;
import gamerguy11.anarchyaddon.modules.Ez;
import gamerguy11.anarchyaddon.modules.InventorySorterModule;
import gamerguy11.anarchyaddon.modules.utility.AntiDrop;
import gamerguy11.anarchyaddon.modules.utility.AutoTpAccept;
import gamerguy11.anarchyaddon.modules.utility.ShulkerView;
import gamerguy11.anarchyaddon.modules.utility.UnblockServers;
import gamerguy11.anarchyaddon.modules.utility.WhisperLogger;
import gamerguy11.anarchyaddon.modules.world.Printer;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.slf4j.Logger;

public class AnarchyAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    public static final Category CATEGORY = new Category("AnarchyAddon");

    public static final HudGroup HUD_GROUP = new HudGroup("AnarchyAddon");

    public static final Color THEME_COLOR = new Color(0, 182, 182);

    @Override
    public void onInitialize() {
        LOG.info("Initializing AnarchyAddon");

        Modules.get().add(new Efly());
        Modules.get().add(new Ez());
        Modules.get().add(new InventorySorterModule());
        Modules.get().add(new AntiDrop());
        Modules.get().add(new AutoTpAccept());
        Modules.get().add(new ShulkerView());
        Modules.get().add(new WhisperLogger());
        Modules.get().add(new Printer());
        Modules.get().add(new UnblockServers());

        Commands.add(new InventoryCommand());
        Commands.add(new DubCounterCommand());

        Hud.get().register(PlayerTrackerHud.INFO);
        Hud.get().register(DubCounterHud.INFO);
        Hud.get().register(StatsHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "gamerguy11.anarchyaddon";
    }
}
