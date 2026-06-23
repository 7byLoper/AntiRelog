package ru.leymooo.antirelog.api.config;

import java.util.List;

public record ScoreboardConfig(String title, List<String> lines, List<Integer> removingLinesIfNoOpponents) {
}
