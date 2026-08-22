package ru.leymooo.antirelog.api.models.cooldown;

import java.util.Locale;
import java.util.Optional;

public enum CooldownAction {
    ATTACK,
    USE,
    EAT,
    ALL;

    public static Optional<CooldownAction> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
