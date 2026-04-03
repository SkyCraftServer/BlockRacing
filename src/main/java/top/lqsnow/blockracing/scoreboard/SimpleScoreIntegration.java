package top.lqsnow.blockracing.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import top.lqsnow.blockracing.Main;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class SimpleScoreIntegration {
    private static final String SCOREBOARD_NAME = "blockracing";
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String SCOREBOARDS_FILE_NAME = "scoreboards.yml";
    private static final String CONDITIONS_FILE_NAME = "conditions.yml";
    private static final int MAX_LAYOUT_LINES = 15;
    private static boolean expansionRegistered = false;

    private SimpleScoreIntegration() {
    }

    public static boolean install() {
        Plugin simpleScore = Bukkit.getPluginManager().getPlugin("SimpleScore");
        if (simpleScore == null) {
            return false;
        }

        if (!writeScoreboardsFile(simpleScore.getDataFolder())) {
            return false;
        }

        return registerExpansion();
    }

    private static boolean writeScoreboardsFile(File simpleScoreDataFolder) {
        if (!simpleScoreDataFolder.exists() && !simpleScoreDataFolder.mkdirs()) {
            Main.getInstance().getLogger().warning("Failed to create SimpleScore data folder: " + simpleScoreDataFolder.getAbsolutePath());
            return false;
        }

        return writeFile(new File(simpleScoreDataFolder, CONFIG_FILE_NAME), buildConfigYaml())
                && writeFile(new File(simpleScoreDataFolder, SCOREBOARDS_FILE_NAME), buildScoreboardsYaml())
                && writeFile(new File(simpleScoreDataFolder, CONDITIONS_FILE_NAME), buildConditionsYaml());
    }

    private static String buildScoreboardsYaml() {
        StringBuilder builder = new StringBuilder();
        builder.append(SCOREBOARD_NAME).append("scoreboard:\n");
        builder.append("  defaultRenderEvery: 1\n");
        builder.append("  defaultVisibleFor: 20\n");
        builder.append("  titles:\n");
        builder.append("    - '%blockracing_title%'\n");
        builder.append("  scores:\n");
        for (int lineIndex = 1; lineIndex <= MAX_LAYOUT_LINES; lineIndex++) {
            builder.append("    - score: ").append(MAX_LAYOUT_LINES - lineIndex + 1).append("\n");
            builder.append("      lines: '%blockracing_line_").append(lineIndex).append("%'\n");
            builder.append("      hideNumber: true\n");
            builder.append("      conditions: 'blockracing_line_count_").append(lineIndex).append("'\n");
        }
        return builder.toString();
    }

    private static String buildConditionsYaml() {
        StringBuilder builder = new StringBuilder();
        for (int lineCount = 1; lineCount <= MAX_LAYOUT_LINES; lineCount++) {
            builder.append("blockracing_line_count_").append(lineCount).append(":\n");
            builder.append("  type: GreaterThan\n");
            builder.append("  input: '%blockracing_scoreboard_line_count%'\n");
            builder.append("  parseInput: true\n");
            builder.append("  value: '").append(lineCount - 1).append("'\n");
            builder.append("  parseValue: false\n");
            builder.append("  ignoreCase: false\n\n");
        }
        return builder.toString().trim();
    }

    private static String buildConfigYaml() {
        return "version: 0\n"
                + "language: en\n"
                + "checkForUpdates: true\n"
                + "taskUpdateTime: 1\n"
                + "scoreboardTaskAsync: true\n"
                + "worlds:\n"
                + "  '[\\w\\s\\-]*': [ 'blockracingscoreboard' ]\n";
    }

    private static boolean writeFile(File file, String content) {
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ex) {
            Main.getInstance().getLogger().warning("Failed to write SimpleScore config file: " + file.getAbsolutePath());
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean registerExpansion() {
        if (expansionRegistered) {
            return true;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Main.getInstance().getLogger().warning("PlaceholderAPI is not installed. SimpleScore cannot receive BlockRacing placeholders.");
            return false;
        }

        expansionRegistered = new BlockRacingPlaceholderExpansion().register();
        return expansionRegistered;
    }
}