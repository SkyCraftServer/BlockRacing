package top.lqsnow.blockracing.voicechat;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import top.lqsnow.blockracing.Main;

import java.util.UUID;

public final class BlockRacingVoicechatPlugin implements VoicechatPlugin {

    public static void register(Main plugin) {
        BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            plugin.getLogger().warning("Simple Voice Chat service was not found. Voice group syncing is disabled.");
            return;
        }
        service.registerPlugin(new BlockRacingVoicechatPlugin());
    }

    /**
     * @deprecated Use {@link VoicechatSyncManager#syncAllPlayers()} instead.
     * This method is kept for backward compatibility but delegates to VoicechatSyncManager.
     */
    @Deprecated
    public static void syncAllPlayers() {
        VoicechatSyncManager.syncAllPlayers();
    }

    /**
     * @deprecated Use {@link VoicechatSyncManager#syncPlayer(org.bukkit.entity.Player)} instead.
     */
    @Deprecated
    public static void syncPlayer(org.bukkit.entity.Player player) {
        VoicechatSyncManager.syncPlayer(player);
    }

    @Override
    public String getPluginId() {
        return "blockracing";
    }

    @Override
    public void initialize(VoicechatApi api) {
        // No-op. We only need the server API from the started event.
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
        registration.registerEvent(PlayerConnectedEvent.class, this::onPlayerConnected);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatSyncManager.serverApi = event.getVoicechat();
        VoicechatSyncManager.ensureGroups();
        VoicechatSyncManager.syncAllPlayers();
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        VoicechatSyncManager.reset();
    }

    private void onPlayerConnected(PlayerConnectedEvent event) {
        if (VoicechatSyncManager.serverApi == null) {
            return;
        }

        VoicechatConnection connection = event.getConnection();
        UUID groupId = VoicechatSyncManager.pendingGroupAssignments.remove(connection.getPlayer().getUuid());
        if (groupId == null) {
            return;
        }

        var group = VoicechatSyncManager.serverApi.getGroup(groupId);
        if (group != null) {
            connection.setGroup(group);
        }
    }
}