package top.lqsnow.blockracing.scoreboard;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.Main;

import java.util.Locale;

public class BlockRacingPlaceholderExpansion extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return Main.getInstance().getDescription().getName().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getAuthor() {
        if (Main.getInstance().getDescription().getAuthors().isEmpty()) {
            return "";
        }
        return Main.getInstance().getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return Main.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        Player online = player != null ? player.getPlayer() : null;
        return BlockRacingScoreboardLayout.resolvePlaceholder(params, online);
    }
}