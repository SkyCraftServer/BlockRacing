package top.lqsnow.blockracing.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import top.lqsnow.blockracing.managers.Config;
import top.lqsnow.blockracing.managers.Setting;

public class AddonMonitorListener implements Listener {
    private static final String ADDON_NAME = "BlockRacingAddon";

    @EventHandler
    public void onAddonEnable(PluginEnableEvent event) {
        if (!ADDON_NAME.equalsIgnoreCase(event.getPlugin().getName())) return;
        if (Setting.refreshAddonAvailability()) {
            Setting.setEnableAddonBlock(Config.ADDON_BLOCK.getBoolean());
        }
    }

    @EventHandler
    public void onAddonDisable(PluginDisableEvent event) {
        if (!ADDON_NAME.equalsIgnoreCase(event.getPlugin().getName())) return;
        Setting.refreshAddonAvailability();
        Setting.setEnableAddonBlock(false);
    }
}
