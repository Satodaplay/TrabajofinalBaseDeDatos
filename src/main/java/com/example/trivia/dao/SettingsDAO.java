// src/main/java/com/example/trivia/dao/SettingsDAO.java
package com.example.trivia.dao;

import com.example.trivia.model.Settings;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

@Repository
public class SettingsDAO {
    private final DataSource ds;

    public SettingsDAO(DataSource ds) {
        this.ds = ds;
    }

    // CREATE
    public UUID insert(int rounds, int timePerRound, int questionsPerRound,
                       String difficulty, int maxPlayersPerTeam) throws SQLException {
        String sql = """
            INSERT INTO settings
             (rounds, time_per_round, questions_per_round, difficulty, max_players_per_team)
            VALUES (?,?,?,?,?)
            RETURNING settings_id
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, rounds);
            st.setInt(2, timePerRound);
            st.setInt(3, questionsPerRound);
            st.setString(4, difficulty);
            st.setInt(5, maxPlayersPerTeam);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("settings_id", UUID.class);
                }
                throw new SQLException("Settings insert failed, no ID returned.");
            }
        }
    }

    // READ
    public Settings getById(UUID settingsId) throws SQLException {
        String sql = "SELECT * FROM settings WHERE settings_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, settingsId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new Settings(
                            rs.getObject("settings_id", UUID.class),
                            rs.getInt("rounds"),
                            rs.getInt("time_per_round"),
                            rs.getInt("questions_per_round"),
                            rs.getString("difficulty"),
                            rs.getInt("max_players_per_team")
                    );
                }
                return null;
            }
        }
    }

    // UPDATE
    public boolean update(Settings s) throws SQLException {
        String sql = """
            UPDATE settings
            SET rounds=?, time_per_round=?, questions_per_round=?, difficulty=?, max_players_per_team=?
            WHERE settings_id=?
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, s.rounds());
            st.setInt(2, s.timePerRound());
            st.setInt(3, s.questionsPerRound());
            st.setString(4, s.difficulty());
            st.setInt(5, s.maxPlayersPerTeam());
            st.setObject(6, s.settingsId());
            return st.executeUpdate() > 0;
        }
    }

    // DELETE (opcional)
    public boolean delete(UUID settingsId) throws SQLException {
        String sql = "DELETE FROM settings WHERE settings_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, settingsId);
            return st.executeUpdate() > 0;
        }
    }
}
