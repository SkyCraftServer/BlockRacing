package top.lqsnow.blockracing.managers;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
    
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.voicechat.BlockRacingVoicechatPlugin;
import top.lqsnow.blockracing.utils.ColorUtil;
import top.lqsnow.blockracing.utils.TranslationUtil;

import java.util.*;

import java.time.Duration;

import static top.lqsnow.blockracing.listeners.BasicListener.editAmountPlayer;
import static top.lqsnow.blockracing.managers.Block.*;
import static top.lqsnow.blockracing.managers.Gui.*;
import static top.lqsnow.blockracing.managers.Scoreboard.updateScoreboard;
import static top.lqsnow.blockracing.managers.Team.*;
import static top.lqsnow.blockracing.utils.ColorUtil.t;
import static top.lqsnow.blockracing.utils.CommandUtil.*;

public class Game {
    public enum GameState {
        PREGAME, INGAME, END
    }

    public static GameState currentGameState = GameState.PREGAME;
    public static List<String> readyPlayers = new ArrayList<>();
    public static int redTeamScore = 0;
    public static int blueTeamScore = 0;
    public static int redTeamCurrentBlockAmount = 0;
    public static int blueTeamCurrentBlockAmount = 0;
    public static int redTeamTotalBlockAmount = 0;
    public static int blueTeamTotalBlockAmount = 0;
    public static List<String> freeRandomTPList = new ArrayList<>();

    public static ArrayList<Inventory> redTeamChest = new ArrayList<>();
    public static ArrayList<Inventory> blueTeamChest = new ArrayList<>();

    public static HashMap<Integer, Location> redWaypoint = new HashMap<>();
    public static HashMap<Integer, Location> blueWaypoint = new HashMap<>();

    public static HashMap<Integer, CompMaterial> redWaypointIconCache = new HashMap<>();
    public static HashMap<Integer, CompMaterial> blueWaypointIconCache = new HashMap<>();

    public static int redTeamRollCount;
    public static int blueTeamRollCount;
    public static int contestModeRollCount;
    public static List<String> redRollPlayers = new ArrayList<>();
    public static List<String> blueRollPlayers = new ArrayList<>();
    public static List<String> contestModeRollPlayers = new ArrayList<>();
    public static List<String> inGamePlayers = new ArrayList<>();
    public static ArrayList<String> locateCommandPermission = new ArrayList<>();
    public static int locateCost;
    public static Map<String, Integer> collectAmount = new HashMap<>();
    private static final Deque<Location> randomTpPool = new ArrayDeque<>();

    private static World activeGameWorld;

    private static BukkitRunnable timeModeTask;
    private static int timeModeRemainingSeconds;
    private static int timeModeDurationSeconds;
    private static boolean timeModeOvertime;
    private enum ComebackBuffState { NONE, RED, BLUE }
    private static ComebackBuffState comebackBuffState = ComebackBuffState.NONE;

    public static boolean isTimeModeActive() {
        return Setting.getCurrentGameMode().equals(Setting.GameMode.TIME);
    }

    public static boolean isContestModeActive() {
        return Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST);
    }

    public static boolean isTimeModeOvertime() {
        return timeModeOvertime;
    }

    public static int getTimeModeRemainingSeconds() {
        return timeModeRemainingSeconds;
    }

    public static int getTimeModeDurationSeconds() {
        return timeModeDurationSeconds;
    }

    public static String getFormattedTimeModeRemaining() {
        int seconds = Math.max(0, timeModeRemainingSeconds);
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long secs = duration.toSecondsPart();
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }

    private static World resolveGameWorld() {
        if (Setting.isNetherMode()) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NETHER) {
                    return world;
                }
            }
            World fallback = Bukkit.getWorld("world_nether");
            if (fallback != null) {
                return fallback;
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private static World getActiveGameWorld() {
        if (activeGameWorld != null) {
            return activeGameWorld;
        }
        World resolved = resolveGameWorld();
        if (resolved == null) {
            return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        return resolved;
    }

    public static void initChest() {
        int teamChestNum = Setting.getMaxTeamChestNum();
        for (int i = 0; i < teamChestNum; i++) {
            redTeamChest.add(Bukkit.createInventory(null, 6 * 9, Message.MENU_RED_CHEST.getString() + (i + 1)));
            blueTeamChest.add(Bukkit.createInventory(null, 6 * 9, Message.MENU_BLUE_CHEST.getString() + (i + 1)));
        }
    }

    // Team spawn locations (shared by team members)
    public static org.bukkit.Location redTeamSpawn = null;
    public static org.bukkit.Location blueTeamSpawn = null;

    // Generate safe team spawns for both teams
    public static void generateTeamSpawns() {
        World world = getActiveGameWorld();
        if (world == null) {
            return;
        }
        redTeamSpawn = generateSafeSpawn(world);
        if (Setting.isSharedTeamSpawn()) {
            blueTeamSpawn = redTeamSpawn == null ? null : redTeamSpawn.clone();
            return;
        }

        blueTeamSpawn = generateSafeSpawn(world);
        // ensure spawns are not too close
        if (redTeamSpawn != null && blueTeamSpawn != null && redTeamSpawn.getWorld().equals(blueTeamSpawn.getWorld())) {
            if (redTeamSpawn.distance(blueTeamSpawn) < 10) {
                blueTeamSpawn.add(20, 0, 0);
            }
        }
    }

    private static org.bukkit.Location generateSafeSpawn(World world) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return findSafeNetherLocation(world, new Random());
        }

        Random random = new Random();
        org.bukkit.Location loc;
        int attempts = 0;
        do {
            double randX = random.nextInt(20000) - 10000;
            double randZ = random.nextInt(20000) - 10000;
            loc = world.getHighestBlockAt(new org.bukkit.Location(world, randX, 0, randZ)).getLocation();
            loc.setY(loc.getY() + 1);
            attempts++;
            Biome biome = loc.getBlock().getBiome();
            boolean isOcean = biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.DEEP_COLD_OCEAN
                    || biome == Biome.LUKEWARM_OCEAN || biome == Biome.DEEP_FROZEN_OCEAN || biome == Biome.COLD_OCEAN
                    || biome == Biome.WARM_OCEAN || biome == Biome.DEEP_LUKEWARM_OCEAN || biome == Biome.FROZEN_OCEAN;
            if (!isOcean) break;
        } while (attempts < 30);
        return loc;
    }

    private static Location findSafeNetherLocation(World world, Random random) {
        if (world == null) {
            return null;
        }
        int attempt = 0;
        int maxAttempts = 100;
        while (attempt++ < maxAttempts) {
            int randX = random.nextInt(20000) - 10000;
            int randZ = random.nextInt(20000) - 10000;
            int ceilingLimit = Math.min(world.getMaxHeight() - 5, 118);
            for (int y = ceilingLimit; y >= 20; y--) {
                org.bukkit.block.Block floor = world.getBlockAt(randX, y, randZ);
                if (!isSafeNetherFloor(floor)) {
                    continue;
                }
                org.bukkit.block.Block head = floor.getRelative(0, 1, 0);
                org.bukkit.block.Block above = floor.getRelative(0, 2, 0);
                if (!isSafeNetherAir(head) || !isSafeNetherAir(above)) {
                    continue;
                }
                Location candidate = head.getLocation().add(0.5, 0, 0.5);
                if (candidate.getBlock().getType() == Material.LAVA) {
                    continue;
                }
                return candidate;
            }
        }
        return world.getSpawnLocation();
    }

    private static boolean isSafeNetherFloor(org.bukkit.block.Block block) {
        Material type = block.getType();
        if (type == Material.LAVA || type == Material.BEDROCK || type == Material.MAGMA_BLOCK) {
            return false;
        }
        return type.isSolid();
    }

    private static boolean isSafeNetherAir(org.bukkit.block.Block block) {
        Material type = block.getType();
        return type.isAir() || type == Material.CAVE_AIR;
    }

    public static void playerLogin(Player player) {
        Scoreboard.showScoreboard(player);

        if (getCurrentGameState().equals(GameState.PREGAME)) {
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Message.NOTICE_WELCOME.getString());
            player.sendMessage(t(
                    "&eNot your language? Please follow the tutorial to change the language: https://github.com/SkyCraftServer/BlockRacing/blob/3.0/docs/en/TranslationTutorial-en.md"));
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        } else if (getCurrentGameState().equals(GameState.INGAME)) {
            // Spectator
            if (!redTeamPlayers.contains(player.getName()) && !blueTeamPlayers.contains(player.getName())) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Message.NOTICE_SPECTATOR_JOIN.getString());
                return;
            }

            // Players who choose a team before the start of the game and exit, but enter
            // after the start of the game
            if (!inGamePlayers.contains(player.getName())) {
                initPlayer(player);
                freeRandomTPList.add(player.getName());
            }
        }

        // The permissions will disappear when the player exits and re-enters,
        // permissions need to be given again.
        if (locateCommandPermission.contains(player.getName())) {
            player.addAttachment(Main.getInstance(), "minecraft.command.locate", true);
        }

        BlockRacingVoicechatPlugin.syncPlayer(player);
        checkUpdate(player);
    }

    private static void checkUpdate(Player player) {
        player.resetTitle();
        if (!Config.CONFIG_VERSION.getString().equals(Main.getInstance().getDescription().getVersion())
            || !Message.MESSAGE_VERSION.getString().equals(Main.getInstance().getDescription().getVersion())) {
            if (Message.NOTICE_VERSION_MISMATCH.getString() != null) {
                player.sendMessage(Message.NOTICE_VERSION_MISMATCH.getString());
                player.sendTitle(Message.NOTICE_VERSION_MISMATCH_TITLE.getString(),
                        Message.NOTICE_VERSION_MISMATCH_SUBTITLE.getString(), 0, 2000, 0);
            } else {
                player.sendMessage(ColorUtil.t(
                    "&cWarning! The current file versions of your config.yml and language file do not correspond to the plugin version! You may have updated the plugin, but did not update the configuration file! This may lead to some unexpected errors! You can delete the configuration files in the \\plugins\\BlockRacing folder, and then restart the server, or download the latest version of the configuration files on GitHub to replace them!"));
                player.sendTitle(ColorUtil.t("&cWarning! Version Mismatch!"),
                        ColorUtil.t("&cPlease check the specific information in the chat!"), 20, 0, 0);
            }
            Bukkit.getLogger().severe(Message.NOTICE_VERSION_MISMATCH.getString());
        }
    }

    public static void playerQuit(Player player) {
        redRollPlayers.remove(player.getName());
        blueRollPlayers.remove(player.getName());
        readyPlayers.remove(player.getName());
        editAmountPlayer.remove(player.getName());
    }

    public static void playerReady(Player player) {
        if (!readyPlayers.contains(player.getName())) {
            readyPlayers.add(player.getName());
            sendAll(Message.NOTICE_READY.getString().replace("%player%", player.getName()));
            // Check if the game can start, just notice players
            if (readyPlayers.size() > 1 && readyPlayers.size() == Bukkit.getOnlinePlayers().size()) {
                sendAll(Message.NOTICE_ALL_READY.getString());
            }
        } else {
            readyPlayers.remove(player.getName());
            sendAll(Message.NOTICE_CANCEL_READY.getString().replace("%player%", player.getName()));
        }
    }

    // Check if the game can start. If not, send the reason to the player; if
    // possible, start the game directly
    public static void checkStartDemands(Player player) {

        // Game already start
        if (getCurrentGameState().equals(GameState.INGAME))
            return;

        // Not enough players
        if (!(Bukkit.getOnlinePlayers().size() > 1)) {
            player.sendMessage(Message.NOTICE_NOT_ENOUGH_PLAYERS.getString());
            return;
        }

        // Exist unready players
        if (!(readyPlayers.size() == Bukkit.getOnlinePlayers().size())) {
            List<String> unreadyPlayers = getOnlinePlayersString();
            unreadyPlayers.removeAll(readyPlayers);
            player.sendMessage(Message.NOTICE_EXIST_UNREADY.getString());
            player.sendMessage(Message.NOTICE_UNREADY_PLAYERS.getString() + unreadyPlayers);
            return;
        }

        // Exist empty team
        if (redTeamPlayers.isEmpty() || blueTeamPlayers.isEmpty()) {
            player.sendMessage(Message.NOTICE_EMPTY_TEAM.getString());
            return;
        }

        // Blocks have problems
        if (!checkBlock()) {
            return;
        }

        // Start the game
        sendAll(Message.NOTICE_START.getString());
        startGame();
    }

    private static void prepareTimeModeState() {
        stopTimeModeCountdown();
        if (isTimeModeActive()) {
            timeModeDurationSeconds = Math.max(60, Setting.getTimeModeDurationSeconds());
            timeModeRemainingSeconds = timeModeDurationSeconds;
        } else {
            timeModeDurationSeconds = 0;
            timeModeRemainingSeconds = 0;
        }
        timeModeOvertime = false;
    }

    private static void startTimeModeCountdownIfNeeded() {
        if (!isTimeModeActive()) {
            return;
        }
        stopTimeModeCountdown();
        timeModeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!getCurrentGameState().equals(GameState.INGAME)) {
                    stopTimeModeCountdown();
                    return;
                }
                if (timeModeOvertime) {
                    updateScoreboard();
                    return;
                }
                if (timeModeRemainingSeconds <= 0) {
                    handleTimeModeCountdownFinished();
                    return;
                }
                timeModeRemainingSeconds--;
                updateScoreboard();
                if (timeModeRemainingSeconds == 0) {
                    handleTimeModeCountdownFinished();
                }
            }
        };
        timeModeTask.runTaskTimer(Main.getInstance(), 20L, 20L);
    }

    private static void stopTimeModeCountdown() {
        if (timeModeTask != null) {
            timeModeTask.cancel();
            timeModeTask = null;
        }
    }

    private static void handleTimeModeCountdownFinished() {
        if (!isTimeModeActive() || timeModeOvertime || !getCurrentGameState().equals(GameState.INGAME)) {
            return;
        }
        // Compare collected block counts in time mode rather than time-weighted scores
        if (redTeamCurrentBlockAmount > blueTeamCurrentBlockAmount) {
            redWin();
            showRanking();
        } else if (blueTeamCurrentBlockAmount > redTeamCurrentBlockAmount) {
            blueWin();
            showRanking();
        } else {
            beginTimeModeOvertime();
        }
    }

    private static void beginTimeModeOvertime() {
        if (timeModeOvertime) {
            return;
        }
        timeModeOvertime = true;
        timeModeRemainingSeconds = 0;
        sendAll(Message.NOTICE_OVERTIME_START.getString());
        updateScoreboard();
    }

    private static void finalizeOvertimeWin(boolean redTeam) {
        if (!isTimeModeActive() || !timeModeOvertime || !getCurrentGameState().equals(GameState.INGAME)) {
            return;
        }
        if (redTeam) {
            redWin();
        } else {
            blueWin();
        }
        showRanking();
    }

    public static void startGame() {
        // Init
        setCurrentGameState(GameState.INGAME);
        closeAllPlayersMenu();
        editAmountPlayer.clear();
        
        // Reset roll counts and players for all game modes
        redTeamRollCount = 0;
        blueTeamRollCount = 0;
        contestModeRollCount = 0;
        redRollPlayers.clear();
        blueRollPlayers.clear();
        contestModeRollPlayers.clear();
        
        setupBlocks();
        switch (Setting.getCurrentGameMode()) {
            case TIME -> {
                // In time mode we don't predefine ordered block lists; use pool size as total
                redTeamTotalBlockAmount = Block.blocks.size();
                blueTeamTotalBlockAmount = Block.blocks.size();
                prepareTimeModeState();
            }
            case CONTEST -> {
                redTeamTotalBlockAmount = Setting.getBlockAmount();
                blueTeamTotalBlockAmount = Setting.getBlockAmount();
            }
            default -> {
                redTeamTotalBlockAmount = redTeamBlocks.size();
                blueTeamTotalBlockAmount = blueTeamBlocks.size();
            }
        }
        setLocateScore();
        updateScoreboard();
        Bukkit.getOnlinePlayers().forEach((Player player) -> freeRandomTPList.add(player.getName()));
        new runPer5Tick().runTaskTimer(Main.getInstance(), 0L, 5L);
        activeGameWorld = resolveGameWorld();
        World world = getActiveGameWorld();
        if (world == null) {
            world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            Bukkit.getLogger().severe("[BlockRacing] No world available for teleport!");
            return;
        }
        world.setDifficulty(Difficulty.EASY);
        world.setTime(1000);
        world.setStorm(false);
        world.setThundering(false);
        world.getEntities().stream().filter(e -> e instanceof Item).forEach(Entity::remove);

        // World border
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(59999968);

        // Generate shared team spawns for this game
        generateTeamSpawns();

        // Processing of unselected team players (spectators)
        inGamePlayers.addAll(getOnlinePlayersString());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!redTeamPlayers.contains(player.getName()) && !blueTeamPlayers.contains(player.getName())) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Message.NOTICE_SPECTATOR.getString());
                inGamePlayers.remove(player.getName());
            }
        }

        // Settings for each player
        for (String p : inGamePlayers) {
            // General
            Player player = Bukkit.getPlayer(p);
            initPlayer(player);
        }

        BlockRacingVoicechatPlugin.syncAllPlayers();

        Bukkit.getLogger().info("Red team players: " + redTeamPlayers.toString());
        Bukkit.getLogger().info("Blue team players: " + blueTeamPlayers.toString());
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL))
            Bukkit.getLogger().info("Game mode: Normal");
        else if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING))
            Bukkit.getLogger().info("Game mode: Racing");
        else if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST))
            Bukkit.getLogger().info("Game mode: Contest");
        else if (Setting.getCurrentGameMode().equals(Setting.GameMode.TIME))
            Bukkit.getLogger().info("Game mode: Time");
        Bukkit.getLogger().info(Setting.isSpeedMode() ? "Speed mode: On" : "Speed mode: Off");
        Bukkit.getLogger().info(Setting.isNetherMode() ? "Nether mode: On" : "Nether mode: Off");
        startTimeModeCountdownIfNeeded();
    }

    // Player init
    public static void initPlayer(Player player) {
        // General
        player.getInventory().clear();
        // Teleport to team spawn if available, otherwise random teleport
        org.bukkit.Location spawn = null;
        if (redTeamPlayers.contains(player.getName()) && redTeamSpawn != null) spawn = redTeamSpawn;
        else if (blueTeamPlayers.contains(player.getName()) && blueTeamSpawn != null) spawn = blueTeamSpawn;
        if (spawn != null) player.teleport(spawn);
        else randomTeleport(player, true);
        player.setHealth(20);
        player.setExp(0);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().addItem(ItemCreator.fromMaterial(CompMaterial.STONE_PICKAXE).amount(1).make());
        player.getInventory().addItem(ItemCreator.fromMaterial(CompMaterial.STONE_AXE).amount(1).make());
        player.getInventory().addItem(ItemCreator.fromMaterial(CompMaterial.STONE_SHOVEL).amount(1).make());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));

        // Speed mode
        if (Setting.isSpeedMode()) {
            for (ItemStack stack : Setting.getSpeedModeItems()) {
                if (stack == null) {
                    continue;
                }
                player.getInventory().addItem(stack.clone());
            }
            applyConfiguredSpeedModeEffects(player);
        }

        refreshComebackEffects();
    }

    // Roll
    public static void roll(Player player) {
        if (isContestModeActive()) {
            // 争夺模式：全场轮换，只有一次机会
            if (contestModeRollCount >= 1) {
                player.sendMessage(Message.NOTICE_CANNOT_ROLL_CONTEST.getString());
                return;
            }
            if (!contestModeRollPlayers.contains(player.getName())) {
                contestModeRollPlayers.add(player.getName());
                sendAll(Message.NOTICE_ROLL_REQUEST_CONTEST.getString().replace("%player%", player.getName()));
            } else {
                contestModeRollPlayers.remove(player.getName());
                sendAll(Message.NOTICE_ROLL_REQUEST_CANCEL.getString().replace("%player%", player.getName()));
            }
        } else {
            // 其他模式：队伍轮换，每队三次
            if (redTeamPlayers.contains(player.getName())) {
                List<String> candidates = new ArrayList<>(blocks);
                candidates.removeAll(redTeamBlocks);
                if (candidates.isEmpty()) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                    return;
                }
                if (redTeamRollCount >= 3) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL.getString());
                    return;
                }
                if (!redRollPlayers.contains(player.getName())) {
                    redRollPlayers.add(player.getName());
                    sendRed(Message.NOTICE_ROLL_REQUEST.getString().replace("%player%", player.getName()));
                } else {
                    redRollPlayers.remove(player.getName());
                    sendRed(Message.NOTICE_ROLL_REQUEST_CANCEL.getString().replace("%player%", player.getName()));
                }
            } else if (blueTeamPlayers.contains(player.getName())) {
                List<String> candidates = new ArrayList<>(blocks);
                candidates.removeAll(blueTeamBlocks);
                if (candidates.isEmpty()) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                    return;
                }
                if (blueTeamRollCount >= 3) {
                    player.sendMessage(Message.NOTICE_CANNOT_ROLL.getString());
                    return;
                }
                if (!blueRollPlayers.contains(player.getName())) {
                    blueRollPlayers.add(player.getName());
                    sendBlue(Message.NOTICE_ROLL_REQUEST.getString().replace("%player%", player.getName()));
                } else {
                    blueRollPlayers.remove(player.getName());
                    sendBlue(Message.NOTICE_ROLL_REQUEST_CANCEL.getString().replace("%player%", player.getName()));
                }
            }
        }
    }

    public static void locate(Player player) {
        if (locateCommandPermission.contains(player.getName())) {
            player.sendMessage(Message.NOTICE_LOCATE_ALREADY_BOUGHT.getString());
            return;
        }
        if (redTeamPlayers.contains(player.getName())) {
            if (redTeamScore >= locateCost) {
                redTeamScore -= locateCost;
                updateScoreboard();
                locateCommandPermission.add(player.getName());
                sendAll(Message.NOTICE_BUY_LOCATE.getString().replace("%player%", player.getName()));
                player.addAttachment(Main.getInstance(), "minecraft.command.locate", true);
            } else {
                player.sendMessage(Message.NOTICE_NOT_ENOUGH_SCORE.getString());
            }
        } else if (blueTeamPlayers.contains(player.getName())) {
            if (blueTeamScore >= locateCost) {
                blueTeamScore -= locateCost;
                updateScoreboard();
                locateCommandPermission.add(player.getName());
                sendAll(Message.NOTICE_BUY_LOCATE.getString().replace("%player%", player.getName()));
                player.addAttachment(Main.getInstance(), "minecraft.command.locate", true);
            } else {
                player.sendMessage(Message.NOTICE_NOT_ENOUGH_SCORE.getString());
            }
        }
    }

    // Random Teleport
    public static void randomTeleport(Player player, boolean avoidOcean) {
        World world = getActiveGameWorld();
        if (world == null) {
            return;
        }

        Location offset;
        if (Setting.isNetherMode() && world.getEnvironment() == World.Environment.NETHER) {
            offset = findSafeNetherLocation(world, new Random());
        } else {
            offset = findSafeOverworldLocation(world, avoidOcean);
        }

        if (offset == null) {
            return;
        }

        player.teleport(offset);

        String x = String.format("%.1f", offset.getX());
        String y = String.format("%.1f", offset.getY());
        String z = String.format("%.1f", offset.getZ());

        player.sendMessage(Message.NOTICE_TP_SUCCESS.getString().replace("%x%", x).replace("%y%", y).replace("%z%", z));
    }

    private static Location findSafeOverworldLocation(World world, boolean avoidOcean) {
        // Try to use a candidate from the pool first
        Location candidate = pollRandomTeleportCandidate();
        if (candidate != null) {
            double randX = candidate.getX();
            double randZ = candidate.getZ();
            Location loc = world.getHighestBlockAt(new Location(world, randX, 0, randZ)).getLocation();
            loc.setY(loc.getY() + 1);
            if (!avoidOcean || !isOceanBiome(loc.getBlock().getBiome())) {
                return loc;
            }
        }

        // Fall back to random location generation
        Random random = new Random();
        Location fallback = world.getSpawnLocation();
        for (int attempts = 0; attempts < 60; attempts++) {
            double randX = random.nextInt(20000) - 10000;
            double randZ = random.nextInt(20000) - 10000;
            Location loc = world.getHighestBlockAt(new Location(world, randX, 0, randZ)).getLocation();
            loc.setY(loc.getY() + 1);
            if (avoidOcean && isOceanBiome(loc.getBlock().getBiome())) {
                continue;
            }
            return loc;
        }
        return fallback;
    }

    private static boolean isOceanBiome(Biome biome) {
        return biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.DEEP_COLD_OCEAN
                || biome == Biome.LUKEWARM_OCEAN || biome == Biome.DEEP_FROZEN_OCEAN || biome == Biome.COLD_OCEAN
                || biome == Biome.WARM_OCEAN || biome == Biome.DEEP_LUKEWARM_OCEAN || biome == Biome.FROZEN_OCEAN;
    }

    public static synchronized void addRandomTeleportCandidate(Location location) {
        if (location != null) {
            randomTpPool.addLast(location);
        }
    }

    public static synchronized Location pollRandomTeleportCandidate() {
        return randomTpPool.pollFirst();
    }

    public static synchronized int getRandomTeleportPoolSize() {
        return randomTpPool.size();
    }

    public static synchronized List<Location> getRandomTeleportPoolSnapshot() {
        return List.copyOf(randomTpPool);
    }

    // Waypoints
    // Return value: true -> waypoint changed, false -> waypoint doesn't change
    public static boolean waypoint(Player player, int index, ClickType clickType) {
        String team = redTeamPlayers.contains(player.getName()) ? "red"
                : (blueTeamPlayers.contains(player.getName()) ? "blue" : "");

        if (!team.isEmpty()) {
            Location waypoint = getWaypoint(team, index);
            String action = "";
            if (clickType.equals(ClickType.LEFT)) {
                action = "left";
            } else if (clickType.equals(ClickType.RIGHT)) {
                action = "right";
            }

            switch (action) {
                case "left" -> {
                    if (waypoint == null) {
                        setWaypoint(player, team, index);
                        return true;
                    } else {
                        player.teleport(waypoint);
                        String x = String.format("%.1f", waypoint.getX());
                        String y = String.format("%.1f", waypoint.getY());
                        String z = String.format("%.1f", waypoint.getZ());

                        player.sendMessage(Message.NOTICE_TP_SUCCESS.getString().replace("%x%", x).replace("%y%", y)
                                .replace("%z%", z));
                        return false;
                    }
                }
                case "right" -> removeWaypoint(player, index);
            }
        }
        return false;
    }

    public static Location getWaypoint(String team, int index) {
        return switch (team) {
            case "red" -> redWaypoint.get(index);
            case "blue" -> blueWaypoint.get(index);
            default -> null;
        };
    }

    public static void setWaypoint(Player player, String team, int index) {
        Location waypoint = player.getLocation();
        switch (team) {
            case "red" -> {
                redWaypoint.put(index, waypoint);
            }
            case "blue" -> {
                blueWaypoint.put(index, waypoint);
            }
        }
    }

    private static void removeWaypoint(Player player, int index) {
        TextComponent message = new TextComponent(
                Message.NOTICE_REMOVE_WAYPOINT.getString().replace("%index%", String.valueOf(index)));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/waypoint remove " + index));
        player.spigot().sendMessage(message);
        player.closeInventory();
    }

    // Run per 2t
    // Before the game, provide regeneration and saturation effects
    public static class runPer2Tick extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10, 255));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 10, 255));
            }
            if (getCurrentGameState().equals(GameState.INGAME))
                this.cancel();
        }
    }

    // Run per 5t
    // During the game
    public static class runPer5Tick extends BukkitRunnable {

        @Override
        public void run() {

            if (!getCurrentGameState().equals(GameState.INGAME)) {
                this.cancel();
                return;
            }

            // Inventory check
            checkRedInventory();
            checkBlueInventory();

            // Win check
            if (!isContestModeActive()) {
                if (redTeamRemainingBlocks.isEmpty()) {
                    redWin();
                    showRanking();
                    this.cancel();
                }
                if (blueTeamRemainingBlocks.isEmpty()) {
                    blueWin();
                    showRanking();
                    this.cancel();
                }
            }

            // Roll check
            if (isContestModeActive()) {
                if (!contestModeRollPlayers.isEmpty())
                    checkContestModeRoll();
            } else {
                if (!redRollPlayers.isEmpty())
                    checkRedRoll();
                if (!blueRollPlayers.isEmpty())
                    checkBlueRoll();
            }

        }
    }

    private static void showRanking() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(collectAmount.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        sendAll(Message.NOTICE_RANKING.getString());
        for (Map.Entry<String, Integer> entry : entries) {
            if (redTeamPlayers.contains(entry.getKey())) {
                sendAll(Message.NOTICE_RANKING_RED.getString().replace("%player%", entry.getKey()).replace("%amount%",
                        entry.getValue().toString()));
            } else if (blueTeamPlayers.contains(entry.getKey())) {
                sendAll(Message.NOTICE_RANKING_BLUE.getString().replace("%player%", entry.getKey()).replace("%amount%",
                        entry.getValue().toString()));
            } else {
                sendAll(Message.NOTICE_RANKING_OFFLINE.getString().replace("%player%", entry.getKey())
                        .replace("%amount%", entry.getValue().toString()));
            }
        }
    }

    private static void checkRedRoll() {
        Set<String> redSet = new HashSet<>(redRollPlayers);
        Set<String> redOnlineSet = new HashSet<>(getOnlineTeamPlayers("red"));
        if (!getOnlineTeamPlayers("red").isEmpty() && redSet.containsAll(redOnlineSet)) {
            List<String> b = new ArrayList<>(blocks);
            b.removeAll(redTeamBlocks);
            if (b.isEmpty()) {
                sendRed(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                redRollPlayers.clear();
                return;
            }
            Random random = new Random();
            int rollAmount = getCurrentBlocks("red").size();
            for (int i = 0; i < rollAmount; i++) {
                redTeamRemainingBlocks.set(i, b.get(random.nextInt(b.size())));
            }
            sendAll(Message.NOTICE_RED_ROLL_SUCCESS.getString());
            redTeamRollCount += 1;
            redRollPlayers.clear();
            updateScoreboard();
        }
    }

    private static void checkBlueRoll() {
        Set<String> blueSet = new HashSet<>(blueRollPlayers);
        Set<String> blueOnlineSet = new HashSet<>(getOnlineTeamPlayers("blue"));
        if (!getOnlineTeamPlayers("blue").isEmpty() && blueSet.containsAll(blueOnlineSet)) {
            List<String> b = new ArrayList<>(blocks);
            b.removeAll(blueTeamBlocks);
            if (b.isEmpty()) {
                sendBlue(Message.NOTICE_CANNOT_ROLL_NO_CANDIDATE.getString());
                blueRollPlayers.clear();
                return;
            }
            Random random = new Random();
            int rollAmount = getCurrentBlocks("blue").size();
            for (int i = 0; i < rollAmount; i++) {
                blueTeamRemainingBlocks.set(i, b.get(random.nextInt(b.size())));
            }
            sendAll(Message.NOTICE_BLUE_ROLL_SUCCESS.getString());
            blueTeamRollCount += 1;
            blueRollPlayers.clear();
            updateScoreboard();
        }
    }

    private static void checkContestModeRoll() {
        // 获取所有在线玩家
        Set<String> allOnlinePlayers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (inGamePlayers.contains(player.getName())) {
                allOnlinePlayers.add(player.getName());
            }
        }
        
        Set<String> contestRollSet = new HashSet<>(contestModeRollPlayers);
        
        // 检查是否所有在线玩家都同意轮换
        if (!allOnlinePlayers.isEmpty() && contestRollSet.containsAll(allOnlinePlayers)) {
            List<String> b = new ArrayList<>(blocks);
            Random random = new Random();
            int rollAmount = redTeamRemainingBlocks.size();
            for (int i = 0; i < rollAmount; i++) {
                redTeamRemainingBlocks.set(i, b.get(random.nextInt(b.size())));
            }
            sendAll(Message.NOTICE_RED_ROLL_SUCCESS.getString());
            contestModeRollCount += 1;
            contestModeRollPlayers.clear();
            updateScoreboard();
        }
    }

    private static void setLocateScore() {
        if (Setting.getBlockAmount() <= 20)
            locateCost = 5;
        else if (Setting.getBlockAmount() <= 50)
            locateCost = 8;
        else if (Setting.getBlockAmount() <= 100)
            locateCost = 16;
        else if (Setting.getBlockAmount() <= 200)
            locateCost = 20;
        else
            locateCost = 30;
    }

    private static void checkRedInventory() {
        // In CONTEST mode, check if both teams have the same block first
        if (isContestModeActive()) {
            for (String block : getCurrentBlocks("red")) {
                // Check if red team has the block
                boolean redHasBlock = false;
                for (String player : redTeamPlayers) {
                    Player p = Bukkit.getPlayer(player);
                    if (p != null && p.getInventory().contains(Material.valueOf(block))) {
                        redHasBlock = true;
                        break;
                    }
                }
                if (!redHasBlock) {
                    for (Inventory chest : redTeamChest) {
                        if (chest.contains(Material.valueOf(block))) {
                            redHasBlock = true;
                            break;
                        }
                    }
                }
                
                // Check if blue team has the block
                boolean blueHasBlock = false;
                for (String player : blueTeamPlayers) {
                    Player p = Bukkit.getPlayer(player);
                    if (p != null && p.getInventory().contains(Material.valueOf(block))) {
                        blueHasBlock = true;
                        break;
                    }
                }
                if (!blueHasBlock) {
                    for (Inventory chest : blueTeamChest) {
                        if (chest.contains(Material.valueOf(block))) {
                            blueHasBlock = true;
                            break;
                        }
                    }
                }
                
                // If both teams have the block, execute joint completion
                if (redHasBlock && blueHasBlock) {
                    bothTaskComplete(block);
                    return;
                }
            }
        }
        
        // Normal mode: Complete from player
        for (String player : redTeamPlayers) {
            for (String block : getCurrentBlocks("red")) {
                Player p = Bukkit.getPlayer(player);
                if (p == null)
                    continue;
                if (p.getInventory().contains(Material.valueOf(block))) {
                    redTaskComplete(block, player);
                    return;
                }
            }
        }
        // Complete from team chest
        for (String block : getCurrentBlocks("red")) {
            for (Inventory chest : redTeamChest) {
                if (chest.contains(Material.valueOf(block))) {
                    redTaskComplete(block, Message.NOTICE_RED_TEAM_CHEST.getString());
                    return;
                }
            }
        }
    }

    private static void checkBlueInventory() {
        // In CONTEST mode, both-team completion is handled in checkRedInventory.
        // Still allow blue to complete individually here.
        
        // Complete from player
        for (String player : blueTeamPlayers) {
            for (String block : getCurrentBlocks("blue")) {
                Player p = Bukkit.getPlayer(player);
                if (p == null)
                    continue;
                if (p.getInventory().contains(Material.valueOf(block))) {
                    blueTaskComplete(block, player);
                    return;
                }
            }
        }
        // Complete from team chest
        for (String block : getCurrentBlocks("blue")) {
            for (Inventory chest : blueTeamChest) {
                if (chest.contains(Material.valueOf(block))) {
                    blueTaskComplete(block, Message.NOTICE_BLUE_TEAM_CHEST.getString());
                    return;
                }
            }
        }
    }

    public static void bothTaskComplete(String block) {
        // Both teams collect the same block in CONTEST mode
        sendAll(Message.NOTICE_BOTH_COLLECT_CONTEST.getString().replace("%block%", TranslationUtil.getValue(block)));
        Bukkit.getLogger().info(Message.NOTICE_BOTH_COLLECT_CONTEST.getString()
                .replace("%block%", TranslationUtil.getValue(block)).replaceAll("§.", ""));
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        
        // Remove block from both teams' remaining blocks
        redTeamRemainingBlocks.remove(block);
        blueTeamRemainingBlocks.remove(block);
        
        // Mark as completed for global tracking
        redCompletedBlocks.add(block);
        blueCompletedBlocks.add(block);
        
        // Both teams get 1 point
        redTeamScore += 1;
        blueTeamScore += 1;
        redTeamCurrentBlockAmount += 1;
        blueTeamCurrentBlockAmount += 1;
        
        updateScoreboard();
        
        // Check if either team reached target amount
        if (redTeamCurrentBlockAmount >= Setting.getBlockAmount()
                && blueTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            drawGame();
            showRanking();
            return;
        }
        if (redTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            redWin();
            showRanking();
            return;
        }
        if (blueTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            blueWin();
            showRanking();
            return;
        }
    }

    public static void redTaskComplete(String block, String player) {
        sendAll(Message.NOTICE_RED_COLLECT.getString().replace("%block%", TranslationUtil.getValue(block))
                .replace("%player%", player));
        Bukkit.getLogger().info(Message.NOTICE_RED_COLLECT.getString()
                .replace("%block%", TranslationUtil.getValue(block)).replace("%player%", player).replaceAll("§.", ""));
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        redTeamRemainingBlocks.remove(block);
        // mark as completed for global tracking
        redCompletedBlocks.add(block);
        redTeamScore += 1;
        redTeamCurrentBlockAmount += 1;
        collect(player);
        updateScoreboard();
        if (isContestModeActive() && redTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            redWin();
            showRanking();
            return;
        }
        // Only declare victory for this team if, in time mode, that team's
        // completed list contains all global blocks (i.e. the team alone
        // has collected every block in the pool).
        if (isTimeModeActive() && redCompletedBlocks.containsAll(blocks)) {
            redWin();
            showRanking();
            return;
        }
        // In time mode, when a block is completed (and not in overtime),
        // generate and add a replacement target so teams always have targets.
        if (isTimeModeActive() && !timeModeOvertime) {
            float progress = 0f;
            if (timeModeDurationSeconds > 0) {
                progress = (float) (timeModeDurationSeconds - timeModeRemainingSeconds) / (float) timeModeDurationSeconds;
                progress = Math.max(0f, Math.min(1f, progress));
            }
            String newBlock = selectBlockByTimeProgress("red", progress);
            redTeamRemainingBlocks.add(newBlock);
            updateScoreboard();
        }
        if (isTimeModeActive() && timeModeOvertime) {
            finalizeOvertimeWin(true);
            return;
        }
        // Put items into the opponent's team chest
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
            for (int i = blueTeamChest.size() - 1; i >= 0; i--) {
                Inventory chest = blueTeamChest.get(i);
                int emptyPos = chest.firstEmpty();
                if (emptyPos == -1) {
                    continue;
                }
                chest.setItem(emptyPos, ItemCreator.fromMaterial(CompMaterial.valueOf(block)).amount(64).make());
                return;
            }
            sendAll(Message.NOTICE_TEAM_CHEST_FULL.getString().replace("%team%", Message.TEAM_BLUE_NAME.getString())
                    .replace("%block%", TranslationUtil.getValue(block)));
        }
    }

    public static void blueTaskComplete(String block, String player) {
        sendAll(Message.NOTICE_BLUE_COLLECT.getString().replace("%block%", TranslationUtil.getValue(block))
                .replace("%player%", player));
        Bukkit.getLogger().info(Message.NOTICE_BLUE_COLLECT.getString()
                .replace("%block%", TranslationUtil.getValue(block)).replace("%player%", player).replaceAll("§.", ""));
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        blueTeamRemainingBlocks.remove(block);
        // mark as completed for global tracking
        blueCompletedBlocks.add(block);
        blueTeamScore += 1;
        blueTeamCurrentBlockAmount += 1;
        collect(player);
        updateScoreboard();
        if (isContestModeActive() && blueTeamCurrentBlockAmount >= Setting.getBlockAmount()) {
            blueWin();
            showRanking();
            return;
        }
        // Only declare victory for this team if, in time mode, that team's
        // completed list contains all global blocks (i.e. the team alone
        // has collected every block in the pool).
        if (isTimeModeActive() && blueCompletedBlocks.containsAll(blocks)) {
            blueWin();
            showRanking();
            return;
        }
        // In time mode, when a block is completed (and not in overtime),
        // generate and add a replacement target so teams always have targets.
        if (isTimeModeActive() && !timeModeOvertime) {
            float progress = 0f;
            if (timeModeDurationSeconds > 0) {
                progress = (float) (timeModeDurationSeconds - timeModeRemainingSeconds) / (float) timeModeDurationSeconds;
                progress = Math.max(0f, Math.min(1f, progress));
            }
            String newBlock = selectBlockByTimeProgress("blue", progress);
            blueTeamRemainingBlocks.add(newBlock);
            updateScoreboard();
        }
        if (isTimeModeActive() && timeModeOvertime) {
            finalizeOvertimeWin(false);
            return;
        }
        // Put items into the opponent's team chest
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
            for (int i = redTeamChest.size() - 1; i >= 0; i--) {
                Inventory chest = redTeamChest.get(i);
                int emptyPos = chest.firstEmpty();
                if (emptyPos == -1) {
                    continue;
                }
                chest.setItem(emptyPos, ItemCreator.fromMaterial(CompMaterial.valueOf(block)).amount(64).make());
                return;
            }
            sendAll(Message.NOTICE_TEAM_CHEST_FULL.getString().replace("%team%", Message.TEAM_RED_NAME.getString())
                    .replace("%block%", TranslationUtil.getValue(block)));
        }
    }

    public static void redWin() {
        stopTimeModeCountdown();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            player.sendTitle(Message.NOTICE_RED_WIN.getString(), null);
            player.setGameMode(GameMode.SPECTATOR);
        }
        sendAll(Message.NOTICE_RED_WIN.getString());
        playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        setCurrentGameState(GameState.END);
        BlockRacingVoicechatPlugin.syncAllPlayers();
        updateScoreboard();
    }

    public static void blueWin() {
        stopTimeModeCountdown();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            player.sendTitle(Message.NOTICE_BLUE_WIN.getString(), null);
            player.setGameMode(GameMode.SPECTATOR);
        }
        sendAll(Message.NOTICE_BLUE_WIN.getString());
        playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        setCurrentGameState(GameState.END);
        BlockRacingVoicechatPlugin.syncAllPlayers();
        updateScoreboard();
    }

    public static void drawGame() {
        stopTimeModeCountdown();
        String drawMessage = Message.NOTICE_DRAW.getString();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
            player.sendTitle(drawMessage, null);
            player.setGameMode(GameMode.SPECTATOR);
        }
        sendAll(drawMessage);
        playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        setCurrentGameState(GameState.END);
        BlockRacingVoicechatPlugin.syncAllPlayers();
        updateScoreboard();
    }

    public static List<String> getCurrentBlocks(String team) {
        return switch (team) {
            case "red" -> redTeamRemainingBlocks.subList(0, Math.min(redTeamRemainingBlocks.size(), 4));
            case "blue" -> blueTeamRemainingBlocks.subList(0, Math.min(blueTeamRemainingBlocks.size(), 4));
            default -> throw new IllegalStateException("Unexpected value: " + team);
        };
    }

    public static List<String> getOnlinePlayersString() {
        List<String> onlinePlayers = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach((Player player) -> onlinePlayers.add(player.getName()));
        return onlinePlayers;
    }

    public static List<String> getOnlineTeamPlayers(String team) {
        List<String> onlineTeamPlayers = new ArrayList<>();

        if (team.equals("red")) {
            for (String player : redTeamPlayers) {
                if (Bukkit.getPlayer(player) != null) {
                    onlineTeamPlayers.add(player);
                }
            }
        } else {
            for (String player : blueTeamPlayers) {
                if (Bukkit.getPlayer(player) != null) {
                    onlineTeamPlayers.add(player);
                }
            }
        }
        return onlineTeamPlayers;
    }

    public static void playSound(Sound sound) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player, sound, 1F, 1F);
        }
    }

    public static String getCoords(Location location) {
        return String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    public static void collect(String p) {
        if (collectAmount.containsKey(p)) {
            int currentAmount = collectAmount.get(p);
            collectAmount.put(p, currentAmount + 1);
        } else {
            collectAmount.put(p, 1);
        }
    }

    public static void applyConfiguredSpeedModeEffects(Player player) {
        if (player == null || !Setting.isSpeedMode()) {
            return;
        }
        for (Setting.SpeedModeEffectOption option : Setting.getSpeedModeEffects()) {
            if (option == null || option.getEffect() == null) {
                continue;
            }
            if (option.getDelayTicks() > 0L) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                        () -> player.addPotionEffect(option.getEffect()), option.getDelayTicks());
            } else {
                player.addPotionEffect(option.getEffect());
            }
        }
    }

    private static void reapplyBaselineMobility(Player player) {
        if (player == null) {
            return;
        }
        // Maintain base speed/resistance that are expected for in-game players and preserve speed-mode perks.
        int amplifier = Setting.isSpeedMode() ? 1 : 1;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, amplifier, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, amplifier, false, false));
    }

    private static void clearComebackEffects(List<String> members) {
        for (String name : members) {
            Player player = Bukkit.getPlayer(name);
            if (player == null) {
                continue;
            }
            boolean removed = false;

            for (PotionEffect configured : Setting.getComebackBuffEffects()) {
                if (configured == null || configured.getType() == null) {
                    continue;
                }
                PotionEffect active = player.getPotionEffect(configured.getType());
                if (active != null && active.getAmplifier() >= configured.getAmplifier()) {
                    player.removePotionEffect(configured.getType());
                    removed = true;
                }
            }

            if (removed) {
                reapplyBaselineMobility(player);
            }
        }
    }

    private static void applyComebackEffects(String team) {
        List<String> members = getOnlineTeamPlayers(team);
        for (String name : members) {
            Player player = Bukkit.getPlayer(name);
            if (player == null) {
                continue;
            }
            for (PotionEffect effect : Setting.getComebackBuffEffects()) {
                if (effect == null || effect.getType() == null) {
                    continue;
                }
                player.addPotionEffect(effect);
            }
        }
    }

    public static void refreshComebackEffects() {
        int threshold = Math.max(1, Setting.getComebackBuffThresholdPoints());
        if (!getCurrentGameState().equals(GameState.INGAME) || !Setting.isComebackBuffEnabled()) {
            if (comebackBuffState != ComebackBuffState.NONE) {
                sendAll(Message.NOTICE_COMEBACK_CLEAR.getString().replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.NONE;
            clearComebackEffects(redTeamPlayers);
            clearComebackEffects(blueTeamPlayers);
            return;
        }

        int diff = redTeamScore - blueTeamScore;
        if (Math.abs(diff) < threshold) {
            if (comebackBuffState != ComebackBuffState.NONE) {
                sendAll(Message.NOTICE_COMEBACK_CLEAR.getString().replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.NONE;
            clearComebackEffects(redTeamPlayers);
            clearComebackEffects(blueTeamPlayers);
            return;
        }

        if (diff >= threshold) {
            if (comebackBuffState != ComebackBuffState.BLUE) {
                sendAll(Message.NOTICE_COMEBACK_APPLY.getString()
                        .replace("%team%", Message.TEAM_BLUE_NAME.getString())
                        .replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.BLUE;
            applyComebackEffects("blue");
            clearComebackEffects(redTeamPlayers);
        } else if (diff <= -threshold) {
            if (comebackBuffState != ComebackBuffState.RED) {
                sendAll(Message.NOTICE_COMEBACK_APPLY.getString()
                        .replace("%team%", Message.TEAM_RED_NAME.getString())
                        .replace("%points%", String.valueOf(threshold)));
            }
            comebackBuffState = ComebackBuffState.RED;
            applyComebackEffects("red");
            clearComebackEffects(blueTeamPlayers);
        }
    }

    public static GameState getCurrentGameState() {
        return currentGameState;
    }

    public static void setCurrentGameState(GameState currentGameState) {
        Game.currentGameState = currentGameState;
        BlockRacingVoicechatPlugin.syncAllPlayers();
        Motd.refresh();
    }
}
