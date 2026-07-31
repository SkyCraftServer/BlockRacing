package top.lqsnow.blockracing.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Block;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.LanguageManager;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.scoreboard.SimpleScoreIntegration;
import top.lqsnow.blockracing.utils.MiniMessageUtil;
import top.lqsnow.blockracing.utils.TranslationUtil;

import java.util.HashMap;
import java.util.Map;

import static top.lqsnow.blockracing.managers.Game.*;
import static top.lqsnow.blockracing.managers.Block.*;


public class Scoreboard {
    public static org.bukkit.scoreboard.Scoreboard scoreboard;
    public static Objective sidebar;
    public static org.bukkit.scoreboard.Scoreboard scoreboardEn;
    private static Objective sidebarEn;
    private static boolean available = true;
    private static final Map<Integer, String> foliaSlotEntries = new HashMap<>();
    private static boolean externalScoreboardMode = false;

    // Context for dual-language scoreboard building
    private static org.bukkit.scoreboard.Scoreboard targetBoard;
    private static Objective targetSidebar;
    private static boolean buildingChinese = true;

    private static boolean isFolia() {
        return Main.getFoliaLib() != null && Main.getFoliaLib().isFolia();
    }

    private static boolean deferToGlobalIfNeeded(Runnable action) {
        if (Main.getFoliaLib() != null
                && Main.getFoliaLib().isFolia()
                && !Main.getFoliaLib().getScheduler().isGlobalTickThread()) {
            Main.getFoliaLib().getScheduler().runNextTick(task -> action.run());
            return true;
        }
        return false;
    }

    public static void createScoreboard() {
        if (!available) {
            return;
        }
        if (deferToGlobalIfNeeded(Scoreboard::createScoreboard)) {
            return;
        }
        if (isFolia()) {
            if (externalScoreboardMode) {
                return;
            }
            if (installExternalScoreboardProvider()) {
                externalScoreboardMode = true;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sb reload");
                return;
            }

            externalScoreboardMode = false;
            available = false;
            Main.getInstance().getLogger().warning("Folia detected but no compatible external scoreboard integration (SimpleScore or Scoreboard-Folia with PlaceholderAPI) is available. Sidebar display is disabled.");
            return;
        }
        if (scoreboard != null && sidebar != null) {
            return;
        }
        if (Bukkit.getScoreboardManager() == null) {
            return;
        }
        try {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            sidebar = scoreboard.getObjective("blockracing_sidebar");
            if (sidebar == null) {
                sidebar = scoreboard.registerNewObjective("blockracing_sidebar", "dummy");
            }
            sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

            scoreboardEn = Bukkit.getScoreboardManager().getNewScoreboard();
            sidebarEn = scoreboardEn.getObjective("blockracing_sidebar_en");
            if (sidebarEn == null) {
                sidebarEn = scoreboardEn.registerNewObjective("blockracing_sidebar_en", "dummy");
            }
            sidebarEn.setDisplaySlot(DisplaySlot.SIDEBAR);

            if (!isFolia()) {
                for (int i = 1; i <= 15; i++) {
                    Team team = scoreboard.getTeam("SLOT_" + i);
                    if (team == null) {
                        team = scoreboard.registerNewTeam("SLOT_" + i);
                    }
                    team.addEntry(genEntry(i));

                    Team teamEn = scoreboardEn.getTeam("SLOT_" + i);
                    if (teamEn == null) {
                        teamEn = scoreboardEn.registerNewTeam("SLOT_" + i);
                    }
                    teamEn.addEntry(genEntry(i));
                }
            }
        } catch (UnsupportedOperationException ex) {
            available = false;
            Main.getInstance().getLogger().warning("Scoreboard API is not available on this runtime. Disabling BlockRacing scoreboard updates.");
        }
    }

    private static boolean ensureInitialized() {
        if (!available) {
            return false;
        }
        if (isFolia()) {
            if (externalScoreboardMode) {
                return true;
            }
            externalScoreboardMode = installExternalScoreboardProvider();
            if (externalScoreboardMode) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sb reload");
            }
            return externalScoreboardMode;
        }
        if (scoreboard == null || sidebar == null) {
            createScoreboard();
        }
        return available && scoreboard != null && sidebar != null;
    }

    public static void setPreGameScoreboard() {
        if (deferToGlobalIfNeeded(Scoreboard::setPreGameScoreboard)) return;
        if (!ensureInitialized()) return;
        if (isFolia()) return;
        clearSlots(11);

        // Title
        setTitleLegacy(msgLang(Message.SCOREBOARD_PREGAME_TITLE));

        // Generate displayed game mode
        String displayedGameMode = resolveDisplayedGameModeMini();

        boolean timeMode = Setting.getCurrentGameMode().equals(Setting.GameMode.TIME);
        int minutes = Math.max(1, Setting.getTimeModeDurationSeconds() / 60);

        // Generate blocks / time text
        String blocks;
        if (Setting.isNetherMode()) {
            blocks = msgLang(Message.SCOREBOARD_BLOCKS_NETHER);
        } else {
            blocks = String.format("%s%s%s%s%s%s",
                msgLang(Message.SCOREBOARD_BLOCKS_EASY),
                (Setting.isEnableMediumBlock() ? " " + msgLang(Message.SCOREBOARD_BLOCKS_MEDIUM) : ""),
                (Setting.isEnableHardBlock() ? " " + msgLang(Message.SCOREBOARD_BLOCKS_HARD) : ""),
                (Setting.isEnableDyedBlock() ? " " + msgLang(Message.SCOREBOARD_BLOCKS_DYED) : ""),
                (Setting.isEnableEndBlock() ? " " + msgLang(Message.SCOREBOARD_BLOCKS_END) : ""),
                (Setting.isAddonAvailable() && Setting.isEnableAddonBlock() ? " " + msgLang(Message.SCOREBOARD_BLOCKS_ADDON) : ""));
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
            String originalMessage = msgLang(message);
            if (timeMode && messageKey.equals("SCOREBOARD_PREGAME_SLOT5")) {
                originalMessage = msgLang(Message.SCOREBOARD_PREGAME_TIME_MODE_INFO);
            }
            if (originalMessage == null || originalMessage.isEmpty()) continue;
            String formattedMessage = applyPlaceholders(originalMessage,
                    "%game_mode%", displayedGameMode,
                    "%block_amount%", blockAmount,
                    "%minutes%", String.valueOf(minutes),
                    "%blocks%", blocks);

            setSlot(slot, formattedMessage);
        }

        // Brand line
        setSlot(1, brandLine());
    }

    public static void setInGameScoreboard() {
        if (deferToGlobalIfNeeded(Scoreboard::setInGameScoreboard)) return;
        if (!ensureInitialized()) return;
        if (isFolia()) return;
        clearSlots(14);

        setTitleLegacy(msgLang(Message.SCOREBOARD_INGAME_TITLE));

        // Mode display lines (top of 14-line layout)
        setSlot(14, modeLine());
        setSlot(13, modeDetailLine());

        // Contest mode: both teams share the same targets, show only 4 blocks total to reduce height
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
            setSlot(12, buildTeamScoreLine(true, false));
            setSlot(11, buildTeamScoreLine(false, false));
            setSlot(10, msgLang(Message.SCOREBOARD_DIVIDING_LINE));
            for (int i = 0; i < 4; i++) {
                int slotIndex = 9 - i; // 9,8,7,6
                if (i < getCurrentBlocks("red").size()) {
                    setSlot(slotIndex, getBlockDisplay(redTeamRemainingBlocks.get(i)));
                } else {
                    setSlot(slotIndex, "");
                }
            }
            // Keep brand at the bottom
            setSlot(1, brandLine());
            // Clear leftover slots below the shared block list
            resetSlot(5);
            resetSlot(4);
            resetSlot(3);
            resetSlot(2);
            return;
        }

        // Red team score and blocks (4 entries)
        setSlot(12, buildTeamScoreLine(true, false));
        for (int i = 0; i < 4; i++) {
            int slotIndex = 11 - i; // 11,10,9,8
            if (i < getCurrentBlocks("red").size()) {
                setSlot(slotIndex, getBlockDisplay(redTeamRemainingBlocks.get(i)));
            } else {
                setSlot(slotIndex, "");
            }
        }

        // Dividing line
        setSlot(7, msgLang(Message.SCOREBOARD_DIVIDING_LINE));

        // Blue team score and blocks (4 entries)
        setSlot(6, buildTeamScoreLine(false, false));
        for (int i = 0; i < 4; i++) {
            int slotIndex = 5 - i; // 5,4,3,2
            if (i < getCurrentBlocks("blue").size()) {
                setSlot(slotIndex, getBlockDisplay(blueTeamRemainingBlocks.get(i)));
            } else {
                setSlot(slotIndex, "");
            }
        }

        // Bottom display (keep brand on 1)
        setSlot(1, brandLine());
    }

    public static void setEndGameScoreboard() {
        if (deferToGlobalIfNeeded(Scoreboard::setEndGameScoreboard)) return;
        if (!ensureInitialized()) return;
        if (isFolia()) return;
        clearSlots(15);

        String endTitle = msgLang(Message.SCOREBOARD_END_TITLE);
        if (endTitle == null || endTitle.isEmpty()) {
            endTitle = msgLang(Message.SCOREBOARD_INGAME_TITLE);
        }
        setTitleLegacy(endTitle);

        // Compact layout (6 lines): mode, status, winner, red score, blue score, brand
        setSlot(6, modeLine());
        setSlot(5, msgLang(Message.SCOREBOARD_END_STATUS));
        setSlot(4, determineWinnerLine());
        setSlot(3, buildTeamScoreLine(true, true));
        setSlot(2, buildTeamScoreLine(false, true));
        setSlot(1, brandLine());

        // Remove any other slots so no blank padding remains
        for (int slot = 7; slot <= 15; slot++) {
            resetSlot(slot);
        }
    }

    public static String getBlockDisplay(String block) {
        String difficulty;
        if (Setting.isNetherMode() && netherBlocks.contains(block)) {
            difficulty = msgLang(Message.SCOREBOARD_BLOCK_DIFFICULTY_NETHER);
        } else if (easyBlocks.contains(block)) {
            difficulty = msgLang(Message.SCOREBOARD_BLOCK_DIFFICULTY_EASY);
        } else if (mediumBlocks.contains(block)) {
            difficulty = msgLang(Message.SCOREBOARD_BLOCK_DIFFICULTY_MEDIUM);
        } else if (hardBlocks.contains(block)) {
            difficulty = msgLang(Message.SCOREBOARD_BLOCK_DIFFICULTY_HARD);
        } else if (dyedBlocks.contains(block)) {
            difficulty = msgLang(Message.SCOREBOARD_BLOCK_DIFFICULTY_DYED);
        } else if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock() && addonBlocks.contains(block)) {
            difficulty = msgLang(Message.SCOREBOARD_BLOCK_DIFFICULTY_ADDON);
        } else if (endBlocks.contains(block)) {
            difficulty = msgLang(Message.SCOREBOARD_BLOCK_DIFFICULTY_END);
        } else {
            return "";
        }

        String blockLine = applyPlaceholders(msgLang(Message.SCOREBOARD_BLOCK_FORMAT),
                "%difficulty%", difficulty,
                "%block%", TranslationUtil.getValue(block));
        return blockLine;
    }

    private static String resolveDisplayedGameModeMini() {
        return resolveDisplayedGameModeMini(true);
    }

    private static String resolveDisplayedGameModeMini(boolean includeOptionalModes) {
        String base;
        if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
            base = msgLang(Message.SCOREBOARD_MODE_NORMAL);
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING)) {
            base = msgLang(Message.SCOREBOARD_MODE_RACING);
        } else if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
            base = msgLang(Message.SCOREBOARD_MODE_CONTEST);
        } else {
            base = msgLang(Message.SCOREBOARD_MODE_TIME);
        }
        if (Setting.isNetherMode()) {
            base = base + " + " + msgLang(Message.SCOREBOARD_MODE_NETHER);
        }
        if (includeOptionalModes && Setting.isSpeedMode()) {
            base = base + " + " + msgLang(Message.SCOREBOARD_MODE_SPEED);
        }
        return base;
    }

    public static void showScoreboard(Player player) {
        if (!ensureInitialized()) return;
        if (isFolia()) {
            return;
        }
        // Assign per-language scoreboard based on player preference
        if (scoreboardEn != null && !LanguageManager.usesChinese(player)) {
            player.setScoreboard(scoreboardEn);
        } else {
            player.setScoreboard(scoreboard != null ? scoreboard : Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    private static boolean installExternalScoreboardProvider() {
        if (SimpleScoreIntegration.install()) {
            return true;
        }
        return false;
    }

    public static void updateScoreboard() {
        if (deferToGlobalIfNeeded(Scoreboard::updateScoreboard)) return;
        if (!ensureInitialized()) return;
        if (isFolia()) {
            refreshComebackEffects();
            return;
        }
        // Build Chinese scoreboard
        targetBoard = scoreboard;
        targetSidebar = sidebar;
        buildingChinese = true;
        buildCurrentPhase();
        // Build English scoreboard
        if (scoreboardEn != null && sidebarEn != null) {
            targetBoard = scoreboardEn;
            targetSidebar = sidebarEn;
            buildingChinese = false;
            buildCurrentPhase();
        }
        refreshComebackEffects();
    }

    private static void buildCurrentPhase() {
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
        Objective sb = targetSidebar != null ? targetSidebar : sidebar;
        sb.setDisplayName(trimLegacyToVisibleLength(legacy, 32));
    }

    private static void setTitleLegacy(String legacyTitle) {
        Objective sb = targetSidebar != null ? targetSidebar : sidebar;
        sb.setDisplayName(trimLegacyToVisibleLength(legacyTitle == null ? "" : legacyTitle, 32));
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
        if (isFolia()) {
            setSlotFolia(slot, text);
            return;
        }

        org.bukkit.scoreboard.Scoreboard board = targetBoard != null ? targetBoard : scoreboard;
        Objective obj = targetSidebar != null ? targetSidebar : sidebar;
        Team team = board.getTeam("SLOT_" + slot);
        String entry = genEntry(slot);
        if (!board.getEntries().contains(entry)) {
            obj.getScore(entry).setScore(slot);
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

    private static void setSlotFolia(int slot, String text) {
        if (text == null || text.isEmpty()) {
            resetSlot(slot);
            return;
        }

        String legacy = ChatColor.translateAlternateColorCodes('&', text);
        String trimmed = trimLegacyToVisibleLength(legacy, 30);
        if (trimmed.isEmpty()) {
            trimmed = " ";
        }

        // Use hidden formatting suffix to keep each slot entry unique.
        String unique = trimmed + ChatColor.RESET + ChatColor.values()[slot];

        String old = foliaSlotEntries.get(slot);
        if (old != null && !old.equals(unique)) {
            scoreboard.resetScores(old);
        }

        sidebar.getScore(unique).setScore(slot);
        foliaSlotEntries.put(slot, unique);
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
        if (isFolia()) {
            for (int i = 1; i <= 15; i++) {
                if (i > maxSlot) {
                    resetSlot(i);
                } else {
                    setSlot(i, "");
                }
            }
            return;
        }

        org.bukkit.scoreboard.Scoreboard board = targetBoard != null ? targetBoard : scoreboard;
        // Clear desired range
        for (int i = 1; i <= maxSlot; i++) {
            setSlot(i, "");
        }
        // Remove any higher slots that might remain from previous phases
        for (int i = maxSlot + 1; i <= 15; i++) {
            String entry = genEntry(i);
            if (board.getEntries().contains(entry)) {
                board.resetScores(entry);
            }
            Team team = board.getTeam("SLOT_" + i);
            if (team != null) {
                team.setPrefix("");
                team.setSuffix("");
            }
        }
    }

    private static void resetSlot(int slot) {
        if (isFolia()) {
            String old = foliaSlotEntries.remove(slot);
            if (old != null) {
                scoreboard.resetScores(old);
            }
            return;
        }

        org.bukkit.scoreboard.Scoreboard board = targetBoard != null ? targetBoard : scoreboard;
        String entry = genEntry(slot);
        if (board.getEntries().contains(entry)) {
            board.resetScores(entry);
        }
        Team team = board.getTeam("SLOT_" + slot);
        if (team != null) {
            team.setPrefix("");
            team.setSuffix("");
        }
    }

    private static String modeLine() {
        String template = msgLang(Message.SCOREBOARD_COMMON_MODE_LINE);
        if (template == null || template.isEmpty()) {
            template = "§bMode: §e%mode%";
        }
        boolean includeSpeed = !getCurrentGameState().equals(GameState.INGAME);
        return applyPlaceholders(template, "%mode%", resolveDisplayedGameModeMini(includeSpeed));
    }

    private static String modeDetailLine() {
        if (Game.isTimeModeActive()) {
            if (Game.isTimeModeOvertime()) {
                String template = msgLang(Message.SCOREBOARD_COMMON_MODE_DETAIL_OVERTIME);
                if (template == null || template.isEmpty()) {
                    template = "§cOvertime";
                }
                return template;
            }
            String template = msgLang(Message.SCOREBOARD_COMMON_MODE_DETAIL_TIME_LEFT);
            if (template == null || template.isEmpty()) {
                template = "§6Time Left: §f%time%";
            }
            return applyPlaceholders(template, "%time%", Game.getFormattedTimeModeRemaining());
        }

        String template = msgLang(Message.SCOREBOARD_COMMON_MODE_DETAIL_DEFAULT);
        return template == null ? "" : template;
    }

    private static String brandLine() {
        String template = msgLang(Message.SCOREBOARD_COMMON_BRAND);
        if (template == null || template.isEmpty()) {
            template = "§7BlockRacing §ev%version%";
        }
        String addonSuffix = Setting.isAddonAvailable() ? " §bADDON" : "";
        return applyPlaceholders(template + addonSuffix, "%version%", Main.getInstance().getDescription().getVersion());
    }

    private static String determineWinnerLine() {
        if (redTeamScore > blueTeamScore) {
            String value = msgLang(Message.SCOREBOARD_END_WINNER_RED);
            return (value == null || value.isEmpty()) ? "§cRed team wins!" : value;
        } else if (blueTeamScore > redTeamScore) {
            String value = msgLang(Message.SCOREBOARD_END_WINNER_BLUE);
            return (value == null || value.isEmpty()) ? "§9Blue team wins!" : value;
        }
        String value = msgLang(Message.SCOREBOARD_END_WINNER_DRAW);
        return (value == null || value.isEmpty()) ? "§eDraw" : value;
    }

    private static String buildTeamScoreLine(boolean red, boolean endPhase) {
        boolean timeMode = Game.isTimeModeActive();

        String template;
        if (endPhase) {
            if (timeMode) {
                template = red ? msgLang(Message.SCOREBOARD_END_RED_SCORE_TIME)
                        : msgLang(Message.SCOREBOARD_END_BLUE_SCORE_TIME);
            } else {
                template = red ? msgLang(Message.SCOREBOARD_END_RED_SCORE)
                        : msgLang(Message.SCOREBOARD_END_BLUE_SCORE);
            }
        } else {
            if (timeMode) {
                template = red ? msgLang(Message.SCOREBOARD_RED_SCORE_TIME)
                        : msgLang(Message.SCOREBOARD_BLUE_SCORE_TIME);
            } else {
                template = red ? msgLang(Message.SCOREBOARD_RED_SCORE)
                        : msgLang(Message.SCOREBOARD_BLUE_SCORE);
            }
        }

        if (template == null || template.isEmpty()) {
            template = red ? msgLang(Message.SCOREBOARD_RED_SCORE) : msgLang(Message.SCOREBOARD_BLUE_SCORE);
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

    /**
     * Resolves a message to legacy text for the current building language context.
     * Used in dual-language scoreboard generation.
     */
    private static String msgLang(Message message) {
        try {
            return LanguageManager.getStringForLanguage(message, buildingChinese);
        } catch (Exception ex) {
            return mm(message.getMiniMessage());
        }
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

    /**
     * Synchronizes the Bukkit scoreboard teams with the plugin's team player lists.
     */
    public static void syncPlayerTeams() {
        if (scoreboard == null) return;
        org.bukkit.scoreboard.Team red = scoreboard.getTeam("red");
        org.bukkit.scoreboard.Team blue = scoreboard.getTeam("blue");
        if (red != null) {
            for (String entry : red.getEntries()) {
                if (!top.lqsnow.blockracing.managers.Team.redTeamPlayers.contains(entry)) {
                    red.removeEntry(entry);
                }
            }
            for (String name : top.lqsnow.blockracing.managers.Team.redTeamPlayers) {
                if (!red.hasEntry(name)) {
                    red.addEntry(name);
                }
            }
        }
        if (blue != null) {
            for (String entry : blue.getEntries()) {
                if (!top.lqsnow.blockracing.managers.Team.blueTeamPlayers.contains(entry)) {
                    blue.removeEntry(entry);
                }
            }
            for (String name : top.lqsnow.blockracing.managers.Team.blueTeamPlayers) {
                if (!blue.hasEntry(name)) {
                    blue.addEntry(name);
                }
            }
        }
    }

    /**
     * Refreshes the scoreboard display for a specific player.
     * Re-assigns the correct language scoreboard and updates content.
     */
    public static void refreshPlayer(Player player) {
        showScoreboard(player);
        updateScoreboard();
    }

}
