package top.lqsnow.blockracing.voicechat;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Team;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockRacingVoicechatPlugin implements VoicechatPlugin {

    private static final String RED_GROUP_NAME = "BlockRacing Red Team";
    private static final String BLUE_GROUP_NAME = "BlockRacing Blue Team";

    private static VoicechatServerApi serverApi;
    private static Group redTeamGroup;
    private static Group blueTeamGroup;
    private static final ConcurrentHashMap<UUID, UUID> pendingGroupAssignments = new ConcurrentHashMap<>();

    public static void register(Main plugin) {
        BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            plugin.getLogger().warning("Simple Voice Chat service was not found. Voice group syncing is disabled.");
            return;
        }
        service.registerPlugin(new BlockRacingVoicechatPlugin());
    }

    public static void syncAllPlayers() {
        if (serverApi == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncPlayer(player);
        }
    }

    public static void syncPlayer(Player player) {
        if (serverApi == null || player == null) {
            return;
        }

        VoicechatConnection connection = serverApi.getConnectionOf(player.getUniqueId());

        UUID targetGroupId = null;
        if (Game.getCurrentGameState() == Game.GameState.INGAME) {
            if (Team.redTeamPlayers.contains(player.getName())) {
                ensureGroups();
                targetGroupId = redTeamGroup == null ? null : redTeamGroup.getId();
            } else if (Team.blueTeamPlayers.contains(player.getName())) {
                ensureGroups();
                targetGroupId = blueTeamGroup == null ? null : blueTeamGroup.getId();
            }
        }

        if (connection == null || !connection.isConnected()) {
            if (targetGroupId != null) {
                pendingGroupAssignments.put(player.getUniqueId(), targetGroupId);
            } else {
                pendingGroupAssignments.remove(player.getUniqueId());
            }
            return;
        }

        if (Game.getCurrentGameState() != Game.GameState.INGAME) {
            connection.setGroup(null);
            pendingGroupAssignments.remove(player.getUniqueId());
            return;
        }

        pendingGroupAssignments.remove(player.getUniqueId());
        if (targetGroupId != null) {
            Group targetGroup = serverApi.getGroup(targetGroupId);
            if (targetGroup != null) {
                connection.setGroup(targetGroup);
                return;
            }
        }

        connection.setGroup(null);
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
        serverApi = event.getVoicechat();
        ensureGroups();
        syncAllPlayers();
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        serverApi = null;
        redTeamGroup = null;
        blueTeamGroup = null;
        pendingGroupAssignments.clear();
    }

    private void onPlayerConnected(PlayerConnectedEvent event) {
        if (serverApi == null) {
            return;
        }

        VoicechatConnection connection = event.getConnection();
        UUID groupId = pendingGroupAssignments.remove(connection.getPlayer().getUuid());
        if (groupId == null) {
            return;
        }

        Group group = serverApi.getGroup(groupId);
        if (group != null) {
            connection.setGroup(group);
        }
    }

    private static void ensureGroups() {
        if (serverApi == null) {
            return;
        }
        if (redTeamGroup == null) {
            redTeamGroup = findOrCreateGroup(RED_GROUP_NAME);
        }
        if (blueTeamGroup == null) {
            blueTeamGroup = findOrCreateGroup(BLUE_GROUP_NAME);
        }
    }

    private static Group findOrCreateGroup(String groupName) {
        Group existingGroup = serverApi.getGroups().stream()
                .filter(group -> groupName.equals(group.getName()))
                .findFirst()
                .orElse(null);
        if (existingGroup != null) {
            return existingGroup;
        }

        return serverApi.groupBuilder()
                .setPersistent(true)
                .setName(groupName)
                .setType(Group.Type.ISOLATED)
                .build();
    }
}