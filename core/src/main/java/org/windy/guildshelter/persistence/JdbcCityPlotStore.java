package org.windy.guildshelter.persistence;

import org.windy.guildshelter.domain.model.CityPlot;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.port.CityPlotStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JDBC 实现的主城子地块（{@code city_plot} 表，SQLite/MySQL 共用，按 (guild,name) upsert）。 */
public final class JdbcCityPlotStore implements CityPlotStore {

    private final JdbcDatabase db;
    private final boolean sqlite;

    public JdbcCityPlotStore(JdbcDatabase db, boolean sqlite) {
        this.db = db;
        this.sqlite = sqlite;
    }

    @Override
    public List<CityPlot> list(GuildId guild) {
        List<CityPlot> out = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT name, min_cx, min_cz, max_cx, max_cz, assignee_uuid FROM city_plot WHERE guild_id=?")) {
            ps.setString(1, guild.value());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String a = rs.getString("assignee_uuid");
                    UUID assignee = null;
                    if (a != null && !a.isEmpty()) {
                        try { assignee = UUID.fromString(a); } catch (IllegalArgumentException ignored) {}
                    }
                    out.add(new CityPlot(rs.getString("name"), rs.getInt("min_cx"), rs.getInt("min_cz"),
                            rs.getInt("max_cx"), rs.getInt("max_cz"), assignee));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("查询主城子地块失败: " + guild.value(), e);
        }
        return out;
    }

    @Override
    public void save(GuildId guild, CityPlot plot) {
        String sql = sqlite
                ? "INSERT INTO city_plot(guild_id,name,min_cx,min_cz,max_cx,max_cz,assignee_uuid) VALUES(?,?,?,?,?,?,?) "
                + "ON CONFLICT(guild_id,name) DO UPDATE SET min_cx=excluded.min_cx,min_cz=excluded.min_cz,"
                + "max_cx=excluded.max_cx,max_cz=excluded.max_cz,assignee_uuid=excluded.assignee_uuid"
                : "INSERT INTO city_plot(guild_id,name,min_cx,min_cz,max_cx,max_cz,assignee_uuid) VALUES(?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE min_cx=VALUES(min_cx),min_cz=VALUES(min_cz),"
                + "max_cx=VALUES(max_cx),max_cz=VALUES(max_cz),assignee_uuid=VALUES(assignee_uuid)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, guild.value());
            ps.setString(2, plot.name());
            ps.setInt(3, plot.minCx());
            ps.setInt(4, plot.minCz());
            ps.setInt(5, plot.maxCx());
            ps.setInt(6, plot.maxCz());
            ps.setString(7, plot.assignee() == null ? null : plot.assignee().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("保存主城子地块失败: " + guild.value(), e);
        }
    }

    @Override
    public void remove(GuildId guild, String name) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM city_plot WHERE guild_id=? AND name=?")) {
            ps.setString(1, guild.value());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("删除主城子地块失败: " + guild.value(), e);
        }
    }

    @Override
    public void unassignAllOf(GuildId guild, UUID player) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE city_plot SET assignee_uuid=NULL WHERE guild_id=? AND assignee_uuid=?")) {
            ps.setString(1, guild.value());
            ps.setString(2, player.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("取消主城子地块指派失败: " + guild.value(), e);
        }
    }

    @Override
    public void clear(GuildId guild) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM city_plot WHERE guild_id=?")) {
            ps.setString(1, guild.value());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("清除主城子地块失败: " + guild.value(), e);
        }
    }
}
