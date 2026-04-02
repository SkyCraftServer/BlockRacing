package top.lqsnow.blockracing.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import top.lqsnow.blockracing.managers.Game;

public class ForceStart implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            return true;
        }

        Game.forceStart(sender);
        return true;
    }
}
