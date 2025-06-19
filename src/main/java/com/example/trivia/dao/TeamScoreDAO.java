package com.example.trivia.dao;

import com.example.trivia.model.TeamScore;
import java.sql.*;
import java.util.UUID;

public class TeamScoreDAO {
    private final Connection connection;

    public TeamScoreDAO(Connection connection) {
        this.connection = connection;
    }

    // CREATE
    public UUID insert(UUID roundId, UUID teamId, int points) throws SQLException {
        String sql = """
            INSERT INTO team_scores (round_id, team_id, points)
            VALUES (?, ?, ?)
            RETURNING score_id;
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, roundId);
            stmt.setObject(2, teamId);
            stmt.setInt(3, points);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("score_id");
            } else {
                throw new SQLException("Insert failed, no ID returned.");
            }
        }
    }

    // READ
    public TeamScore getById(UUID scoreId) throws SQLException {
        String sql = "SELECT * FROM team_scores WHERE score_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, scoreId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new TeamScore(
                    (UUID) rs.getObject("score_id"),
                    (UUID) rs.getObject("round_id"),
                    (UUID) rs.getObject("team_id"),
                    rs.getInt("points")
                );
            } else {
                return null;
            }
        }
    }

    // UPDATE
    public boolean update(TeamScore teamScore) throws SQLException {
        String sql = """
            UPDATE team_scores
            SET round_id = ?, team_id = ?, points = ?
            WHERE score_id = ?;
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, teamScore.roundId());
            stmt.setObject(2, teamScore.teamId());
            stmt.setInt(3, teamScore.points());
            stmt.setObject(4, teamScore.scoreId());
            int affected = stmt.executeUpdate();
            return affected > 0;
        }
    }

    // DELETE
    public boolean delete(UUID scoreId) throws SQLException {
        String sql = "DELETE FROM team_scores WHERE score_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, scoreId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        }
    }
}