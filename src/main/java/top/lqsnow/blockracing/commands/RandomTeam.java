package top.lqsnow.blockracing.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Team;
import top.lqsnow.blockracing.toolkit.text.Texts;
import top.lqsnow.blockracing.utils.CommandUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static top.lqsnow.blockracing.managers.Game.getCurrentGameState;

public class RandomTeam implements CommandExecutor {
    private static final Map<UUID, Long> pendingConfirmations = new ConcurrentHashMap<>();
    private static final long CONFIRM_TIMEOUT_MS = 15_000;

    /**
     * Called from the PreGameMenu button. Sends a clickable confirmation message.
     */
    public static void requestConfirmation(Player player) {
        if (!getCurrentGameState().equals(Game.GameState.PREGAME)) {
            player.sendMessage(Message.NOTICE_GAME_HAS_START.getString(player));
            return;
        }
        sendConfirmPrompt(player);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }
        if (!getCurrentGameState().equals(Game.GameState.PREGAME)) {
            sender.sendMessage(Message.NOTICE_GAME_HAS_START.getString());
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            handleConfirm(player);
            return true;
        }
        sendConfirmPrompt(player);
        return true;
    }

    private static void sendConfirmPrompt(Player player) {
        pendingConfirmations.put(player.getUniqueId(), System.currentTimeMillis());
        Component prompt = Texts.component(Message.NOTICE_TEAM_SHUFFLE_CONFIRM.getString(player))
                .append(Component.text(" "))
                .append(Texts.component(Message.NOTICE_TEAM_SHUFFLE_CONFIRM_BUTTON.getString(player))
                        .clickEvent(ClickEvent.runCommand("/randomteam confirm"))
                        .hoverEvent(HoverEvent.showText(
                                Texts.component(Message.NOTICE_TEAM_SHUFFLE_CONFIRM_HOVER.getString(player)))));
        player.sendMessage(prompt);
        scheduleExpiry(player.getUniqueId());
    }

    private static void handleConfirm(Player player) {
        Long timestamp = pendingConfirmations.remove(player.getUniqueId());
        if (timestamp == null || System.currentTimeMillis() - timestamp > CONFIRM_TIMEOUT_MS) {
            player.sendMessage(Message.NOTICE_TEAM_SHUFFLE_CONFIRM_EXPIRED.getString(player));
            return;
        }
        shuffleTeams();
    }

    private static void scheduleExpiry(UUID uuid) {
        Runnable check = () -> {
            Long timestamp = pendingConfirmations.get(uuid);
            if (timestamp != null && System.currentTimeMillis() - timestamp > CONFIRM_TIMEOUT_MS) {
                pendingConfirmations.remove(uuid);
            }
        };
        if (Main.getFoliaLib() != null && Main.getFoliaLib().isFolia()) {
            Main.getFoliaLib().getScheduler().runLater(() -> check.run(), 20L * 16);
        } else {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), check, 20L * 16);
        }
    }

    private static void shuffleTeams() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<List<Player>> teams = splitPlayers(players, new Random());
        for (Player p : teams.get(0)) {
            Team.joinTeam(p, "red", false);
        }
        for (Player p : teams.get(1)) {
            Team.joinTeam(p, "blue", false);
        }

        CommandUtil.sendAll(Message.NOTICE_TEAM_SHUFFLE.getString());
        String redMembers = Team.redTeamPlayers.stream().collect(Collectors.joining(", "));
        String blueMembers = Team.blueTeamPlayers.stream().collect(Collectors.joining(", "));
        CommandUtil.sendAll("&c红队: &f" + (redMembers.isEmpty() ? "(无)" : redMembers));
        CommandUtil.sendAll("&9蓝队: &f" + (blueMembers.isEmpty() ? "(无)" : blueMembers));
    }

    /**
     * Splits a list of players into two balanced teams randomly.
     */
    public static <T> List<List<T>> splitPlayers(List<T> players, Random random) {
        List<T> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, random);
        int sizeA = shuffled.size() / 2;
        List<T> teamA = new ArrayList<>(shuffled.subList(0, sizeA));
        List<T> teamB = new ArrayList<>(shuffled.subList(sizeA, shuffled.size()));
        if (random.nextBoolean()) {
            return List.of(teamA, teamB);
        } else {
            return List.of(teamB, teamA);
        }
    }
}
