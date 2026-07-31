package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Setting {
    private static void persistConfigNow() {
        Config.saveConfig();
    }

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
    private static boolean teamChestGift;
    @Getter
    private static int teamChestGiftAmount;
    @Getter
    private static boolean sharedTeamSpawn;
    @Getter
    private static boolean endPortalCoordinateBroadcast;
    @Getter
    private static boolean comebackBuffEnabled;
    @Getter
    private static int comebackBuffThresholdPoints;
    @Getter
    private static int timeModeDurationMinutes;
    @Getter
    private static List<ItemStack> baseModeItems = List.of();
    @Getter
    private static List<ItemStack> speedModeItems = List.of();
    @Getter
    private static List<SpeedModeEffectOption> speedModeEffects = List.of();
    @Getter
    private static List<PotionEffect> comebackBuffEffects = List.of();

    @Getter
    public static class SpeedModeEffectOption {
        private final PotionEffect effect;
        private final long delayTicks;

        public SpeedModeEffectOption(PotionEffect effect, long delayTicks) {
            this.effect = effect;
            this.delayTicks = Math.max(0L, delayTicks);
        }
    }

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
        teamChestGift = Config.TEAM_CHEST_GIFT.getBoolean();
        teamChestGiftAmount = Math.max(1, Math.min(64, Config.TEAM_CHEST_GIFT_AMOUNT.getInt()));
        sharedTeamSpawn = Config.SHARED_TEAM_SPAWN.getBoolean();
        endPortalCoordinateBroadcast = Config.END_PORTAL_COORDINATE_BROADCAST.getBoolean();
        int configuredThreshold = Math.max(0, Config.COMEBACK_BUFF_THRESHOLD.getInt());
        comebackBuffThresholdPoints = configuredThreshold;
        comebackBuffEnabled = comebackBuffThresholdPoints > 0;
        maxTeamChestNum = Config.MAX_TEAM_CHEST_NUM.getInt();
        maxTeamWaypointNum = Config.MAX_TEAM_WAYPOINT_NUM.getInt();
        setCurrentGameMode(GameMode.fromConfig(Config.GAME_MODE.getString()), false);

        int configuredMinutes = Config.TIME_MODE_DURATION.getInt();
        if (configuredMinutes <= 0) {
            timeModeDurationMinutes = 90;
            Config.TIME_MODE_DURATION.setInt(timeModeDurationMinutes);
        } else {
            timeModeDurationMinutes = configuredMinutes;
        }

        loadSpeedModeKitConfig();
        loadComebackBuffEffectConfig();
    }

    private static void loadSpeedModeKitConfig() {
        List<ItemStack> parsedBaseItems = parseConfiguredItems("base-mode-items", "base-mode item");
        List<ItemStack> parsedItems = parseSpeedModeItems();
        List<SpeedModeEffectOption> parsedEffects = parseSpeedModeEffects();

        if (parsedBaseItems.isEmpty()) {
            parsedBaseItems = createDefaultBaseModeItems();
        }
        if (parsedItems.isEmpty()) {
            parsedItems = createDefaultSpeedModeItems();
        }
        if (parsedEffects.isEmpty()) {
            parsedEffects = createDefaultSpeedModeEffects();
        }

        baseModeItems = List.copyOf(parsedBaseItems);
        speedModeItems = List.copyOf(parsedItems);
        speedModeEffects = List.copyOf(parsedEffects);
    }

    private static List<ItemStack> parseSpeedModeItems() {
        return parseConfiguredItems("speed-mode-items", "speed-mode item");
    }

    private static List<ItemStack> parseConfiguredItems(String path, String logLabel) {
        List<ItemStack> result = new ArrayList<>();
        List<Map<?, ?>> rawItems = Config.getConfig().getMapList(path);
        for (Map<?, ?> rawItem : rawItems) {
            try {
                ItemStack item = buildItemFromConfig(rawItem);
                if (item != null) {
                    result.add(item);
                }
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[BlockRacing] Invalid " + logLabel + " config: " + rawItem);
            }
        }
        return result;
    }

    private static ItemStack buildItemFromConfig(Map<?, ?> rawItem) {
        Object materialObj = rawItem.get("material");
        if (materialObj == null) {
            return null;
        }

        Material material = Material.matchMaterial(String.valueOf(materialObj));
        if (material == null) {
            return null;
        }

        int amount = parseInt(rawItem.get("amount"), 1);
        amount = Math.max(1, Math.min(64, amount));

        ItemStack item = new ItemStack(material, amount);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Object enchantsObj = rawItem.get("enchants");
        if (enchantsObj instanceof Map<?, ?> enchants) {
            for (Map.Entry<?, ?> entry : enchants.entrySet()) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(String.valueOf(entry.getKey()).toLowerCase()));
                if (enchantment == null) {
                    continue;
                }
                int level = Math.max(1, parseInt(entry.getValue(), 1));
                meta.addEnchant(enchantment, level, true);
            }
        }

        Object storedObj = rawItem.get("stored-enchants");
        if (storedObj instanceof Map<?, ?> stored && meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<?, ?> entry : stored.entrySet()) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(String.valueOf(entry.getKey()).toLowerCase()));
                if (enchantment == null) {
                    continue;
                }
                int level = Math.max(1, parseInt(entry.getValue(), 1));
                storageMeta.addStoredEnchant(enchantment, level, true);
            }
            meta = storageMeta;
        }

        if (meta instanceof Repairable repairable) {
            int repairCost = parseInt(rawItem.get("repair-cost"), -1);
            if (repairCost >= 0) {
                repairable.setRepairCost(repairCost);
            }
        }

        item.setItemMeta(meta);

        int damageMaxOffset = parseInt(rawItem.get("damage-max-offset"), 0);
        if (damageMaxOffset > 0 && item.getType().getMaxDurability() > 0) {
            ItemMeta damageMeta = item.getItemMeta();
            if (damageMeta instanceof Damageable damageable) {
                damageable.setDamage(Math.max(0, item.getType().getMaxDurability() - damageMaxOffset));
                item.setItemMeta(damageMeta);
            }
        }

        return item;
    }

    private static List<SpeedModeEffectOption> parseSpeedModeEffects() {
        List<SpeedModeEffectOption> result = new ArrayList<>();
        List<Map<?, ?>> rawEffects = Config.getConfig().getMapList("speed-mode-effects");
        for (Map<?, ?> rawEffect : rawEffects) {
            try {
                SpeedModeEffectOption option = buildEffectFromConfig(rawEffect);
                if (option != null) {
                    result.add(option);
                }
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[BlockRacing] Invalid speed-mode effect config: " + rawEffect);
            }
        }
        return result;
    }

    private static void loadComebackBuffEffectConfig() {
        List<PotionEffect> parsedEffects = parseComebackBuffEffects();
        if (parsedEffects.isEmpty()) {
            parsedEffects = createDefaultComebackBuffEffects();
        }
        comebackBuffEffects = List.copyOf(parsedEffects);
    }

    private static List<PotionEffect> parseComebackBuffEffects() {
        List<PotionEffect> result = new ArrayList<>();
        List<Map<?, ?>> rawEffects = Config.getConfig().getMapList("comeback-buff-effects");
        for (Map<?, ?> rawEffect : rawEffects) {
            try {
                PotionEffect effect = buildPotionEffectFromConfig(rawEffect);
                if (effect != null) {
                    result.add(effect);
                }
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[BlockRacing] Invalid comeback-buff effect config: " + rawEffect);
            }
        }
        return result;
    }

    private static SpeedModeEffectOption buildEffectFromConfig(Map<?, ?> rawEffect) {
        PotionEffect effect = buildPotionEffectFromConfig(rawEffect);
        if (effect == null) {
            return null;
        }
        long delayTicks = Math.max(0L, parseLong(rawEffect.get("delay-ticks"), 0L));

        return new SpeedModeEffectOption(effect, delayTicks);
    }

    private static PotionEffect buildPotionEffectFromConfig(Map<?, ?> rawEffect) {
        Object typeObj = rawEffect.get("type");
        if (typeObj == null) {
            return null;
        }
        PotionEffectType effectType = Registry.EFFECT.get(NamespacedKey.minecraft(String.valueOf(typeObj).toLowerCase()));
        if (effectType == null) {
            return null;
        }

        int amplifier = Math.max(0, parseInt(rawEffect.get("amplifier"), 0));
        int durationTicks = parseInt(rawEffect.get("duration-ticks"), -1);
        boolean ambient = parseBoolean(rawEffect.get("ambient"), false);
        boolean particles = parseBoolean(rawEffect.get("particles"), false);
        boolean icon = parseBoolean(rawEffect.get("icon"), false);

        return new PotionEffect(effectType, durationTicks, amplifier, ambient, particles, icon);
    }

    private static List<ItemStack> createDefaultSpeedModeItems() {
        List<ItemStack> defaults = new ArrayList<>();

        ItemStack pickaxe = new ItemStack(Material.IRON_PICKAXE, 1);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        if (pickaxeMeta != null) {
            pickaxeMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
            pickaxe.setItemMeta(pickaxeMeta);
        }
        defaults.add(pickaxe);

        defaults.add(new ItemStack(Material.GOLDEN_CARROT, 64));

        ItemStack elytra = new ItemStack(Material.ELYTRA, 1);
        ItemMeta elytraMeta = elytra.getItemMeta();
        if (elytraMeta instanceof Damageable damageable && elytra.getType().getMaxDurability() > 0) {
            damageable.setDamage(elytra.getType().getMaxDurability() - 1);
            elytra.setItemMeta(elytraMeta);
        }
        elytraMeta = elytra.getItemMeta();
        if (elytraMeta instanceof Repairable repairable) {
            repairable.setRepairCost(15);
            elytra.setItemMeta((ItemMeta) repairable);
        }
        defaults.add(elytra);

        ItemStack mendingBook = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta mendingMeta = mendingBook.getItemMeta();
        if (mendingMeta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.addStoredEnchant(Enchantment.MENDING, 1, true);
            mendingBook.setItemMeta(storageMeta);
        }
        defaults.add(mendingBook);

        return defaults;
    }

    private static List<ItemStack> createDefaultBaseModeItems() {
        List<ItemStack> defaults = new ArrayList<>();
        defaults.add(new ItemStack(Material.STONE_PICKAXE, 1));
        defaults.add(new ItemStack(Material.STONE_AXE, 1));
        defaults.add(new ItemStack(Material.STONE_SHOVEL, 1));
        return defaults;
    }

    private static List<SpeedModeEffectOption> createDefaultSpeedModeEffects() {
        List<SpeedModeEffectOption> defaults = new ArrayList<>();
        defaults.add(new SpeedModeEffectOption(new PotionEffect(PotionEffectType.HASTE, -1, 4, false, false), 0L));
        defaults.add(new SpeedModeEffectOption(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false), 0L));
        defaults.add(new SpeedModeEffectOption(new PotionEffect(PotionEffectType.RESISTANCE, -1, 1, false, false), 1300L));
        return defaults;
    }

    private static List<PotionEffect> createDefaultComebackBuffEffects() {
        List<PotionEffect> defaults = new ArrayList<>();
        defaults.add(new PotionEffect(PotionEffectType.SPEED, -1, 2, false, false));
        defaults.add(new PotionEffect(PotionEffectType.RESISTANCE, -1, 2, false, false));
        return defaults;
    }

    private static int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static long parseLong(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean parseBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static void setEnableMediumBlock(boolean enableMediumBlock) {
        Setting.enableMediumBlock = enableMediumBlock;
        Config.MEDIUM_BLOCK.setBoolean(enableMediumBlock);
        persistConfigNow();
    }

    public static void setEnableHardBlock(boolean enableHardBlock) {
        Setting.enableHardBlock = enableHardBlock;
        Config.HARD_BLOCK.setBoolean(enableHardBlock);
        persistConfigNow();
    }

    public static void setEnableDyedBlock(boolean enableDyedBlock) {
        Setting.enableDyedBlock = enableDyedBlock;
        Config.DYED_BLOCK.setBoolean(enableDyedBlock);
        persistConfigNow();
    }

    public static void setEnableEndBlock(boolean enableEndBlock) {
        Setting.enableEndBlock = enableEndBlock;
        Config.END_BLOCK.setBoolean(enableEndBlock);
        persistConfigNow();
    }

    public static void setEnableAddonBlock(boolean enableAddonBlock) {
        if (!addonAvailable) {
            Setting.enableAddonBlock = false;
            Config.ADDON_BLOCK.setBoolean(false);
            persistConfigNow();
            return;
        }
        Setting.enableAddonBlock = enableAddonBlock;
        Config.ADDON_BLOCK.setBoolean(enableAddonBlock);
        persistConfigNow();
    }

    public static void setNetherMode(boolean netherMode) {
        Setting.netherMode = netherMode;
        Config.NETHER_MODE.setBoolean(netherMode);
        persistConfigNow();
    }

    public static void setBlockAmount(int blockAmount) {
        Setting.blockAmount = blockAmount;
        Config.BLOCK_AMOUNT.setInt(blockAmount);
        persistConfigNow();
    }

    public static void setMaxTeamChestNum(int chestNum) {
        Setting.maxTeamChestNum = Math.max(1, Math.min(53, chestNum));
        Config.MAX_TEAM_CHEST_NUM.setInt(Setting.maxTeamChestNum);
        persistConfigNow();
    }

    public static void setMaxTeamWaypointNum(int waypointNum) {
        Setting.maxTeamWaypointNum = Math.max(1, Math.min(53, waypointNum));
        Config.MAX_TEAM_WAYPOINT_NUM.setInt(Setting.maxTeamWaypointNum);
        persistConfigNow();
    }

    public static void setMaxRollCount(int rollCount) {
        int clamped = Math.max(0, Math.min(100, rollCount));
        Config.MAX_ROLL_COUNT.setInt(clamped);
        persistConfigNow();
    }

    public static void setSpeedMode(boolean speedMode) {
        Setting.speedMode = speedMode;
        Config.SPEED_MODE.setBoolean(speedMode);
        persistConfigNow();
    }

    public static void setTeamChestGift(boolean teamChestGift) {
        Setting.teamChestGift = teamChestGift;
        Config.TEAM_CHEST_GIFT.setBoolean(teamChestGift);
        persistConfigNow();
    }

    public static void setTeamChestGiftAmount(int amount) {
        Setting.teamChestGiftAmount = Math.max(1, Math.min(64, amount));
        Config.TEAM_CHEST_GIFT_AMOUNT.setInt(Setting.teamChestGiftAmount);
        persistConfigNow();
    }

    public static void setSharedTeamSpawn(boolean sharedTeamSpawn) {
        Setting.sharedTeamSpawn = sharedTeamSpawn;
        Config.SHARED_TEAM_SPAWN.setBoolean(sharedTeamSpawn);
        persistConfigNow();
    }

    public static void setEndPortalCoordinateBroadcast(boolean endPortalCoordinateBroadcast) {
        Setting.endPortalCoordinateBroadcast = endPortalCoordinateBroadcast;
        Config.END_PORTAL_COORDINATE_BROADCAST.setBoolean(endPortalCoordinateBroadcast);
        persistConfigNow();
    }

    public static void setComebackBuffEnabled(boolean enabled) {
        if (!enabled) {
            setComebackBuffThresholdPoints(0);
            return;
        }
        if (comebackBuffThresholdPoints <= 0) {
            setComebackBuffThresholdPoints(20);
            return;
        }
        Setting.comebackBuffEnabled = true;
    }

    public static void setComebackBuffThresholdPoints(int points) {
        int finalPoints = Math.max(0, Math.min(1000, points));
        Setting.comebackBuffThresholdPoints = finalPoints;
        Config.COMEBACK_BUFF_THRESHOLD.setInt(finalPoints);
        Setting.comebackBuffEnabled = finalPoints > 0;
        persistConfigNow();
    }

    public static void setTimeModeDurationMinutes(int minutes) {
        Setting.timeModeDurationMinutes = minutes;
        Config.TIME_MODE_DURATION.setInt(minutes);
        persistConfigNow();
    }

    public static void setCurrentGameMode(GameMode mode) {
        setCurrentGameMode(mode, true);
    }

    private static void setCurrentGameMode(GameMode mode, boolean persist) {
        if (mode == null) {
            mode = GameMode.NORMAL;
        }
        Setting.currentGameMode = mode;
        Config.GAME_MODE.setString(mode.getConfigValue());
        if (persist) {
            persistConfigNow();
        }
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

    public static void toggleTeamChestGift() {
        setTeamChestGift(!isTeamChestGift());
    }

    public static void toggleSharedTeamSpawn() {
        setSharedTeamSpawn(!isSharedTeamSpawn());
    }

    public static void toggleEndPortalCoordinateBroadcast() {
        setEndPortalCoordinateBroadcast(!isEndPortalCoordinateBroadcast());
    }

    public static void toggleComebackBuff() {
        setComebackBuffEnabled(!isComebackBuffEnabled());
    }

    public static int getMaxRollCount() {
        return Config.MAX_ROLL_COUNT.getInt();
    }
}
