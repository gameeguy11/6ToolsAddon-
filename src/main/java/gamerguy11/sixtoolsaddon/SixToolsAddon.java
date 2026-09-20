package gamerguy11.sixtoolsaddon;

import com.mojang.logging.LogUtils;
import gamerguy11.sixtoolsaddon.commands.DubCounterCommand;
import gamerguy11.sixtoolsaddon.commands.EnemyCommand;
import gamerguy11.sixtoolsaddon.commands.InventoryCommand;
import gamerguy11.sixtoolsaddon.commands.SetDiscordCommand;
import gamerguy11.sixtoolsaddon.gui.EnemiesTab;
import gamerguy11.sixtoolsaddon.hud.DubCounterHud;
import gamerguy11.sixtoolsaddon.hud.PlayerTrackerHud;
import gamerguy11.sixtoolsaddon.hud.StatsHud;
import gamerguy11.sixtoolsaddon.sound.SoundEngine;
import gamerguy11.sixtoolsaddon.modules.Efly;
import gamerguy11.sixtoolsaddon.modules.ForeverForward;
import gamerguy11.sixtoolsaddon.modules.Ez;
import gamerguy11.sixtoolsaddon.modules.InventorySorterModule;
import gamerguy11.sixtoolsaddon.modules.utility.AntiDrop;
import gamerguy11.sixtoolsaddon.modules.utility.AutoTpAccept;
import gamerguy11.sixtoolsaddon.modules.utility.ChatHighlighter;
import gamerguy11.sixtoolsaddon.modules.utility.DiscordNotifier;
import gamerguy11.sixtoolsaddon.modules.utility.Homes;
import gamerguy11.sixtoolsaddon.modules.utility.ShulkerView;
import gamerguy11.sixtoolsaddon.modules.utility.SoundEditor;
import gamerguy11.sixtoolsaddon.modules.utility.WhisperLogger;
import gamerguy11.sixtoolsaddon.modules.visual.Parkinsons;
import gamerguy11.sixtoolsaddon.modules.visual.SwingSpeed;
import gamerguy11.sixtoolsaddon.anarchymod.Domains;
import gamerguy11.sixtoolsaddon.anarchymod.JoinPayload;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.builtin.FriendsTab;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.slf4j.Logger;

import java.util.List;

public class SixToolsAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    public static final Category CATEGORY = new Category("6Tools Addon");

    public static final HudGroup HUD_GROUP = new HudGroup("6Tools Addon");

    public static final Color THEME_COLOR = new Color(0, 182, 182);

    private static final GithubRepo REPO = new GithubRepo("gameeguy11", "6ToolsAddon-");

    private static final String FIRST_COMMIT = "0e646b42cc8ab11c24a48417f14d295d393a09a7";

    @Override
    public GithubRepo getRepo() {
        return REPO;
    }

    @Override
    public String getCommit() {
        return FIRST_COMMIT;
    }

    @Override
    public void onInitialize() {
        LOG.info("Initializing SixToolsAddon");

        SoundEngine.INSTANCE.rescan();

        JoinPayload.register();
        Domains.initialize();

        Modules.get().add(new Efly());
        Modules.get().add(new ForeverForward());
        Modules.get().add(new Ez());
        Modules.get().add(new InventorySorterModule());
        Modules.get().add(new AntiDrop());
        Modules.get().add(new AutoTpAccept());
        Modules.get().add(new Homes());
        Modules.get().add(new DiscordNotifier());
        Modules.get().add(new ChatHighlighter());
        Modules.get().add(new ShulkerView());
        Modules.get().add(new WhisperLogger());
        Modules.get().add(new SwingSpeed());
        Modules.get().add(new Parkinsons());
        Modules.get().add(new SoundEditor());

        Commands.add(new InventoryCommand());
        Commands.add(new DubCounterCommand());
        Commands.add(new SetDiscordCommand());
        Commands.add(new EnemyCommand());

        Tabs.add(new EnemiesTab());
        moveTabAfter(EnemiesTab.class, FriendsTab.class);

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
        return "gamerguy11.sixtoolsaddon";
    }

    private static void moveTabAfter(Class<? extends Tab> tabToMove, Class<? extends Tab> anchor) {
        List<Tab> tabs = Tabs.get();

        Tab moving = Tabs.get(tabToMove);
        if (moving == null) return;

        int anchorIndex = -1;
        for (int i = 0; i < tabs.size(); i++) {
            if (anchor.isInstance(tabs.get(i))) {
                anchorIndex = i;
                break;
            }
        }

        tabs.remove(moving);
        tabs.add(anchorIndex >= 0 ? anchorIndex + 1 : tabs.size(), moving);
    }
}
