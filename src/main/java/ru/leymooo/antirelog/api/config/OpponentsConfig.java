package ru.leymooo.antirelog.api.config;

import lombok.Data;

@Data
public class OpponentsConfig {
    private final int maxOpponents;

    private final String oneLine;
    private final String nextLine;
    private final String empty;
}
