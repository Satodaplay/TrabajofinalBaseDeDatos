package com.example.trivia.model;

import java.util.UUID;

public record TeamScore(
    UUID scoreId,
    UUID roundId,
    UUID teamId,
    int points
) {}