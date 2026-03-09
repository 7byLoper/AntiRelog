package ru.leymooo.antirelog.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.leymooo.antirelog.manager.PvPManager;

public class AntirelogPlaceholder extends PlaceholderExpansion {
    private final PvPManager pvpManger;

    public AntirelogPlaceholder(PvPManager pvpManger) {

        this.pvpManger = pvpManger;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "antirelog";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Loper";
    }

    @Override
    public @NotNull String getVersion() {
        return "3.0.11";
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "???";
        }

        return switch (params.toLowerCase()) {
            case "is_in_pvp" -> pvpManger.isInPvP(player) ? "yes" : "no";
            case "time_left" -> {
                if (!pvpManger.isInPvP(player)) {
                    yield "0";
                }

                yield String.valueOf(pvpManger.getTimeRemainingInPvP(player));
            }
            default -> "???";
        };
    }
}
