package org.windy.guildshelter.adapter.bukkit.command;

/**
 * 待确认的危险操作记录。30 秒过期。
 *
 * @param sub    原始子命令名
 * @param args   原始参数（含 args[0]）
 * @param expireAt 过期时间戳（毫秒）
 */
record PendingAction(String sub, String[] args, long expireAt) {
    boolean expired() {
        return System.currentTimeMillis() > expireAt;
    }
}
