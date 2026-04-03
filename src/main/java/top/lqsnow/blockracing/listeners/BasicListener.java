package top.lqsnow.blockracing.listeners;

import org.bukkit.Bukkit;
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
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.*;
import top.lqsnow.blockracing.menus.PreGameMenu;
import top.lqsnow.blockracing.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
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
        COMEBACK_THRESHOLD
    }

    public static Map<String, EditType> editAmountPlayer = new ConcurrentHashMap<>();

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Game.playerLogin(event.getPlayer());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
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
                } else {
                    setBlockAmount(blockAmount, true);
                }
                editAmountPlayer.remove(player.getName());
            }
        }

        // Change chat format
        if (isPlayerInRedTeam(player)) {
            event.setFormat(t(Message.TEAM_RED_CHAT.getString()));
        } else if (isPlayerInBlueTeam(player)) {
            event.setFormat(t(Message.TEAM_BLUE_CHAT.getString()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerRespawn(PlayerRespawnEvent event) {
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
            if (isInOriginRange(p.getLocation())) {
                p.teleport(teamSpawn);
            }
            // Keep player's personal respawn fallback aligned to team spawn during the match.
            Game.applyTeamRespawnLocation(p);
        }, 20L);

        Main.getFoliaLib().getScheduler().runAtEntityLater(event.getPlayer(), () -> {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
            if (Game.getCurrentGameState().equals(Game.GameState.INGAME) && Setting.isSpeedMode()) {
                Game.applyConfiguredSpeedModeEffects(event.getPlayer());
            }
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false));
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 1, false, false));
            Game.refreshComebackEffects();
        }, 10L);

        Main.getFoliaLib().getScheduler().runAtEntityLater(event.getPlayer(), () -> {
            if (Game.getCurrentGameState().equals(Game.GameState.INGAME) && Setting.isSpeedMode()) {
                Game.applyConfiguredSpeedModeEffects(event.getPlayer());
            }
        }, 60L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerDeath(PlayerDeathEvent event) {
        if (!Game.getCurrentGameState().equals(Game.GameState.INGAME)) {
            return;
        }

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
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
        if (location == null) {
            return false;
        }

        // Treat coordinates near world origin (0,0,0) as fallback-spawn area.
        return Math.abs(location.getX()) <= 256.0
                && Math.abs(location.getY()) <= 256.0
                && Math.abs(location.getZ()) <= 256.0;
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
}
