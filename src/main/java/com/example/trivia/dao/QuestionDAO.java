package com.example.trivia.dao;

import com.example.trivia.model.Question;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class QuestionDAO {
    private final DataSource ds;

    public QuestionDAO(DataSource ds) {
        this.ds = ds;
    }

    /** READ list by round */
    public List<Question> findByRoundId(UUID roundId) throws SQLException {
        String sql = """
            SELECT question_id,
                   round_id,
                   type,
                   text,
                   media_url
              FROM questions
             WHERE round_id = ?
            """;
        List<Question> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Question(
                            rs.getObject("question_id", UUID.class),
                            rs.getObject("round_id",   UUID.class),
                            rs.getString("type"),
                            rs.getString("text"),
                            rs.getString("media_url")
                    ));
                }
            }
        }
        return out;
    }

    /** READ single */
    public Question getById(UUID questionId) throws SQLException {
        String sql = """
            SELECT question_id,
                   round_id,
                   type,
                   text,
                   media_url
              FROM questions
             WHERE question_id = ?
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Question(
                            rs.getObject("question_id", UUID.class),
                            rs.getObject("round_id",   UUID.class),
                            rs.getString("type"),
                            rs.getString("text"),
                            rs.getString("media_url")
                    );
                }
            }
        }
        return null;
    }
}
