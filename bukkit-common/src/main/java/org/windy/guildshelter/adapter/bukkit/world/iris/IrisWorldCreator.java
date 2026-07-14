package org.windy.guildshelter.adapter.bukkit.world.iris;

import com.volmit.iris.core.tools.IrisToolbelt;
import org.bukkit.World;

/**
 * 用 <b>Iris 自己的异步建世界管线</b>（{@link IrisToolbelt#createWorld()}）创建公会世界——与 {@code /iris create}
 * 同一条路，<b>不走 {@code Bukkit.createWorld}</b>。
 *
 * <p>为什么：{@code Bukkit.createWorld} 是主线程同步调用，会一次性同步生成出生点周围一大片区块
 * （"preparing spawn area"）；挂上 Iris 重型生成器后这步在主线程冻几秒~几十秒 → 看门狗 → 掉线/卡服。
 * {@code IrisToolbelt} 由 Iris 接管生成与出生区块处理，不阻塞主线程冻服。
 *
 * <p><b>直接引用 Iris 类，惰性隔离</b>：{@code Iris} 是 compileOnly（不打包，运行期由 Iris 插件提供）。
 * 本类仅在 Iris 在场时由 {@code WorldManager} 实例化/调用（JVM 惰性解析：Iris 不在则本类永不加载）。须在主线程调用。
 */
public final class IrisWorldCreator {

    private IrisWorldCreator() {
    }

    /** 无进度受众的便捷重载（进度只入控制台）。 */
    public static World create(String worldName, String dimension, long seed) {
        return create(worldName, dimension, seed, null);
    }

    /**
     * 经 Iris 创建（或加载已存在的）世界。
     *
     * @param worldName 世界名（公会营地名）
     * @param dimension Iris 维度包名（config {@code iris.dimension}）
     * @param seed      世界种子
     * @param progressTo 进度<b>受众</b>：传触发建会的在线玩家则其客户端显示 Iris 生成进度条
     *                   （{@code IrisCreator} 在生成出生区块时对 {@code sender.isPlayer()} 调
     *                   {@code sendProgress}，与 {@code /iris create} 同款）；传 {@code null} 则进度仅入控制台。
     * @return 创建好的 Bukkit {@link World}；失败抛 {@link RuntimeException}（调用方据此处理）。
     */
    public static World create(String worldName, String dimension, long seed,
                              org.bukkit.command.CommandSender progressTo) {
        try {
            var creator = IrisToolbelt.createWorld()
                    .name(worldName)
                    .dimension(dimension)
                    .seed(seed);
            if (progressTo != null) {
                // 未设 sender 时 IrisCreator 默认落到控制台 sender → 玩家看不到进度条。设成触发者本人即可显示。
                creator = creator.sender(new com.volmit.iris.util.plugin.VolmitSender(progressTo));
            }
            return creator.create();
        } catch (Throwable t) {
            throw new RuntimeException("Iris 建世界失败(world=" + worldName + ", dimension=" + dimension + "): " + t, t);
        }
    }
}
