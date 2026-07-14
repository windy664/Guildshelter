package org.windy.guildshelter.persistence;

import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.port.AuditStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC 实现的审计日志（{@code gs_audit} 表，SQLite/MySQL 共用，append-only）。 */
public final class JdbcAuditStore implements AuditStore {

    private final JdbcDatabase db;

    public JdbcAuditStore(JdbcDatabase db) {
        this.db = db;
    }

    @Override
    public void record(Entry e) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO gs_audit(ts, guild_id, actor_uuid, action, target, detail) VALUES(?,?,?,?,?,?)")) {
            ps.setLong(1, e.ts());
            ps.setString(2, e.guildId());
            ps.setString(3, e.actorUuid());
            ps.setString(4, e.action());
            ps.setString(5, e.target());
            ps.setString(6, e.detail());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new PersistenceException("写入审计失败: " + e.action(), ex);
        }
    }

    @Override
    public List<Entry> recent(GuildId guild, int limit, int offset) {
        List<Entry> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, ts, guild_id, actor_uuid, action, target, detail FROM gs_audit "
                     + "WHERE guild_id=? ORDER BY ts DESC, id DESC LIMIT ? OFFSET ?")) {
            ps.setString(1, guild.value());
            ps.setInt(2, Math.max(0, limit));
            ps.setInt(3, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Entry(rs.getLong("id"), rs.getLong("ts"), rs.getString("guild_id"),
                            rs.getString("actor_uuid"), rs.getString("action"),
                            rs.getString("target"), rs.getString("detail")));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("查询审计失败: " + guild.value(), e);
        }
        return out;
    }

    @Override
    public void purgeOld(long beforeMillis) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM gs_audit WHERE ts < ?")) {
            ps.setLong(1, beforeMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("清理旧审计失败", e);
        }
    }

    @Override
    public void clear(GuildId guild) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM gs_audit WHERE guild_id=?")) {
            ps.setString(1, guild.value());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("清除审计失败: " + guild.value(), e);
        }
    }
}
