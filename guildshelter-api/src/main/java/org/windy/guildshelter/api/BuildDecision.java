package org.windy.guildshelter.api;

/**
 * {@link BuildCheckProvider} 的表态。聚合规则：任一 provider {@link #DENY} 即拒绝（覆盖默认允许）；
 * 默认拒绝时，任一 provider {@link #ALLOW} 即放行；{@link #PASS} = 不表态。
 */
public enum BuildDecision {
    /** 额外放行（覆盖核心的"默认拒绝"——如共享农场/子地块委托）。 */
    ALLOW,
    /** 额外拒绝（覆盖核心的"默认允许"——加额外限制）。 */
    DENY,
    /** 不表态，交给其它 provider / 核心默认。 */
    PASS
}
