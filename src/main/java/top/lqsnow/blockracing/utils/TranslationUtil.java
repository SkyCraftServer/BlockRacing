package top.lqsnow.blockracing.utils;

import org.bukkit.Material;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Message;

import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import java.util.logging.Level;

public class TranslationUtil {
    public static String getValue(String block) {
        try {
            String key = Objects.requireNonNull(Material.getMaterial(block)).getTranslationKey();
            if (block.equalsIgnoreCase("NETHER_WART")) {
                key = "block.minecraft.nether_wart";
            }
            File file = resolveLangFile();
            FileReader reader = new FileReader(file);
            JSONParser parser = new JSONParser();
            Object object = parser.parse(reader);
            JSONObject jsonObject = (JSONObject) object;
            return (String) jsonObject.get(key);
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "[BlockRacing] Error getting value of blocks!", e);
        }
        return null;
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
