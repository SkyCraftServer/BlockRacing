package top.lqsnow.blockracing.scoreboard;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class BlockRacingPlaceholderExpansion extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return "blockracing";
    }

    @Override
    public String getAuthor() {
        return "LQ_Snow";
    }

    @Override
    public String getVersion() {
        return "3.6";
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
        return ScoreboardFoliaIntegration.resolvePlaceholder(params);
    }
}