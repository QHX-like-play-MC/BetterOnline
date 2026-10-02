# BetterOnline

> 为 Minecraft 1.21.11 Fabric 服务端打造的轻量级实用指令模组

**BetterOnline** 提供 **19 个实用指令** 和 **6 个自定义游戏规则**，全部逻辑运行在服务端，**兼容原版客户端**，无需玩家安装任何模组。

---

##  特性

-  **完整传送系统** — TPA 请求、返回死亡点、随机传送、召集玩家
-  **多家庭管理** — 支持中文命名，数据永久保存，钻石购买额度
-  **状态管理** — 治疗、喂食，OP 可无视游戏规则
-  **私聊消息** — 简洁的 `/msg` 私聊
-  **权限管理** — OP 授予与撤销，主机权限绝对保护
-  **6 个自定义游戏规则** — 全局控制 PVP、治疗、移动等
-  **纯服务端架构** — 客户端零负担
-  **数据持久化** — JSON 格式保存在存档目录，支持中文

---

## 📖 指令列表

<details>
<summary><b> 传送系统（8 个）</b></summary>

| 指令 | 功能 | 特殊机制 |
|:---|:---|:---|
| `/tpa <玩家>` | 请求传送到指定玩家身边 | 5 秒发送冷却 |
| `/tpahere <玩家>` | 请求指定玩家传送到你身边 | 5 秒发送冷却 |
| `/tpaaccept` | 同意最近的传送请求 | 敲钟声 + 3 秒黑暗效果 + 倒计时 Title |
| `/tparefuse` | 拒绝最近的传送请求 | 通知双方，解除请求锁定 |
| `/back` | 传送回上一次死亡的地点 | 自动记录死亡坐标与维度 |
| `/spawn` | 传送回世界主出生点 | — |
| `/rtp <x> <z> [true\|false]` | 随机传送到安全位置 | 5000 格范围；`true`=圆形（默认），`false`=方形 |
| `/tpall` | 召集所有在线玩家到你身边 | **仅限 OP（等级 ≥ 2）** |

</details>

<details>
<summary><b> 家系统（5 个）</b></summary>

> 初始限额 **5 个家**，家名需用**双引号**包裹以支持中文。

| 指令 | 功能 | 特殊机制 |
|:---|:---|:---|
| `/home list` | 列出所有家 | 点击家名可直接传送 |
| `/home add "名字"` | 设置新家 | 例：`/home add "我的家"` |
| `/home remove "名字"` | 删除家 | — |
| `/home tp "名字"` | 传送到指定的家 | 安全检测：被堵塞或岩浆中拒绝传送 |
| `/home buy <数量>` | 购买额外额度 | 每 1 个额度消耗 **5 个钻石** |

</details>

<details>
<summary><b> 状态管理（4 个）</b></summary>

| 指令 | 功能 | 权限 |
|:---|:---|:---|
| `/heal` | 恢复自身满血量 + 清除负面效果 | 所有人（受 `canheal` 限制） |
| `/heal <玩家>` | 恢复指定玩家 | **OP ≥ 2**，无视 `canheal` |
| `/feed` | 恢复自身满饱食度 | 所有人（受 `canfeed` 限制） |
| `/feed <玩家>` | 恢复指定玩家 | **OP ≥ 2**，无视 `canfeed` |

</details>

<details>
<summary><b> 聊天系统（1 个）</b></summary>

| 指令 | 功能 |
|:---|:---|
| `/msg <玩家> <消息>` | 发送私密消息（格式：`[我 -> 玩家] 消息`） |

</details>

<details>
<summary><b> 权限与管理（3 个）</b></summary>

| 指令 | 功能 | 特殊机制 |
|:---|:---|:---|
| `/op <玩家>` | 授予 OP | **仅限主机**，授予后即时刷新命令树 |
| `/deop <玩家>` | 撤销 OP | **仅限主机**，无法撤销主机自身 OP |
| `/boh [页码]` | 详细帮助菜单 | 分 2 页，指令名可点击填入聊天栏 |

</details>

---

##  自定义游戏规则

使用 `/gamerule betteronline:<规则名> <true/false>` 控制：

| 规则名 | 默认 | 描述 |
|:---|:---:|:---|
| `betteronline:pvp` | `true` | 允许或禁止玩家之间的 PVP 伤害 |
| `betteronline:canheal` | `true` | 启用或禁用 `/heal` 自身 |
| `betteronline:canfeed` | `true` | 启用或禁用 `/feed` 自身 |
| `betteronline:nocooldown` | `false` | 禁用攻击冷却（预留，暂未实现） |
| `betteronline:move` | `true` | 允许玩家移动，`false` 强制拉回 |
| `betteronline:fly` | `false` | 预留规则（暂未实现） |

> **提示**：1.21.11 起 GameRule 使用 Identifier 命名，规则名前必须加命名空间 `betteronline:`。

---

##  安装

### 服务端

1. 安装 **Fabric Loader ≥ 0.18.3**（[下载](https://fabricmc.net/use/installer/)）
2. 下载 **Fabric API 0.141.5+1.21.11**（[Modrinth](https://modrinth.com/mod/fabric-api)）
3. 将 `betteronline-x.x.x.jar` 和 Fabric API 放入 `mods/` 文件夹
4. 启动服务器

### 客户端

**无需安装**，使用原版客户端即可连接。

---

##  数据存储

家数据以 **UTF-8 编码的 JSON** 保存在世界存档根目录：

```
<world>/
├── level.dat
├── betteronline_homes.json    ← 家数据
└── ...
```

**示例结构**：

```json
{
  "玩家UUID": {
    "maxHomes": 5,
    "homes": {
      "我的家": {
        "world": "minecraft:overworld",
        "x": 100.5, "y": 64.0, "z": 200.5,
        "yaw": 0.0, "pitch": 0.0
      }
    }
  }
}
```

TPA 请求、私聊记录等临时数据**仅在内存中**，服务器重启后清空。

---

##  技术特性

| 特性 | 说明 |
|:---|:---|
| **纯服务端架构** | 所有逻辑在服务端处理，客户端零负担 |
| **并发锁与防刷** | TPA 内置 5 秒冷却，60 秒超时清理 |
| **主机权限保护** | 单人/局域网房主的 OP 不可被剥夺 |
| **智能安全传送** | 检查脚底实心、头部空气、无岩浆 |
| **权限 API 集成** | 与 LuckPerms 等权限插件兼容 |
| **数据持久化** | JSON 格式，支持中文命名 |

---

##  环境要求

| 项目 | 版本 |
|:---|:---|
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.18.3 |
| Fabric API | 0.141.5+1.21.11 或更高 |
| Java | ≥ 21 |

---

##  项目结构

```
src/main/
├── java/com/betteronline/
│   ├── BetterOnline.java              # 入口点
│   ├── command/
│   │   ├── CommandRegistry.java        # 指令注册
│   │   ├── TeleportCommands.java       # 传送系统
│   │   ├── HomeCommands.java           # 家系统
│   │   ├── StatusCommands.java         # 状态管理
│   │   ├── ChatCommands.java           # 聊天系统
│   │   └── AdminCommands.java          # 权限管理
│   ├── gamerule/
│   │   └── ModGameRules.java           # 自定义 GameRule
│   ├── data/
│   │   ├── HomeData.java               # Home 持久化
│   │   └── TpaManager.java             # TPA 请求管理
│   ├── event/
│   │   ├── PlayerEventListener.java    # 事件监听
│   │   └── ServerTickHandler.java      # Tick 调度
│   └── util/
│       ├── TeleportUtil.java           # 安全传送
│       └── SafeLocationFinder.java     # RTP 位置搜索
└── resources/
    └── fabric.mod.json
```

---

##  Roadmap

- [x] 传送系统（TPA、back、spawn、rtp、tpall）
- [x] 家系统（增删改查、购买额度）
- [x] 状态管理（heal、feed）
- [x] 聊天系统（msg）
- [x] 权限管理（op、deop、帮助菜单）
- [x] 6 个自定义游戏规则
- [ ] `nocooldown` 规则完整实现
- [ ] `fly` 规则完整实现
- [ ] 玩家间共享家（家庭组）
- [ ] 多语言支持（en_us）

---

##  贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支（`git checkout -b feature/AmazingFeature`）
3. 提交修改（`git commit -m 'Add some AmazingFeature'`）
4. 推送到分支（`git push origin feature/AmazingFeature`）
5. 开启 Pull Request

---

##  许可

本项目采用 **MIT License** 授权 —— 详见 [LICENSE](LICENSE) 文件。

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐️ Star 支持一下！**

</div>
