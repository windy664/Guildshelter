package org.windy.guildshelter.api;

/**
 * 动作类别（供 {@link BuildCheckProvider} 判定）。破坏/放置走硬保护判定；
 * 交互/容器走交互判定（如开箱/堆肥桶）——附属可对这些类别额外放行/拒绝。
 */
public enum BuildAction {
    BREAK,
    PLACE,
    /** 右键无库存方块（门/按钮/拉杆等）。 */
    INTERACT,
    /** 打开带库存方块（箱子/堆肥桶/熔炉等）。 */
    CONTAINER
}
