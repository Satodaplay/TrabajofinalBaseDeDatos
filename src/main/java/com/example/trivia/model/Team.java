package com.example.trivia.model;

import java.util.UUID;

public record Team(
    UUID teamId,
    UUID roomId,
    String name
) {}