package top.lqsnow.blockracing.managers;

import top.lqsnow.blockracing.utils.ColorUtil;

public final class Motd {

    private Motd() {
    }

    public static void refresh() {
        org.bukkit.Bukkit.setMotd(buildMotd());
    }

    public static String buildMotd() {
        return switch (Game.getCurrentGameState()) {
            case PREGAME -> buildPreGameMotd();
            case INGAME -> buildInGameMotd();
            case END -> buildEndGameMotd();
        };
    }

    private static String buildPreGameMotd() {
        return applyPlaceholders(Message.MOTD_PREGAME.getMiniMessage(),
                "%mode%", resolveGameModeName());
    }

    private static String buildInGameMotd() {
        String timePart = "";
        if (Game.isTimeModeActive()) {
            timePart = " <gray>|</gray> <yellow>Time</yellow><white> " + Game.getFormattedTimeModeRemaining() + "</white>";
        }
        return applyPlaceholders(Message.MOTD_INGAME.getMiniMessage(),
                "%red_score%", String.valueOf(Game.redTeamScore),
                "%blue_score%", String.valueOf(Game.blueTeamScore),
                "%time_part%", timePart);
    }

    private static String buildEndGameMotd() {
        return ColorUtil.t(Message.MOTD_END.getMiniMessage());
    }

    private static String resolveGameModeName() {
        return switch (Setting.getCurrentGameMode()) {
            case NORMAL -> Message.SCOREBOARD_MODE_NORMAL.getMiniMessage();
            case RACING -> Message.SCOREBOARD_MODE_RACING.getMiniMessage();
            case CONTEST -> Message.SCOREBOARD_MODE_CONTEST.getMiniMessage();
            case TIME -> Message.SCOREBOARD_MODE_TIME.getMiniMessage();
        };
    }

    private static String applyPlaceholders(String input, String... replacements) {
        String output = input;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            output = output.replace(replacements[i], replacements[i + 1]);
        }
        return ColorUtil.t(output);
    }
}