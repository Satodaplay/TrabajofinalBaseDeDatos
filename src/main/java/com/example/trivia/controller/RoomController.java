package com.example.trivia.controller;

import com.example.trivia.dao.*;
import com.example.trivia.model.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomDAO roomDAO;
    private final SettingsDAO settingsDAO;
    private final PlayerDAO playerDAO;
    private final TeamDAO teamDAO;

    public RoomController(
            RoomDAO roomDAO,
            SettingsDAO settingsDAO,
            PlayerDAO playerDAO,
            TeamDAO teamDAO
    ) {
        this.roomDAO      = roomDAO;
        this.settingsDAO  = settingsDAO;
        this.playerDAO    = playerDAO;
        this.teamDAO      = teamDAO;
    }

    /** R01: Crear sala + settings por defecto */
    @PostMapping
    public ResponseEntity<Room> createRoom() {
        try {
            // 1) Insertar settings por defecto
            Settings defaults = new Settings(
                    null,
                    10,
                    60,
                    5,
                    "easy",
                    5
            );
            UUID settingsId = settingsDAO.insert(
                    defaults.rounds(),
                    defaults.timePerRound(),
                    defaults.questionsPerRound(),
                    defaults.difficulty(),
                    defaults.maxPlayersPerTeam()
            );
            Settings savedSettings = settingsDAO.getById(settingsId);

            // 2) Insertar room
            Timestamp now = Timestamp.from(Instant.now());
            UUID roomId = roomDAO.insert(now, settingsId);
            Room room = roomDAO.getById(roomId);

            URI location = URI.create("/rooms/" + roomId);
            return ResponseEntity.created(location).body(room);

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R02–R03: Unirse a sala */
    @PostMapping("/{roomId}/players")
    public ResponseEntity<Player> joinRoom(
            @PathVariable UUID roomId,
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        try {
            Room room = roomDAO.getById(roomId);
            if (room == null) {
                return ResponseEntity.notFound().build();
            }
            String username = (String) body.get("username");
            // Determinar host si es el primer jugador
            boolean isHost = playerDAO.countByRoomId(roomId) == 0;

            UUID playerId = playerDAO.insert(
                    roomId,
                    username,
                    isHost,
                    null
            );
            Player player = playerDAO.getById(playerId);
            session.setAttribute(roomId.toString(), player);

            URI location = URI.create("/rooms/" + roomId + "/players/" + playerId);
            return ResponseEntity.created(location).body(player);

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R02: Listar jugadores de la sala */
    @GetMapping("/{roomId}/players")
    public ResponseEntity<List<Player>> getRoomPlayers(@PathVariable UUID roomId) {
        try {
            if (roomDAO.getById(roomId) == null) {
                return ResponseEntity.notFound().build();
            }
            List<Player> players = playerDAO.findByRoomId(roomId);
            return ResponseEntity.ok(players);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R04: Obtener settings de la sala */
    @GetMapping("/{roomId}/settings")
    public ResponseEntity<Settings> getSettings(@PathVariable UUID roomId) {
        try {
            Room room = roomDAO.getById(roomId);
            if (room == null) {
                return ResponseEntity.notFound().build();
            }
            Settings s = settingsDAO.getById(room.settingsId());
            return ResponseEntity.ok(s);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** R04: Actualizar settings (solo host) */
    @PutMapping("/{roomId}/settings")
    public ResponseEntity<Settings> updateSettings(
            @PathVariable UUID roomId,
            @RequestBody Settings newSettings,
            HttpSession session
    ) {
        try {
            Room room = roomDAO.getById(roomId);
            if (room == null) {
                return ResponseEntity.notFound().build();
            }
            Player current = (Player) session.getAttribute(roomId.toString());
            if (current == null || !current.isHost()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Reconstruimos el record Settings con el mismo settingsId
            Settings toUpdate = new Settings(
                    room.settingsId(),
                    newSettings.rounds(),
                    newSettings.timePerRound(),
                    newSettings.questionsPerRound(),
                    newSettings.difficulty(),
                    newSettings.maxPlayersPerTeam()
            );
            boolean ok = settingsDAO.update(toUpdate);
            if (!ok) {
                return ResponseEntity.notFound().build();
            }
            Settings updated = settingsDAO.getById(room.settingsId());
            return ResponseEntity.ok(updated);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Si tienes más endpoints (teams, asignaciones, etc.), los adaptamos igual:
    // reemplaza repo → DAO, captura SQLException, devuelve 500 en caso de error.
}
