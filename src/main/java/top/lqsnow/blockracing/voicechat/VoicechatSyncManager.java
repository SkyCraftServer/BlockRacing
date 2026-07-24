package top.lqsnow.blockracing.voicechat;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Team;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds voice chat group state and sync methods.
 * Must NOT implement VoicechatPlugin or import any voicechat event classes
 * so that this class can be loaded without Simple Voice Chat on the classpath.
 */
public final class VoicechatSyncManager {

    static final String RED_GROUP_NAME = "BlockRacing Red Team";
    static final String BLUE_GROUP_NAME = "BlockRacing Blue Team";

    static VoicechatServerApi serverApi;
    static Group redTeamGroup;
    static Group blueTeamGroup;
    static final ConcurrentHashMap<UUID, UUID> pendingGroupAssignments = new ConcurrentHashMap<>();

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

    static void ensureGroups() {
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

    static void reset() {
        serverApi = null;
        redTeamGroup = null;
        blueTeamGroup = null;
        pendingGroupAssignments.clear();
    }
}