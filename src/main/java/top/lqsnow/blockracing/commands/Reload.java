package top.lqsnow.blockracing.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import top.lqsnow.blockracing.managers.*;

import static top.lqsnow.blockracing.utils.CommandUtil.sendAll;

public class Reload implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Reload config and messages
        Config.load();
        Message.load();
        // Reload settings
        Setting.getSettings();
        // Reload block lists
        Block.reloadBlock();

        // Recreate team chests according to new config
        Game.redTeamChest.clear();
        Game.blueTeamChest.clear();
        Game.initChest();

        // Update scoreboard and show for online players
        Scoreboard.updateScoreboard();
        Bukkit.getOnlinePlayers().forEach(Scoreboard::showScoreboard);

        // Validate blocks and notify
        boolean ok = Block.checkBlock();
        if (ok) {
            sendAll(Message.NOTICE_RELOAD_COMPLETE.getString());
        } else {
            sendAll(Message.NOTICE_RELOAD_COMPLETE_ERROR.getString());
        }
        return true;
    }
}
