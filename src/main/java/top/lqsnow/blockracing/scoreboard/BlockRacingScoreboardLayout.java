package top.lqsnow.blockracing.scoreboard;

import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Block;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.utils.MiniMessageUtil;
import top.lqsnow.blockracing.utils.TranslationUtil;

import java.util.ArrayList;
import java.util.List;

public final class BlockRacingScoreboardLayout {
    private BlockRacingScoreboardLayout() {
    }

    public static String resolvePlaceholder(String params) {
        if (params == null || params.isBlank()) {
            return null;
        }

        String key = params.toLowerCase();
        if (key.equals("title")) {
            return applyGlobalPlaceholders(currentTitle());
        }
        if (key.equals("scoreboard_line_count") || key.equals("line_count")) {
            return String.valueOf(currentLines().size());
        }
        if (key.startsWith("line_")) {
            int index = parseLineIndex(key.substring(5));
            if (index > 0) {
                List<String> lines = currentLines();
                if (index <= lines.size()) {
                    return applyGlobalPlaceholders(lines.get(index - 1));
                }
            }
        }

        return null;
    }

    private static int parseLineIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String applyGlobalPlaceholders(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replace("%version%", Main.getInstance().getDescription().getVersion());
    }

    private static String currentTitle() {
        return switch (Game.getCurrentGameState()) {
            case PREGAME -> legacyOrFallback(Message.SCOREBOARD_PREGAME_TITLE.getString(), "<gold>BlockRacing</gold>");
            case INGAME -> legacyOrFallback(Message.SCOREBOARD_INGAME_TITLE.getString(), "<gold>BlockRacing</gold>");
            case END -> legacyOrFallback(Message.SCOREBOARD_END_TITLE.getString(), Message.SCOREBOARD_INGAME_TITLE.getMiniMessage());
        };
    }

    private static List<String> currentLines() {
        return switch (Game.getCurrentGameState()) {
            case PREGAME -> buildPreGameLines();
            case INGAME -> Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST) ? buildContestLines() : buildInGameLines();
            case END -> buildEndLines();
        };
    }

    private static List<String> buildPreGameLines() {
        List<String> lines = new ArrayList<>();
        String displayedGameMode = resolveDisplayedGameMode(true);
        boolean timeMode = Setting.getCurrentGameMode().equals(Setting.GameMode.TIME);
        int minutes = Math.max(1, Setting.getTimeModeDurationSeconds() / 60);

        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT11.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT10.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT9.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT8.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT7.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT6.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));

        String slot5 = Message.SCOREBOARD_PREGAME_SLOT5.getString();
        if (timeMode) {
            slot5 = Message.SCOREBOARD_PREGAME_TIME_MODE_INFO.getString();
        }
        addIfNotBlank(lines, applyPlaceholders(slot5, "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));

        String blockSummary = buildBlockSummary();
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT4.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", blockSummary));
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT3.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", blockSummary));
        addIfNotBlank(lines, applyPlaceholders(Message.SCOREBOARD_PREGAME_SLOT2.getString(), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", blockSummary));

        // Keep slot1 reserved for brandLine to match the native scoreboard behavior.
        addIfNotBlank(lines, brandLine());
        return lines;
    }

    private static List<String> buildInGameLines() {
        List<String> lines = new ArrayList<>();
        addIfNotBlank(lines, modeLine());
        addIfNotBlank(lines, modeDetailLine());
        addIfNotBlank(lines, buildTeamScoreLine(true, false));
        addBlockLines(lines, Game.getCurrentBlocks("red"));
        addIfNotBlank(lines, legacyOrFallback(Message.SCOREBOARD_DIVIDING_LINE.getString(), "&7&m----------------"));
        addIfNotBlank(lines, buildTeamScoreLine(false, false));
        addBlockLines(lines, Game.getCurrentBlocks("blue"));
        addIfNotBlank(lines, brandLine());
        return lines;
    }

    private static List<String> buildContestLines() {
        List<String> lines = new ArrayList<>();
        addIfNotBlank(lines, modeLine());
        addIfNotBlank(lines, modeDetailLine());
        addIfNotBlank(lines, buildTeamScoreLine(true, false));
        addIfNotBlank(lines, buildTeamScoreLine(false, false));
        addIfNotBlank(lines, legacyOrFallback(Message.SCOREBOARD_DIVIDING_LINE.getString(), "&7&m----------------"));
        addBlockLines(lines, Game.getCurrentBlocks("red"));
        addIfNotBlank(lines, brandLine());
        return lines;
    }

    private static List<String> buildEndLines() {
        List<String> lines = new ArrayList<>();
        addIfNotBlank(lines, modeLine());
        addIfNotBlank(lines, legacyOrFallback(Message.SCOREBOARD_END_STATUS.getString(), "<gray>Game ended</gray>"));
        addIfNotBlank(lines, determineWinnerLine());
        addIfNotBlank(lines, buildTeamScoreLine(true, true));
        addIfNotBlank(lines, buildTeamScoreLine(false, true));
        addIfNotBlank(lines, brandLine());
        return lines;
    }

    private static void addBlockLines(List<String> lines, List<String> blocks) {
        for (String block : blocks) {
            String display = getBlockDisplay(block);
            if (!display.isBlank()) {
                lines.add(display);
            }
        }
    }

    private static void addIfNotBlank(List<String> lines, String text) {
        if (text != null && !text.isBlank()) {
            lines.add(text);
        }
    }

    private static String buildBlockList() {
        List<String> blocks = Block.blocks;
        if (blocks == null || blocks.isEmpty()) {
            blocks = Game.getCurrentBlocks("red");
        }
        return String.join(" ", blocks);
    }

    private static String buildBlockSummary() {
        List<String> parts = new ArrayList<>();
        if (Setting.isNetherMode()) {
            parts.add(Message.SCOREBOARD_BLOCKS_NETHER.getString());
            return String.join(" ", parts);
        }

        parts.add(Message.SCOREBOARD_BLOCKS_EASY.getString());
        if (Setting.isEnableMediumBlock()) {
            parts.add(Message.SCOREBOARD_BLOCKS_MEDIUM.getString());
        }
        if (Setting.isEnableHardBlock()) {
            parts.add(Message.SCOREBOARD_BLOCKS_HARD.getString());
        }
        if (Setting.isEnableDyedBlock()) {
            parts.add(Message.SCOREBOARD_BLOCKS_DYED.getString());
        }
        if (Setting.isEnableEndBlock()) {
            parts.add(Message.SCOREBOARD_BLOCKS_END.getString());
        }
        if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock()) {
            parts.add(Message.SCOREBOARD_BLOCKS_ADDON.getString());
        }

        return String.join(" ", parts);
    }

    private static String getBlockDisplay(String block) {
        String difficulty;
        if (Setting.isNetherMode() && listContains(Block.netherBlocks, block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_NETHER.getString();
        } else if (listContains(Block.easyBlocks, block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_EASY.getString();
        } else if (listContains(Block.mediumBlocks, block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_MEDIUM.getString();
        } else if (listContains(Block.hardBlocks, block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_HARD.getString();
        } else if (listContains(Block.dyedBlocks, block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_DYED.getString();
        } else if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock() && listContains(Block.addonBlocks, block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_ADDON.getString();
        } else if (listContains(Block.endBlocks, block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_END.getString();
        } else {
            return legacyOrFallback(TranslationUtil.getValue(block), block);
        }

        String blockName = TranslationUtil.getValue(block);
        String template = Message.SCOREBOARD_BLOCK_FORMAT.getString();
        if (template == null || template.isBlank()) {
            template = "&7[%difficulty%] &f%block%";
        }
        return applyPlaceholders(template, "%difficulty%", difficulty, "%block%", blockName);
    }

    private static boolean listContains(List<String> list, String value) {
        return list != null && value != null && list.contains(value);
    }

    private static String resolveDisplayedGameMode(boolean includeOptionalModes) {
        String base;
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
            base = Message.SCOREBOARD_MODE_NORMAL.getString();
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING)) {
            base = Message.SCOREBOARD_MODE_RACING.getString();
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
            base = Message.SCOREBOARD_MODE_CONTEST.getString();
        } else {
            base = Message.SCOREBOARD_MODE_TIME.getString();
        }

        if (Setting.isNetherMode()) {
            base = appendMode(base, Message.SCOREBOARD_MODE_NETHER.getString());
        }
        if (includeOptionalModes && Setting.isSpeedMode()) {
            base = appendMode(base, Message.SCOREBOARD_MODE_SPEED.getString());
        }
        return base;
    }

    private static String appendMode(String base, String extra) {
        if (base == null || base.isBlank()) {
            return extra == null ? "" : extra;
        }
        if (extra == null || extra.isBlank()) {
            return base;
        }
        return base + " + " + extra;
    }

    private static String modeLine() {
        String template = Message.SCOREBOARD_COMMON_MODE_LINE.getString();
        if (template == null || template.isBlank()) {
            template = "<aqua>Mode:</aqua> <yellow>%mode%</yellow>";
        }
        boolean includeSpeed = !Game.getCurrentGameState().equals(Game.GameState.INGAME);
        return applyPlaceholders(template, "%mode%", resolveDisplayedGameMode(includeSpeed));
    }

    private static String modeDetailLine() {
        if (Game.isTimeModeActive()) {
            if (Game.isTimeModeOvertime()) {
                String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_OVERTIME.getString();
                if (template == null || template.isBlank()) {
                    template = "<red>Overtime</red>";
                }
                return template;
            }
            String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_TIME_LEFT.getString();
            if (template == null || template.isBlank()) {
                template = "<gold>Time Left: <white>%time%</white></gold>";
            }
            return applyPlaceholders(template, "%time%", Game.getFormattedTimeModeRemaining());
        }

        String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_DEFAULT.getString();
        return template == null ? "" : template;
    }

    private static String brandLine() {
        String template = Message.SCOREBOARD_COMMON_BRAND.getString();
        if (template == null || template.isBlank()) {
            template = "<gray>BlockRacing</gray> <yellow>v%version%</yellow>";
        }
        String addonSuffix = Setting.isAddonAvailable()
                ? " <gradient:#b7d9af:#8bd9c0:#5fd9d1:#32d9e2:#06d9f3>ADDON</gradient>"
                : "";
        return applyPlaceholders(template + addonSuffix, "%version%", Main.getInstance().getDescription().getVersion());
    }

    private static String determineWinnerLine() {
        if (Game.redTeamScore > Game.blueTeamScore) {
            String value = Message.SCOREBOARD_END_WINNER_RED.getString();
            return legacyOrFallback(value, "<red>Red team wins!</red>");
        } else if (Game.blueTeamScore > Game.redTeamScore) {
            String value = Message.SCOREBOARD_END_WINNER_BLUE.getString();
            return legacyOrFallback(value, "<blue>Blue team wins!</blue>");
        }
        String value = Message.SCOREBOARD_END_WINNER_DRAW.getString();
        return legacyOrFallback(value, "<yellow>Draw</yellow>");
    }

    private static String buildTeamScoreLine(boolean red, boolean endPhase) {
        boolean timeMode = Game.isTimeModeActive();

        String template;
        if (endPhase) {
            template = timeMode
                    ? (red ? Message.SCOREBOARD_END_RED_SCORE_TIME.getString() : Message.SCOREBOARD_END_BLUE_SCORE_TIME.getString())
                    : (red ? Message.SCOREBOARD_END_RED_SCORE.getString() : Message.SCOREBOARD_END_BLUE_SCORE.getString());
        } else {
            template = timeMode
                    ? (red ? Message.SCOREBOARD_RED_SCORE_TIME.getString() : Message.SCOREBOARD_BLUE_SCORE_TIME.getString())
                    : (red ? Message.SCOREBOARD_RED_SCORE.getString() : Message.SCOREBOARD_BLUE_SCORE.getString());
        }

        if (template == null || template.isBlank()) {
            template = red ? Message.SCOREBOARD_RED_SCORE.getString() : Message.SCOREBOARD_BLUE_SCORE.getString();
        }

        String score = String.valueOf(red ? Game.redTeamScore : Game.blueTeamScore);
        String current = String.valueOf(red ? Game.redTeamCurrentBlockAmount : Game.blueTeamCurrentBlockAmount);
        String total = String.valueOf(red ? Game.redTeamTotalBlockAmount : Game.blueTeamTotalBlockAmount);

        return applyPlaceholders(template, "%score%", score, "%current_block%", current, "%total_block%", total);
    }

    private static String legacyOrFallback(String value, String fallbackMini) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return MiniMessageUtil.toLegacyString(fallbackMini == null ? "" : fallbackMini);
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