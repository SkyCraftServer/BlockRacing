package top.lqsnow.blockracing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlResourceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void yamlResourcesDoNotContainDuplicateKeys() throws Exception {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        for (Path path : List.of(
                Path.of("src/main/resources/config.yml"),
                Path.of("src/main/resources/lang/zh_cn.yml"),
                Path.of("src/main/resources/lang/en_us.yml"),
                Path.of("src/main/resources/lang/en_us_config.yml")
        )) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                yaml.load(reader);
            }
        }
    }

    @Test
    void bundledLanguagesContainTheSameMessageKeys() throws Exception {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        Set<String> chinese = loadLeafKeys(yaml, Path.of("src/main/resources/lang/zh_cn.yml"));
        Set<String> english = loadLeafKeys(yaml, Path.of("src/main/resources/lang/en_us.yml"));

        // English must be a subset of Chinese (Chinese is the primary language)
        assertTrue(chinese.containsAll(english),
                () -> "zh_cn.yml is missing keys present in en_us.yml: " + difference(english, chinese));
    }

    @Test
    void publishedConfigurationsContainTheSameKeys() throws Exception {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(options));
        Set<String> defaults = loadLeafKeys(yaml, Path.of("src/main/resources/config.yml"));
        Set<String> englishConfig = loadLeafKeys(yaml, Path.of("src/main/resources/lang/en_us_config.yml"));

        // en_us_config must be a subset of the main config
        assertTrue(defaults.containsAll(englishConfig),
                () -> "config.yml is missing keys present in en_us_config.yml: " + difference(englishConfig, defaults));
    }

    @Test
    void configurationCommentsSurviveASettingsSave() throws Exception {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.options().parseComments(true);
        configuration.load(Path.of("src/main/resources/config.yml").toFile());
        configuration.set("block-amount", 75);

        Path saved = temporaryDirectory.resolve("config.yml");
        configuration.save(saved.toFile());
        String text = Files.readString(saved, StandardCharsets.UTF_8);
        assertTrue(text.contains("# BlockRacing"));
        assertTrue(text.contains("# 一局游戏需要收集的方块总数"));
    }

    private static Set<String> difference(Set<String> a, Set<String> b) {
        Set<String> diff = new TreeSet<>(a);
        diff.removeAll(b);
        return diff;
    }

    private static Set<String> loadLeafKeys(Yaml yaml, Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Set<String> result = new TreeSet<>();
            collectLeafKeys("", yaml.load(reader), result);
            return result;
        }
    }

    private static void collectLeafKeys(String prefix, Object value, Set<String> result) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, child) -> collectLeafKeys(
                    prefix.isEmpty() ? key.toString() : prefix + "." + key,
                    child,
                    result
            ));
        } else {
            result.add(prefix);
        }
    }
}
