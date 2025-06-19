package com.example.trivia.model;

import java.sql.Timestamp;
import java.util.UUID;

public record Response(
    UUID responseId,
    UUID questionId,
    UUID playerId,
    Timestamp submittedAt,
    String textReply,
    UUID optionId,
    Boolean isCorrect
) {}