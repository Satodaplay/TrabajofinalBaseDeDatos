// src/main/java/com/example/trivia/dao/PlayerDAO.java
package com.example.trivia.dao;

import com.example.trivia.model.Player;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class PlayerDAO {
    private final DataSource ds;

    public PlayerDAO(DataSource ds) {
        this.ds = ds;
    }

    // CREATE
    public UUID insert(UUID roomId, String username, boolean isHost, UUID teamId) throws SQLException {
        String sql = """
            INSERT INTO players (room_id, username, is_host, team_id)
            VALUES (?, ?, ?, ?)
            RETURNING player_id
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roomId);
            st.setString(2, username);
            st.setBoolean(3, isHost);
            if (teamId != null) st.setObject(4, teamId);
            else st.setNull(4, Types.OTHER);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("player_id", UUID.class);
                }
                throw new SQLException("Player insert failed, no ID returned.");
            }
        }
    }

    // READ single
    public Player getById(UUID playerId) throws SQLException {
        String sql = "SELECT * FROM players WHERE player_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, playerId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new Player(
                            rs.getObject("player_id", UUID.class),
                            rs.getObject("room_id", UUID.class),
                            rs.getString("username"),
                            rs.getBoolean("is_host"),
                            rs.getObject("team_id", UUID.class)
                    );
                }
                return null;
            }
        }
    }

    // LIST by room
    public List<Player> findByRoomId(UUID roomId) throws SQLException {
        String sql = "SELECT * FROM players WHERE room_id = ?";
        List<Player> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roomId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    out.add(new Player(
                            rs.getObject("player_id", UUID.class),
                            rs.getObject("room_id", UUID.class),
                            rs.getString("username"),
                            rs.getBoolean("is_host"),
                            rs.getObject("team_id", UUID.class)
                    ));
                }
            }
        }
        return out;
    }

    // COUNT by room (para determinar host)
    public long countByRoomId(UUID roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM players WHERE room_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roomId);
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // DELETE
    public boolean delete(UUID playerId) throws SQLException {
        String sql = "DELETE FROM players WHERE player_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, playerId);
            return st.executeUpdate() > 0;
        }
    }
}
