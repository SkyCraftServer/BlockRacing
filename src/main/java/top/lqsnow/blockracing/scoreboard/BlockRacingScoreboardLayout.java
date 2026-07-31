package top.lqsnow.blockracing.scoreboard;

import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Block;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.utils.MiniMessageUtil;
import top.lqsnow.blockracing.utils.TranslationUtil;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class BlockRacingScoreboardLayout {
    private BlockRacingScoreboardLayout() {
    }

    public static String resolvePlaceholder(String params) {
        return resolvePlaceholder(params, null);
    }

    public static String resolvePlaceholder(String params, Player player) {
        if (params == null || params.isBlank()) {
            return null;
        }

        String key = params.toLowerCase();
        if (key.equals("title")) {
            return applyGlobalPlaceholders(currentTitle(player));
        }
        if (key.equals("scoreboard_line_count") || key.equals("line_count")) {
            return String.valueOf(currentLines(player).size());
        }
        if (key.startsWith("line_")) {
            int index = parseLineIndex(key.substring(5));
            if (index > 0) {
                List<String> lines = currentLines(player);
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

    private static String currentTitle(Player player) {
        return switch (Game.getCurrentGameState()) {
            case PREGAME -> legacyOrFallback(msg(Message.SCOREBOARD_PREGAME_TITLE, player), "<gold>BlockRacing</gold>");
            case INGAME -> legacyOrFallback(msg(Message.SCOREBOARD_INGAME_TITLE, player), "<gold>BlockRacing</gold>");
            case END -> legacyOrFallback(msg(Message.SCOREBOARD_END_TITLE, player), msg(Message.SCOREBOARD_INGAME_TITLE, player));
        };
    }

    private static List<String> currentLines(Player player) {
        return switch (Game.getCurrentGameState()) {
            case PREGAME -> buildPreGameLines(player);
            case INGAME -> Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST) ? buildContestLines(player) : buildInGameLines(player);
            case END -> buildEndLines(player);
        };
    }

    private static List<String> buildPreGameLines(Player player) {
        List<String> lines = new ArrayList<>();
        String displayedGameMode = resolveDisplayedGameMode(true, player);
        boolean timeMode = Setting.getCurrentGameMode().equals(Setting.GameMode.TIME);
        int minutes = Math.max(1, Setting.getTimeModeDurationSeconds() / 60);

        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT11, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT10, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT9, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT8, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT7, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT6, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));

        String slot5 = msg(Message.SCOREBOARD_PREGAME_SLOT5, player);
        if (timeMode) {
            slot5 = msg(Message.SCOREBOARD_PREGAME_TIME_MODE_INFO, player);
        }
        addIfNotBlank(lines, applyPlaceholders(slot5, "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", buildBlockList()));

        String blockSummary = buildBlockSummary(player);
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT4, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", blockSummary));
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT3, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", blockSummary));
        addIfNotBlank(lines, applyPlaceholders(msg(Message.SCOREBOARD_PREGAME_SLOT2, player), "%game_mode%", displayedGameMode, "%block_amount%", String.valueOf(Setting.getBlockAmount()), "%minutes%", String.valueOf(minutes), "%blocks%", blockSummary));

        // Keep slot1 reserved for brandLine to match the native scoreboard behavior.
        addIfNotBlank(lines, brandLine(player));
        return lines;
    }

    private static List<String> buildInGameLines(Player player) {
        List<String> lines = new ArrayList<>();
        addIfNotBlank(lines, modeLine(player));
        addIfNotBlank(lines, modeDetailLine(player));
        addIfNotBlank(lines, buildTeamScoreLine(true, false, player));
        addBlockLines(lines, Game.getCurrentBlocks("red"), player);
        addIfNotBlank(lines, legacyOrFallback(msg(Message.SCOREBOARD_DIVIDING_LINE, player), "&7&m----------------"));
        addIfNotBlank(lines, buildTeamScoreLine(false, false, player));
        addBlockLines(lines, Game.getCurrentBlocks("blue"), player);
        addIfNotBlank(lines, brandLine(player));
        return lines;
    }

    private static List<String> buildContestLines(Player player) {
        List<String> lines = new ArrayList<>();
        addIfNotBlank(lines, modeLine(player));
        addIfNotBlank(lines, modeDetailLine(player));
        addIfNotBlank(lines, buildTeamScoreLine(true, false, player));
        addIfNotBlank(lines, buildTeamScoreLine(false, false, player));
        addIfNotBlank(lines, legacyOrFallback(msg(Message.SCOREBOARD_DIVIDING_LINE, player), "&7&m----------------"));
        addBlockLines(lines, Game.getCurrentBlocks("red"), player);
        addIfNotBlank(lines, brandLine(player));
        return lines;
    }

    private static List<String> buildEndLines(Player player) {
        List<String> lines = new ArrayList<>();
        addIfNotBlank(lines, modeLine(player));
        addIfNotBlank(lines, legacyOrFallback(msg(Message.SCOREBOARD_END_STATUS, player), "<gray>Game ended</gray>"));
        addIfNotBlank(lines, determineWinnerLine(player));
        addIfNotBlank(lines, buildTeamScoreLine(true, true, player));
        addIfNotBlank(lines, buildTeamScoreLine(false, true, player));
        addIfNotBlank(lines, brandLine(player));
        return lines;
    }

    private static void addBlockLines(List<String> lines, List<String> blocks, Player player) {
        for (String block : blocks) {
            String display = getBlockDisplay(block, player);
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

    private static String buildBlockSummary(Player player) {
        List<String> parts = new ArrayList<>();
        if (Setting.isNetherMode()) {
            parts.add(msg(Message.SCOREBOARD_BLOCKS_NETHER, player));
            return String.join(" ", parts);
        }

        parts.add(msg(Message.SCOREBOARD_BLOCKS_EASY, player));
        if (Setting.isEnableMediumBlock()) {
            parts.add(msg(Message.SCOREBOARD_BLOCKS_MEDIUM, player));
        }
        if (Setting.isEnableHardBlock()) {
            parts.add(msg(Message.SCOREBOARD_BLOCKS_HARD, player));
        }
        if (Setting.isEnableDyedBlock()) {
            parts.add(msg(Message.SCOREBOARD_BLOCKS_DYED, player));
        }
        if (Setting.isEnableEndBlock()) {
            parts.add(msg(Message.SCOREBOARD_BLOCKS_END, player));
        }
        if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock()) {
            parts.add(msg(Message.SCOREBOARD_BLOCKS_ADDON, player));
        }

        return String.join(" ", parts);
    }

    private static String getBlockDisplay(String block, Player player) {
        String difficulty;
        if (Setting.isNetherMode() && listContains(Block.netherBlocks, block)) {
            difficulty = msg(Message.SCOREBOARD_BLOCK_DIFFICULTY_NETHER, player);
        } else if (listContains(Block.easyBlocks, block)) {
            difficulty = msg(Message.SCOREBOARD_BLOCK_DIFFICULTY_EASY, player);
        } else if (listContains(Block.mediumBlocks, block)) {
            difficulty = msg(Message.SCOREBOARD_BLOCK_DIFFICULTY_MEDIUM, player);
        } else if (listContains(Block.hardBlocks, block)) {
            difficulty = msg(Message.SCOREBOARD_BLOCK_DIFFICULTY_HARD, player);
        } else if (listContains(Block.dyedBlocks, block)) {
            difficulty = msg(Message.SCOREBOARD_BLOCK_DIFFICULTY_DYED, player);
        } else if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock() && listContains(Block.addonBlocks, block)) {
            difficulty = msg(Message.SCOREBOARD_BLOCK_DIFFICULTY_ADDON, player);
        } else if (listContains(Block.endBlocks, block)) {
            difficulty = msg(Message.SCOREBOARD_BLOCK_DIFFICULTY_END, player);
        } else {
            return legacyOrFallback(TranslationUtil.getValue(block), block);
        }

        String blockName = TranslationUtil.getValue(block);
        String template = msg(Message.SCOREBOARD_BLOCK_FORMAT, player);
        if (template == null || template.isBlank()) {
            template = "&7[%difficulty%] &f%block%";
        }
        return applyPlaceholders(template, "%difficulty%", difficulty, "%block%", blockName);
    }

    private static boolean listContains(List<String> list, String value) {
        return list != null && value != null && list.contains(value);
    }

    private static String resolveDisplayedGameMode(boolean includeOptionalModes, Player player) {
        String base;
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
            base = msg(Message.SCOREBOARD_MODE_NORMAL, player);
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING)) {
            base = msg(Message.SCOREBOARD_MODE_RACING, player);
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
            base = msg(Message.SCOREBOARD_MODE_CONTEST, player);
        } else {
            base = msg(Message.SCOREBOARD_MODE_TIME, player);
        }

        if (Setting.isNetherMode()) {
            base = appendMode(base, msg(Message.SCOREBOARD_MODE_NETHER, player));
        }
        if (includeOptionalModes && Setting.isSpeedMode()) {
            base = appendMode(base, msg(Message.SCOREBOARD_MODE_SPEED, player));
        }
        if (includeOptionalModes && Setting.isTeamChestGift()) {
            base = appendMode(base, msg(Message.SCOREBOARD_MODE_GIFT, player));
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

    private static String modeLine(Player player) {
        String template = msg(Message.SCOREBOARD_COMMON_MODE_LINE, player);
        if (template == null || template.isBlank()) {
            template = "<aqua>Mode:</aqua> <yellow>%mode%</yellow>";
        }
        boolean includeSpeed = !Game.getCurrentGameState().equals(Game.GameState.INGAME);
        return applyPlaceholders(template, "%mode%", resolveDisplayedGameMode(includeSpeed, player));
    }

    private static String modeDetailLine(Player player) {
        if (Game.isTimeModeActive()) {
            if (Game.isTimeModeOvertime()) {
                String template = msg(Message.SCOREBOARD_COMMON_MODE_DETAIL_OVERTIME, player);
                if (template == null || template.isBlank()) {
                    template = "<red>Overtime</red>";
                }
                return template;
            }
            String template = msg(Message.SCOREBOARD_COMMON_MODE_DETAIL_TIME_LEFT, player);
            if (template == null || template.isBlank()) {
                template = "<gold>Time Left: <white>%time%</white></gold>";
            }
            return applyPlaceholders(template, "%time%", Game.getFormattedTimeModeRemaining());
        }

        String template = msg(Message.SCOREBOARD_COMMON_MODE_DETAIL_DEFAULT, player);
        return template == null ? "" : template;
    }

    private static String brandLine(Player player) {
        String template = msg(Message.SCOREBOARD_COMMON_BRAND, player);
        if (template == null || template.isBlank()) {
            template = "<gray>BlockRacing</gray> <yellow>v%version%</yellow>";
        }
        String addonSuffix = Setting.isAddonAvailable()
                ? " <gradient:#b7d9af:#8bd9c0:#5fd9d1:#32d9e2:#06d9f3>ADDON</gradient>"
                : "";
        return applyPlaceholders(template + addonSuffix, "%version%", Main.getInstance().getDescription().getVersion());
    }

    private static String determineWinnerLine(Player player) {
        if (Game.redTeamScore > Game.blueTeamScore) {
            String value = msg(Message.SCOREBOARD_END_WINNER_RED, player);
            return legacyOrFallback(value, "<red>Red team wins!</red>");
        } else if (Game.blueTeamScore > Game.redTeamScore) {
            String value = msg(Message.SCOREBOARD_END_WINNER_BLUE, player);
            return legacyOrFallback(value, "<blue>Blue team wins!</blue>");
        }
        String value = msg(Message.SCOREBOARD_END_WINNER_DRAW, player);
        return legacyOrFallback(value, "<yellow>Draw</yellow>");
    }

    private static String buildTeamScoreLine(boolean red, boolean endPhase, Player player) {
        boolean timeMode = Game.isTimeModeActive();

        String template;
        if (endPhase) {
            template = timeMode
                    ? (red ? msg(Message.SCOREBOARD_END_RED_SCORE_TIME, player) : msg(Message.SCOREBOARD_END_BLUE_SCORE_TIME, player))
                    : (red ? msg(Message.SCOREBOARD_END_RED_SCORE, player) : msg(Message.SCOREBOARD_END_BLUE_SCORE, player));
        } else {
            template = timeMode
                    ? (red ? msg(Message.SCOREBOARD_RED_SCORE_TIME, player) : msg(Message.SCOREBOARD_BLUE_SCORE_TIME, player))
                    : (red ? msg(Message.SCOREBOARD_RED_SCORE, player) : msg(Message.SCOREBOARD_BLUE_SCORE, player));
        }

        if (template == null || template.isBlank()) {
            template = red ? msg(Message.SCOREBOARD_RED_SCORE, player) : msg(Message.SCOREBOARD_BLUE_SCORE, player);
        }

        String score = String.valueOf(red ? Game.redTeamScore : Game.blueTeamScore);
        String current = String.valueOf(red ? Game.redTeamCurrentBlockAmount : Game.blueTeamCurrentBlockAmount);
        String total = String.valueOf(red ? Game.redTeamTotalBlockAmount : Game.blueTeamTotalBlockAmount);

        return applyPlaceholders(template, "%score%", score, "%current_block%", current, "%total_block%", total);
    }

    /**
     * Resolves a message for the given player (per-player language), falling back to global.
     */
    private static String msg(Message message, Player player) {
        if (player != null) {
            return message.getString(player);
        }
        return message.getString();
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