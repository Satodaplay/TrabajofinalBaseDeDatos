package com.example.trivia.model;

import java.util.UUID;

public record Player(
    UUID playerId,
    UUID roomId,
    String username,
    boolean isHost,
    UUID teamId
) {}