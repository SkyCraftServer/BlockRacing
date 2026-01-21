package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.utils.MiniMessageUtil;
import top.lqsnow.blockracing.utils.TranslationUtil;

import static top.lqsnow.blockracing.managers.Game.*;
import static top.lqsnow.blockracing.managers.Block.*;


public class Scoreboard {
    public static org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    public static Objective sidebar;

    public static void createScoreboard() {
        sidebar = scoreboard.registerNewObjective("sidebar", "dummy");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 1; i <= 15; i++) {
            Team team = scoreboard.registerNewTeam("SLOT_" + i);
            team.addEntry(genEntry(i));
        }
    }

    public static void setPreGameScoreboard() {
        clearSlots(11);

        // Title
        setTitleMini(Message.SCOREBOARD_PREGAME_TITLE.getMiniMessage());

        // Generate displayed game mode
        String displayedGameMode = resolveDisplayedGameModeMini();

        boolean timeMode = Setting.getCurrentGameMode().equals(Setting.GameMode.TIME);
        int minutes = Math.max(1, Setting.getTimeModeDurationSeconds() / 60);

        // Generate blocks / time text
        String blocks;
        if (Setting.isNetherMode()) {
            blocks = Message.SCOREBOARD_BLOCKS_NETHER.getMiniMessage();
        } else {
            blocks = String.format("%s%s%s%s%s%s",
                Message.SCOREBOARD_BLOCKS_EASY.getMiniMessage(),
                (Setting.isEnableMediumBlock() ? " " + Message.SCOREBOARD_BLOCKS_MEDIUM.getMiniMessage() : ""),
                (Setting.isEnableHardBlock() ? " " + Message.SCOREBOARD_BLOCKS_HARD.getMiniMessage() : ""),
                (Setting.isEnableDyedBlock() ? " " + Message.SCOREBOARD_BLOCKS_DYED.getMiniMessage() : ""),
                (Setting.isEnableEndBlock() ? " " + Message.SCOREBOARD_BLOCKS_END.getMiniMessage() : ""),
                (Setting.isAddonAvailable() && Setting.isEnableAddonBlock() ? " " + Message.SCOREBOARD_BLOCKS_ADDON.getMiniMessage() : ""));
        }
        String blockAmount = String.valueOf(Setting.getBlockAmount());

        // Generate scoreboard slots (2-11). Slot 1 is reserved for brand line.
        for (int slot = 11; slot >= 2; slot--) {
            String messageKey = "SCOREBOARD_PREGAME_SLOT" + slot;
            Message message;
            try {
                message = Message.valueOf(messageKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            String originalMessage = message.getMiniMessage();
            if (timeMode && messageKey.equals("SCOREBOARD_PREGAME_SLOT5")) {
                originalMessage = Message.SCOREBOARD_PREGAME_TIME_MODE_INFO.getMiniMessage();
            }
            if (originalMessage == null || originalMessage.isEmpty()) continue;
            String formattedMessage = applyPlaceholders(originalMessage,
                    "%game_mode%", displayedGameMode,
                    "%block_amount%", blockAmount,
                    "%minutes%", String.valueOf(minutes),
                    "%blocks%", blocks);

            setSlot(slot, mm(formattedMessage));
        }

        // Brand line
        setSlot(1, mm(brandLine()));
    }

    public static void setInGameScoreboard() {
        clearSlots(14);

        setTitleMini(Message.SCOREBOARD_INGAME_TITLE.getMiniMessage());

        // Mode display lines (top of 14-line layout)
        setSlot(14, mm(modeLine()));
        setSlot(13, mm(modeDetailLine()));

        // Contest mode: both teams share the same targets, show only 4 blocks total to reduce height
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
            setSlot(12, mm(buildTeamScoreLine(true, false)));
            setSlot(11, mm(buildTeamScoreLine(false, false)));
            setSlot(10, mm(Message.SCOREBOARD_DIVIDING_LINE.getMiniMessage()));
            for (int i = 0; i < 4; i++) {
                int slotIndex = 9 - i; // 9,8,7,6
                if (i < getCurrentBlocks("red").size()) {
                    setSlot(slotIndex, getBlockDisplay(redTeamRemainingBlocks.get(i)));
                } else {
                    setSlot(slotIndex, "");
                }
            }
            // Keep brand at the bottom
            setSlot(1, mm(brandLine()));
            // Clear leftover slots below the shared block list
            resetSlot(5);
            resetSlot(4);
            resetSlot(3);
            resetSlot(2);
            return;
        }

        // Red team score and blocks (4 entries)
        setSlot(12, mm(buildTeamScoreLine(true, false)));
        for (int i = 0; i < 4; i++) {
            int slotIndex = 11 - i; // 11,10,9,8
            if (i < getCurrentBlocks("red").size()) {
                setSlot(slotIndex, getBlockDisplay(redTeamRemainingBlocks.get(i)));
            } else {
                setSlot(slotIndex, "");
            }
        }

        // Dividing line
        setSlot(7, mm(Message.SCOREBOARD_DIVIDING_LINE.getMiniMessage()));

        // Blue team score and blocks (4 entries)
        setSlot(6, mm(buildTeamScoreLine(false, false)));
        for (int i = 0; i < 4; i++) {
            int slotIndex = 5 - i; // 5,4,3,2
            if (i < getCurrentBlocks("blue").size()) {
                setSlot(slotIndex, getBlockDisplay(blueTeamRemainingBlocks.get(i)));
            } else {
                setSlot(slotIndex, "");
            }
        }

        // Bottom display (keep brand on 1)
        setSlot(1, mm(brandLine()));
    }

    public static void setEndGameScoreboard() {
        clearSlots(15);

        String endTitle = Message.SCOREBOARD_END_TITLE.getMiniMessage();
        if (endTitle == null || endTitle.isEmpty()) {
            endTitle = Message.SCOREBOARD_INGAME_TITLE.getMiniMessage();
        }
        setTitleMini(endTitle);

        // Compact layout (6 lines): mode, status, winner, red score, blue score, brand
        setSlot(6, mm(modeLine()));
        setSlot(5, mm(Message.SCOREBOARD_END_STATUS.getMiniMessage()));
        setSlot(4, mm(determineWinnerLine()));
        setSlot(3, mm(buildTeamScoreLine(true, true)));
        setSlot(2, mm(buildTeamScoreLine(false, true)));
        setSlot(1, mm(brandLine()));

        // Remove any other slots so no blank padding remains
        for (int slot = 7; slot <= 15; slot++) {
            resetSlot(slot);
        }
    }

    public static String getBlockDisplay(String block) {
        String difficulty;
        if (Setting.isNetherMode() && netherBlocks.contains(block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_NETHER.getMiniMessage();
        } else if (easyBlocks.contains(block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_EASY.getMiniMessage();
        } else if (mediumBlocks.contains(block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_MEDIUM.getMiniMessage();
        } else if (hardBlocks.contains(block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_HARD.getMiniMessage();
        } else if (dyedBlocks.contains(block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_DYED.getMiniMessage();
        } else if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock() && addonBlocks.contains(block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_ADDON.getMiniMessage();
        } else if (endBlocks.contains(block)) {
            difficulty = Message.SCOREBOARD_BLOCK_DIFFICULTY_END.getMiniMessage();
        } else {
            return "";
        }

        String blockLine = applyPlaceholders(Message.SCOREBOARD_BLOCK_FORMAT.getMiniMessage(),
                "%difficulty%", difficulty,
                "%block%", TranslationUtil.getValue(block));
        return mm(blockLine);
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

    public static void showScoreboard(Player player) {
        player.setScoreboard(scoreboard);
    }

    public static void updateScoreboard() {
        if (getCurrentGameState().equals(GameState.PREGAME)) {
            setPreGameScoreboard();
        } else if (getCurrentGameState().equals(GameState.INGAME)) {
            setInGameScoreboard();
        } else if (getCurrentGameState().equals(GameState.END)) {
            setEndGameScoreboard();
        }
    }


    /**
     * https://github.com/Andy-K-Sparklight/PluginDiaryCode/blob/master/RarityCommons/src/main/java/rarityeg/commons/ScoreHelper.java
     * Help build up a scoreboard.
     * Considering RarityCommons isn't designed for Paper only,
     * we won't make migrations before Bukkit and Spigot support Kyori Poweblue Adventure.
     *
     * @author crisdev333
     * @author RarityEG
     */
    private static String genEntry(int slot) {
        return ChatColor.values()[slot].toString();
    }

    private static void setTitleMini(String miniTitle) {
        String legacy = mm(miniTitle);
        // Trim to max 32 visible characters while preserving color/hex sequences (do not split color codes)
        sidebar.setDisplayName(trimLegacyToVisibleLength(legacy, 32));
    }

    private static String trimLegacyToVisibleLength(String input, int maxVisible) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        int visible = 0;
        int i = 0;
        while (i < input.length() && visible < maxVisible) {
            char c = input.charAt(i);
            if (c == '\u00A7') {
                // color/reset/format code or hex start
                if (i + 1 < input.length()) {
                    char code = input.charAt(i + 1);
                    if (code == 'x' || code == 'X') {
                        // hex sequence: §x§R§R§G§G§B§B (14 chars)
                        int end = Math.min(input.length(), i + 14);
                        out.append(input, i, end);
                        i = end;
                        continue; // hex doesn't add visible chars
                    } else {
                        // normal formatting code: two chars
                        out.append(c).append(code);
                        i += 2;
                        continue; // formatting code doesn't add visible chars
                    }
                } else {
                    // stray section char, append and count as visible
                    out.append(c);
                    i++;
                    visible++;
                    continue;
                }
            } else {
                out.append(c);
                i++;
                visible++;
            }
        }

        // If we've cut early and there are still open formatting codes, preserve trailing formatting by
        // appending any reset code is optional; we simply return the built string which contains
        // all color/format sequences encountered before the cutoff.
        return out.toString();
    }

    private static void setSlot(int slot, String text) {
        Team team = scoreboard.getTeam("SLOT_" + slot);
        String entry = genEntry(slot);
        if (!scoreboard.getEntries().contains(entry)) {
            sidebar.getScore(entry).setScore(slot);
        }

        if (text == null) {
            text = "";
        }
        text = ChatColor.translateAlternateColorCodes('&', text);
        int cut = indexAfterVisible(text, 16);
        String pre = text.substring(0, cut);
        String rest = text.substring(cut);
        String suf = takeVisible(ChatColor.getLastColors(pre) + rest, 16);

        if (team == null) {
            return;
        }
        team.setPrefix(pre);
        team.setSuffix(suf);
    }

    private static String takeVisible(String s, int maxVisible) {
        return s.substring(0, indexAfterVisible(s, maxVisible));
    }

    private static int indexAfterVisible(String input, int maxVisible) {
        if (input == null) return 0;
        int visible = 0;
        int i = 0;
        while (i < input.length() && visible < maxVisible) {
            char c = input.charAt(i);
            if (c == '\u00A7') {
                if (i + 1 < input.length()) {
                    char code = input.charAt(i + 1);
                    if (code == 'x' || code == 'X') {
                        int end = Math.min(input.length(), i + 14);
                        i = end;
                        continue;
                    } else {
                        i += 2;
                        continue;
                    }
                } else {
                    i++;
                    visible++;
                    continue;
                }
            }
            i++;
            visible++;
        }
        return i;
    }

    private static void clearSlots(int maxSlot) {
        // Clear desired range
        for (int i = 1; i <= maxSlot; i++) {
            setSlot(i, "");
        }
        // Remove any higher slots that might remain from previous phases
        for (int i = maxSlot + 1; i <= 15; i++) {
            String entry = genEntry(i);
            if (scoreboard.getEntries().contains(entry)) {
                scoreboard.resetScores(entry);
            }
            Team team = scoreboard.getTeam("SLOT_" + i);
            if (team != null) {
                team.setPrefix("");
                team.setSuffix("");
            }
        }
    }

    private static void resetSlot(int slot) {
        String entry = genEntry(slot);
        if (scoreboard.getEntries().contains(entry)) {
            scoreboard.resetScores(entry);
        }
        Team team = scoreboard.getTeam("SLOT_" + slot);
        if (team != null) {
            team.setPrefix("");
            team.setSuffix("");
        }
    }

    private static String modeLine() {
        String template = Message.SCOREBOARD_COMMON_MODE_LINE.getMiniMessage();
        if (template == null || template.isEmpty()) {
            template = "<aqua>Mode:</aqua> <yellow>%mode%</yellow>";
        }
        boolean includeSpeed = !getCurrentGameState().equals(GameState.INGAME);
        return applyPlaceholders(template, "%mode%", resolveDisplayedGameModeMini(includeSpeed));
    }

    private static String modeDetailLine() {
        if (Game.isTimeModeActive()) {
            if (Game.isTimeModeOvertime()) {
                String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_OVERTIME.getMiniMessage();
                if (template == null || template.isEmpty()) {
                    template = "<red>Overtime</red>";
                }
                return template;
            }
            String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_TIME_LEFT.getMiniMessage();
            if (template == null || template.isEmpty()) {
                template = "<gold>Time Left: <white>%time%</white></gold>";
            }
            return applyPlaceholders(template, "%time%", Game.getFormattedTimeModeRemaining());
        }

        String template = Message.SCOREBOARD_COMMON_MODE_DETAIL_DEFAULT.getMiniMessage();
        return template == null ? "" : template;
    }

    private static String brandLine() {
        String template = Message.SCOREBOARD_COMMON_BRAND.getMiniMessage();
        if (template == null || template.isEmpty()) {
            template = "<gray>BlockRacing</gray> <yellow>v%version%</yellow>";
        }
        String addonSuffix = Setting.isAddonAvailable()
                ? " <gradient:#b7d9af:#8bd9c0:#5fd9d1:#32d9e2:#06d9f3>ADDON</gradient>"
                : "";
        return applyPlaceholders(template + addonSuffix, "%version%", Main.getInstance().getDescription().getVersion());
    }

    private static String determineWinnerLine() {
        String fallbackRed = "<red>Red team wins!</red>";
        String fallbackBlue = "<blue>Blue team wins!</blue>";
        String fallbackDraw = "<yellow>Draw</yellow>";
        if (redTeamScore > blueTeamScore) {
            String value = Message.SCOREBOARD_END_WINNER_RED.getMiniMessage();
            return (value == null || value.isEmpty()) ? fallbackRed : value;
        } else if (blueTeamScore > redTeamScore) {
            String value = Message.SCOREBOARD_END_WINNER_BLUE.getMiniMessage();
            return (value == null || value.isEmpty()) ? fallbackBlue : value;
        }
        String value = Message.SCOREBOARD_END_WINNER_DRAW.getMiniMessage();
        return (value == null || value.isEmpty()) ? fallbackDraw : value;
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

        String score = red ? String.valueOf(redTeamScore) : String.valueOf(blueTeamScore);
        String current = red ? String.valueOf(redTeamCurrentBlockAmount) : String.valueOf(blueTeamCurrentBlockAmount);
        String total = red ? String.valueOf(redTeamTotalBlockAmount) : String.valueOf(blueTeamTotalBlockAmount);

        return applyPlaceholders(template,
                "%score%", score,
                "%current_block%", current,
                "%total_block%", total);
    }

    private static String mm(String mini) {
        return MiniMessageUtil.toLegacyString(mini == null ? "" : mini);
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
