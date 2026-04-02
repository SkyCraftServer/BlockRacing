package top.lqsnow.blockracing.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Scoreboard;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.utils.ColorUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class ScoreboardFoliaIntegration {
    private static final String BOARD_NAME = "blockracing";
    private static final String BOARD_FILE_NAME = BOARD_NAME + ".yml";
    private static boolean expansionRegistered = false;

    private ScoreboardFoliaIntegration() {
    }

    public static boolean install() {
        Plugin scoreboardPlugin = findScoreboardPlugin();
        if (scoreboardPlugin == null) {
            return false;
        }

        if (!writeBoardFile(scoreboardPlugin.getDataFolder())) {
            return false;
        }

        return registerExpansion();
    }

    public static String resolvePlaceholder(String params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        if ("title".equalsIgnoreCase(params)) {
            return title();
        }

        if (params.startsWith("line_")) {
            try {
                int slot = Integer.parseInt(params.substring(5));
                return line(slot);
            } catch (NumberFormatException ignored) {
                return "";
            }
        }

        return switch (params.toLowerCase()) {
            case "mode" -> modeLine();
            case "mode_detail" -> modeDetailLine();
            case "brand" -> brandLine();
            default -> "";
        };
    }

    private static Plugin findScoreboardPlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Scoreboard");
        if (plugin != null) {
            return plugin;
        }
        return Bukkit.getPluginManager().getPlugin("Scoreboard-Folia");
    }

    private static boolean writeBoardFile(File scoreboardDataFolder) {
        File boardsFolder = new File(scoreboardDataFolder, "boards");
        if (!boardsFolder.exists() && !boardsFolder.mkdirs()) {
            Main.getInstance().getLogger().warning("Failed to create Scoreboard boards folder: " + boardsFolder.getAbsolutePath());
            return false;
        }

        File boardFile = new File(boardsFolder, BOARD_FILE_NAME);
        String yaml = buildBoardYaml();
        try {
            Files.writeString(boardFile.toPath(), yaml, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ex) {
            Main.getInstance().getLogger().warning("Failed to write Scoreboard board file: " + boardFile.getAbsolutePath());
            ex.printStackTrace();
            return false;
        }
    }

    private static String buildBoardYaml() {
        StringBuilder builder = new StringBuilder();
        builder.append("title:\n");
        builder.append("  interval: 20\n");
        builder.append("  list:\n");
        builder.append("    - '%blockracing_title%'\n\n");
        builder.append("lines:\n");
        for (int slot = 15; slot >= 1; slot--) {
            builder.append("  line-").append(slot).append(":\n");
            builder.append("    interval: 10\n");
            builder.append("    list:\n");
            builder.append("      - '%blockracing_line_").append(slot).append("%'\n");
        }
        return builder.toString();
    }

    private static boolean registerExpansion() {
        if (expansionRegistered) {
            return true;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Main.getInstance().getLogger().warning("PlaceholderAPI is not installed. The Scoreboard-Folia board will not receive BlockRacing placeholders.");
            return false;
        }

        expansionRegistered = new BlockRacingPlaceholderExpansion().register();
        return expansionRegistered;
    }

    private static String title() {
        String value;
        if (Game.getCurrentGameState().equals(Game.GameState.PREGAME)) {
            value = Message.SCOREBOARD_PREGAME_TITLE.getMiniMessage();
        } else if (Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
            value = Message.SCOREBOARD_INGAME_TITLE.getMiniMessage();
        } else {
            value = Message.SCOREBOARD_END_TITLE.getMiniMessage();
            if (value == null || value.isEmpty()) {
                value = Message.SCOREBOARD_INGAME_TITLE.getMiniMessage();
            }
        }
        return ColorUtil.t(value);
    }

    private static String line(int slot) {
        return switch (Game.getCurrentGameState()) {
            case PREGAME -> preGameLine(slot);
            case INGAME -> inGameLine(slot);
            case END -> endGameLine(slot);
        };
    }

    private static String preGameLine(int slot) {
        if (slot == 1) {
            return brandLine();
        }
        if (slot < 2 || slot > 11) {
            return "";
        }

        Message message;
        try {
            message = Message.valueOf("SCOREBOARD_PREGAME_SLOT" + slot);
        } catch (IllegalArgumentException ex) {
            return "";
        }

        String template = message.getMiniMessage();
        if (template == null || template.isEmpty()) {
            return "";
        }

        String displayedGameMode = resolveDisplayedGameModeMini();
        boolean timeMode = Setting.getCurrentGameMode().equals(Setting.GameMode.TIME);
        int minutes = Math.max(1, Setting.getTimeModeDurationSeconds() / 60);

        String blocks;
        if (Setting.isNetherMode()) {
            blocks = Message.SCOREBOARD_BLOCKS_NETHER.getMiniMessage();
        } else {
            blocks = String.join(" ", List.of(
                    Message.SCOREBOARD_BLOCKS_EASY.getMiniMessage(),
                    Setting.isEnableMediumBlock() ? Message.SCOREBOARD_BLOCKS_MEDIUM.getMiniMessage() : "",
                    Setting.isEnableHardBlock() ? Message.SCOREBOARD_BLOCKS_HARD.getMiniMessage() : "",
                    Setting.isEnableDyedBlock() ? Message.SCOREBOARD_BLOCKS_DYED.getMiniMessage() : "",
                    Setting.isEnableEndBlock() ? Message.SCOREBOARD_BLOCKS_END.getMiniMessage() : "",
                    Setting.isAddonAvailable() && Setting.isEnableAddonBlock() ? Message.SCOREBOARD_BLOCKS_ADDON.getMiniMessage() : ""
            )).trim().replaceAll("\\s+", " ");
        }

        if (timeMode && slot == 5) {
            String timeTemplate = Message.SCOREBOARD_PREGAME_TIME_MODE_INFO.getMiniMessage();
            return ColorUtil.t(applyPlaceholders(timeTemplate, "%minutes%", String.valueOf(minutes)));
        }

        String formattedMessage = applyPlaceholders(template,
                "%game_mode%", displayedGameMode,
                "%block_amount%", String.valueOf(Setting.getBlockAmount()),
                "%minutes%", String.valueOf(minutes),
                "%blocks%", blocks);
        return ColorUtil.t(formattedMessage);
    }

    private static String inGameLine(int slot) {
        if (slot == 1) {
            return brandLine();
        }
        if (slot == 14) {
            return modeLine();
        }
        if (slot == 13) {
            return modeDetailLine();
        }

        if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
            if (slot == 12) {
                return buildTeamScoreLine(true, false);
            }
            if (slot == 11) {
                return buildTeamScoreLine(false, false);
            }
            if (slot == 10) {
                return ColorUtil.t(Message.SCOREBOARD_DIVIDING_LINE.getMiniMessage());
            }
            if (slot >= 6 && slot <= 9) {
                return getBlockEntry("red", 9 - slot);
            }
            return "";
        }

        if (slot == 12) {
            return buildTeamScoreLine(true, false);
        }
        if (slot >= 8 && slot <= 11) {
            return getBlockEntry("red", 11 - slot);
        }
        if (slot == 7) {
            return ColorUtil.t(Message.SCOREBOARD_DIVIDING_LINE.getMiniMessage());
        }
        if (slot == 6) {
            return buildTeamScoreLine(false, false);
        }
        if (slot >= 2 && slot <= 5) {
            return getBlockEntry("blue", 5 - slot);
        }
        return "";
    }

    private static String endGameLine(int slot) {
        if (slot == 1) {
            return brandLine();
        }
        if (slot == 6) {
            return modeLine();
        }
        if (slot == 5) {
            String value = Message.SCOREBOARD_END_STATUS.getMiniMessage();
            return ColorUtil.t(value == null ? "" : value);
        }
        if (slot == 4) {
            return determineWinnerLine();
        }
        if (slot == 3) {
            return buildTeamScoreLine(true, true);
        }
        if (slot == 2) {
            return buildTeamScoreLine(false, true);
        }
        return "";
    }

    private static String getBlockEntry(String team, int index) {
        List<String> blocks = Game.getCurrentBlocks(team);
        if (index < 0 || index >= blocks.size()) {
            return "";
        }
        return Scoreboard.getBlockDisplay(blocks.get(index));
    }

    private static String resolveDisplayedGameModeMini() {
        return resolveDisplayedGameModeMini(true);
    }

    private static String resolveDisplayedGameModeMini(boolean includeOptionalModes) {
        String base;
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
            base = Message.SCOREBOARD_MODE_NORMAL.getMiniMessage();
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING)) {
            base = Message.SCOREBOARD_MODE_RACING.getMiniMessage();
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
            base = Message.SCOREBOARD_MODE_CONTEST.getMiniMessage();
        } else {
            base = Message.SCOREBOARD_MODE_TIME.getMiniMessage();
        }
        if (Setting.isNetherMode()) {
            base = base + " + " + Message.SCOREBOARD_MODE_NETHER.getMiniMessage();
        }
        if (includeOptionalModes && Setting.isSpeedMode()) {
            base = base + " + " + Message.SCOREBOARD_MODE_SPEED.getMiniMessage();
        }
        return base;
    }

    private static String modeLine() {
        String template = Message.SCOREBOARD_COMMON_MODE_LINE.getMiniMessage();
        if (template == null || template.isEmpty()) {
            template = "<aqua>Mode:</aqua> <yellow>%mode%</yellow>";
        }
        boolean includeSpeed = !Game.getCurrentGameState().equals(Game.GameState.INGAME);
        return ColorUtil.t(applyPlaceholders(template, "%mode%", resolveDisplayedGameModeMini(includeSpeed)));
    }

    private static String modeDetailLine() {
        if (Game.isTimeModeActive()) {
            if (Game.isTimeModeOvertime()) {
                String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_OVERTIME.getMiniMessage();
                if (template == null || template.isEmpty()) {
                    template = "<red>Overtime</red>";
                }
                return ColorUtil.t(template);
            }
            String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_TIME_LEFT.getMiniMessage();
            if (template == null || template.isEmpty()) {
                template = "<gold>Time Left: <white>%time%</white></gold>";
            }
            return ColorUtil.t(applyPlaceholders(template, "%time%", Game.getFormattedTimeModeRemaining()));
        }

        String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_DEFAULT.getMiniMessage();
        return ColorUtil.t(template == null ? "" : template);
    }

    private static String brandLine() {
        String template = Message.SCOREBOARD_COMMON_BRAND.getMiniMessage();
        if (template == null || template.isEmpty()) {
            template = "<gray>BlockRacing</gray> <yellow>v%version%</yellow>";
        }
        String addonSuffix = Setting.isAddonAvailable()
                ? " <gradient:#b7d9af:#8bd9c0:#5fd9d1:#32d9e2:#06d9f3>ADDON</gradient>"
                : "";
        return ColorUtil.t(applyPlaceholders(template + addonSuffix, "%version%", Main.getInstance().getDescription().getVersion()));
    }

    private static String determineWinnerLine() {
        String fallbackRed = "<red>Red team wins!</red>";
        String fallbackBlue = "<blue>Blue team wins!</blue>";
        String fallbackDraw = "<yellow>Draw</yellow>";
        if (Game.redTeamScore > Game.blueTeamScore) {
            String value = Message.SCOREBOARD_END_WINNER_RED.getMiniMessage();
            return ColorUtil.t((value == null || value.isEmpty()) ? fallbackRed : value);
        } else if (Game.blueTeamScore > Game.redTeamScore) {
            String value = Message.SCOREBOARD_END_WINNER_BLUE.getMiniMessage();
            return ColorUtil.t((value == null || value.isEmpty()) ? fallbackBlue : value);
        }
        String value = Message.SCOREBOARD_END_WINNER_DRAW.getMiniMessage();
        return ColorUtil.t((value == null || value.isEmpty()) ? fallbackDraw : value);
    }

    private static String buildTeamScoreLine(boolean red, boolean endPhase) {
        boolean timeMode = Game.isTimeModeActive();

        String template;
        if (endPhase) {
            if (timeMode) {
                template = red ? Message.SCOREBOARD_END_RED_SCORE_TIME.getMiniMessage()
                        : Message.SCOREBOARD_END_BLUE_SCORE_TIME.getMiniMessage();
            } else {
                template = red ? Message.SCOREBOARD_END_RED_SCORE.getMiniMessage()
                        : Message.SCOREBOARD_END_BLUE_SCORE.getMiniMessage();
            }
        } else {
            if (timeMode) {
                template = red ? Message.SCOREBOARD_RED_SCORE_TIME.getMiniMessage()
                        : Message.SCOREBOARD_BLUE_SCORE_TIME.getMiniMessage();
            } else {
                template = red ? Message.SCOREBOARD_RED_SCORE.getMiniMessage()
                        : Message.SCOREBOARD_BLUE_SCORE.getMiniMessage();
            }
        }

        if (template == null || template.isEmpty()) {
            template = red ? Message.SCOREBOARD_RED_SCORE.getMiniMessage() : Message.SCOREBOARD_BLUE_SCORE.getMiniMessage();
        }

        String score = red ? String.valueOf(Game.redTeamScore) : String.valueOf(Game.blueTeamScore);
        String current = red ? String.valueOf(Game.redTeamCurrentBlockAmount) : String.valueOf(Game.blueTeamCurrentBlockAmount);
        String total = red ? String.valueOf(Game.redTeamTotalBlockAmount) : String.valueOf(Game.blueTeamTotalBlockAmount);

        return ColorUtil.t(applyPlaceholders(template,
                "%score%", score,
                "%current_block%", current,
                "%total_block%", total));
    }

    private static String applyPlaceholders(String input, String... replacements) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String result = input;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1] == null ? "" : replacements[i + 1];
            result = result.replace(key, value);
        }
        return result;
    }
}