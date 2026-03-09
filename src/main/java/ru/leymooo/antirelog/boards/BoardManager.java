package ru.leymooo.antirelog.boards;

import lombok.NonNull;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.leymooo.antirelog.api.models.Board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoardManager {
    private final @NonNull Map<UUID, Board> map = new ConcurrentHashMap<>();

    public void show(final Player player, final String startEnemy, final int time) {
        if (map.containsKey(player.getUniqueId()) || player.hasPermission("antirelog.bypass")) {
            return;
        }

        final Board board = new Board(player);
        map.put(player.getUniqueId(), board);
        board.showScoreboard(time, startEnemy);
    }

    public @Nullable Board getFrom(final Player player) {
        return map.get(player.getUniqueId());
    }

    public void reset(final Player player) {
        final Board board = getFrom(player);
        if (board == null) {
            return;
        }

        board.resetScoreboard();
        map.remove(player.getUniqueId());
    }

    public void removeAll(final String name) {
        final Collection<Board> boards = new ArrayList<>(map.values());
        for (Board board : boards) {
            if (board == null) {
                continue;
            }

            board.removeEnemy(name);
            try {
                map.replace(board.getPlayer().getUniqueId(), board);
            } catch (Exception ignored) {
            }
        }
    }

    public void resetAll() {
        final Collection<Board> boards = new ArrayList<>(map.values());
        for (Board board : boards) {
            if (board == null) {
                continue;
            }

            board.resetScoreboard();
            map.remove(board.getPlayer().getUniqueId());
        }
    }
}
