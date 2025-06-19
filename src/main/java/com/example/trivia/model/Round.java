package com.example.trivia.model;

import java.sql.Timestamp;
import java.util.UUID;

public record Round(
    UUID roundId,
    UUID gameId,
    int number,
    Timestamp startedAt,
    Timestamp endedAt
) {}