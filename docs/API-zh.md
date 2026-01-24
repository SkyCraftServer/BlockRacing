# BlockRacing API 使用文档

## 简介

BlockRacing 插件提供了一个公共 API，允许其他插件获取游戏状态信息。

## 如何使用

### 1. 在你的插件中添加 BlockRacing 作为依赖

在你的 `plugin.yml` 中添加：

```yaml
depend: [BlockRacing]
```

或者如果是可选依赖：

```yaml
softdepend: [BlockRacing]
```

### 2. 在 Maven 项目中添加依赖

如果你需要在编译时访问 API，需要将 BlockRacing 的 JAR 文件添加到你的项目依赖中。

在 `pom.xml` 中添加：

```xml
<dependencies>
    <dependency>
        <groupId>top.lqsnow</groupId>
        <artifactId>BlockRacing</artifactId>
        <version>3.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 3. 调用 API

在你的插件代码中，可以直接调用 `BlockRacingAPI` 类的静态方法：

```java
import top.lqsnow.blockracing.api.BlockRacingAPI;

public class YourPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // 检查 BlockRacing 插件是否已加载
        if (getServer().getPluginManager().getPlugin("BlockRacing") == null) {
            getLogger().warning("BlockRacing 插件未找到！");
            return;
        }
        
        // 使用 API 获取游戏信息
        boolean gameStarted = BlockRacingAPI.isGameStarted();
        getLogger().info("游戏是否开始: " + gameStarted);
        
        if (gameStarted) {
            int redScore = BlockRacingAPI.getRedTeamCompletedBlocks();
            int blueScore = BlockRacingAPI.getBlueTeamCompletedBlocks();
            getLogger().info("红队得分: " + redScore + ", 蓝队得分: " + blueScore);
        }
    }
}
```

## API 方法列表

### 游戏状态相关

#### `isGameStarted()`
检查游戏是否已开始。
- **返回值**: `boolean` - 游戏是否在进行中
- **示例**:
  ```java
  if (BlockRacingAPI.isGameStarted()) {
      // 游戏正在进行
  }
  ```

#### `getGameState()`
获取当前游戏状态。
- **返回值**: `Game.GameState` - 枚举值
  - `PREGAME` - 游戏前
  - `INGAME` - 游戏中
  - `END` - 游戏结束
- **示例**:
  ```java
  Game.GameState state = BlockRacingAPI.getGameState();
  switch (state) {
      case PREGAME:
          // 游戏准备阶段
          break;
      case INGAME:
          // 游戏进行中
          break;
      case END:
          // 游戏已结束
          break;
  }
  ```

### 游戏模式相关

#### `getGameMode()`
获取当前游戏模式。
- **返回值**: `Setting.GameMode` - 枚举值
  - `NORMAL` - 普通模式
  - `RACING` - 竞速模式
  - `CONTEST` - 竞赛模式
  - `TIME` - 限时模式
- **示例**:
  ```java
  Setting.GameMode mode = BlockRacingAPI.getGameMode();
  if (mode == Setting.GameMode.TIME) {
      int remaining = BlockRacingAPI.getTimeModeRemainingSeconds();
      // 处理限时模式逻辑
  }
  ```

#### `isSpeedModeEnabled()`
检查是否开启极速模式。
- **返回值**: `boolean` - 极速模式是否开启
- **示例**:
  ```java
  boolean speedMode = BlockRacingAPI.isSpeedModeEnabled();
  ```

#### `isNetherModeEnabled()`
检查是否开启下界模式。
- **返回值**: `boolean` - 下界模式是否开启
- **示例**:
  ```java
  boolean netherMode = BlockRacingAPI.isNetherModeEnabled();
  ```

### 队伍得分相关

#### `getRedTeamCompletedBlocks()`
获取红队当前完成的方块数。
- **返回值**: `int` - 红队已完成的方块数量
- **示例**:
  ```java
  int redScore = BlockRacingAPI.getRedTeamCompletedBlocks();
  ```

#### `getBlueTeamCompletedBlocks()`
获取蓝队当前完成的方块数。
- **返回值**: `int` - 蓝队已完成的方块数量
- **示例**:
  ```java
  int blueScore = BlockRacingAPI.getBlueTeamCompletedBlocks();
  ```

#### `getRedTeamRemainingBlocks()`
获取红队当前方块任务的剩余数量。
- **返回值**: `int` - 红队剩余需要完成的方块数量
- **示例**:
  ```java
  int redRemaining = BlockRacingAPI.getRedTeamRemainingBlocks();
  ```

#### `getBlueTeamRemainingBlocks()`
获取蓝队当前方块任务的剩余数量。
- **返回值**: `int` - 蓝队剩余需要完成的方块数量
- **示例**:
  ```java
  int blueRemaining = BlockRacingAPI.getBlueTeamRemainingBlocks();
  ```

#### `getRedTeamTotalBlocks()`
获取红队方块总数。
- **返回值**: `int` - 红队需要完成的方块总数
- **示例**:
  ```java
  int redTotal = BlockRacingAPI.getRedTeamTotalBlocks();
  ```

#### `getBlueTeamTotalBlocks()`
获取蓝队方块总数。
- **返回值**: `int` - 蓝队需要完成的方块总数
- **示例**:
  ```java
  int blueTotal = BlockRacingAPI.getBlueTeamTotalBlocks();
  ```

### 其他游戏设置

#### `isComebackBuffEnabled()`
检查是否开启逆风增益。
- **返回值**: `boolean` - 逆风增益是否开启
- **示例**:
  ```java
  boolean comebackBuff = BlockRacingAPI.isComebackBuffEnabled();
  ```

#### `isTimeModeActive()`
检查是否为限时模式。
- **返回值**: `boolean` - 是否为限时模式
- **示例**:
  ```java
  if (BlockRacingAPI.isTimeModeActive()) {
      // 限时模式特殊处理
  }
  ```

#### `isContestModeActive()`
检查是否为竞赛模式。
- **返回值**: `boolean` - 是否为竞赛模式
- **示例**:
  ```java
  if (BlockRacingAPI.isContestModeActive()) {
      // 竞赛模式特殊处理
  }
  ```

#### `getTimeModeRemainingSeconds()`
获取限时模式剩余时间（秒）。
- **返回值**: `int` - 剩余时间（秒），如果不是限时模式则返回 0
- **示例**:
  ```java
  if (BlockRacingAPI.isTimeModeActive()) {
      int seconds = BlockRacingAPI.getTimeModeRemainingSeconds();
      int minutes = seconds / 60;
      int secs = seconds % 60;
      // 显示剩余时间
  }
  ```

#### `isMediumBlockEnabled()`
检查是否开启中等难度方块。
- **返回值**: `boolean`

#### `isHardBlockEnabled()`
检查是否开启困难方块。
- **返回值**: `boolean`

#### `isDyedBlockEnabled()`
检查是否开启染色方块。
- **返回值**: `boolean`

#### `isEndBlockEnabled()`
检查是否开启末地方块。
- **返回值**: `boolean`

#### `getBlockAmount()`
获取每队需要完成的方块数量设置。
- **返回值**: `int` - 方块数量

## 完整示例

以下是一个完整的示例插件，展示如何使用 BlockRacing API：

```java
package com.example.blockracingaddon;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import top.lqsnow.blockracing.api.BlockRacingAPI;
import top.lqsnow.blockracing.managers.Game;
import top.lqsnow.blockracing.managers.Setting;

public class BlockRacingAddon extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // 检查 BlockRacing 插件
        if (getServer().getPluginManager().getPlugin("BlockRacing") == null) {
            getLogger().severe("BlockRacing 插件未找到！插件将被禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getLogger().info("BlockRacing API 集成成功！");
        
        // 启动一个定时任务，每秒检查游戏状态
        new BukkitRunnable() {
            @Override
            public void run() {
                checkGameStatus();
            }
        }.runTaskTimer(this, 0L, 20L); // 每秒执行一次
    }
    
    private void checkGameStatus() {
        // 获取游戏状态
        if (!BlockRacingAPI.isGameStarted()) {
            return; // 游戏未开始，不做处理
        }
        
        // 获取游戏模式
        Setting.GameMode mode = BlockRacingAPI.getGameMode();
        
        // 获取队伍得分
        int redScore = BlockRacingAPI.getRedTeamCompletedBlocks();
        int blueScore = BlockRacingAPI.getBlueTeamCompletedBlocks();
        
        // 检查特殊模式
        boolean speedMode = BlockRacingAPI.isSpeedModeEnabled();
        boolean netherMode = BlockRacingAPI.isNetherModeEnabled();
        
        // 根据游戏状态做一些处理
        if (mode == Setting.GameMode.TIME) {
            int remaining = BlockRacingAPI.getTimeModeRemainingSeconds();
            if (remaining <= 60 && remaining > 0) {
                // 最后一分钟提醒
                Bukkit.broadcastMessage("§c剩余时间不足1分钟！");
            }
        }
        
        // 检查比分差距
        int scoreDiff = Math.abs(redScore - blueScore);
        if (scoreDiff >= 10) {
            String leadingTeam = redScore > blueScore ? "§c红队" : "§9蓝队";
            getLogger().info(leadingTeam + " 领先 " + scoreDiff + " 分！");
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("BlockRacing Addon 已禁用。");
    }
}
```

## 注意事项

1. **插件加载顺序**: 确保在 `plugin.yml` 中正确设置了依赖关系，这样 BlockRacing 会在你的插件之前加载。

2. **线程安全**: 所有 API 方法都应该在主线程中调用。如果需要在异步任务中使用，请使用 `Bukkit.getScheduler().runTask()` 回到主线程。

3. **游戏状态变化**: 游戏状态可能随时改变，建议定期检查或监听相关事件（如果需要事件监听功能，可以考虑为 BlockRacing 添加自定义事件）。

4. **空值处理**: 虽然当前 API 方法不会返回 null，但在使用枚举类型时请注意处理。

## 版本兼容性

- **当前 API 版本**: 3.0
- **最低 BlockRacing 版本**: 3.0
- **Bukkit API**: 1.20+

## 反馈与支持

如果你在使用 API 时遇到问题，或者需要额外的 API 功能，请联系 BlockRacing 插件作者。
