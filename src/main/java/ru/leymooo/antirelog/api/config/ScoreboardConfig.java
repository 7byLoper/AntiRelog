package ru.leymooo.antirelog.api.config;

import lombok.Data;

import java.util.List;

@Data
public class ScoreboardConfig {
    private final String title;
    private final List<String> lines;
    private final List<Integer> removingLinesIfNoOpponents;
}
