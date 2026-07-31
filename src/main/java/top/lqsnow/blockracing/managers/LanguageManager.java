package top.lqsnow.blockracing.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

import static top.lqsnow.blockracing.utils.ColorUtil.t;

public final class LanguageManager {
    public enum Preference {
        AUTO, ZH_CN, EN_US
    }

    private static YamlConfiguration chinese;
    private static YamlConfiguration english;
    private static YamlConfiguration preferences;
    private static File preferencesFile;

    private LanguageManager() {
    }

    public static void load() {
        File langDir = new File(Main.getInstance().getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        chinese = loadLanguage(new File(langDir, "zh_cn.yml"), "lang/zh_cn.yml");
        english = loadLanguage(new File(langDir, "en_us.yml"), "lang/en_us.yml");
        preferencesFile = new File(Main.getInstance().getDataFolder(), "language-preferences.yml");
        preferences = YamlConfiguration.loadConfiguration(preferencesFile);
    }

    public static void shutdown() {
        if (preferences != null && preferencesFile != null) {
            savePreferences();
        }
    }

    public static synchronized boolean registerFirstJoin(Player player) {
        String path = player.getUniqueId().toString();
        if (preferences.contains(path)) {
            return false;
        }
        preferences.set(path, Preference.AUTO.name());
        savePreferences();
        return true;
    }

    public static synchronized void setPreference(Player player, Preference preference) {
        preferences.set(player.getUniqueId().toString(), preference.name());
        savePreferences();
    }

    public static synchronized Preference getPreference(Player player) {
        String value = preferences.getString(player.getUniqueId().toString(), Preference.AUTO.name());
        try {
            return Preference.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return Preference.AUTO;
        }
    }

    public static String getString(Message message, Player player) {
        YamlConfiguration config = configuration(player);
        String value = config.getString(message.getPath());
        if (value == null) {
            config = chinese;
            value = chinese.getString(message.getPath(), message.getPath());
        }
        return t(replacePrefix(value, config));
    }

    /**
     * Gets the legacy string for a specific language (true=Chinese, false=English).
     * Used for building per-language scoreboards without a player instance.
     */
    public static String getStringForLanguage(Message message, boolean chineseLang) {
        YamlConfiguration config = chineseLang ? chinese : english;
        String value = config.getString(message.getPath());
        if (value == null) {
            config = chinese;
            value = chinese.getString(message.getPath(), message.getPath());
        }
        return t(replacePrefix(value, config));
    }

    public static List<String> getStringList(Message message, Player player) {
        YamlConfiguration config = configuration(player);
        List<String> values = config.getStringList(message.getPath());
        if (values.isEmpty()) {
            config = chinese;
            values = chinese.getStringList(message.getPath());
        }
        final YamlConfiguration prefixConfig = config;
        return values.stream().map(value -> t(replacePrefix(value, prefixConfig))).toList();
    }

    /**
     * Substitutes the %prefix% placeholder with the language-specific prefix
     * (falls back to the Chinese prefix) before color/MiniMessage conversion.
     */
    private static String replacePrefix(String value, YamlConfiguration config) {
        if (value == null || !value.contains("%prefix%")) return value;
        String prefix = config.getString("prefix", "");
        if (prefix.isEmpty()) {
            prefix = chinese.getString("prefix", "");
        }
        return value.replace("%prefix%", prefix);
    }

    public static boolean usesChinese(Player player) {
        return usesChinese(getPreference(player), player.locale());
    }

    static boolean usesChinese(Preference preference, Locale clientLocale) {
        return switch (preference) {
            case ZH_CN -> true;
            case EN_US -> false;
            case AUTO -> clientLocale.getLanguage().equalsIgnoreCase(Locale.CHINESE.getLanguage());
        };
    }

    private static YamlConfiguration configuration(Player player) {
        return usesChinese(player) ? chinese : english;
    }

    private static YamlConfiguration loadLanguage(File file, String resourcePath) {
        if (!file.exists()) {
            try {
                Main.getInstance().saveResource(resourcePath, false);
            } catch (Exception ex) {
                Main.getInstance().getLogger().log(Level.WARNING, "Unable to save language file " + resourcePath, ex);
            }
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader reader = new InputStreamReader(
                Main.getInstance().getResource(resourcePath),
                StandardCharsets.UTF_8
        )) {
            configuration.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (Exception ex) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Unable to load language " + resourcePath, ex);
        }
        return configuration;
    }

    private static void savePreferences() {
        try {
            preferences.save(preferencesFile);
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Unable to save language preferences", ex);
        }
    }
}
