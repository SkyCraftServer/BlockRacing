package top.lqsnow.blockracing;

import lombok.Getter;
import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import top.lqsnow.blockracing.commands.*;
import top.lqsnow.blockracing.listeners.BasicListener;
import top.lqsnow.blockracing.listeners.AddonMonitorListener;
import top.lqsnow.blockracing.listeners.MotdListener;
import top.lqsnow.blockracing.managers.*;
import top.lqsnow.blockracing.scoreboard.Scoreboard;
import top.lqsnow.blockracing.voicechat.BlockRacingVoicechatPlugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;

import static org.bukkit.Bukkit.getPluginManager;


public class Main extends JavaPlugin {
    private static Main instance;
    @Getter
    private static FoliaLib foliaLib;

    public static Main getInstance() {
        return instance;
    }

    public static String getVersion() {
        return instance.getPluginMeta().getVersion();
    }

    @Override
    public void onLoad() {
        instance = this;
        WorldResetMarker.processPendingReset(this);
    }

    @Override
    public void onEnable() {
        instance = this;
        foliaLib = new FoliaLib(this);

        // Load config first (needed for world regeneration check)
        Config.saveDefaultConfig();
        Config.load();

        // Regenerate worlds if enabled in config
        if (WorldManager.shouldRegenerateWorlds()) {
            WorldManager.regenerateWorlds();
        }

        // Register events
        getPluginManager().registerEvents(new BasicListener(), this);
        getPluginManager().registerEvents(new AddonMonitorListener(), this);
        getPluginManager().registerEvents(new MotdListener(), this);
        getPluginManager().registerEvents(new top.lqsnow.blockracing.toolkit.menu.MenuListener(), this);

        // Register commands (use helper to avoid NPE if server/another plugin owns the command)
        registerCommand("debug", new Debug());
        registerCommand("menu", new Menu());
        registerCommand("waypoint", new WayPoint());
        registerCommand("locatestructure", new LocateStructure());
        registerCommand("locatebiome", new LocateBiome());
        registerCommand("restartgame", new Restart());
        registerCommand("tp", new Teleport());
        registerCommand("block", new GetBlock());
        registerCommand("breload", new top.lqsnow.blockracing.commands.Reload());
        registerCommand("randomteam", new RandomTeam());
        registerCommand("forcestart", new ForceStart());
        registerCommand("shout", new Shout());
        Language language = new Language();
        registerCommand("language", language);

        // Set tab completers where applicable
        setTabCompleterIfPossible("debug", new Debug());
        setTabCompleterIfPossible("menu", new Menu());
        setTabCompleterIfPossible("locatestructure", new LocateStructure());
        setTabCompleterIfPossible("locatebiome", new LocateBiome());
        setTabCompleterIfPossible("block", new GetBlock());
        setTabCompleterIfPossible("language", language);

        // Save resources
        saveIfAbsent(
                "EasyBlocks.txt",
                "MediumBlocks.txt",
                "HardBlocks.txt",
                "DyedBlocks.txt",
                "EndBlocks.txt",
                "NetherBlocks.txt"
        );
        // Always overwrite managed language/translation resources (kept in sync with plugin version)
        saveAlways("minecraftlang/zh_cn.json", "minecraftlang/en_us.json");

        // Re-check addon on the first server tick after startup (other plugins are fully enabled by then)
        foliaLib.getScheduler().runLater(() -> {
            if (Setting.refreshAddonAvailability()) {
                saveIfAbsent("AddonBlocks.txt");
                Setting.setEnableAddonBlock(Config.ADDON_BLOCK.getBoolean());
            }
        }, 1L);


        // Load managers (Config already loaded earlier)
        Message.saveDefaultConfig();
        Message.load();
        LanguageManager.load();
        Setting.getSettings();
        Game.initChest();
        foliaLib.getScheduler().runNextTick(task -> {
            Scoreboard.createScoreboard();
            Team.createTeam();
            new Block();
            Block.checkBlock();
            Block.refreshAvailableBlocksAndClampAmount();
            boolean recoveredGame = GameProgressStore.load();
            if (recoveredGame) {
                Game.resumeRecoveredGame();
                Scoreboard.setInGameScoreboard();
            } else {
                Scoreboard.setPreGameScoreboard();
                Game.startPreGameLoop();
            }
            GameProgressStore.startAutosave();
            Bukkit.getOnlinePlayers().forEach(Scoreboard::showScoreboard);
            Motd.refresh();
            registerVoicechatIntegration();
        });

        // Init world settings
        foliaLib.getScheduler().runLater(() -> {
            World world = Bukkit.getWorlds().get(0);
            world.setDifficulty(Difficulty.PEACEFUL);
            Bukkit.setSpawnRadius(10);
            for (World w : Bukkit.getWorlds()) {
                w.setGameRule(GameRules.KEEP_INVENTORY, true);
                w.setGameRule(GameRules.LOCATOR_BAR, false);
                // Avoid chunk access on Folia global scheduler thread.
                int spawnY = Math.max(w.getMinHeight() + 1, w.getSpawnLocation().getBlockY());
                w.setSpawnLocation(0, spawnY, 0);
            }
            world.setTime(1000);
            world.getWorldBorder().setCenter(world.getSpawnLocation());
            world.getWorldBorder().setSize(32);
        }, 5);

        // Complete
        Bukkit.getLogger().info("[BlockRacing] Load Complete!");
    }

    @Override
    public void onDisable() {
        if (foliaLib != null) {
            foliaLib.getScheduler().cancelAllTasks();
        }
        if (Game.getCurrentGameState() == Game.GameState.INGAME) {
            GameProgressStore.saveNow();
        } else {
            GameProgressStore.clear();
        }
        LanguageManager.shutdown();
        Config.saveConfig();
    }

    private void saveIfAbsent(String... paths) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        for (String path : paths) {
            java.io.File out = new java.io.File(getDataFolder(), path);
            java.io.File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!out.exists()) {
                saveResource(path, false);
            }
        }
    }

    private void saveAlways(String... paths) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        for (String path : paths) {
            java.io.File out = new java.io.File(getDataFolder(), path);
            java.io.File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            saveResource(path, true);
        }
    }

    // Helper: safely register a command (avoid NPE if command is not owned by this plugin)
    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand cmd = this.getCommand(name);
        if (cmd == null) {
            getLogger().warning("Command '" + name + "' not found in plugin.yml or is already owned by the server/another plugin. Skipping registration.");
            return;
        }
        cmd.setExecutor(executor);
    }

    // Helper: set tab completer when possible
    private void setTabCompleterIfPossible(String name, TabCompleter completer) {
        PluginCommand cmd = this.getCommand(name);
        if (cmd == null) return;
        cmd.setTabCompleter(completer);
    }

    private void registerVoicechatIntegration() {
        if (getServer().getPluginManager().getPlugin("voicechat") == null) {
            return;
        }
        BlockRacingVoicechatPlugin.register(this);
    }

    private World getPrimaryWorld() {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No overworld is loaded"));
    }
}
