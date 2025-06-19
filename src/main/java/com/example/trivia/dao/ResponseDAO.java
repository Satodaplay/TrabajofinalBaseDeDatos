package com.example.trivia.dao;

import com.example.trivia.model.Response;
import com.example.trivia.model.Player;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ResponseDAO {
    private final DataSource ds;

    public ResponseDAO(DataSource ds) {
        this.ds = ds;
    }

    /** Comprueba si ya existe una respuesta para ese juego/ronda/pregunta/jugador */
    public boolean exists(
            UUID gameId,
            UUID roundId,
            UUID questionId,
            UUID playerId
    ) throws SQLException {
        String sql = """
            SELECT 1
              FROM responses
             WHERE game_id    = ?
               AND round_id   = ?
               AND question_id= ?
               AND player_id  = ?
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, gameId);
            ps.setObject(2, roundId);
            ps.setObject(3, questionId);
            ps.setObject(4, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserta una nueva respuesta y devuelve su ID */
    public UUID insert(
            UUID gameId,
            UUID roundId,
            UUID questionId,
            UUID playerId,
            Timestamp submittedAt,
            String textReply,
            UUID optionId,
            Boolean isCorrect
    ) throws SQLException {
        String sql = """
            INSERT INTO responses
              (game_id, round_id, question_id, player_id, submitted_at, text_reply, option_id, is_correct)
            VALUES (?,?,?,?,?,?,?,?)
            RETURNING response_id
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, gameId);
            ps.setObject(2, roundId);
            ps.setObject(3, questionId);
            ps.setObject(4, playerId);
            ps.setTimestamp(5, submittedAt);
            ps.setString(6, textReply);
            if (optionId != null) ps.setObject(7, optionId);
            else ps.setNull(7, Types.OTHER);
            if (isCorrect != null) ps.setBoolean(8, isCorrect);
            else ps.setNull(8, Types.BOOLEAN);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("response_id", UUID.class);
                }
                throw new SQLException("Insert de Response falló, no devolvió ID");
            }
        }
    }

    /** Mapea una fila de ResultSet a tu record Response */
    private Response mapRow(ResultSet rs) throws SQLException {
        return new Response(
                rs.getObject("response_id", UUID.class),
                rs.getObject("question_id", UUID.class),
                rs.getObject("player_id", UUID.class),
                rs.getTimestamp("submitted_at"),
                rs.getString("text_reply"),
                rs.getObject("option_id", UUID.class),
                rs.getObject("is_correct", Boolean.class)
        );
    }

    /** Lee una respuesta o devuelve null si no existe */
    public Response find(
            UUID gameId,
            UUID roundId,
            UUID questionId,
            UUID playerId
    ) throws SQLException {
        String sql = """
            SELECT
              response_id,
              question_id,
              player_id,
              submitted_at,
              text_reply,
              option_id,
              is_correct
            FROM responses
           WHERE game_id    = ?
             AND round_id   = ?
             AND question_id= ?
             AND player_id  = ?
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, gameId);
            ps.setObject(2, roundId);
            ps.setObject(3, questionId);
            ps.setObject(4, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Alias con el nombre que usa tu controller.
     * Envuelve el resultado en Optional para evitar NPE.
     */
    public Optional<Response> findByGameRoundQuestionPlayer(
            UUID gameId,
            UUID roundId,
            UUID questionId,
            Player player
    ) throws SQLException {
        return Optional.ofNullable(
                find(
                        gameId,
                        roundId,
                        questionId,
                        player.playerId()
                )
        );
    }
}
