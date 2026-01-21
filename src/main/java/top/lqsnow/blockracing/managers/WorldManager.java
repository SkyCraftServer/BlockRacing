package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;

public class WorldManager {
    private static final Logger logger = Bukkit.getLogger();

    /**
     * 在插件启动时重新生成三个世界（主世界、下界、末地）
     */
    public static void regenerateWorlds() {
        logger.info("[BlockRacing] Regenerate worlds...");
        
        // 获取世界名称
        String overworldName = Bukkit.getWorlds().get(0).getName();
        String netherName = overworldName + "_nether";
        String endName = overworldName + "_the_end";
        
        // 先卸载次要世界（下界和末地）
        unloadWorld(netherName);
        unloadWorld(endName);
        
        // 删除次要世界文件夹
        deleteWorldFolder(netherName);
        deleteWorldFolder(endName);
        
        // 处理主世界：先卸载再删除
        unloadWorld(overworldName);
        deleteWorldFolder(overworldName);
        
        // 重新创建主世界（必须先创建主世界）
        logger.info("[BlockRacing] Creating overworld...");
        World overworld = new WorldCreator(overworldName)
                .environment(World.Environment.NORMAL)
                .type(WorldType.NORMAL)
                .generateStructures(true)
                .createWorld();
        
        if (overworld != null) {
            logger.info("[BlockRacing] Overworld created successfully!");
        } else {
            logger.severe("[BlockRacing] Failed to create overworld!");
            return;
        }
        
        // 创建下界
        logger.info("[BlockRacing] Creating nether...");
        World nether = new WorldCreator(netherName)
                .environment(World.Environment.NETHER)
                .generateStructures(true)
                .createWorld();
        
        if (nether != null) {
            logger.info("[BlockRacing] Nether created successfully!");
        } else {
            logger.warning("[BlockRacing] Failed to create nether!");
        }
        
        // 创建末地
        logger.info("[BlockRacing] Creating end...");
        World end = new WorldCreator(endName)
                .environment(World.Environment.THE_END)
                .generateStructures(true)
                .createWorld();
        
        if (end != null) {
            logger.info("[BlockRacing] End created successfully!");
        } else {
            logger.warning("[BlockRacing] Failed to create end!");
        }
        
        logger.info("[BlockRacing] Worlds regeneration complete!");
    }

    /**
     * 卸载指定世界
     */
    private static void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            logger.info("[BlockRacing] Unload world: " + worldName);
            // 保存并卸载世界
            world.save();
            Bukkit.unloadWorld(world, false); // false = 不保存世界数据
        }
    }

    /**
     * 删除世界文件夹（使用 Java NIO）
     */
    private static void deleteWorldFolder(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists() && worldFolder.isDirectory()) {
            logger.info("[BlockRacing] Deleting world folder: " + worldName);
            try {
                // 使用 Java NIO 递归删除文件夹
                Files.walk(worldFolder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                logger.info("[BlockRacing] Successfully deleted world folder: " + worldName);
            } catch (IOException e) {
                logger.severe("[BlockRacing] Failed to delete world folder: " + worldName);
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查是否需要重新生成世界
     * 可以通过配置文件控制是否启用此功能
     */
    public static boolean shouldRegenerateWorlds() {
        // 从配置文件读取
        File configFile = new File(top.lqsnow.blockracing.Main.getInstance().getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return false;
        }
        org.bukkit.configuration.file.FileConfiguration config = 
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        return config.getBoolean("regenerate-worlds-on-startup", false);
    }
}
