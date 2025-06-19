package com.example.trivia.model;

import java.sql.Timestamp;
import java.util.UUID;

public record Room(
    UUID roomId,
    String slug,
    Timestamp createdAt,
    UUID settingsId
) {}