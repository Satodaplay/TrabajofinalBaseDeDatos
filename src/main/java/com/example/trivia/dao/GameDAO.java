// src/main/java/com/example/trivia/dao/GameDAO.java
package com.example.trivia.dao;

import com.example.trivia.model.Game;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class GameDAO {
    private final DataSource ds;

    public GameDAO(DataSource ds) {
        this.ds = ds;
    }

    // CREATE
    public UUID insert(UUID roomId, Timestamp startedAt, Timestamp endedAt) throws SQLException {
        String sql = """
            INSERT INTO games (room_id, started_at, ended_at)
            VALUES (?, ?, ?)
            RETURNING game_id
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roomId);
            st.setTimestamp(2, startedAt);
            if (endedAt != null) st.setTimestamp(3, endedAt);
            else st.setNull(3, Types.TIMESTAMP);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("game_id", UUID.class);
                }
                throw new SQLException("Game insert failed, no ID returned.");
            }
        }
    }

    // READ
    public Game getById(UUID gameId) throws SQLException {
        String sql = "SELECT * FROM games WHERE game_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, gameId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new Game(
                            rs.getObject("game_id", UUID.class),
                            rs.getObject("room_id", UUID.class),
                            rs.getTimestamp("started_at"),
                            rs.getTimestamp("ended_at")
                    );
                }
                return null;
            }
        }
    }

    // DELETE
    public boolean delete(UUID gameId) throws SQLException {
        String sql = "DELETE FROM games WHERE game_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, gameId);
            return st.executeUpdate() > 0;
        }
    }

    // LIST (opcional)
    public List<Game> findAll() throws SQLException {
        String sql = "SELECT * FROM games";
        List<Game> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                out.add(new Game(
                        rs.getObject("game_id", UUID.class),
                        rs.getObject("room_id", UUID.class),
                        rs.getTimestamp("started_at"),
                        rs.getTimestamp("ended_at")
                ));
            }
        }
        return out;
    }
}
