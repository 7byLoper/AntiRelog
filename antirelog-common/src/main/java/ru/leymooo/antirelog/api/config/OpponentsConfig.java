package ru.leymooo.antirelog.api.config;

public record OpponentsConfig(
        int maxOpponents,
        String oneLine,
        String nextLine,
        String endLine,
        String empty) {}
