package top.lqsnow.blockracing.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.scoreboard.Scoreboard;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import top.lqsnow.blockracing.voicechat.BlockRacingVoicechatPlugin;

import static top.lqsnow.blockracing.managers.Game.getCurrentGameState;
import static top.lqsnow.blockracing.managers.Game.GameState.INGAME;
import static top.lqsnow.blockracing.utils.CommandUtil.sendAll;

public class Team {
    public static org.bukkit.scoreboard.Team redTeam;
    public static org.bukkit.scoreboard.Team blueTeam;
    public static List<String> redTeamPlayers = new CopyOnWriteArrayList<>();
    public static List<String> blueTeamPlayers = new CopyOnWriteArrayList<>();

    public static void createTeam() {
        Scoreboard.createScoreboard();
        if (Scoreboard.scoreboard == null) {
            return;
        }
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            redTeam = null;
            blueTeam = null;
            return;
        }
        try {
            redTeam = Scoreboard.scoreboard.getTeam("red");
            if (redTeam == null) {
                redTeam = Scoreboard.scoreboard.registerNewTeam("red");
            }
            blueTeam = Scoreboard.scoreboard.getTeam("blue");
            if (blueTeam == null) {
                blueTeam = Scoreboard.scoreboard.registerNewTeam("blue");
            }
        } catch (UnsupportedOperationException ex) {
            redTeam = null;
            blueTeam = null;
            return;
        }

        redTeam.setDisplayName(Message.TEAM_RED_NAME.getString());
        redTeam.setPrefix(Message.TEAM_RED_PREFIX.getString());
        redTeam.setColor(ChatColor.RED);
        blueTeam.setDisplayName(Message.TEAM_BLUE_NAME.getString());
        blueTeam.setPrefix(Message.TEAM_BLUE_PREFIX.getString());
        blueTeam.setColor(ChatColor.BLUE);
    }

    public static boolean joinTeam(Player player, org.bukkit.scoreboard.Team team, boolean sendMessage) {
        if (team == redTeam) {
            return joinTeam(player, "red", sendMessage);
        }
        if (team == blueTeam) {
            return joinTeam(player, "blue", sendMessage);
        }
        if (redTeam == null || blueTeam == null) {
            createTeam();
        }
        if (team == redTeam) {
            return joinTeam(player, "red", sendMessage);
        }
        if (team == blueTeam) {
            return joinTeam(player, "blue", sendMessage);
        }
        return false;
    }

    public static boolean joinTeam(Player player, String teamName, boolean sendMessage) {
        if (redTeam == null || blueTeam == null) {
            createTeam();
        }
        if (teamName.equalsIgnoreCase("red")) {
            if (redTeamPlayers.contains(player.getName())) {
                if (sendMessage) {
                    player.sendMessage(Message.NOTICE_ALREADY_IN_RED.getString());
                }
                return false;
            }
            if (blueTeamPlayers.contains(player.getName())) {
                if (blueTeam != null) {
                    blueTeam.removeEntry(player.getName());
                }
                blueTeamPlayers.remove(player.getName());
            }
            if (redTeam != null) {
                redTeam.addEntry(player.getName());
            }
            redTeamPlayers.add(player.getName());
            setPlayerListNameByTeam(player, true);
            if (sendMessage) {
                sendAll(Message.NOTICE_JOIN_RED.getString().replace("%player%", player.getName()));
            }
        }
        else if (teamName.equalsIgnoreCase("blue")) {
            if (blueTeamPlayers.contains(player.getName())) {
                if (sendMessage) {
                    player.sendMessage(Message.NOTICE_ALREADY_IN_BLUE.getString());
                }
                return false;
            }
            if (redTeamPlayers.contains(player.getName())) {
                if (redTeam != null) {
                    redTeam.removeEntry(player.getName());
                }
                redTeamPlayers.remove(player.getName());
            }
            if (blueTeam != null) {
                blueTeam.addEntry(player.getName());
            }
            blueTeamPlayers.add(player.getName());
            setPlayerListNameByTeam(player, false);
            if (sendMessage) {
                sendAll(Message.NOTICE_JOIN_BLUE.getString().replace("%player%", player.getName()));
            }
        } else {
            return false;
        }
        if (getCurrentGameState().equals(INGAME)) {
            BlockRacingVoicechatPlugin.syncPlayer(player);
        }
        return true;
    }

    public static boolean isPlayerInRedTeam(Player player) {
        return redTeamPlayers.contains(player.getName());
    }

    public static boolean isPlayerInBlueTeam(Player player) {
        return blueTeamPlayers.contains(player.getName());
    }

    public static void refreshPlayerListName(Player player) {
        if (player == null) {
            return;
        }
        if (redTeamPlayers.contains(player.getName())) {
            setPlayerListNameByTeam(player, true);
            return;
        }
        if (blueTeamPlayers.contains(player.getName())) {
            setPlayerListNameByTeam(player, false);
            return;
        }

        Runnable action = () -> player.setPlayerListName(player.getName());
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> action.run());
            return;
        }
        action.run();
    }

    private static void setPlayerListNameByTeam(Player player, boolean red) {
        if (player == null) {
            return;
        }

        String color = red ? Message.TEAM_RED_COLOR.getString() : Message.TEAM_BLUE_COLOR.getString();
        Runnable action = () -> player.setPlayerListName(color + player.getName());

        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> action.run());
            return;
        }
        action.run();
    }

}
