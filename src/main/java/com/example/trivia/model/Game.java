package com.example.trivia.model;

import java.sql.Timestamp;
import java.util.UUID;

public record Game(
    UUID gameId,
    UUID roomId,
    Timestamp startedAt,
    Timestamp endedAt
) {}