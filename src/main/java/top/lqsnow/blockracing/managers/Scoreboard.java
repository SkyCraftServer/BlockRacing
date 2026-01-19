package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;
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
        // Generate displayed game mode
        String displayedGameMode = resolveDisplayedGameMode();

        // Generate blocks
        String blocks = String.format("%s%s%s%s%s", Message.SCOREBOARD_BLOCKS_EASY.getString(), (Setting.isEnableMediumBlock() ? " " + Message.SCOREBOARD_BLOCKS_MEDIUM.getString() : ""), (Setting.isEnableHardBlock() ? " " + Message.SCOREBOARD_BLOCKS_HARD.getString() : ""), (Setting.isEnableDyedBlock() ? " " + Message.SCOREBOARD_BLOCKS_DYED.getString() : ""), (Setting.isEnableEndBlock() ? " " + Message.SCOREBOARD_BLOCKS_END.getString() : ""));

        // Generate scoreboard
        setTitle(Message.SCOREBOARD_PREGAME_TITLE.getString());
        for (int slot = 11; slot >= 1; slot--) {
            String messageKey = "SCOREBOARD_PREGAME_SLOT" + slot;
            String originalMessage = Message.valueOf(messageKey).getString();
            if (originalMessage.equals("")) continue;
            String formattedMessage = originalMessage
                    .replace("%game_mode%", displayedGameMode)
                    .replace("%block_amount%", String.valueOf(Setting.getBlockAmount()))
                    .replace("%blocks%", blocks);

            setSlot(slot, formattedMessage);
        }
    }

    public static void setInGameScoreboard() {
        // Generate scoreboard
        setTitle(Message.SCOREBOARD_INGAME_TITLE.getString());

        if (Game.isTimeModeActive()) {
            if (Game.isTimeModeOvertime()) {
                setSlot(15, Message.SCOREBOARD_TIME_MODE_OVERTIME.getString());
            } else {
                setSlot(15, Message.SCOREBOARD_TIME_MODE_TIME_LEFT.getString().replace("%time%", Game.getFormattedTimeModeRemaining()));
            }
            setSlot(14, "");
        } else {
            setSlot(15, "");
            setSlot(14, "");
        }
        // Set red team score display
        setSlot(12, Message.SCOREBOARD_RED_SCORE.getString().replace("%score%", String.valueOf(redTeamScore)).replace("%current_block%", String.valueOf(redTeamCurrentBlockAmount)).replace("%total_block%", String.valueOf(redTeamTotalBlockAmount)));
        // Clean red team blocks display
        for (int i = getCurrentBlocks("red").size(); i < 3; i++) {
            int slotIndex = 11 - i;
            setSlot(slotIndex, "");
        }
        // Set red team blocks display
        for (int i = 0; i < getCurrentBlocks("red").size(); i++) {
            int slotIndex = 11 - i;
            setSlot(slotIndex, getBlockDisplay(redTeamRemainingBlocks.get(i)));
        }
        // Set dividing line
        setSlot(7, Message.SCOREBOARD_DIVIDING_LINE.getString());
        // Set blue team score display
        setSlot(6, Message.SCOREBOARD_BLUE_SCORE.getString().replace("%score%", String.valueOf(blueTeamScore)).replace("%current_block%", String.valueOf(blueTeamCurrentBlockAmount)).replace("%total_block%", String.valueOf(blueTeamTotalBlockAmount)));
        // Clean blue team blocks display
        for (int i = getCurrentBlocks("blue").size(); i < 3; i++) {
            int slotIndex = 5 - i;
            setSlot(slotIndex, "");
        }
        // Set blue team blocks display
        for (int i = 0; i < getCurrentBlocks("blue").size(); i++) {
            int slotIndex = 5 - i;
            setSlot(slotIndex, getBlockDisplay(blueTeamRemainingBlocks.get(i)));
        }
        // Set bottom display
        setSlot(1, Message.SCOREBOARD_BOTTOM_SLOT.getString());
    }

    public static String getBlockDisplay(String block) {
        if (easyBlocks.contains(block)) {
            return String.format(Message.SCOREBOARD_BLOCK_FORMAT.getString().replace("%difficulty%", Message.SCOREBOARD_BLOCK_DIFFICULTY_EASY.getString()).replace("%block%", TranslationUtil.getValue(block)));
        } else if (mediumBlocks.contains(block)) {
            return String.format(Message.SCOREBOARD_BLOCK_FORMAT.getString().replace("%difficulty%", Message.SCOREBOARD_BLOCK_DIFFICULTY_MEDIUM.getString()).replace("%block%", TranslationUtil.getValue(block)));
        } else if (hardBlocks.contains(block)) {
            return String.format(Message.SCOREBOARD_BLOCK_FORMAT.getString().replace("%difficulty%", Message.SCOREBOARD_BLOCK_DIFFICULTY_HARD.getString()).replace("%block%", TranslationUtil.getValue(block)));
        } else if (dyedBlocks.contains(block)) {
            return String.format(Message.SCOREBOARD_BLOCK_FORMAT.getString().replace("%difficulty%", Message.SCOREBOARD_BLOCK_DIFFICULTY_DYED.getString()).replace("%block%", TranslationUtil.getValue(block)));
        } else if (endBlocks.contains(block)) {
            return String.format(Message.SCOREBOARD_BLOCK_FORMAT.getString().replace("%difficulty%", Message.SCOREBOARD_BLOCK_DIFFICULTY_END.getString()).replace("%block%", TranslationUtil.getValue(block)));
        }
        return null;
    }

    private static String resolveDisplayedGameMode() {
        String base;
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
            base = Message.SCOREBOARD_MODE_NORMAL.getString();
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING)) {
            base = Message.SCOREBOARD_MODE_RACING.getString();
        } else {
            base = Message.SCOREBOARD_MODE_TIME.getString();
        }
        if (Setting.isSpeedMode()) {
            base = String.format("%s + %s", base, Message.SCOREBOARD_MODE_SPEED.getString());
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

    private static void setTitle(String title) {
        // Allow & codes in config but assume title may already be in legacy form (contains '§').
        if (title == null) title = "";
        // translate '&' to legacy '§' when present
        title = ChatColor.translateAlternateColorCodes('&', title);
        // Trim to max 32 visible characters while preserving color/hex sequences (do not split color codes)
        sidebar.setDisplayName(trimLegacyToVisibleLength(title, 32));
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

        text = ChatColor.translateAlternateColorCodes('&', text);
        String pre = getFirstSplit(text);
        String suf = getFirstSplit(ChatColor.getLastColors(pre) + getSecondSplit(text));

        // Edited
        if (pre.endsWith("§")) {
            pre = pre.substring(0, pre.length() - 1);
            if (suf.startsWith("§")) {
                suf = suf.substring(0, 2) + "§" + suf.substring(2);
            } else {
                suf = "§" + suf;
            }
        }

        if (team == null) {
            return;
        }
        team.setPrefix(pre);
        team.setSuffix(suf);
    }

    private static String getFirstSplit(String s) {
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    private static String getSecondSplit(String s) {
        if (s.length() > 32) {
            s = s.substring(0, 32);
        }
        return s.length() > 16 ? s.substring(16) : "";
    }

}
