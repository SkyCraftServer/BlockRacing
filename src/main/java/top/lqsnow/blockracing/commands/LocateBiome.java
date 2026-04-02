package top.lqsnow.blockracing.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.utils.BiomeTranslation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocateBiome implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Message.NOTICE_LOCATE_BIOME_USAGE.getString());
            return true;
        }

        if (!Game.canAffordLocate(player)) {
            return true;
        }

        Biome biome = resolveBiomeInput(args[0]);
        if (biome == null) {
            player.sendMessage(Message.NOTICE_LOCATE_BIOME_INVALID.getString());
            return true;
        }

        World world = player.getWorld();
        Location source = player.getLocation();
        org.bukkit.util.BiomeSearchResult result = world.locateNearestBiome(source, 6400, biome);
        if (result == null) {
            player.sendMessage(Message.NOTICE_LOCATE_BIOME_NOT_FOUND.getString());
            return true;
        }

        if (!Game.chargeLocateCost(player)) {
            return true;
        }

        Location target = result.getLocation();
        int distance = (int) Math.round(source.distance(target));
        player.sendMessage(Message.NOTICE_LOCATE_BIOME_SUCCESS.getString()
            .replace("%biome%", BiomeTranslation.getValue(biome))
            .replace("%x%", String.valueOf(target.getBlockX()))
            .replace("%y%", String.valueOf(target.getBlockY()))
            .replace("%z%", String.valueOf(target.getBlockZ()))
            .replace("%distance%", String.valueOf(distance)));

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        List<String> candidates;
        if (isChineseLanguageConfig()) {
            candidates = new ArrayList<>();
            for (Biome biome : Biome.values()) {
                String name = BiomeTranslation.getValue(biome);
                if (name != null && !name.isBlank()) {
                    candidates.add(name);
                }
            }
        } else {
            candidates = Arrays.stream(Biome.values())
                    .map(b -> b.name().toLowerCase(Locale.ROOT))
                    .toList();
        }

        if (strings.length == 0) {
            return candidates;
        }

        String prefix = strings[strings.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private Biome resolveBiomeInput(String input) {
        String normalizedInput = input == null ? "" : input.trim();
        try {
            return Biome.valueOf(normalizedInput.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            // Continue and try localized input in Chinese mode.
        }

        if (!isChineseLanguageConfig()) {
            return null;
        }

        for (Biome biome : Biome.values()) {
            String localized = BiomeTranslation.getValue(biome);
            if (localized != null && localized.equalsIgnoreCase(normalizedInput)) {
                return biome;
            }
        }
        return null;
    }

    private boolean isChineseLanguageConfig() {
        String languageCode = Message.getLanguageCode();
        return languageCode != null && languageCode.toLowerCase(Locale.ROOT).startsWith("zh");
    }
}
