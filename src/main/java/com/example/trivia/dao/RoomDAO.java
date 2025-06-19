package com.example.trivia.dao;

import com.example.trivia.model.Room;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RoomDAO {
    private final DataSource ds;

    public RoomDAO(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Crea una sala, genera internamente un slug de 8 caracteres
     * y devuelve su room_id.
     */
    public UUID insert(Timestamp createdAt, UUID settingsId) throws SQLException {
        String slug = generateSlug();
        String sql = """
            INSERT INTO rooms (slug, created_at, settings_id)
            VALUES (?, ?, ?)
            RETURNING room_id
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, slug);
            ps.setTimestamp(2, createdAt);
            ps.setObject(3, settingsId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("room_id", UUID.class);
                }
                throw new SQLException("Room.insert no devolvió ID");
            }
        }
    }

    /**
     * Devuelve la sala completa, incluyendo el slug, para que tu controller
     * pueda devolvérselo al cliente.
     */
    public Room getById(UUID roomId) throws SQLException {
        String sql = """
            SELECT room_id,
                   slug,
                   created_at,
                   settings_id
              FROM rooms
             WHERE room_id = ?
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Room(
                            rs.getObject("room_id",   UUID.class),
                            rs.getString("slug"),
                            rs.getTimestamp("created_at"),
                            rs.getObject("settings_id", UUID.class)
                    );
                }
            }
        }
        return null;
    }

    /** (Opcional) Listar todas las salas */
    public List<Room> findAll() throws SQLException {
        String sql = """
            SELECT room_id,
                   slug,
                   created_at,
                   settings_id
              FROM rooms
            """;
        List<Room> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Room(
                        rs.getObject("room_id",   UUID.class),
                        rs.getString("slug"),
                        rs.getTimestamp("created_at"),
                        rs.getObject("settings_id", UUID.class)
                ));
            }
        }
        return out;
    }

    /** (Opcional) Eliminar sala por ID */
    public boolean delete(UUID roomId) throws SQLException {
        String sql = "DELETE FROM rooms WHERE room_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, roomId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Genera un slug alfanumérico de 8 caracteres */
    private String generateSlug() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }
}
