package com.example.trivia.dao;

import com.example.trivia.model.QuestionOption;
import java.sql.*;
import java.util.UUID;

public class QuestionOptionDAO {
    private final Connection connection;

    public QuestionOptionDAO(Connection connection) {
        this.connection = connection;
    }

    // CREATE
    public UUID insert(UUID questionId, String text, boolean isCorrect) throws SQLException {
        String sql = """
            INSERT INTO question_options (question_id, text, is_correct)
            VALUES (?, ?, ?)
            RETURNING option_id;
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, questionId);
            stmt.setString(2, text);
            stmt.setBoolean(3, isCorrect);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("option_id");
            } else {
                throw new SQLException("Insert failed, no ID returned.");
            }
        }
    }

    // READ
    public QuestionOption getById(UUID optionId) throws SQLException {
        String sql = "SELECT * FROM question_options WHERE option_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, optionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new QuestionOption(
                    (UUID) rs.getObject("option_id"),
                    (UUID) rs.getObject("question_id"),
                    rs.getString("text"),
                    rs.getBoolean("is_correct")
                );
            } else {
                return null;
            }
        }
    }

    // UPDATE
    public boolean update(QuestionOption option) throws SQLException {
        String sql = """
            UPDATE question_options
            SET question_id = ?, text = ?, is_correct = ?
            WHERE option_id = ?;
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, option.questionId());
            stmt.setString(2, option.text());
            stmt.setBoolean(3, option.isCorrect());
            stmt.setObject(4, option.optionId());
            int affected = stmt.executeUpdate();
            return affected > 0;
        }
    }

    // DELETE
    public boolean delete(UUID optionId) throws SQLException {
        String sql = "DELETE FROM question_options WHERE option_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, optionId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        }
    }
}