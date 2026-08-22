package ru.leymooo.antirelog.boards;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.leymooo.antirelog.api.models.Board;

public class BoardManager {

    private final @NonNull Map<UUID, Board> map = new ConcurrentHashMap<>();

    public void show(@NonNull Player player, @NonNull String startEnemy, final int time) {
        if (player.hasPermission("antirelog.bypass")) {
            return;
        }

        map.computeIfAbsent(player.getUniqueId(), ignored -> {
            final Board board = new Board(player);
            return board.showScoreboard(time, startEnemy) ? board : null;
        });
    }

    public @Nullable Board getFrom(@NonNull Player player) {
        return map.get(player.getUniqueId());
    }

    public void reset(@NonNull Player player) {
        final Board board = map.remove(player.getUniqueId());
        if (board == null) {
            return;
        }

        board.resetScoreboard();
    }

    public void removeAll(@NonNull String name) {
        map.values().forEach(board -> board.removeEnemy(name));
    }

    public void resetAll() {
        final List<Board> boards = List.copyOf(map.values());
        map.clear();
        boards.forEach(Board::resetScoreboard);
    }
}
