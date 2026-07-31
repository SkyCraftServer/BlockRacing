package top.lqsnow.blockracing.menus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import top.lqsnow.blockracing.commands.RandomTeam;
import top.lqsnow.blockracing.managers.Block;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.managers.Team;
import top.lqsnow.blockracing.toolkit.item.ItemBuilder;
import top.lqsnow.blockracing.toolkit.menu.MenuButton;
import top.lqsnow.blockracing.toolkit.menu.MenuView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static top.lqsnow.blockracing.listeners.BasicListener.editAmountPlayer;
import static top.lqsnow.blockracing.listeners.BasicListener.EditType;
import static top.lqsnow.blockracing.managers.Gui.updateMenu;
import static top.lqsnow.blockracing.scoreboard.Scoreboard.updateScoreboard;
import static top.lqsnow.blockracing.managers.Team.redTeam;

public final class PreGameMenu extends MenuView {
    private static final Set<Integer> GREEN_BACKGROUND = Set.of(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53
    );
    private static final Set<Integer> BLUE_BACKGROUND = Set.of(13, 14, 15, 31, 32, 40, 41, 42);

    public PreGameMenu() {
        super(54, player -> Message.MENU_PREGAME_TITLE.getString(player));

        // Join red team (slot 11)
        setButton(11, MenuButton.of(
                player -> item(Material.RED_WOOL, Message.MENU_JOIN_RED.getString(player)),
                (player, click) -> Team.joinTeam(player, redTeam, true)
        ));
        // Join blue team (slot 12)
        setButton(12, MenuButton.of(
                player -> item(Material.BLUE_WOOL, Message.MENU_JOIN_BLUE.getString(player)),
                (player, click) -> Team.joinTeam(player, Team.blueTeam, true)
        ));

        // Team chest gift (slot 13) - CHEST
        setButton(13, MenuButton.of(
                player -> {
                    ItemBuilder builder = ItemBuilder.of(Material.CHEST)
                            .name(Setting.isTeamChestGift()
                                    ? Message.MENU_TEAM_CHEST_GIFT_ENABLED.getString(player)
                                    : Message.MENU_TEAM_CHEST_GIFT_DISABLED.getString(player))
                            .lore(Message.MENU_TEAM_CHEST_GIFT_LORE.getStringList(player).stream()
                                    .map(line -> line.replace("%amount%", String.valueOf(Setting.getTeamChestGiftAmount())))
                                    .toList());
                    if (Setting.isTeamChestGift()) {
                        builder.flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    }
                    return builder.build();
                },
                (player, click) -> {
                    if (click.isRightClick()) {
                        player.closeInventory();
                        editAmountPlayer.put(player.getName(), EditType.TEAM_CHEST_GIFT_AMOUNT);
                        player.sendMessage(Message.NOTICE_SET_TEAM_CHEST_GIFT_AMOUNT.getString(player));
                    } else {
                        Setting.toggleTeamChestGift();
                    }
                    refreshSettings();
                }
        ));

        // Speed mode (slot 14) - ELYTRA
        setButton(14, MenuButton.of(
                player -> {
                    ItemBuilder builder = ItemBuilder.of(Material.ELYTRA)
                            .name(Setting.isSpeedMode()
                                    ? Message.MENU_SPEED_MODE_ENABLED.getString(player)
                                    : Message.MENU_SPEED_MODE_DISABLED.getString(player))
                            .lore(Message.MENU_SPEED_MODE_LORE.getStringList(player));
                    if (Setting.isSpeedMode()) {
                        builder.flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    }
                    return builder.build();
                },
                (player, click) -> {
                    Setting.toggleSpeedMode();
                    refreshSettings();
                }
        ));

        // Change block amount / time mode duration (slot 15)
        setButton(15, MenuButton.of(
                player -> {
                    if (Setting.getCurrentGameMode().equals(Setting.GameMode.TIME)) {
                        int minutes = Math.max(1, Setting.getTimeModeDurationMinutes());
                        return ItemBuilder.of(Material.NAME_TAG)
                                .name(Message.MENU_TIME_MODE_DURATION.getString(player)
                                        .replace("%minutes%", String.valueOf(minutes)))
                                .lore(Message.MENU_TIME_MODE_DURATION_LORE.getStringList(player))
                                .build();
                    }
                    return ItemBuilder.of(Material.NAME_TAG)
                            .name(Message.MENU_BLOCK_AMOUNT.getString(player) + Setting.getBlockAmount())
                            .lore(Message.MENU_BLOCK_AMOUNT_LORE.getStringList(player))
                            .build();
                },
                (player, click) -> {
                    player.closeInventory();
                    if (Setting.getCurrentGameMode().equals(Setting.GameMode.TIME)) {
                        editAmountPlayer.put(player.getName(), EditType.TIME_MODE_MINUTES);
                        player.sendMessage(Message.NOTICE_SET_TIME_MODE_MINUTES.getString(player));
                    } else {
                        editAmountPlayer.put(player.getName(), EditType.BLOCK_AMOUNT);
                        player.sendMessage(Message.NOTICE_SET_BLOCKS.getString(player));
                    }
                }
        ));

        // Medium block (slot 20)
        setButton(20, blockToggleButton(
                Setting::isEnableMediumBlock,
                Setting::toggleMediumBlock,
                Message.MENU_MEDIUM_BLOCKS
        ));
        // Hard block (slot 21)
        setButton(21, blockToggleButton(
                Setting::isEnableHardBlock,
                Setting::toggleHardBlock,
                Message.MENU_HARD_BLOCKS
        ));
        // Dyed block (slot 22)
        setButton(22, blockToggleButton(
                Setting::isEnableDyedBlock,
                Setting::toggleDyedBlock,
                Message.MENU_DYED_BLOCKS
        ));
        // End block (slot 23)
        setButton(23, blockToggleButton(
                Setting::isEnableEndBlock,
                Setting::toggleEndBlock,
                Message.MENU_END_BLOCKS
        ));
        // Addon block (slot 24) - only visible when addon available
        setButton(24, MenuButton.of(
                player -> {
                    if (!Setting.isAddonAvailable()) return null;
                    if (Setting.isNetherMode()) {
                        return item(Material.GRAY_CONCRETE,
                                Message.MENU_ADDON_BLOCKS.getString(player) + Message.MENU_LOCKED_BY_NETHER.getString(player));
                    }
                    return item(
                            Setting.isEnableAddonBlock() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE,
                            Message.MENU_ADDON_BLOCKS.getString(player) + (Setting.isEnableAddonBlock()
                                    ? Message.MENU_ENABLED.getString(player)
                                    : Message.MENU_DISABLED.getString(player))
                    );
                },
                (player, click) -> {
                    if (!Setting.isAddonAvailable()) return;
                    if (Setting.isNetherMode()) {
                        player.sendMessage(Message.MENU_LOCKED_BY_NETHER.getString(player));
                        return;
                    }
                    Setting.toggleAddonBlock();
                    Block.refreshAvailableBlocksAndClampAmount();
                    refreshSettings();
                }
        ));

        // Normal mode (slot 29)
        setButton(29, modeButton(Setting.GameMode.NORMAL,
                Message.MENU_NORMAL_MODE, Message.MENU_NORMAL_MODE_LORE));
        // Racing mode (slot 30)
        setButton(30, modeButton(Setting.GameMode.RACING,
                Message.MENU_RACING_MODE, Message.MENU_RACING_MODE_LORE));
        // Time mode (slot 31)
        setButton(31, modeButton(Setting.GameMode.TIME,
                Message.MENU_TIME_MODE, Message.MENU_TIME_MODE_LORE));
        // Contest mode (slot 32)
        setButton(32, modeButton(Setting.GameMode.CONTEST,
                Message.MENU_CONTEST_MODE, Message.MENU_CONTEST_MODE_LORE));

        // Nether mode (slot 33) - NETHERRACK
        setButton(33, MenuButton.of(
                player -> {
                    ItemBuilder builder = ItemBuilder.of(Material.NETHERRACK)
                            .name(Setting.isNetherMode()
                                    ? Message.MENU_NETHER_MODE_ENABLED.getString(player)
                                    : Message.MENU_NETHER_MODE_DISABLED.getString(player))
                            .lore(Message.MENU_NETHER_MODE_LORE.getStringList(player));
                    if (Setting.isNetherMode()) {
                        builder.flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    }
                    return builder.build();
                },
                (player, click) -> {
                    Setting.toggleNetherMode();
                    Block.refreshAvailableBlocksAndClampAmount();
                    refreshSettings();
                }
        ));

        // Ready (slot 38)
        setButton(38, MenuButton.of(
                player -> ItemBuilder.of(Material.EMERALD)
                        .name(Message.MENU_READY.getString(player))
                        .lore(Message.MENU_READY_LORE.getStringList(player))
                        .build(),
                (player, click) -> Game.playerReady(player)
        ));
        // Start (slot 39)
        setButton(39, MenuButton.of(
                player -> {
                    List<String> lore = new ArrayList<>(Message.MENU_START_LORE.getStringList(player));
                    if (player.isOp()) {
                        lore.add(Message.MENU_START_FORCE_SHIFT_LORE.getString(player));
                    }
                    return ItemBuilder.of(Material.DIAMOND)
                            .name(Message.MENU_START.getString(player))
                            .lore(lore)
                            .build();
                },
                (player, click) -> {
                    if (player.isOp() && click.isShiftClick()) {
                        Game.forceStart(player);
                        return;
                    }
                    Game.checkStartDemands(player);
                }
        ));

        // Comeback threshold (slot 40)
        setButton(40, MenuButton.of(
                player -> {
                    int points = Setting.getComebackBuffThresholdPoints();
                    Material mat = points > 0 ? Material.GREEN_CONCRETE : Material.RED_CONCRETE;
                    return ItemBuilder.of(mat)
                            .name(Message.MENU_COMEBACK_THRESHOLD.getString(player)
                                    .replace("%points%", String.valueOf(points)))
                            .lore(Message.MENU_COMEBACK_THRESHOLD_LORE.getStringList(player))
                            .build();
                },
                (player, click) -> {
                    player.closeInventory();
                    editAmountPlayer.put(player.getName(), EditType.COMEBACK_THRESHOLD);
                    player.sendMessage(Message.NOTICE_SET_COMEBACK_THRESHOLD.getString(player));
                }
        ));

        // Shared team spawn (slot 41) - TOTEM_OF_UNDYING
        setButton(41, MenuButton.of(
                player -> {
                    ItemBuilder builder = ItemBuilder.of(Material.TOTEM_OF_UNDYING)
                            .name(Setting.isSharedTeamSpawn()
                                    ? Message.MENU_SHARED_TEAM_SPAWN_ENABLED.getString(player)
                                    : Message.MENU_SHARED_TEAM_SPAWN_DISABLED.getString(player))
                            .lore(Message.MENU_SHARED_TEAM_SPAWN_LORE.getStringList(player));
                    if (Setting.isSharedTeamSpawn()) {
                        builder.flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    }
                    return builder.build();
                },
                (player, click) -> {
                    Setting.toggleSharedTeamSpawn();
                    refreshSettings();
                }
        ));

        // End portal broadcast (slot 42) - ENDER_EYE
        setButton(42, MenuButton.of(
                player -> {
                    ItemBuilder builder = ItemBuilder.of(Material.ENDER_EYE)
                            .name(Setting.isEndPortalCoordinateBroadcast()
                                    ? Message.MENU_END_PORTAL_BROADCAST_ENABLED.getString(player)
                                    : Message.MENU_END_PORTAL_BROADCAST_DISABLED.getString(player))
                            .lore(Message.MENU_END_PORTAL_BROADCAST_LORE.getStringList(player));
                    if (Setting.isEndPortalCoordinateBroadcast()) {
                        builder.flags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    }
                    return builder.build();
                },
                (player, click) -> {
                    Setting.toggleEndPortalCoordinateBroadcast();
                    refreshSettings();
                }
        ));

        // Info button (slot 43)
        setButton(43, MenuButton.of(
                player -> ItemBuilder.of(Material.WRITABLE_BOOK)
                        .name(Message.MENU_PREGAME_INFO.getString(player))
                        .lore(replaceInfoPlaceholders(Message.MENU_PREGAME_INFO_LORE.getStringList(player)))
                        .build(),
                (player, click) -> {}
        ));
    }

    @Override
    protected ItemStack getBackgroundItem(int slot, Player player) {
        if (GREEN_BACKGROUND.contains(slot)) {
            return item(Material.LIME_STAINED_GLASS_PANE, " ");
        }
        if (BLUE_BACKGROUND.contains(slot)) {
            // Slots 13-14 have game option buttons, slots 40-42 have settings buttons
            if (slot == 13 || slot == 14 || slot == 40 || slot == 41 || slot == 42) {
                return null;
            }
            return item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        }
        if (slot == 10 || slot == 16) {
            return item(Material.YELLOW_STAINED_GLASS_PANE, Message.MENU_SELECT_TEAM.getString(player));
        }
        if (slot == 19 || slot == 25) {
            return item(Material.YELLOW_STAINED_GLASS_PANE, Message.MENU_BLOCK_SETTING.getString(player));
        }
        if (slot == 28 || slot == 34) {
            return item(Material.YELLOW_STAINED_GLASS_PANE, Message.MENU_SELECT_MODE.getString(player));
        }
        if (slot == 37 || slot == 43) {
            return item(Material.YELLOW_STAINED_GLASS_PANE, Message.MENU_READY_AND_START.getString(player));
        }
        if (slot == 24 && !Setting.isAddonAvailable()) {
            return item(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        }
        return null;
    }

    private MenuButton blockToggleButton(BooleanSupplier enabled, Runnable toggle, Message label) {
        return MenuButton.of(
                player -> {
                    if (Setting.isNetherMode()) {
                        return item(Material.GRAY_CONCRETE,
                                label.getString(player) + Message.MENU_LOCKED_BY_NETHER.getString(player));
                    }
                    return item(
                            enabled.getAsBoolean() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE,
                            label.getString(player) + (enabled.getAsBoolean()
                                    ? Message.MENU_ENABLED.getString(player)
                                    : Message.MENU_DISABLED.getString(player))
                    );
                },
                (player, click) -> {
                    if (Setting.isNetherMode()) {
                        player.sendMessage(Message.MENU_LOCKED_BY_NETHER.getString(player));
                        return;
                    }
                    toggle.run();
                    Block.refreshAvailableBlocksAndClampAmount();
                    refreshSettings();
                }
        );
    }

    private MenuButton modeButton(Setting.GameMode mode, Message label, Message lore) {
        return MenuButton.of(
                player -> {
                    boolean selected = Setting.getCurrentGameMode() == mode;
                    return ItemBuilder.of(selected ? Material.GREEN_CONCRETE : Material.YELLOW_CONCRETE)
                            .name((selected ? Message.MENU_CURRENT_MODE : Message.MENU_SWITCH_TO).getString(player)
                                    + label.getString(player))
                            .lore(lore.getStringList(player))
                            .build();
                },
                (player, click) -> {
                    Setting.setCurrentGameMode(mode);
                    refreshSettings();
                }
        );
    }

    private void refreshSettings() {
        updateMenu(this);
        updateScoreboard();
    }

    private static java.util.List<String> replaceInfoPlaceholders(java.util.List<String> lore) {
        String sharedSpawn = Setting.isSharedTeamSpawn() ? "&a✓" : "&c✗";
        String teamChestGift = Setting.isTeamChestGift() ? "&a✓" : "&c✗";
        String endPortalBroadcast = Setting.isEndPortalCoordinateBroadcast() ? "&a✓" : "&c✗";
        String comebackThreshold = String.valueOf(Setting.getComebackBuffThresholdPoints());
        return lore.stream()
                .map(line -> line
                        .replace("%shared_spawn%", sharedSpawn)
                        .replace("%team_chest_gift%", teamChestGift)
                        .replace("%end_portal_broadcast%", endPortalBroadcast)
                        .replace("%comeback_threshold%", comebackThreshold))
                .toList();
    }

    private static ItemStack item(Material material, String name) {
        return ItemBuilder.of(material).name(name).build();
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
