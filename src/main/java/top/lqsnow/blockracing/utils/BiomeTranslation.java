package top.lqsnow.blockracing.utils;

import org.bukkit.block.Biome;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Message;

import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;

public class BiomeTranslation {

    public static String getValue(Biome biome) {
        if (biome == null) return "";

        try {
            String key = "biome.minecraft." + biome.name().toLowerCase();

            File file = resolveLangFile();
            FileReader reader = new FileReader(file);
            JSONParser parser = new JSONParser();
            Object object = parser.parse(reader);
            JSONObject jsonObject = (JSONObject) object;

            Object value = jsonObject.get(key);
            if (value != null) return value.toString();
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.FINE, "[BlockRacing] Error getting biome translation", e);
        }

        // Fallback: make a user-friendly string from enum name
        String nice = biome.name().toLowerCase().replace('_', ' ');
        // Capitalize first letter
        if (nice.length() > 0) {
            nice = nice.substring(0, 1).toUpperCase() + nice.substring(1);
        }
        return nice;
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
