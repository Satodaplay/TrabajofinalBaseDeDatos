package com.example.trivia.model;

import java.util.UUID;

public record Settings(
    UUID settingsId,
    int rounds,
    int timePerRound,
    int questionsPerRound,
    String difficulty,
    int maxPlayersPerTeam
) {}