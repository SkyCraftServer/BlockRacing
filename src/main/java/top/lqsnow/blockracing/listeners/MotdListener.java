package top.lqsnow.blockracing.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import top.lqsnow.blockracing.managers.Motd;

public class MotdListener implements Listener {

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        event.setMotd(Motd.buildMotd());
    }
}