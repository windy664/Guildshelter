package org.windy.guildshelter.domain.layout;

/**
 * 公会营地布局参数，全部以 <b>chunk</b> 为单位（管理员配置，世界边界等由此派生）。
 *
 * <p><b>主城和成员庄园同款算法</b>：主城<b>独占中心那一格(cell 0)</b>整块——整格 plot footprint 都是主城领地。
 * {@code mainCityInitialChunks}/{@code mainCityMaxChunks} 是主城的<b>解锁额度上限</b>边长（会长能解锁多少 chunk），
 * 随公会等级从 initial 长到 max（封顶 ≤ plotChunks，即整格容量）；超出额度的城地属主城但保持<b>未解锁</b>。
 * 成员 slot 从 cell 0 之外的螺旋格（1,2,…）开始铺；预留/边界按 max 算，升级随之外扩。
 *
 * @param plotChunks            单个庄园边长（chunk），即满级 slot 大小
 * @param roadChunks            庄园之间的间距/路宽（chunk）
 * @param mainCityInitialChunks 主城初始<b>解锁额度</b>边长（chunk，1 级公会），∈ [1, mainCityMaxChunks]
 * @param mainCityMaxChunks     主城最大<b>解锁额度</b>边长（chunk，公会满级达到），∈ [initial, plotChunks]；<b>非</b>城territory边界(整格皆主城)
 * @param plotDefaultChunks     庄园初始实占边长（chunk，1 级庄园），≤ plotChunks
 * @param plotChunksPerLevel    庄园每升一级实占边长增加多少 chunk
 * @param baseY                 地面高度（生成器用；放这里方便统一配置）
 * @param marginChunks          世界边界在已分配范围外额外留的余量（chunk）
 */
public record LayoutConfig(
        int plotChunks,
        int roadChunks,
        int mainCityInitialChunks,
        int mainCityMaxChunks,
        int plotDefaultChunks,
        int plotChunksPerLevel,
        int baseY,
        int marginChunks
) {

    public LayoutConfig {
        if (plotChunks < 1) {
            throw new IllegalArgumentException("plotChunks 必须 ≥1");
        }
        if (roadChunks < 0) {
            throw new IllegalArgumentException("roadChunks 必须 ≥0");
        }
        if (mainCityInitialChunks < 1) {
            throw new IllegalArgumentException("mainCityInitialChunks 必须 ≥1");
        }
        if (mainCityMaxChunks < mainCityInitialChunks) {
            throw new IllegalArgumentException("mainCityMaxChunks 必须 ≥ initial");
        }
        if (mainCityMaxChunks > plotChunks) {
            throw new IllegalArgumentException("mainCityMaxChunks 必须 ≤ plotChunks（主城解锁额度不能超过中心格 plotChunks² 容量）");
        }
        if (plotDefaultChunks < 1 || plotDefaultChunks > plotChunks) {
            throw new IllegalArgumentException("plotDefaultChunks 必须在 [1, plotChunks]");
        }
        if (plotChunksPerLevel < 0) {
            throw new IllegalArgumentException("plotChunksPerLevel 必须 ≥0");
        }
    }

    /** 网格节距：一个庄园 + 一条路。 */
    public int pitchChunks() {
        return plotChunks + roadChunks;
    }

    /**
     * 主城占据的中心螺旋 slot 数。主城就是<b>中心一格(cell 0)</b>，故恒为 1；成员 slot 从 cell 1 起。
     */
    public int mainCityBase() {
        return 1;
    }

    /** 庄园初始（1 级）解锁的正方形边长 = {@code plotDefaultChunks}。首次分配即解锁这块。 */
    public int initialUnlockSide() {
        return plotDefaultChunks;
    }

    /** 主城建会时初始解锁的角落正方形边长 = {@code mainCityInitialChunks}。 */
    public int initialCityUnlockSide() {
        return mainCityInitialChunks;
    }

    /**
     * 一份合理的默认配置：庄园满级 15 chunk(240×240)、路 1 chunk、
     * 主城额度边长上限 15 chunk、庄园初始解锁 6 chunk(96×96)。
     */
    public static LayoutConfig defaults() {
        return new LayoutConfig(15, 1, 6, 15, 6, 1, 64, 2);
    }
}
