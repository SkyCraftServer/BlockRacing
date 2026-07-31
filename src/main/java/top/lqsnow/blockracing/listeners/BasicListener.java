package top.lqsnow.blockracing.listeners;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.*;
import top.lqsnow.blockracing.menus.PreGameMenu;
import top.lqsnow.blockracing.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static top.lqsnow.blockracing.managers.Gui.updateMenu;
import static top.lqsnow.blockracing.scoreboard.Scoreboard.updateScoreboard;
import static top.lqsnow.blockracing.managers.Team.isPlayerInBlueTeam;
import static top.lqsnow.blockracing.managers.Team.isPlayerInRedTeam;
import static top.lqsnow.blockracing.utils.ColorUtil.t;
import static top.lqsnow.blockracing.managers.Block.*;
import static top.lqsnow.blockracing.utils.CommandUtil.sendAll;

public class BasicListener implements Listener {
    public enum EditType {
        BLOCK_AMOUNT,
        TIME_MODE_MINUTES,
        COMEBACK_THRESHOLD,
        TEAM_CHEST_GIFT_AMOUNT
    }

    public static Map<String, EditType> editAmountPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, WrappedTask> respawnFallbackTasks = new ConcurrentHashMap<>();

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Game.playerLogin(event.getPlayer());
    }

    @EventHandler
    private void onWorldLoad(WorldLoadEvent event) {
        // Keep world gamerules consistent for late-loaded dimensions on Folia.
        event.getWorld().setGameRule(GameRules.KEEP_INVENTORY, true);
        event.getWorld().setGameRule(GameRules.LOCATOR_BAR, false);
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        cancelRespawnFallbackTask(event.getPlayer().getUniqueId());
        Game.clearRenameRequest(event.getPlayer().getName());
        Game.playerQuit(event.getPlayer());
    }

    @EventHandler
    private void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        // Open menu
        if (event.getPlayer().isSneaking()) {
            event.setCancelled(true);
            Gui.openMenu(event.getPlayer());
        }
    }

    @EventHandler
    private void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (Game.hasRenameRequest(player)) {
            event.setCancelled(true);
            String input = event.getMessage();
            Main.getFoliaLib().getScheduler().runAtEntity(player, task -> Game.handleRenameInput(player, input));
            return;
        }

        // Change block amount
        EditType editType = editAmountPlayer.get(player.getName());
        if (editType != null) {
            if (!Game.getCurrentGameState().equals(Game.GameState.PREGAME)) return;
            if (event.getMessage().equals("quit")) {
                player.sendMessage(Message.NOTICE_SET_BLOCKS_QUIT.getString());
                editAmountPlayer.remove(player.getName());
                event.setCancelled(true);
                return;
            }
            boolean flag;
            int blockAmount = 0;
            try {
                blockAmount = Integer.parseInt(event.getMessage());
                flag = true;
            } catch (Exception ex) {
                if (editType == EditType.COMEBACK_THRESHOLD) {
                    player.sendMessage(Message.NOTICE_SET_COMEBACK_THRESHOLD_ERROR.getString());
                } else if (editType == EditType.TEAM_CHEST_GIFT_AMOUNT) {
                    player.sendMessage(Message.NOTICE_SET_TEAM_CHEST_GIFT_AMOUNT_ERROR.getString());
                } else {
                    player.sendMessage(Message.NOTICE_SET_BLOCKS_ERROR.getString());
                }
                flag = false;
            } finally {
                event.setCancelled(true);
            }
            if (flag) {
                if (editType == EditType.TIME_MODE_MINUTES) {
                    setTimeModeDurationMinutes(blockAmount, true);
                } else if (editType == EditType.COMEBACK_THRESHOLD) {
                    setComebackThresholdPoints(blockAmount, true);
                } else if (editType == EditType.TEAM_CHEST_GIFT_AMOUNT) {
                    setTeamChestGiftAmount(blockAmount, true);
                } else {
                    setBlockAmount(blockAmount, true);
                }
                editAmountPlayer.remove(player.getName());
            }
        }

        // Change chat format (only during PREGAME; INGAME team chat is handled by AsyncChatEvent)
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
            if (isPlayerInRedTeam(player)) {
                event.setFormat(t(Message.TEAM_RED_CHAT.getString()));
            } else if (isPlayerInBlueTeam(player)) {
                event.setFormat(t(Message.TEAM_BLUE_CHAT.getString()));
            }
        }
    }

    @EventHandler
    private void onAsyncChat(AsyncChatEvent event) {
        TeamChat.handle(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // If personal respawn is not actually used (invalid/missing bed or anchor), fallback to team spawn.
        if (Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
            Player p = event.getPlayer();
            Location teamSpawn = getTeamSpawn(p);
            if (teamSpawn != null) {
                if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
                    event.setRespawnLocation(teamSpawn);
                } else if (isInOriginRange(event.getRespawnLocation())) {
                    event.setRespawnLocation(teamSpawn);
                }
            }
        }
        event.getPlayer().sendMessage(Message.NOTICE_SPAWN_PROTECT.getString());

        Main.getFoliaLib().getScheduler().runAtEntityLater(event.getPlayer(), () -> {
            if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
                return;
            }
            Player p = event.getPlayer();
            Location teamSpawn = getTeamSpawn(p);
            if (teamSpawn == null) {
                return;
            }
            boolean personalRespawnLikelyValid = event.isBedSpawn() || event.isAnchorSpawn();
            if (!personalRespawnLikelyValid || shouldFallbackToTeamSpawn(p, teamSpawn)) {
                p.teleport(teamSpawn);
            }
            // Keep player's personal respawn fallback aligned to team spawn during the match.
            Game.applyTeamRespawnLocation(p);
        }, 20L);

        applyPostRespawnEffects(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryCloseForFoliaRespawn(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
            return;
        }
        if (event.getInventory().getType() != InventoryType.CRAFTING) {
            return;
        }
        // Folia workaround from issue #105: detect respawn flow via closing player's crafting inventory while dead.
        if (!player.isDead() || !player.isOnline() || player.getHealth() > 0.0D) {
            return;
        }
        scheduleRespawnFallback(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
            return;
        }

        // Explicitly keep inventory/level as a Folia-safe fallback.
        // Some dimensions may not have KEEP_INVENTORY gamerule applied in time.
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);


        scheduleRespawnFallback(event.getEntity());
    }

    private void applyPostRespawnEffects(Player player) {
        if (player == null) {
            return;
        }

        Main.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
                return;
            }
            Location teamSpawn = getTeamSpawn(player);
            if (teamSpawn == null) {
                return;
            }
            if (shouldFallbackToTeamSpawn(player, teamSpawn)) {
                player.teleport(teamSpawn);
            }
            Game.applyTeamRespawnLocation(player);
        }, 5L);

        Main.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
                return;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
            if (Setting.isSpeedMode()) {
                Game.applyConfiguredSpeedModeEffects(player, true);
            }
            Game.refreshComebackEffects();
        }, 10L);

        Main.getFoliaLib().getScheduler().runAtEntityLater(player, () -> {
            if (Game.getCurrentGameState().equals(Game.GameState.INGAME) && Setting.isSpeedMode()) {
                Game.applyConfiguredSpeedModeEffects(player, true);
            }
        }, 60L);
    }

    private void scheduleRespawnFallback(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (respawnFallbackTasks.containsKey(uuid)) {
            return;
        }

        final int[] pollTicks = {0};
        WrappedTask task = Main.getFoliaLib().getScheduler().runTimer(() -> {
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || !online.isOnline() || !Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
                cancelRespawnFallbackTask(uuid);
                return;
            }

            pollTicks[0]++;
            if (!online.isDead() && online.getHealth() > 0.0D) {
                cancelRespawnFallbackTask(uuid);
                applyPostRespawnEffects(online);
                return;
            }

            if (pollTicks[0] >= 200) {
                cancelRespawnFallbackTask(uuid);
            }
        }, 1L, 1L);

        respawnFallbackTasks.put(uuid, task);
    }

    private void cancelRespawnFallbackTask(UUID uuid) {
        WrappedTask task = respawnFallbackTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerPortal(PlayerPortalEvent event) {
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) return;
        if (!Setting.isNetherMode()) return;
        if (event.getCause() == TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Message.NOTICE_NETHER_PORTAL_BLOCKED.getString());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) return;
        if (!Setting.isNetherMode()) return;
        if (event.getCause() == TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Message.NOTICE_NETHER_PORTAL_BLOCKED.getString());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPortalCreate(PortalCreateEvent event) {
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) return;
        if (!Setting.isNetherMode()) return;
        if (event.getReason() == PortalCreateEvent.CreateReason.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onEndPortalActivate(PlayerInteractEvent event) {
        if (!Setting.isEndPortalCoordinateBroadcast()) return;
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.END_PORTAL_FRAME) return;
        if (event.getItem() == null || event.getItem().getType() != Material.ENDER_EYE) return;

        Location clicked = event.getClickedBlock().getLocation();
        boolean hadPortal = hasNearbyEndPortal(clicked);

        Main.getFoliaLib().getScheduler().runAtLocationLater(clicked, () -> {
            if (hadPortal || !hasNearbyEndPortal(clicked)) {
                return;
            }

            Player player = event.getPlayer();
            String teamName;
            if (isPlayerInRedTeam(player)) {
                teamName = Message.TEAM_RED_NAME.getString();
            } else if (isPlayerInBlueTeam(player)) {
                teamName = Message.TEAM_BLUE_NAME.getString();
            } else {
                teamName = "&7未知队伍";
            }

            Location portalLoc = findNearbyEndPortal(clicked);
            if (portalLoc == null) {
                portalLoc = clicked;
            }

            sendAll(Message.NOTICE_END_PORTAL_OPEN.getString()
                    .replace("%player%", player.getName())
                    .replace("%team%", teamName)
                    .replace("%x%", String.valueOf(portalLoc.getBlockX()))
                    .replace("%y%", String.valueOf(portalLoc.getBlockY()))
                    .replace("%z%", String.valueOf(portalLoc.getBlockZ())));
        }, 1L);
    }

    private boolean hasNearbyEndPortal(Location center) {
        return findNearbyEndPortal(center) != null;
    }

    private Location getTeamSpawn(Player player) {
        if (isPlayerInRedTeam(player)) {
            return Game.redTeamSpawn;
        }
        if (isPlayerInBlueTeam(player)) {
            return Game.blueTeamSpawn;
        }
        return null;
    }

    private boolean isInOriginRange(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Location origin = new Location(location.getWorld(), 0.0, location.getY(), 0.0);
        return origin.distanceSquared(location) <= 20.0D;
    }

    private boolean shouldFallbackToTeamSpawn(Player player, Location teamSpawn) {
        if (player == null || teamSpawn == null || player.getWorld() == null) {
            return false;
        }
        Location current = player.getLocation();
        if (current == null) {
            return true;
        }
        if (!current.getWorld().equals(teamSpawn.getWorld())) {
            return true;
        }
        if (isInOriginRange(current)) {
            return true;
        }
        Location worldSpawn = player.getWorld().getSpawnLocation();
        return worldSpawn != null
                && worldSpawn.getWorld() != null
                && worldSpawn.getWorld().equals(current.getWorld())
            && worldSpawn.distanceSquared(current) <= 20.0D;
    }

    private Location findNearbyEndPortal(Location center) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        org.bukkit.block.Block base = center.getBlock();
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    org.bukkit.block.Block check = base.getRelative(x, y, z);
                    if (check.getType() == Material.END_PORTAL) {
                        return check.getLocation();
                    }
                }
            }
        }
        return null;
    }

    public static void setBlockAmount(int blockAmount, Boolean sendMessage) {
        Block.addUpBlocks();
        if (blockAmount < 10) {
            if (sendMessage) sendAll(Message.NOTICE_SET_BLOCKS_SUCCESS.getString() + 10);
            blockAmount = 10;
        } else if (blockAmount > maxBlockAmount) {
            if (sendMessage) sendAll(Message.NOTICE_SET_BLOCKS_SUCCESS.getString() + maxBlockAmount);
            blockAmount = maxBlockAmount;
        } else {
            if (sendMessage) sendAll(Message.NOTICE_SET_BLOCKS_SUCCESS.getString() + blockAmount);
        }
        Setting.setBlockAmount(blockAmount);
        updateMenu(new PreGameMenu());
        updateScoreboard();
    }

    public static void setTimeModeDurationMinutes(int minutes, Boolean sendMessage) {
        int minAllowed = 1;
        int maxAllowed = 1440;
        int finalMinutes = minutes;

        if (minutes < minAllowed) {
            finalMinutes = minAllowed;
        } else if (minutes > maxAllowed) {
            finalMinutes = maxAllowed;
        }

        if (sendMessage) {
            sendAll(Message.NOTICE_SET_TIME_MODE_MINUTES_SUCCESS.getString()
                    .replace("%minutes%", String.valueOf(finalMinutes)));
        }

        Setting.setTimeModeDurationMinutes(finalMinutes);
        updateMenu(new PreGameMenu());
        updateScoreboard();
    }

    public static void setComebackThresholdPoints(int points, Boolean sendMessage) {
        int minAllowed = 0;
        int maxAllowed = 1000;
        int finalPoints = Math.max(minAllowed, Math.min(maxAllowed, points));

        if (sendMessage) {
            sendAll(Message.NOTICE_SET_COMEBACK_THRESHOLD_SUCCESS.getString()
                    .replace("%points%", String.valueOf(finalPoints)));
        }

        Setting.setComebackBuffThresholdPoints(finalPoints);
        updateMenu(new PreGameMenu());
        updateScoreboard();
    }

    public static void setTeamChestGiftAmount(int amount, Boolean sendMessage) {
        int finalAmount = Math.max(1, Math.min(64, amount));

        if (sendMessage) {
            sendAll(Message.NOTICE_SET_TEAM_CHEST_GIFT_AMOUNT_SUCCESS.getString()
                    .replace("%amount%", String.valueOf(finalAmount)));
        }

        Setting.setTeamChestGiftAmount(finalAmount);
        updateMenu(new PreGameMenu());
        updateScoreboard();
    }
}
