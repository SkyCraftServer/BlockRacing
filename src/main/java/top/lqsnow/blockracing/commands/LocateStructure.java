package top.lqsnow.blockracing.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;

import java.util.Arrays;
import java.util.List;

import static top.lqsnow.blockracing.managers.Team.blueTeamPlayers;
import static top.lqsnow.blockracing.managers.Team.redTeamPlayers;

public class LocateStructure implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_USAGE.getString());
            return true;
        }

        if (!Game.canAffordLocate(player)) {
            return true;
        }

        Structure structure;
        try {
            java.lang.reflect.Field field = Structure.class.getField(args[0].toUpperCase());
            structure = (Structure) field.get(null);
        } catch (Exception ex) {
            player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_INVALID.getString());
            return true;
        }

        World world = player.getWorld();
        Location source = player.getLocation();
        org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(source, structure, 100, false);
        if (result == null) {
            player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_NOT_FOUND.getString());
            return true;
        }

        if (!Game.chargeLocateCost(player)) {
            return true;
        }

        Location target = result.getLocation();
        int distance = (int) Math.round(source.distance(target));
        player.sendMessage(Message.NOTICE_LOCATE_STRUCTURE_SUCCESS.getString()
            .replace("%structure%", args[0].toLowerCase())
            .replace("%x%", String.valueOf(target.getBlockX()))
            .replace("%y%", String.valueOf(target.getBlockY()))
            .replace("%z%", String.valueOf(target.getBlockZ()))
            .replace("%distance%", String.valueOf(distance)));

        List<String> teammates = redTeamPlayers.contains(player.getName()) ? redTeamPlayers
                : (blueTeamPlayers.contains(player.getName()) ? blueTeamPlayers : List.of());

        for (String teammateName : teammates) {
            if (teammateName.equals(player.getName())) {
                continue;
            }
            Player teammate = Bukkit.getPlayer(teammateName);
            if (teammate == null || !teammate.isOnline()) {
                continue;
            }

            int teammateDistance = teammate.getWorld().equals(target.getWorld())
                    ? (int) Math.round(teammate.getLocation().distance(target))
                    : -1;
            String distanceText = teammateDistance >= 0 ? String.valueOf(teammateDistance) : "?";

            sendMessageFoliaSafe(teammate, Message.NOTICE_LOCATE_STRUCTURE_TEAM_SUCCESS.getString()
                    .replace("%player%", player.getName())
                    .replace("%structure%", args[0].toLowerCase())
                    .replace("%x%", String.valueOf(target.getBlockX()))
                    .replace("%y%", String.valueOf(target.getBlockY()))
                    .replace("%z%", String.valueOf(target.getBlockZ()))
                    .replace("%distance%", distanceText));
        }

        return true;
    }

    private static void sendMessageFoliaSafe(Player player, String message) {
        if (player == null) {
            return;
        }
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> player.sendMessage(message));
            return;
        }
        player.sendMessage(message);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Arrays.asList("ancient_city", "buried_treasure", "end_city", "fortress", "mansion", "mineshaft", "mineshaft_mesa", "monument", "ocean_ruin_cold", "ocean_ruin_warm", "shipwreck", "shipwreck_beached", "stronghold", "desert_pyramid", "igloo", "jungle_pyramid", "swamp_hut", "village_desert", "village_plains", "village_savanna", "village_snowy", "village_taiga", "pillager_outpost", "nether_fossil", "bastion_remnant", "ruined_portal", "ruined_portal_desert", "ruined_portal_jungle", "ruined_portal_mountain", "ruined_portal_ocean", "ruined_portal_swamp", "ruined_portal_nether", "trial_chambers");
    }
}
