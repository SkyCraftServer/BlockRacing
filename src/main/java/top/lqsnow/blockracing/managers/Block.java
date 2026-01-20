package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
    
import top.lqsnow.blockracing.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static top.lqsnow.blockracing.managers.Gui.checkBlockInventory;
import static top.lqsnow.blockracing.utils.CommandUtil.sendAll;

public class Block {
    public static List<String> easyBlocks, mediumBlocks, hardBlocks, dyedBlocks, endBlocks, addonBlocks, blocks;
    public static List<String> allBlocks = new ArrayList<>();
    public static int maxBlockAmount;
    public static List<String> redTeamBlocks = new ArrayList<>();
    public static List<String> blueTeamBlocks = new ArrayList<>();
    public static List<String> redTeamRemainingBlocks = new ArrayList<>();
    public static List<String> blueTeamRemainingBlocks = new ArrayList<>();
    public static List<String> redCompletedBlocks = new ArrayList<>();
    public static List<String> blueCompletedBlocks = new ArrayList<>();

    public Block() {
        easyBlocks = List.of(readFile("EasyBlocks.txt"));
        mediumBlocks = List.of(readFile("MediumBlocks.txt"));
        hardBlocks = List.of(readFile("HardBlocks.txt"));
        dyedBlocks = List.of(readFile("DyedBlocks.txt"));
        endBlocks = List.of(readFile("EndBlocks.txt"));
        addonBlocks = Setting.isAddonAvailable() ? List.of(readFile("AddonBlocks.txt")) : List.of();
        addUpBlocks();
    }

    public static void addUpBlocks() {
        allBlocks.clear();
        allBlocks.addAll(List.copyOf(easyBlocks));
        if (Setting.isEnableMediumBlock()) allBlocks.addAll(List.copyOf(mediumBlocks));
        if (Setting.isEnableHardBlock()) allBlocks.addAll(List.copyOf(hardBlocks));
        if (Setting.isEnableDyedBlock()) allBlocks.addAll(List.copyOf(dyedBlocks));
        if (Setting.isEnableEndBlock()) allBlocks.addAll(List.copyOf(endBlocks));
        if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock()) allBlocks.addAll(List.copyOf(addonBlocks));
        blocks = List.copyOf(allBlocks);
        maxBlockAmount = blocks.size();
    }

    public static void setupBlocks() {
        addUpBlocks();
        redTeamRemainingBlocks = new ArrayList<>();
        blueTeamRemainingBlocks = new ArrayList<>();

        switch (Setting.getCurrentGameMode()) {
            case NORMAL -> {
                redTeamBlocks = generateBlocks();
                blueTeamBlocks = generateBlocks();
                redTeamRemainingBlocks.addAll(List.copyOf(redTeamBlocks));
                blueTeamRemainingBlocks.addAll(List.copyOf(blueTeamBlocks));
            }
            case RACING -> {
                List<String> shared = generateBlocks();
                redTeamBlocks = new ArrayList<>(shared);
                blueTeamBlocks = new ArrayList<>(shared);
                redTeamRemainingBlocks.addAll(List.copyOf(shared));
                blueTeamRemainingBlocks.addAll(List.copyOf(shared));
            }
            case CONTEST -> {
                int contestTarget = Math.max(1, (Setting.getBlockAmount() * 2) - 1);
                List<String> sharedPool = generateBlocks(contestTarget);
                redTeamBlocks = new ArrayList<>(sharedPool);
                blueTeamBlocks = new ArrayList<>(sharedPool);
                // Shared remaining list so that completion by one team removes the target for both
                List<String> sharedRemaining = new ArrayList<>(sharedPool);
                redTeamRemainingBlocks = sharedRemaining;
                blueTeamRemainingBlocks = sharedRemaining;
            }
            case TIME -> {
                redTeamBlocks = new ArrayList<>();
                blueTeamBlocks = new ArrayList<>();

                // Initialize up to 4 visible targets for each team at game start.
                int initialVisible = Math.min(4, blocks.size());
                for (int i = 0; i < initialVisible; i++) {
                    redTeamRemainingBlocks.add(selectBlockByTimeProgress("red", 0f));
                    blueTeamRemainingBlocks.add(selectBlockByTimeProgress("blue", 0f));
                }
            }
        }
        redCompletedBlocks.clear();
        blueCompletedBlocks.clear();
        Bukkit.getLogger().info("[BlockRacing] Blocks generate complete.");
        Bukkit.getLogger().info("Red team blocks: " + redTeamBlocks.toString());
        Bukkit.getLogger().info("Blue team blocks: " + blueTeamBlocks.toString());
    }

    // Select a block based on time-progress-derived weights for the specified team.
    // progress: 0..1 (0 at game start, 1 at game end)
    public static String selectBlockByTimeProgress(String team, float progress) {
        // Build available lists excluding already used (remaining + completed) for that team
        Set<String> used = new HashSet<>();
        if (team.equals("red")) {
            used.addAll(redCompletedBlocks);
            used.addAll(redTeamRemainingBlocks);
        } else {
            used.addAll(blueCompletedBlocks);
            used.addAll(blueTeamRemainingBlocks);
        }

        List<String> easyTemp = new ArrayList<>();
        for (String s : easyBlocks) if (!used.contains(s)) easyTemp.add(s);
        List<String> mediumTemp = new ArrayList<>();
        for (String s : mediumBlocks) if (!used.contains(s)) mediumTemp.add(s);
        List<String> hardTemp = new ArrayList<>();
        for (String s : hardBlocks) if (!used.contains(s)) hardTemp.add(s);
        List<String> dyedTemp = new ArrayList<>();
        for (String s : dyedBlocks) if (!used.contains(s)) dyedTemp.add(s);
        List<String> endTemp = new ArrayList<>();
        for (String s : endBlocks) if (!used.contains(s)) endTemp.add(s);
        List<String> addonTemp = new ArrayList<>();
        if (Setting.isAddonAvailable() && Setting.isEnableAddonBlock()) {
            for (String s : addonBlocks) if (!used.contains(s)) addonTemp.add(s);
        }

        int easyWeight = easyTemp.isEmpty() ? 0 : calculateEasyBlocksWeight(progress);
        int mediumWeight = mediumTemp.isEmpty() ? 0 : (Setting.isEnableMediumBlock() ? calculateMediumBlocksWeight(progress) : 0);
        int hardWeight = hardTemp.isEmpty() ? 0 : (Setting.isEnableHardBlock() ? calculateHardBlocksWeight(progress) : 0);
        int dyedWeight = dyedTemp.isEmpty() ? 0 : (Setting.isEnableDyedBlock() ? calculateDyedBlocksWeight(progress) : 0);
        int endWeight = endTemp.isEmpty() ? 0 : (Setting.isEnableEndBlock() ? calculateEndBlocksWeight(progress) : 0);
        int addonWeight = addonTemp.isEmpty() ? 0 : (Setting.isAddonAvailable() && Setting.isEnableAddonBlock() ? calculateAddonBlocksWeight(progress) : 0);

        int totalWeight = easyWeight + mediumWeight + hardWeight + dyedWeight + endWeight + addonWeight;

        List<String> available = new ArrayList<>();
        for (String s : blocks) if (!used.contains(s)) available.add(s);

        Random random = new Random();
        if (totalWeight <= 0) {
            // If no weighted choice possible, fallback to any available block
            if (!available.isEmpty()) return available.get(random.nextInt(available.size()));
            // If none available (all used), allow repeating by picking from global pool
            return blocks.get(random.nextInt(blocks.size()));
        }

        int r = random.nextInt(totalWeight);
        if (r < easyWeight && !easyTemp.isEmpty()) return easyTemp.get(random.nextInt(easyTemp.size()));
        r -= easyWeight;
        if (r < mediumWeight && !mediumTemp.isEmpty()) return mediumTemp.get(random.nextInt(mediumTemp.size()));
        r -= mediumWeight;
        if (r < hardWeight && !hardTemp.isEmpty()) return hardTemp.get(random.nextInt(hardTemp.size()));
        r -= hardWeight;
        if (r < dyedWeight && !dyedTemp.isEmpty()) return dyedTemp.get(random.nextInt(dyedTemp.size()));
        r -= dyedWeight;
        if (r < addonWeight && !addonTemp.isEmpty()) return addonTemp.get(random.nextInt(addonTemp.size()));
        r -= addonWeight;
        if (!endTemp.isEmpty()) return endTemp.get(random.nextInt(endTemp.size()));

        // Final fallback
        if (!available.isEmpty()) return available.get(random.nextInt(available.size()));
        return blocks.get(random.nextInt(blocks.size()));
    }

    

    private static List<String> generateBlocks() {
        return generateBlocks(Setting.getBlockAmount());
    }

    private static List<String> generateBlocks(int targetAmount) {

        addUpBlocks();

        // Create temporary file to generate blocks
        List<String> blocksTemp = new ArrayList<>(blocks);
        List<String> easyTemp = new ArrayList<>(easyBlocks);
        List<String> mediumTemp = new ArrayList<>(mediumBlocks);
        List<String> hardTemp = new ArrayList<>(hardBlocks);
        List<String> dyedTemp = new ArrayList<>(dyedBlocks);
        List<String> endTemp = new ArrayList<>(endBlocks);
        List<String> addonTemp = new ArrayList<>(addonBlocks);

        int blockAmount = Math.min(targetAmount, blocksTemp.size());
        List<String> targetBlocks = new ArrayList<>();

        for (int i = 0; i < blockAmount; i++) {
            // Calculate weights for each difficulty
            int easyWeight = 0;
            int mediumWeight = 0;
            int hardWeight = 0;
            int dyedWeight = 0;
            int endWeight = 0;
            int addonWeight = 0;

            float progress = blockAmount <= 1 ? 1f : (float) i / (float) blockAmount;

            if (!easyTemp.isEmpty()) easyWeight = calculateEasyBlocksWeight(progress);

            if (!mediumTemp.isEmpty())
                mediumWeight = Setting.isEnableMediumBlock() ? calculateMediumBlocksWeight(progress) : 0;

            if (!hardTemp.isEmpty())
                hardWeight = Setting.isEnableHardBlock() ? calculateHardBlocksWeight(progress) : 0;

            if (!dyedTemp.isEmpty())
                dyedWeight = Setting.isEnableDyedBlock() ? calculateDyedBlocksWeight(progress) : 0;

            if (!endTemp.isEmpty())
                endWeight = Setting.isEnableEndBlock() ? calculateEndBlocksWeight(progress) : 0;

            if (!addonTemp.isEmpty())
                addonWeight = (Setting.isAddonAvailable() && Setting.isEnableAddonBlock()) ? calculateAddonBlocksWeight(progress) : 0;


            // Choose difficulty based on weights
            String difficulty = chooseDifficulty(easyWeight, mediumWeight, hardWeight, dyedWeight, endWeight, addonWeight);

            // Select a block from the corresponding difficulty list
            String selectedBlock = selectBlock(difficulty, easyTemp, mediumTemp, hardTemp, dyedTemp, endTemp, addonTemp);

            // Add the selected block to targetBlocks
            targetBlocks.add(selectedBlock);

            // Remove the selected block from the corresponding difficulty list
            switch (difficulty) {
                case "easy" -> easyTemp.remove(selectedBlock);
                case "medium" -> mediumTemp.remove(selectedBlock);
                case "hard" -> hardTemp.remove(selectedBlock);
                case "dyed" -> dyedTemp.remove(selectedBlock);
                case "end" -> endTemp.remove(selectedBlock);
                case "addon" -> addonTemp.remove(selectedBlock);
                default -> throw new IllegalArgumentException("Invalid difficulty");
            }

            // Remove the selected block from the total blocksTemp
            blocksTemp.remove(selectedBlock);
        }

        return targetBlocks;
    }

    // Method to choose difficulty based on weights
    private static String chooseDifficulty(int easyWeight, int mediumWeight, int hardWeight, int dyedWeight, int endWeight, int addonWeight) {
        int totalWeight = easyWeight + mediumWeight + hardWeight + dyedWeight + endWeight + addonWeight;
        int randomNumber = new Random().nextInt(totalWeight);

        if (randomNumber < easyWeight) {
            return "easy";
        } else if (randomNumber < easyWeight + mediumWeight) {
            return "medium";
        } else if (randomNumber < easyWeight + mediumWeight + hardWeight) {
            return "hard";
        } else if (randomNumber < easyWeight + mediumWeight + hardWeight + dyedWeight) {
            return "dyed";
        } else if (randomNumber < easyWeight + mediumWeight + hardWeight + dyedWeight + addonWeight) {
            return "addon";
        } else {
            return "end";
        }
    }

    // Method to select a block from the corresponding difficulty list
    private static String selectBlock(String difficulty, List<String> easyTemp, List<String> mediumTemp,
                                      List<String> hardTemp, List<String> dyedTemp, List<String> endTemp, List<String> addonTemp) {
        return switch (difficulty) {
            case "easy" -> selectRandomBlockFromList(easyTemp);
            case "medium" -> selectRandomBlockFromList(mediumTemp);
            case "hard" -> selectRandomBlockFromList(hardTemp);
            case "dyed" -> selectRandomBlockFromList(dyedTemp);
            case "addon" -> selectRandomBlockFromList(addonTemp);
            case "end" -> selectRandomBlockFromList(endTemp);
            default -> throw new IllegalArgumentException("Invalid difficulty");
        };
    }

    // Method to select a random block from a list
    private static String selectRandomBlockFromList(List<String> blockList) {
        int randomIndex = new Random().nextInt(blockList.size());
        return blockList.get(randomIndex);
    }

    // Calculate weight for easy blocks
    // Weight decreases from 100 to 20 as progress goes from 0 to 1
    public static int calculateEasyBlocksWeight(float progress) {
        return (int) (100 - 80 * progress);
    }

    // Calculate weight for medium blocks
    // Weight increases from 20 to 60 as progress goes from 0 to 0.4,
    // then remains constant at 60 as progress goes from 0.4 to 1
    public static int calculateMediumBlocksWeight(float progress) {
        if (progress <= 0.4) {
            return (int) (20 + 40 * progress / 0.4);
        } else {
            return 60;
        }
    }

    // Calculate weight for hard blocks
    // Weight increases from 1 to 20 as progress goes from 0 to 0.5,
    // then increases from 20 to 60 as progress goes from 0.5 to 1
    public static int calculateHardBlocksWeight(float progress) {
        if (progress <= 0.5) {
            return (int) (1 + 19 * progress / 0.5);
        } else {
            return (int) (20 + 40 * (progress - 0.5) / 0.5);
        }
    }

    // Calculate weight for dyed blocks
    // Weight remains constant at 10 regardless of progress
    public static int calculateDyedBlocksWeight(float progress) {
        return 10;
    }

    // Calculate weight for end blocks
    // Weight is 0 for progress from 0 to 0.8 (Unless only the end block is left),
    // then increases to 60 as progress goes from 0.8 to 1
    public static int calculateEndBlocksWeight(float progress) {
        float prop = (float) endBlocks.size() / blocks.size();
        if (progress <= 1 - prop) {
            if (progress <= 0.7) return 0;
            if (progress > 0.7) return (int) (70 * (progress - 0.7) / 0.2);
        } else {
            return 60;
        }
        return 0;
    }

    // Calculate weight for addon blocks
    // Increases linearly up to progress 0.75, then stays at 10
    public static int calculateAddonBlocksWeight(float progress) {
        if (progress >= 0.75f) {
            return 10;
        }
        return Math.max(1, (int) Math.ceil(10 * (progress / 0.75f)));
    }

    // Check if there are any problems with the blocks imported from the file
    public static boolean checkBlock() {
        boolean flag = true;
        for (String str : blocks) {
            try {
                ItemStack item = ItemCreator.fromMaterial(CompMaterial.fromMaterial(Material.valueOf(str))).amount(64).make();
                checkBlockInventory.setItem(0, item);
            } catch (Exception e) {
                Bukkit.getLogger().severe(String.format("[BlockRacing] " + Message.NOTICE_ERROR_BLOCK.getString(), str));
                sendAll(String.format(Message.NOTICE_ERROR_BLOCK.getString(), str));
                flag = false;
            }
        }
        return flag;
    }

    public static void reloadBlock() {
        easyBlocks = List.of(readFile("EasyBlocks.txt"));
        mediumBlocks = List.of(readFile("MediumBlocks.txt"));
        hardBlocks = List.of(readFile("HardBlocks.txt"));
        dyedBlocks = List.of(readFile("DyedBlocks.txt"));
        endBlocks = List.of(readFile("EndBlocks.txt"));
        addonBlocks = Setting.isAddonAvailable() ? List.of(readFile("AddonBlocks.txt")) : List.of();
        addUpBlocks();
    }

    public static String[] readFile(String fileName) {
        try {
            File file = new File(Main.getInstance().getDataFolder(), fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
            reader.close();
            return lines.toArray(new String[0]);
        } catch (IOException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "[BlockRacing] Error reading blocks file!", e);
        }
        return new String[0];
    }
}
