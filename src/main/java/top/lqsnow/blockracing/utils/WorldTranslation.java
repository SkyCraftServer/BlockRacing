package top.lqsnow.blockracing.utils;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Message;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;

public class WorldTranslation {

    public static String getValue(World world) {
        if (world == null) return "";

        try {
            Environment env = world.getEnvironment();
            String key;
            switch (env) {
                case NETHER:
                    key = "world_nether";
                    break;
                case THE_END:
                    key = "world_the_end";
                    break;
                case NORMAL:
                default:
                    key = "world";
                    break;
            }

            File file = resolveLangFile();
            FileReader reader = new FileReader(file);
            JSONParser parser = new JSONParser();
            Object object = parser.parse(reader);
            JSONObject jsonObject = (JSONObject) object;

            Object value = jsonObject.get(key);
            if (value != null) return value.toString();
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.FINE, "[BlockRacing] Error getting world translation", e);
        }

        // Fallback to world folder name
        return world.getName();
    }

    private static File resolveLangFile() {
        String langCode = Message.getLanguageCode();
        File dir = new File(Main.getInstance().getDataFolder(), "minecraftlang");
        File langFile = new File(dir, langCode + ".json");

        if (!langFile.exists()) {
            langFile = new File(dir, Message.getDefaultLanguageCode() + ".json");
        }

        return langFile;
    }
}
