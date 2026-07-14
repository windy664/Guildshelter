package org.windy.guildshelter.api;

/**
 * 公会营地的<b>只读引用</b>（API DTO）。第三方附属拿它作 key，不触碰内部可变模型。
 *
 * @param id        公会 id（宿主公会插件里的公会标识，等同内部 {@code GuildId.value()}）
 * @param worldName 该营地的世界名（registry key；混合端=维度 path {@code guild_xxx}）
 */
public record GuildRef(String id, String worldName) {
}
