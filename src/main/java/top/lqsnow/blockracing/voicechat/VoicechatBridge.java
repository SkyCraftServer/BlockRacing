package top.lqsnow.blockracing.voicechat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Bridge class that isolates voicechat API references from callers.
 * <p>
 * This class does NOT import any {@code de.maxhenkel.voicechat} types in its
 * field/method signatures, so it can always be safely loaded regardless of
 * whether Simple Voice Chat is installed. References to {@link VoicechatSyncManager}
 * appear only inside method bodies and are resolved lazily by the JVM.
 */
public final class VoicechatBridge {

    private static Boolean available = null;

    private VoicechatBridge() {
    }

    /**
     * Returns whether the Simple Voice Chat plugin is present on the server.
     */
    public static boolean isAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().getPlugin("voicechat") != null;
        }
        return available;
    }

    /**
     * Safely delegates to {@link VoicechatSyncManager#syncAllPlayers()}.
     * No-op if Simple Voice Chat is not installed.
     */
    public static void syncAllPlayers() {
        if (!isAvailable()) return;
        VoicechatSyncManager.syncAllPlayers();
    }

    /**
     * Safely delegates to {@link VoicechatSyncManager#syncPlayer(Player)}.
     * No-op if Simple Voice Chat is not installed.
     */
    public static void syncPlayer(Player player) {
        if (!isAvailable()) return;
        VoicechatSyncManager.syncPlayer(player);
    }
}
