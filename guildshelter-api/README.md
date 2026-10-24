# GuildShelter API — 附属插件开发指南（第一期：只读服务）

GuildShelter 把"花哨"玩法做成可选**附属插件**,核心只暴露 API。本模块是对外契约:
**纯接口 + 只读 DTO**,第三方 `compileOnly` 依赖、**绝不 shade**(运行期由主插件 jar 提供这些类)。

## 接入三步

1. 你的附属 `plugin.yml`:
   ```yaml
   name: MyGuildShelterAddon
   depend: [GuildShelter]      # 确保主插件先加载、API 已注册
   ```
2. 构建脚本 `compileOnly` 依赖本模块(Maven 坐标随主插件发布;本地开发可 compileOnly 主插件 jar):
   ```gradle
   compileOnly 'org.windy:guildshelter-api:<version>'
   ```
3. 运行期从 Bukkit `ServicesManager` 取 API:
   ```java
   var rsp = Bukkit.getServicesManager().getRegistration(GuildShelterAPI.class);
   if (rsp == null) { getLogger().warning("GuildShelter 未就绪"); return; }
   GuildShelterAPI api = rsp.getProvider();
   ```

## 能查什么(`GuildShelterAPI`)
- `guildAt(loc)` / `kindAt(loc)`(主城/庄园/路/营地外) / `manorAt(loc)`
- `isFarmZone(loc)` —— 该处是否共享农场(挂 members-farm 的庄园/主城),**位置级、不含成员身份**
- `booleanFlag(loc, flagId)` —— 解析该处布尔 flag(庄园/主城,未设取默认)
- `guildLevel(guildRef)` / `manorLevel(manorRef)`
- `isMember(guild, uuid)` / `isGuildAdmin(guild, uuid)`

## 范例:官方农场附属(GuildShelterFarm,模块 `addon-farm`)
完整 dogfooding 范例,一个附属用全套 API 实现三块玩法:
- **共享农场(A1)**:`FarmCheckProvider`(BuildCheckProvider)对会内成员在农场区种/收/开容器返回 `ALLOW`。
- **生长加速(A2)**:`FarmGrowthListener` 监听 `BlockGrowEvent`/`EntityBreedEvent`,`isFarmZone` 命中则按 `guildLevel` 概率加速。
- **偷菜**:`FarmCheckProvider` 对非成员 BREAK 成熟作物返回 `ALLOW`(每日额度),`FarmStealListener` 在真偷成时计数+通知。

```java
// 加速核心(只读 API + 原版事件)
@EventHandler
public void onGrow(BlockGrowEvent e) {
    Location loc = e.getBlock().getLocation();
    if (!api.isFarmZone(loc)) return;                          // 只在共享农场区加速
    int level = api.guildAt(loc).map(api::guildLevel).orElse(0);
    if (ThreadLocalRandom.current().nextDouble() < level * 0.08) {
        // 额外推进一阶生长(对 Ageable 方块 +1 age)
    }
}
```
要点:农场这类玩法**完全活在附属里**,主插件只回答"这是不是农场/公会几级/是不是成员",并提供 `members-farm` 标记位与
`BuildCheckProvider` 决策口。你照这个模式即可写自己的玩法(排行榜、领地经济、活动放行……)。

## 领域事件(已可用,`org.windy.guildshelter.api.event`)
标准 Bukkit `@EventHandler` 监听即可：
- `GuildCampCreatedEvent`(公会营地建成)
- `ManorAssignedEvent` / `ManorReleasedEvent`(成员入会获庄园 / 退会释放)
- `ManorUpgradedEvent`(slot/旧级/新级/庄主) / `GuildUpgradedEvent`(新等级)
- `ChunkUnlockedEvent`(slot/chunk 坐标/玩家)
- `GuildDissolvedEvent`(公会解散)
- `PlayerEnterTerritoryEvent` / `PlayerLeaveTerritoryEvent`(进/出公会领地,世界级)
```java
@EventHandler
public void onJoin(ManorAssignedEvent e) {
    getLogger().info(e.player() + " 加入了公会 " + e.guild().id());
}
```

## 保护决策参与(已可用):`BuildCheckProvider`
附属可在破坏/放置判定里**额外放行或额外拒绝**(共享农场/子地块这类"特定条件放行建造"就靠它):
```java
api.registerBuildCheck(this, (player, loc, action, blockId) -> {
    // 例:活动期间放行某区域的建造
    if (eventActive && inEventZone(loc)) return BuildDecision.ALLOW;
    return BuildDecision.PASS;   // 不干预交回核心
}, 100);
// 聚合:任一 provider DENY 即拒;默认拒时任一 ALLOW 即放行。禁用时自动注销(也可手动 unregisterBuildChecks)。
```

## 后续期(规划中,见 PLAN_API.md)
- 受控写(set flag / 子地块)
- 命令注册(挂 `/gs <子命令>`)
