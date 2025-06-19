// src/main/java/com/example/trivia/dao/TeamDAO.java
package com.example.trivia.dao;

import com.example.trivia.model.Team;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class TeamDAO {
    private final DataSource ds;

    public TeamDAO(DataSource ds) {
        this.ds = ds;
    }

    // CREATE
    public UUID insert(UUID roomId, String name) throws SQLException {
        String sql = """
            INSERT INTO teams (room_id, name)
            VALUES (?, ?)
            RETURNING team_id
            """;
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roomId);
            st.setString(2, name);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("team_id", UUID.class);
                }
                throw new SQLException("Team insert failed, no ID returned.");
            }
        }
    }

    // READ single
    public Team getById(UUID teamId) throws SQLException {
        String sql = "SELECT * FROM teams WHERE team_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, teamId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new Team(
                            rs.getObject("team_id", UUID.class),
                            rs.getObject("room_id", UUID.class),
                            rs.getString("name")
                    );
                }
                return null;
            }
        }
    }

    // LIST by room
    public List<Team> findByRoomId(UUID roomId) throws SQLException {
        String sql = "SELECT * FROM teams WHERE room_id = ?";
        List<Team> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, roomId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    out.add(new Team(
                            rs.getObject("team_id", UUID.class),
                            rs.getObject("room_id", UUID.class),
                            rs.getString("name")
                    ));
                }
            }
        }
        return out;
    }

    // UPDATE (opcional)
    public boolean update(Team t) throws SQLException {
        String sql = "UPDATE teams SET name = ? WHERE team_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, t.name());
            st.setObject(2, t.teamId());
            return st.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(UUID teamId) throws SQLException {
        String sql = "DELETE FROM teams WHERE team_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setObject(1, teamId);
            return st.executeUpdate() > 0;
        }
    }
}
