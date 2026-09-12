package gamerguy11.anarchyaddon.modules.utility;

import gamerguy11.anarchyaddon.AnarchyAddon;
import gamerguy11.anarchyaddon.anarchymod.Domains;
import meteordevelopment.meteorclient.systems.modules.Module;

public class UnblockServers extends Module {
    public UnblockServers() {
        super(AnarchyAddon.CATEGORY, "unblock-servers", "Prevents known anarchy servers from being flagged as blocked by Mojang's server blocklist.");
    }

    @Override
    public void onActivate() {
        Domains.initialize();
    }

    public boolean isDomainAllowed(String server) {
        return isActive() && Domains.contains(server);
    }
}
