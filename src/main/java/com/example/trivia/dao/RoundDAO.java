// src/main/java/com/example/trivia/dao/RoundDAO.java
package com.example.trivia.dao;

import com.example.trivia.model.Round;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RoundDAO {
    private final DataSource ds;

    public RoundDAO(DataSource ds) {
        this.ds = ds;
    }

    // CREATE
    public UUID insert(UUID gameId, int number, Timestamp startedAt, Timestamp endedAt) throws SQLException {
        String sql = """
            INSERT INTO rounds (game_id, number, started_at, ended_at)
            VALUES (?, ?, ?, ?)
            RETURNING round_id
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, gameId);
            st.setInt(2, number);
            st.setTimestamp(3, startedAt);
            st.setTimestamp(4, endedAt);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("round_id", UUID.class);
                }
                throw new SQLException("Round insert failed, no ID returned.");
            }
        }
    }

    // READ single
    public Round getById(UUID roundId) throws SQLException {
        String sql = "SELECT * FROM rounds WHERE round_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roundId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new Round(
                            rs.getObject("round_id", UUID.class),
                            rs.getObject("game_id", UUID.class),
                            rs.getInt("number"),
                            rs.getTimestamp("started_at"),
                            rs.getTimestamp("ended_at")
                    );
                }
                return null;
            }
        }
    }

    // READ list by game
    public List<Round> findByGameId(UUID gameId) throws SQLException {
        String sql = "SELECT * FROM rounds WHERE game_id = ? ORDER BY number";
        List<Round> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, gameId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    out.add(new Round(
                            rs.getObject("round_id", UUID.class),
                            rs.getObject("game_id", UUID.class),
                            rs.getInt("number"),
                            rs.getTimestamp("started_at"),
                            rs.getTimestamp("ended_at")
                    ));
                }
            }
        }
        return out;
    }

    // DELETE
    public boolean delete(UUID roundId) throws SQLException {
        String sql = "DELETE FROM rounds WHERE round_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roundId);
            return st.executeUpdate() > 0;
        }
    }
}
