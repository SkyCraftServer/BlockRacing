package top.lqsnow.blockracing;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.mineacademy.fo.platform.BukkitPlugin;
import top.lqsnow.blockracing.commands.*;
import top.lqsnow.blockracing.listeners.BasicListener;
import top.lqsnow.blockracing.listeners.AddonMonitorListener;
import top.lqsnow.blockracing.listeners.MotdListener;
import top.lqsnow.blockracing.managers.*;
import top.lqsnow.blockracing.voicechat.BlockRacingVoicechatPlugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;

import static org.bukkit.Bukkit.getPluginManager;


public class Main extends BukkitPlugin {
    @Getter
    private static Main instance;

    @Override
    protected void onPluginStart() {
        instance = this;

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

        // Set tab completers where applicable
        setTabCompleterIfPossible("debug", new Debug());
        setTabCompleterIfPossible("menu", new Menu());
        setTabCompleterIfPossible("locatestructure", new LocateStructure());
        setTabCompleterIfPossible("locatebiome", new LocateBiome());
        setTabCompleterIfPossible("block", new GetBlock());

        // Save resources
        saveIfAbsent(
                "EasyBlocks.txt",
                "MediumBlocks.txt",
                "HardBlocks.txt",
                "DyedBlocks.txt",
                "EndBlocks.txt",
                "NetherBlocks.txt",
                "minecraftlang/zh_cn.json",
                "minecraftlang/en_us.json"
        );

        // Re-check addon on the first server tick after startup (other plugins are fully enabled by then)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Setting.refreshAddonAvailability()) {
                saveIfAbsent("AddonBlocks.txt");
                Setting.setEnableAddonBlock(Config.ADDON_BLOCK.getBoolean());
            }
        }, 1L);


        // Load managers (Config already loaded earlier)
        Message.saveDefaultConfig();
        Message.load();
        Setting.getSettings();
        Game.initChest();
        Team.createTeam();
        Scoreboard.createScoreboard();
        Scoreboard.setPreGameScoreboard();
        Motd.refresh();
        registerVoicechatIntegration();
        new Block();
        // Validate blocks at plugin startup (detect missing/invalid materials in files)
        boolean ok = top.lqsnow.blockracing.managers.Block.checkBlock();
        if (ok) {
            Bukkit.getLogger().info("[BlockRacing] Block file check passed.");
        } else {
            Bukkit.getLogger().warning("[BlockRacing] Block file check failed. Check console for details.");
        }
        new Game.runPer2Tick().runTaskTimer(this, 0L, 2L);

        // Init world settings
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            World world = Bukkit.getWorlds().get(0);
            world.setDifficulty(Difficulty.PEACEFUL);
            // overworld
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.LOCATOR_BAR, false);
            // nether
            Bukkit.getWorlds().get(1).setGameRule(GameRule.KEEP_INVENTORY, true);
            Bukkit.getWorlds().get(1).setGameRule(GameRule.LOCATOR_BAR, false);
            // end
            Bukkit.getWorlds().get(2).setGameRule(GameRule.KEEP_INVENTORY, true);
            Bukkit.getWorlds().get(2).setGameRule(GameRule.LOCATOR_BAR, false);
            world.setTime(1000);
        }, 5);

        // Set world border
        World world = Bukkit.getWorlds().get(0);
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(32);

        // Complete
        Bukkit.getLogger().info("[BlockRacing] Load Complete!");
    }

    @Override
    protected void onPluginStop() {
        super.onPluginStop();
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
                saveResource(path, false); // 只在缺失时复制，避免 WARNING
            }
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
}
