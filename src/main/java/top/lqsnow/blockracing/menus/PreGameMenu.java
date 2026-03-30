package top.lqsnow.blockracing.menus;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import top.lqsnow.blockracing.listeners.BasicListener;
import top.lqsnow.blockracing.managers.Block;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Setting;
import top.lqsnow.blockracing.managers.Team;

import static top.lqsnow.blockracing.listeners.BasicListener.editAmountPlayer;
import static top.lqsnow.blockracing.managers.Gui.updateMenu;
import static top.lqsnow.blockracing.managers.Scoreboard.updateScoreboard;
import static top.lqsnow.blockracing.managers.Team.redTeam;

public class PreGameMenu extends Menu {

    // Define buttons
    @Position(11)
    private final Button joinRedButton;

    @Position(12)
    private final Button joinBlueButton;

    @Position(20)
    private final Button mediumBlock;

    @Position(21)
    private final Button hardBlock;

    @Position(22)
    private final Button dyedBlock;

    @Position(23)
    private final Button endBlock;

    @Position(24)
    private final Button addonBlock;

    @Position(15)
    private final Button changeBlockAmount;

    @Position(29)
    private final Button normalMode;

    @Position(30)
    private final Button racingMode;

    @Position(31)
    private final Button timeMode;

    @Position(32)
    private final Button contestMode;

    @Position(14)
    private final Button speedMode;

    @Position(33)
    private final Button netherMode;

    @Position(40)
    private final Button comebackThreshold;

    @Position(41)
    private final Button sharedTeamSpawn;

    @Position(38)
    private final Button ready;

    @Position(39)
    private final Button start;

    public PreGameMenu() {
        setTitle(Message.MENU_PREGAME_TITLE.getString());
        setSize(6 * 9);

        // Join red team
        this.joinRedButton = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                Team.joinTeam(player, redTeam, true);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.from(CompMaterial.RED_WOOL, Message.MENU_JOIN_RED.getString()).make();
            }
        };

        // Join blue team
        this.joinBlueButton = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                Team.joinTeam(player, Team.blueTeam, true);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.from(CompMaterial.BLUE_WOOL, Message.MENU_JOIN_BLUE.getString()).make();
            }
        };

        // Toggle medium block
        this.mediumBlock = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (Setting.isNetherMode()) {
                    player.sendMessage(Message.MENU_LOCKED_BY_NETHER.getString());
                    return;
                }
                Setting.toggleMediumBlock();
                Block.addUpBlocks();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.isNetherMode()) return ItemCreator.from(CompMaterial.GRAY_CONCRETE, Message.MENU_MEDIUM_BLOCKS.getString() + Message.MENU_LOCKED_BY_NETHER.getString()).make();
                if (Setting.isEnableMediumBlock()) return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_MEDIUM_BLOCKS.getString() + Message.MENU_ENABLED.getString()).make();
                else return ItemCreator.from(CompMaterial.RED_CONCRETE, Message.MENU_MEDIUM_BLOCKS.getString() + Message.MENU_DISABLED.getString()).make();
            }
        };

        // Toggle hard block
        this.hardBlock = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (Setting.isNetherMode()) {
                    player.sendMessage(Message.MENU_LOCKED_BY_NETHER.getString());
                    return;
                }
                Setting.toggleHardBlock();
                Block.addUpBlocks();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.isNetherMode()) return ItemCreator.from(CompMaterial.GRAY_CONCRETE, Message.MENU_HARD_BLOCKS.getString() + Message.MENU_LOCKED_BY_NETHER.getString()).make();
                if (Setting.isEnableHardBlock()) return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_HARD_BLOCKS.getString() + Message.MENU_ENABLED.getString()).make();
                else return ItemCreator.from(CompMaterial.RED_CONCRETE, Message.MENU_HARD_BLOCKS.getString() + Message.MENU_DISABLED.getString()).make();
            }
        };

        // Toggle dyed block
        this.dyedBlock = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (Setting.isNetherMode()) {
                    player.sendMessage(Message.MENU_LOCKED_BY_NETHER.getString());
                    return;
                }
                Setting.toggleDyedBlock();
                Block.addUpBlocks();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.isNetherMode()) return ItemCreator.from(CompMaterial.GRAY_CONCRETE, Message.MENU_DYED_BLOCKS.getString() + Message.MENU_LOCKED_BY_NETHER.getString()).make();
                if (Setting.isEnableDyedBlock()) return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_DYED_BLOCKS.getString() + Message.MENU_ENABLED.getString()).make();
                else return ItemCreator.from(CompMaterial.RED_CONCRETE, Message.MENU_DYED_BLOCKS.getString() + Message.MENU_DISABLED.getString()).make();
            }
        };

        // Toggle end block
        this.endBlock = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (Setting.isNetherMode()) {
                    player.sendMessage(Message.MENU_LOCKED_BY_NETHER.getString());
                    return;
                }
                Setting.toggleEndBlock();
                Block.addUpBlocks();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.isNetherMode()) return ItemCreator.from(CompMaterial.GRAY_CONCRETE, Message.MENU_END_BLOCKS.getString() + Message.MENU_LOCKED_BY_NETHER.getString()).make();
                if (Setting.isEnableEndBlock()) return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_END_BLOCKS.getString() + Message.MENU_ENABLED.getString()).make();
                else return ItemCreator.from(CompMaterial.RED_CONCRETE, Message.MENU_END_BLOCKS.getString() + Message.MENU_DISABLED.getString()).make();
            }
        };

        // Toggle addon block (only visible when addon plugin is installed)
        this.addonBlock = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (!Setting.isAddonAvailable()) return;
                if (Setting.isNetherMode()) {
                    player.sendMessage(Message.MENU_LOCKED_BY_NETHER.getString());
                    return;
                }
                Setting.toggleAddonBlock();
                Block.addUpBlocks();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (!Setting.isAddonAvailable()) return null;
                if (Setting.isNetherMode()) return ItemCreator.from(CompMaterial.GRAY_CONCRETE, Message.MENU_ADDON_BLOCKS.getString() + Message.MENU_LOCKED_BY_NETHER.getString()).make();
                if (Setting.isEnableAddonBlock()) return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_ADDON_BLOCKS.getString() + Message.MENU_ENABLED.getString()).make();
                else return ItemCreator.from(CompMaterial.RED_CONCRETE, Message.MENU_ADDON_BLOCKS.getString() + Message.MENU_DISABLED.getString()).make();
            }
        };

        // Change block amount
        this.changeBlockAmount = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                player.closeInventory();
                if (Setting.getCurrentGameMode().equals(Setting.GameMode.TIME)) {
                    editAmountPlayer.put(player.getName(), BasicListener.EditType.TIME_MODE_MINUTES);
                    player.sendMessage(Message.NOTICE_SET_TIME_MODE_MINUTES.getString());
                } else {
                    editAmountPlayer.put(player.getName(), BasicListener.EditType.BLOCK_AMOUNT);
                    player.sendMessage(Message.NOTICE_SET_BLOCKS.getString());
                }
            }

            @Override
            public ItemStack getItem() {
                if (Setting.getCurrentGameMode().equals(Setting.GameMode.TIME)) {
                    int minutes = Math.max(1, Setting.getTimeModeDurationMinutes());
                    return ItemCreator.from(CompMaterial.NAME_TAG,
                                    Message.MENU_TIME_MODE_DURATION.getString().replace("%minutes%", String.valueOf(minutes)),
                                    Message.MENU_TIME_MODE_DURATION_LORE.getStringList())
                            .make();
                }

                return ItemCreator.from(CompMaterial.NAME_TAG, Message.MENU_BLOCK_AMOUNT.getString() + Setting.getBlockAmount(), Message.MENU_BLOCK_AMOUNT_LORE.getStringList()).make();
            }
        };

        // Switch to normal mode
        this.normalMode = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (!Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL))
                    Setting.setCurrentGameMode(Setting.GameMode.NORMAL);
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                    if (Setting.getCurrentGameMode().equals(Setting.GameMode.NORMAL)) {
                    return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_CURRENT_MODE.getString() + Message.MENU_NORMAL_MODE.getString(), Message.MENU_NORMAL_MODE_LORE.getStringList()).make();
                } else {
                    return ItemCreator.from(CompMaterial.YELLOW_CONCRETE, Message.MENU_SWITCH_TO.getString() + Message.MENU_NORMAL_MODE.getString(), Message.MENU_NORMAL_MODE_LORE.getStringList()).make();
                }
            }
        };

        // Switch to racing mode
        this.racingMode = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (!Setting.getCurrentGameMode().equals(Setting.GameMode.RACING))
                    Setting.setCurrentGameMode(Setting.GameMode.RACING);
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.getCurrentGameMode().equals(Setting.GameMode.RACING)) {
                    return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_CURRENT_MODE.getString() + Message.MENU_RACING_MODE.getString(), Message.MENU_RACING_MODE_LORE.getStringList()).make();
                } else {
                    return ItemCreator.from(CompMaterial.YELLOW_CONCRETE, Message.MENU_SWITCH_TO.getString() + Message.MENU_RACING_MODE.getString(), Message.MENU_RACING_MODE_LORE.getStringList()).make();
                }
            }

        };

        // Switch to time mode
        this.timeMode = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (!Setting.getCurrentGameMode().equals(Setting.GameMode.TIME))
                    Setting.setCurrentGameMode(Setting.GameMode.TIME);
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.getCurrentGameMode().equals(Setting.GameMode.TIME)) {
                    return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_CURRENT_MODE.getString() + Message.MENU_TIME_MODE.getString(), Message.MENU_TIME_MODE_LORE.getStringList()).make();
                } else {
                    return ItemCreator.from(CompMaterial.YELLOW_CONCRETE, Message.MENU_SWITCH_TO.getString() + Message.MENU_TIME_MODE.getString(), Message.MENU_TIME_MODE_LORE.getStringList()).make();
                }
            }
        };

        // Switch to contest mode
        this.contestMode = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                if (!Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST))
                    Setting.setCurrentGameMode(Setting.GameMode.CONTEST);
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.getCurrentGameMode().equals(Setting.GameMode.CONTEST)) {
                    return ItemCreator.from(CompMaterial.GREEN_CONCRETE, Message.MENU_CURRENT_MODE.getString() + Message.MENU_CONTEST_MODE.getString(), Message.MENU_CONTEST_MODE_LORE.getStringList()).make();
                } else {
                    return ItemCreator.from(CompMaterial.YELLOW_CONCRETE, Message.MENU_SWITCH_TO.getString() + Message.MENU_CONTEST_MODE.getString(), Message.MENU_CONTEST_MODE_LORE.getStringList()).make();
                }
            }
        };

        // Toggle speed mode
        this.speedMode = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                Setting.toggleSpeedMode();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.isSpeedMode()) {
                    return ItemCreator.from(CompMaterial.ELYTRA, Message.MENU_SPEED_MODE_ENABLED.getString(), Message.MENU_SPEED_MODE_LORE.getStringList())
                            .glow(true)
                            .make();
                }

                return ItemCreator.from(CompMaterial.ELYTRA, Message.MENU_SPEED_MODE_DISABLED.getString(), Message.MENU_SPEED_MODE_LORE.getStringList()).make();
            }
        };

        // Toggle nether mode
        this.netherMode = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                Setting.toggleNetherMode();
                Block.addUpBlocks();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.isNetherMode()) {
                    return ItemCreator.from(CompMaterial.NETHERRACK, Message.MENU_NETHER_MODE_ENABLED.getString(), Message.MENU_NETHER_MODE_LORE.getStringList()).glow(true).make();
                }
                return ItemCreator.from(CompMaterial.NETHERRACK, Message.MENU_NETHER_MODE_DISABLED.getString(), Message.MENU_NETHER_MODE_LORE.getStringList()).make();
            }
        };

        // Set comeback threshold (0 disables)
        this.comebackThreshold = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                player.closeInventory();
                editAmountPlayer.put(player.getName(), BasicListener.EditType.COMEBACK_THRESHOLD);
                player.sendMessage(Message.NOTICE_SET_COMEBACK_THRESHOLD.getString());
            }

            @Override
            public ItemStack getItem() {
                int points = Setting.getComebackBuffThresholdPoints();
                CompMaterial material = points > 0 ? CompMaterial.GREEN_CONCRETE : CompMaterial.RED_CONCRETE;
                return ItemCreator.from(material,
                                Message.MENU_COMEBACK_THRESHOLD.getString().replace("%points%", String.valueOf(points)),
                                Message.MENU_COMEBACK_THRESHOLD_LORE.getStringList())
                        .make();
            }
        };

        // Toggle shared team spawn
        this.sharedTeamSpawn = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                Setting.toggleSharedTeamSpawn();
                updateMenu(PreGameMenu.this);
                updateScoreboard();
            }

            @Override
            public ItemStack getItem() {
                if (Setting.isSharedTeamSpawn()) {
                    return ItemCreator.from(CompMaterial.TOTEM_OF_UNDYING,
                                    Message.MENU_SHARED_TEAM_SPAWN_ENABLED.getString(),
                                    Message.MENU_SHARED_TEAM_SPAWN_LORE.getStringList())
                            .glow(true)
                            .make();
                }
                return ItemCreator.from(CompMaterial.TOTEM_OF_UNDYING,
                                Message.MENU_SHARED_TEAM_SPAWN_DISABLED.getString(),
                                Message.MENU_SHARED_TEAM_SPAWN_LORE.getStringList())
                        .make();
            }
        };

        // Ready
        this.ready = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                Game.playerReady(player);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.from(CompMaterial.EMERALD, Message.MENU_READY.getString(), Message.MENU_READY_LORE.getStringList()).make();
            }
        };

        // Start
        this.start = new Button() {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType click) {
                Game.checkStartDemands(player);
            }

            @Override
            public ItemStack getItem() {
                return ItemCreator.from(CompMaterial.DIAMOND, Message.MENU_START.getString(), Message.MENU_START_LORE.getStringList()).make();
            }
        };

    }

    // Generate background glass pane
    @Override
    public ItemStack getItemAt(int slot) {
        if (isGreenBackgroundSlot(slot)) {
            return ItemCreator.from(CompMaterial.LIME_STAINED_GLASS_PANE, " ").make();
        }
        if (isBlueBackgroundSlot(slot)) {
            return ItemCreator.from(CompMaterial.LIGHT_BLUE_STAINED_GLASS_PANE, " ").make();
        }
        if (slot == 10 || slot == 16) {
            return ItemCreator.from(CompMaterial.YELLOW_STAINED_GLASS_PANE, Message.MENU_SELECT_TEAM.getString()).make();
        }
        if (slot == 19 || slot == 25) {
            return ItemCreator.from(CompMaterial.YELLOW_STAINED_GLASS_PANE, Message.MENU_BLOCK_SETTING.getString()).make();
        }
        if (slot == 28 || slot == 34) {
            return ItemCreator.from(CompMaterial.YELLOW_STAINED_GLASS_PANE, Message.MENU_SELECT_MODE.getString()).make();
        }
        if (slot == 37 || slot == 43) {
            return ItemCreator.from(CompMaterial.YELLOW_STAINED_GLASS_PANE, Message.MENU_READY_AND_START.getString()).make();
        }
        if (slot == 24 && !Setting.isAddonAvailable()) {
            return ItemCreator.from(CompMaterial.LIGHT_BLUE_STAINED_GLASS_PANE, " ").make();
        }

        return super.getItemAt(slot);
    }

    private boolean isGreenBackgroundSlot(int slot) {
        int[] validSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};

        for (int validSlot : validSlots) {
            if (validSlot == slot) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlueBackgroundSlot(int slot) {
        int[] validSlots = {13, 14, 15, 31, 32, 40, 41, 42};

        for (int validSlot : validSlots) {
            if (validSlot == slot) {
                return true;
            }
        }

        return false;
    }
}
