package com.example.trivia.model;

import java.util.UUID;

public record QuestionOption(
    UUID optionId,
    UUID questionId,
    String text,
    boolean isCorrect
) {}