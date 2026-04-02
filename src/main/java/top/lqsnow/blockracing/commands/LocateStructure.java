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
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.utils.ColorUtil;

import java.util.Arrays;
import java.util.List;

import static top.lqsnow.blockracing.managers.Game.locateCommandPermission;

public class LocateStructure implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString());
            return true;
        }

        if (!locateCommandPermission.contains(player.getName())) {
            player.sendMessage(Message.NOTICE_LOCATE_NO_PERMISSION.getString());
            return true;
        }

        Structure structure;
        try {
            java.lang.reflect.Field field = Structure.class.getField(args[0].toUpperCase());
            structure = (Structure) field.get(null);
        } catch (Exception ex) {
            player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString());
            return true;
        }

        World world = player.getWorld();
        Location source = player.getLocation();
        org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(source, structure, 100, false);
        if (result == null) {
            player.sendMessage(ColorUtil.t("&c未找到该结构，未扣除积分。"));
            return true;
        }

        if (!Game.consumeLocatePermissionWithCharge(player)) {
            return true;
        }

        Location target = result.getLocation();
        player.sendMessage(ColorUtil.t("&a已定位到结构 &e" + args[0].toLowerCase()
                + " &a坐标：&b" + target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ()));

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return Arrays.asList("ancient_city", "buried_treasure", "end_city", "fortress", "mansion", "mineshaft", "mineshaft_mesa", "monument", "ocean_ruin_cold", "ocean_ruin_warm", "shipwreck", "shipwreck_beached", "stronghold", "desert_pyramid", "igloo", "jungle_pyramid", "swamp_hut", "village_desert", "village_plains", "village_savanna", "village_snowy", "village_taiga", "pillager_outpost", "nether_fossil", "bastion_remnant", "ruined_portal", "ruined_portal_desert", "ruined_portal_jungle", "ruined_portal_mountain", "ruined_portal_ocean", "ruined_portal_swamp", "ruined_portal_nether", "trial_chambers");
    }
}
