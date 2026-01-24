package top.lqsnow.blockracing.api;

import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Setting;

/**
 * BlockRacing 插件的公共 API
 * 允许其他插件访问游戏状态信息
 */
public class BlockRacingAPI {

    /**
     * 检查游戏是否已开始
     * @return 如果游戏状态为 INGAME，返回 true；否则返回 false
     */
    public static boolean isGameStarted() {
        return Game.getCurrentGameState() == Game.GameState.INGAME;
    }

    /**
     * 获取当前游戏状态
     * @return GameState 枚举值：PREGAME（游戏前）、INGAME（游戏中）、END（游戏结束）
     */
    public static Game.GameState getGameState() {
        return Game.getCurrentGameState();
    }

    /**
     * 获取当前游戏模式
     * @return GameMode 枚举值：NORMAL（普通模式）、RACING（竞速模式）、CONTEST（竞赛模式）、TIME（限时模式）
     */
    public static Setting.GameMode getGameMode() {
        return Setting.getCurrentGameMode();
    }

    /**
     * 检查是否开启极速模式
     * @return 如果极速模式已开启，返回 true；否则返回 false
     */
    public static boolean isSpeedModeEnabled() {
        return Setting.isSpeedMode();
    }

    /**
     * 检查是否开启下界模式
     * @return 如果下界模式已开启，返回 true；否则返回 false
     */
    public static boolean isNetherModeEnabled() {
        return Setting.isNetherMode();
    }

    /**
     * 获取红队当前完成的方块数
     * @return 红队已完成的方块数量
     */
    public static int getRedTeamCompletedBlocks() {
        return Game.redTeamScore;
    }

    /**
     * 获取蓝队当前完成的方块数
     * @return 蓝队已完成的方块数量
     */
    public static int getBlueTeamCompletedBlocks() {
        return Game.blueTeamScore;
    }

    /**
     * 获取红队当前方块任务的剩余数量
     * @return 红队剩余需要完成的方块数量
     */
    public static int getRedTeamRemainingBlocks() {
        return Game.redTeamCurrentBlockAmount;
    }

    /**
     * 获取蓝队当前方块任务的剩余数量
     * @return 蓝队剩余需要完成的方块数量
     */
    public static int getBlueTeamRemainingBlocks() {
        return Game.blueTeamCurrentBlockAmount;
    }

    /**
     * 获取红队方块总数
     * @return 红队需要完成的方块总数
     */
    public static int getRedTeamTotalBlocks() {
        return Game.redTeamTotalBlockAmount;
    }

    /**
     * 获取蓝队方块总数
     * @return 蓝队需要完成的方块总数
     */
    public static int getBlueTeamTotalBlocks() {
        return Game.blueTeamTotalBlockAmount;
    }

    /**
     * 检查是否开启逆风增益
     * @return 如果逆风增益已开启，返回 true；否则返回 false
     */
    public static boolean isComebackBuffEnabled() {
        return Setting.isComebackBuffEnabled();
    }

    /**
     * 检查是否为限时模式
     * @return 如果当前为限时模式，返回 true；否则返回 false
     */
    public static boolean isTimeModeActive() {
        return Game.isTimeModeActive();
    }

    /**
     * 检查是否为竞赛模式
     * @return 如果当前为竞赛模式，返回 true；否则返回 false
     */
    public static boolean isContestModeActive() {
        return Game.isContestModeActive();
    }

    /**
     * 获取限时模式剩余时间（秒）
     * @return 剩余时间，如果不是限时模式则返回 0
     */
    public static int getTimeModeRemainingSeconds() {
        if (!isTimeModeActive()) {
            return 0;
        }
        return Game.getTimeModeRemainingSeconds();
    }

    /**
     * 检查是否开启中等难度方块
     * @return 如果已开启，返回 true；否则返回 false
     */
    public static boolean isMediumBlockEnabled() {
        return Setting.isEnableMediumBlock();
    }

    /**
     * 检查是否开启困难方块
     * @return 如果已开启，返回 true；否则返回 false
     */
    public static boolean isHardBlockEnabled() {
        return Setting.isEnableHardBlock();
    }

    /**
     * 检查是否开启染色方块
     * @return 如果已开启，返回 true；否则返回 false
     */
    public static boolean isDyedBlockEnabled() {
        return Setting.isEnableDyedBlock();
    }

    /**
     * 检查是否开启末地方块
     * @return 如果已开启，返回 true；否则返回 false
     */
    public static boolean isEndBlockEnabled() {
        return Setting.isEnableEndBlock();
    }

    /**
     * 获取每队需要完成的方块数量设置
     * @return 方块数量
     */
    public static int getBlockAmount() {
        return Setting.getBlockAmount();
    }
}
