package com.example.trivia.model;

import java.util.UUID;

public record Question(
    UUID questionId,
    UUID roundId,
    String type,
    String text,
    String mediaUrl
) {}