package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import lombok.Getter;

public class Setting {
    @Getter
    private static boolean enableMediumBlock;
    @Getter
    private static boolean enableHardBlock;
    @Getter
    private static boolean enableDyedBlock;
    @Getter
    private static boolean enableEndBlock;
    @Getter
    private static boolean enableAddonBlock;
    @Getter
    private static boolean addonAvailable;
    @Getter
    private static boolean netherMode;
    @Getter
    private static int blockAmount;
    @Getter
    private static int maxTeamChestNum;
    @Getter
    private static int maxTeamWaypointNum;
    @Getter
    private static boolean speedMode;
    @Getter
    private static boolean comebackBuffEnabled;
    @Getter
    private static int timeModeDurationMinutes;

    public enum GameMode {
        NORMAL("normal"),
        RACING("racing"),
        CONTEST("contest"),
        TIME("time");

        private final String configValue;

        GameMode(String configValue) {
            this.configValue = configValue;
        }

        public String getConfigValue() {
            return configValue;
        }

        public static GameMode fromConfig(String raw) {
            if (raw == null) {
                return NORMAL;
            }
            for (GameMode mode : values()) {
                if (mode.configValue.equalsIgnoreCase(raw)) {
                    return mode;
                }
            }
            return NORMAL;
        }
    }
    @Getter
    private static GameMode currentGameMode = GameMode.NORMAL;

    /**
     * Refresh the addon availability flag without altering other settings.
     * Returns true when BlockRacingAddon is present and enabled.
     */
    public static boolean refreshAddonAvailability() {
        org.bukkit.plugin.Plugin addonPlugin = Bukkit.getPluginManager().getPlugin("BlockRacingAddon");
        addonAvailable = addonPlugin != null && addonPlugin.isEnabled();

        if (!addonAvailable) {
            // Ensure addon-specific switches are off when the addon is missing
            enableAddonBlock = false;
        }
        return addonAvailable;
    }

    public static void getSettings(){
        refreshAddonAvailability();
        enableMediumBlock = Config.MEDIUM_BLOCK.getBoolean();
        enableHardBlock = Config.HARD_BLOCK.getBoolean();
        enableDyedBlock = Config.DYED_BLOCK.getBoolean();
        enableEndBlock = Config.END_BLOCK.getBoolean();
        enableAddonBlock = addonAvailable && Config.ADDON_BLOCK.getBoolean();
        netherMode = Config.NETHER_MODE.getBoolean();
        blockAmount = Config.BLOCK_AMOUNT.getInt();
        speedMode = Config.SPEED_MODE.getBoolean();
        comebackBuffEnabled = Config.COMEBACK_BUFF.getBoolean();
        maxTeamChestNum = Config.MAX_TEAM_CHEST_NUM.getInt();
        maxTeamWaypointNum = Config.MAX_TEAM_WAYPOINT_NUM.getInt();
        setCurrentGameMode(GameMode.fromConfig(Config.GAME_MODE.getString()));

        int configuredMinutes = Config.TIME_MODE_DURATION.getInt();
        if (configuredMinutes <= 0) {
            timeModeDurationMinutes = 90;
            Config.TIME_MODE_DURATION.setInt(timeModeDurationMinutes);
        } else {
            timeModeDurationMinutes = configuredMinutes;
        }
    }

    public static void setEnableMediumBlock(boolean enableMediumBlock) {
        Setting.enableMediumBlock = enableMediumBlock;
        Config.MEDIUM_BLOCK.setBoolean(enableMediumBlock);
    }

    public static void setEnableHardBlock(boolean enableHardBlock) {
        Setting.enableHardBlock = enableHardBlock;
        Config.HARD_BLOCK.setBoolean(enableHardBlock);
    }

    public static void setEnableDyedBlock(boolean enableDyedBlock) {
        Setting.enableDyedBlock = enableDyedBlock;
        Config.DYED_BLOCK.setBoolean(enableDyedBlock);
    }

    public static void setEnableEndBlock(boolean enableEndBlock) {
        Setting.enableEndBlock = enableEndBlock;
        Config.END_BLOCK.setBoolean(enableEndBlock);
    }

    public static void setEnableAddonBlock(boolean enableAddonBlock) {
        if (!addonAvailable) {
            Setting.enableAddonBlock = false;
            Config.ADDON_BLOCK.setBoolean(false);
            return;
        }
        Setting.enableAddonBlock = enableAddonBlock;
        Config.ADDON_BLOCK.setBoolean(enableAddonBlock);
    }

    public static void setNetherMode(boolean netherMode) {
        Setting.netherMode = netherMode;
        Config.NETHER_MODE.setBoolean(netherMode);
    }

    public static void setBlockAmount(int blockAmount) {
        Setting.blockAmount = blockAmount;
        Config.BLOCK_AMOUNT.setInt(blockAmount);
    }

    public static void setMaxTeamChestNum(int chestNum) {
        Setting.maxTeamChestNum = chestNum;
        Config.MAX_TEAM_CHEST_NUM.setInt(chestNum);
    }

    public static void setSpeedMode(boolean speedMode) {
        Setting.speedMode = speedMode;
        Config.SPEED_MODE.setBoolean(speedMode);
    }

    public static void setComebackBuffEnabled(boolean enabled) {
        Setting.comebackBuffEnabled = enabled;
        Config.COMEBACK_BUFF.setBoolean(enabled);
    }

    public static void setTimeModeDurationMinutes(int minutes) {
        Setting.timeModeDurationMinutes = minutes;
        Config.TIME_MODE_DURATION.setInt(minutes);
    }

    public static void setCurrentGameMode(GameMode mode) {
        if (mode == null) {
            mode = GameMode.NORMAL;
        }
        Setting.currentGameMode = mode;
        Config.GAME_MODE.setString(mode.getConfigValue());
    }

    public static int getTimeModeDurationSeconds() {
        return timeModeDurationMinutes * 60;
    }

    public static void toggleMediumBlock() {
        setEnableMediumBlock(!isEnableMediumBlock());
    }

    public static void toggleHardBlock() {
        setEnableHardBlock(!isEnableHardBlock());
    }

    public static void toggleDyedBlock() {
        setEnableDyedBlock(!isEnableDyedBlock());
    }

    public static void toggleEndBlock() {
        setEnableEndBlock(!isEnableEndBlock());
    }

    public static void toggleAddonBlock() {
        setEnableAddonBlock(!isEnableAddonBlock());
    }

    public static void toggleNetherMode() {
        setNetherMode(!isNetherMode());
    }

    public static void toggleSpeedMode() {
        setSpeedMode(!isSpeedMode());
    }

    public static void toggleComebackBuff() {
        setComebackBuffEnabled(!isComebackBuffEnabled());
    }
}
