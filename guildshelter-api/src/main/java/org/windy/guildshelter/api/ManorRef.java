package org.windy.guildshelter.api;

import java.util.UUID;

/**
 * 成员庄园的<b>只读引用</b>（API DTO）。
 *
 * @param guild 所属公会
 * @param slot  庄园螺旋 slot 编号
 * @param owner 庄主 UUID
 */
public record ManorRef(GuildRef guild, int slot, UUID owner) {
}
