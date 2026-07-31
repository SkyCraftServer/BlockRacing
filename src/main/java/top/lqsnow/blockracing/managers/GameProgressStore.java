package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import top.lqsnow.blockracing.Main;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static top.lqsnow.blockracing.managers.Block.*;
import static top.lqsnow.blockracing.managers.Game.*;
import static top.lqsnow.blockracing.managers.Team.blueTeamPlayers;
import static top.lqsnow.blockracing.managers.Team.redTeamPlayers;

public final class GameProgressStore {
    private static final int FORMAT_VERSION = 1;
    private static File file;
    private static boolean recoveredGame;

    private GameProgressStore() {
    }

    public static void startAutosave() {
        Main.getFoliaLib().getScheduler().runTimer(() -> {
            if (Game.getCurrentGameState() == Game.GameState.INGAME) {
                saveNow();
            }
        }, 100L, 100L);
    }

    public static boolean load() {
        file = new File(Main.getInstance().getDataFolder(), "game-progress.yml");
        if (!file.isFile()) {
            return false;
        }
        YamlConfiguration state = YamlConfiguration.loadConfiguration(file);
        if (!Game.GameState.INGAME.name().equals(state.getString("state"))) {
            clear();
            return false;
        }
        try {
            Game.currentGameState = Game.GameState.INGAME;
            Game.redTeamScore = state.getInt("scores.red");
            Game.blueTeamScore = state.getInt("scores.blue");
            Game.redTeamCurrentBlockAmount = state.getInt("progress.red-current");
            Game.blueTeamCurrentBlockAmount = state.getInt("progress.blue-current");
            Game.redTeamTotalBlockAmount = state.getInt("progress.red-total");
            Game.blueTeamTotalBlockAmount = state.getInt("progress.blue-total");
            Game.redTeamRollCount = state.getInt("rolls.red");
            Game.blueTeamRollCount = state.getInt("rolls.blue");
            Game.contestModeRollCount = state.getInt("rolls.contest", 0);
            Game.locateCost = state.getInt("locate-cost");
            // Restore time-mode countdown state (TIME mode); resumeRecoveredGame restarts the timer.
            Game.restoreRecoveredTimeMode(
                    state.getInt("time-mode.duration-seconds", 0),
                    state.getInt("time-mode.remaining-seconds", 0),
                    state.getBoolean("time-mode.overtime", false));
            Setting.setSpeedMode(state.getBoolean("settings.speed-mode", Setting.isSpeedMode()));
            try {
                Setting.setCurrentGameMode(Setting.GameMode.valueOf(
                        state.getString("settings.game-mode", Setting.getCurrentGameMode().name())));
            } catch (IllegalArgumentException ignored) {
                // Keep the configured mode if an older/corrupt state contains an unknown value.
            }

            redTeamBlocks = new ArrayList<>(state.getStringList("blocks.red-all"));
            blueTeamBlocks = new ArrayList<>(state.getStringList("blocks.blue-all"));
            redTeamRemainingBlocks = new ArrayList<>(state.getStringList("blocks.red-remaining"));
            blueTeamRemainingBlocks = new ArrayList<>(state.getStringList("blocks.blue-remaining"));

            Team.restoreTeams(state.getStringList("teams.red"), state.getStringList("teams.blue"));
            replace(Game.inGamePlayers, state.getStringList("players.in-game"));
            replace(Game.freeRandomTPList, state.getStringList("players.free-random-tp"));
            replace(Game.locateCommandPermission, state.getStringList("players.locate-permission"));
            Game.collectAmount = readIntegerMap(state.getConfigurationSection("collected"));

            Game.redWaypoint = readLocations(state.getConfigurationSection("waypoints.red"));
            Game.blueWaypoint = readLocations(state.getConfigurationSection("waypoints.blue"));
            // Restore the biome/icon caches captured when each waypoint was recorded, so the menu
            // never has to read world biome data again (Folia thread-restricted world access).
            Game.redWaypointBiomeCache.clear();
            Game.blueWaypointBiomeCache.clear();
            Game.redWaypointIconCache.clear();
            Game.blueWaypointIconCache.clear();
            readStringMap(state.getConfigurationSection("waypoint-biomes.red")).forEach(Game.redWaypointBiomeCache::put);
            readStringMap(state.getConfigurationSection("waypoint-biomes.blue")).forEach(Game.blueWaypointBiomeCache::put);
            readMaterialMap(state.getConfigurationSection("waypoint-icons.red")).forEach(Game.redWaypointIconCache::put);
            readMaterialMap(state.getConfigurationSection("waypoint-icons.blue")).forEach(Game.blueWaypointIconCache::put);
            restoreChests(state, "chests.red", Game.redTeamChest);
            restoreChests(state, "chests.blue", Game.blueTeamChest);
            recoveredGame = true;
            Main.getInstance().getLogger().info("Recovered unfinished BlockRacing game progress.");
            return true;
        } catch (RuntimeException ex) {
            Main.getInstance().getLogger().log(Level.SEVERE,
                    "Unable to restore game-progress.yml; starting in pregame instead.", ex);
            Game.currentGameState = Game.GameState.PREGAME;
            return false;
        }
    }

    public static synchronized void saveNow() {
        if (file == null) {
            file = new File(Main.getInstance().getDataFolder(), "game-progress.yml");
        }
        YamlConfiguration state = snapshot();
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), state.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary.toPath(), file.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Unable to save game progress.", ex);
        }
    }

    public static boolean hasRecoveredGame() {
        return recoveredGame;
    }

    public static void clear() {
        recoveredGame = false;
        if (file == null) {
            file = new File(Main.getInstance().getDataFolder(), "game-progress.yml");
        }
        try {
            Files.deleteIfExists(file.toPath());
            Files.deleteIfExists(new File(file.getParentFile(), file.getName() + ".tmp").toPath());
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.WARNING, "Unable to remove old game progress.", ex);
        }
    }

    private static YamlConfiguration snapshot() {
        YamlConfiguration state = new YamlConfiguration();
        state.set("format-version", FORMAT_VERSION);
        state.set("state", Game.getCurrentGameState().name());
        state.set("scores.red", Game.redTeamScore);
        state.set("scores.blue", Game.blueTeamScore);
        state.set("progress.red-current", Game.redTeamCurrentBlockAmount);
        state.set("progress.blue-current", Game.blueTeamCurrentBlockAmount);
        state.set("progress.red-total", Game.redTeamTotalBlockAmount);
        state.set("progress.blue-total", Game.blueTeamTotalBlockAmount);
        state.set("rolls.red", Game.redTeamRollCount);
        state.set("rolls.blue", Game.blueTeamRollCount);
        state.set("rolls.contest", Game.contestModeRollCount);
        state.set("locate-cost", Game.locateCost);
        // Time-mode countdown state (so a recovered TIME game resumes its timer).
        state.set("time-mode.duration-seconds", Game.getTimeModeDurationSeconds());
        state.set("time-mode.remaining-seconds", Game.getTimeModeRemainingSeconds());
        state.set("time-mode.overtime", Game.isTimeModeOvertime());
        state.set("settings.speed-mode", Setting.isSpeedMode());
        state.set("settings.game-mode", Setting.getCurrentGameMode().name());
        state.set("blocks.red-all", snapshotStrings(redTeamBlocks));
        state.set("blocks.blue-all", snapshotStrings(blueTeamBlocks));
        state.set("blocks.red-remaining", snapshotStrings(redTeamRemainingBlocks));
        state.set("blocks.blue-remaining", snapshotStrings(blueTeamRemainingBlocks));
        state.set("teams.red", new ArrayList<>(redTeamPlayers));
        state.set("teams.blue", new ArrayList<>(blueTeamPlayers));
        state.set("players.in-game", new ArrayList<>(Game.inGamePlayers));
        state.set("players.free-random-tp", new ArrayList<>(Game.freeRandomTPList));
        state.set("players.locate-permission", new ArrayList<>(Game.locateCommandPermission));
        Game.collectAmount.forEach((name, amount) -> state.set("collected." + name, amount));
        Game.redWaypoint.forEach((index, location) -> state.set("waypoints.red." + index, location));
        Game.blueWaypoint.forEach((index, location) -> state.set("waypoints.blue." + index, location));
        // Persist biome/icon caches captured when each waypoint was recorded.
        Game.redWaypointBiomeCache.forEach((index, biome) -> state.set("waypoint-biomes.red." + index, biome));
        Game.blueWaypointBiomeCache.forEach((index, biome) -> state.set("waypoint-biomes.blue." + index, biome));
        Game.redWaypointIconCache.forEach((index, icon) -> state.set("waypoint-icons.red." + index, icon.name()));
        Game.blueWaypointIconCache.forEach((index, icon) -> state.set("waypoint-icons.blue." + index, icon.name()));
        saveChests(state, "chests.red", Game.redTeamChest);
        saveChests(state, "chests.blue", Game.blueTeamChest);
        return state;
    }

    private static void saveChests(YamlConfiguration state, String path, List<Inventory> chests) {
        for (int index = 0; index < chests.size(); index++) {
            state.set(path + "." + index, Arrays.asList(chests.get(index).getContents()));
        }
    }

    private static void restoreChests(YamlConfiguration state, String path, List<Inventory> chests) {
        for (int index = 0; index < chests.size(); index++) {
            List<?> values = state.getList(path + "." + index, List.of());
            ItemStack[] contents = new ItemStack[chests.get(index).getSize()];
            for (int slot = 0; slot < values.size() && slot < contents.length; slot++) {
                if (values.get(slot) instanceof ItemStack item) {
                    contents[slot] = item;
                }
            }
            chests.get(index).setContents(contents);
        }
    }

    private static HashMap<Integer, Location> readLocations(ConfigurationSection section) {
        HashMap<Integer, Location> locations = new HashMap<>();
        if (section == null) {
            return locations;
        }
        for (String key : section.getKeys(false)) {
            Location location = section.getLocation(key);
            if (location != null) {
                locations.put(Integer.parseInt(key), location);
            }
        }
        return locations;
    }

    private static Map<Integer, String> readStringMap(ConfigurationSection section) {
        Map<Integer, String> values = new HashMap<>();
        if (section == null) {
            return values;
        }
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null) {
                values.put(Integer.parseInt(key), value);
            }
        }
        return values;
    }

    private static Map<Integer, Material> readMaterialMap(ConfigurationSection section) {
        Map<Integer, Material> values = new HashMap<>();
        if (section == null) {
            return values;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(section.getString(key, ""));
            if (material != null) {
                values.put(Integer.parseInt(key), material);
            }
        }
        return values;
    }

    private static Map<String, Integer> readIntegerMap(ConfigurationSection section) {
        Map<String, Integer> values = new HashMap<>();
        if (section != null) {
            section.getKeys(false).forEach(key -> values.put(key, section.getInt(key)));
        }
        return values;
    }

    private static void replace(List<String> target, List<String> values) {
        target.clear();
        target.addAll(values);
    }

    /**
     * Thread-safe snapshot of a list that the game thread may be mutating concurrently.
     * On Folia the autosave runs on the global scheduler while region threads modify the
     * block pools; indexed access avoids ConcurrentModificationException and the size
     * re-check avoids IndexOutOfBoundsException if the list shrinks mid-copy.
     */
    private static List<String> snapshotStrings(List<String> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        int count = source.size();
        List<String> copy = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (i < source.size()) {
                copy.add(source.get(i));
            }
        }
        return copy;
    }
}
