package com.example.trivia.controller;

import com.example.trivia.dao.*;
import com.example.trivia.model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameDAO      gameDAO;
    private final RoomDAO      roomDAO;
    private final RoundDAO     roundDAO;
    private final QuestionDAO  questionDAO;
    private final PlayerDAO    playerDAO;
    private final ResponseDAO  responseDAO;
    private final SettingsDAO  settingsDAO;

    public GameController(
            GameDAO gameDAO,
            RoomDAO roomDAO,
            RoundDAO roundDAO,
            QuestionDAO questionDAO,
            PlayerDAO playerDAO,
            ResponseDAO responseDAO,
            SettingsDAO settingsDAO
    ) {
        this.gameDAO      = gameDAO;
        this.roomDAO      = roomDAO;
        this.roundDAO     = roundDAO;
        this.questionDAO  = questionDAO;
        this.playerDAO    = playerDAO;
        this.responseDAO  = responseDAO;
        this.settingsDAO  = settingsDAO;
    }

    /** R05: Crear juego y sus rondas */
    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody Map<String, String> body) {
        try {
            UUID roomId = UUID.fromString(body.get("roomId"));
            Room room   = roomDAO.getById(roomId);
            if (room == null) return ResponseEntity.notFound().build();

            Timestamp now  = Timestamp.from(Instant.now());
            UUID gameId    = gameDAO.insert(roomId, now, null);

            Settings cfg = settingsDAO.getById(room.settingsId());
            for (int i = 1; i <= cfg.rounds(); i++) {
                Instant start = now.toInstant().plusSeconds((long) cfg.timePerRound() * (i - 1));
                Instant end   = start.plusSeconds(cfg.timePerRound());
                roundDAO.insert(gameId, i, Timestamp.from(start), Timestamp.from(end));
            }

            Game created = gameDAO.getById(gameId);
            return ResponseEntity
                    .created(URI.create("/games/" + gameId))
                    .body(created);

        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R06: Obtener juego por ID */
    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGame(@PathVariable UUID gameId) {
        try {
            Game game = gameDAO.getById(gameId);
            return game == null
                    ? ResponseEntity.notFound().build()
                    : ResponseEntity.ok(game);
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R07: Eliminar juego (sólo el host) */
    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(
            @PathVariable UUID gameId,
            HttpSession session
    ) {
        try {
            Game game = gameDAO.getById(gameId);
            if (game == null) return ResponseEntity.notFound().build();

            Player host = (Player) session.getAttribute(game.roomId().toString());
            if (host == null || !host.isHost()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            gameDAO.delete(gameId);
            return ResponseEntity.noContent().build();
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R08: Listar rondas de un juego */
    @GetMapping("/{gameId}/rounds")
    public ResponseEntity<List<Round>> listRounds(@PathVariable UUID gameId) {
        try {
            if (gameDAO.getById(gameId) == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(roundDAO.findByGameId(gameId));
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R09: Listar preguntas (sólo si ya arrancó la ronda) */
    @GetMapping("/{gameId}/rounds/{roundId}/questions")
    public ResponseEntity<List<Question>> listQuestions(
            @PathVariable UUID gameId,
            @PathVariable UUID roundId
    ) {
        try {
            Game  game  = gameDAO.getById(gameId);
            Round round = roundDAO.getById(roundId);
            if (game == null || round == null) {
                return ResponseEntity.notFound().build();
            }
            if (Instant.now().isBefore(round.startedAt().toInstant())) {
                return ResponseEntity.badRequest().build();
            }
            List<Question> qs = questionDAO.findByRoundId(roundId);
            return ResponseEntity.ok(qs);
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R10: Enviar respuesta (tras acabar la ronda, sin duplicados) */
    @PostMapping("/{gameId}/rounds/{roundId}/questions/{questionId}/players/{playerId}")
    public ResponseEntity<Void> submitAnswer(
            @PathVariable UUID gameId,
            @PathVariable UUID roundId,
            @PathVariable UUID questionId,
            @PathVariable UUID playerId,
            @RequestBody Map<String, String> body,
            HttpSession session
    ) {
        try {
            Game     game     = gameDAO.getById(gameId);
            Round    round    = roundDAO.getById(roundId);
            Question question = questionDAO.getById(questionId);
            Player   player   = playerDAO.getById(playerId);

            if (game == null || round == null || question == null || player == null) {
                return ResponseEntity.notFound().build();
            }
            if (Instant.now().isBefore(round.endedAt().toInstant())) {
                return ResponseEntity.badRequest().build();
            }

            Player sess = (Player) session.getAttribute(game.roomId().toString());
            if (sess == null || !sess.playerId().equals(playerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (responseDAO.exists(gameId, roundId, questionId, playerId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            String answer = body.get("answer");
            // los dos últimos params (optionId/isCorrect) déjalos null si tu lógica no los usa
            responseDAO.insert(
                    gameId, roundId, questionId, playerId,
                    Timestamp.from(Instant.now()),
                    answer,
                    null,
                    null
            );

            return ResponseEntity.ok().build();
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R11: Consultar respuesta de un jugador */
    @GetMapping("/{gameId}/rounds/{roundId}/questions/{questionId}/players/{playerId}")
    public ResponseEntity<Response> getAnswer(
            @PathVariable UUID gameId,
            @PathVariable UUID roundId,
            @PathVariable UUID questionId,
            @PathVariable UUID playerId,
            HttpSession session
    ) {
        try {
            Game  game   = gameDAO.getById(gameId);
            Round round  = roundDAO.getById(roundId);
            Player pl    = playerDAO.getById(playerId);

            if (game == null || round == null || pl == null) {
                return ResponseEntity.notFound().build();
            }
            Player sess = (Player) session.getAttribute(game.roomId().toString());
            if (sess == null || !sess.playerId().equals(playerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Optional<Response> resp = responseDAO
                    .findByGameRoundQuestionPlayer(gameId, roundId, questionId, pl);
            return resp
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
