package ru.leymooo.antirelog.api.models;

import lombok.Getter;
import lombok.NonNull;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.AntiRelog;
import ru.leymooo.antirelog.api.config.OpponentsConfig;
import ru.leymooo.antirelog.api.config.ScoreboardConfig;
import ru.leymooo.antirelog.manager.PvPManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Board {

    @Getter
    private final @NonNull Player player;
    private final @NonNull TabPlayer tabPlayer;
    private final @NonNull ScoreboardManager scoreboardManager;
    private final @NonNull ScoreboardConfig scoreboardConfig;
    private final @NonNull OpponentsConfig opponentsConfig;
    private final @NonNull PvPManager pvpManager;
    private final @NonNull Set<String> enemies = ConcurrentHashMap.newKeySet();

    public Board(@NonNull Player player) {
        this.scoreboardManager = Objects.requireNonNull(TabAPI.getInstance().getScoreboardManager());
        this.player = player;
        this.tabPlayer = Objects.requireNonNull(TabAPI.getInstance().getPlayer(player.getUniqueId()));

        AntiRelog antirelog = AntiRelog.getPlugin(AntiRelog.class);
        this.scoreboardConfig = antirelog.getConfigManager().getScoreboardConfig();
        this.opponentsConfig = antirelog.getConfigManager().getOpponentsConfig();
        this.pvpManager = antirelog.getPvpManager();
    }

    public void showScoreboard(final int time, @NonNull String startEnemy) {
        if (scoreboardManager.hasCustomScoreboard(tabPlayer)) {
            return;
        }

        addEnemy(startEnemy);
        final Scoreboard scoreboard = scoreboardManager.createScoreboard(
                player.getName(), scoreboardConfig.title(), buildEnemies(time)
        );
        scoreboardManager.showScoreboard(tabPlayer, scoreboard);
    }

    public void updateScoreboard(int time) {
        final Scoreboard scoreboard = scoreboardManager.createScoreboard(
                player.getName(),
                scoreboardConfig.title(),
                buildEnemies(time)
        );
        Optional.of(tabPlayer).ifPresent(tp -> {
            try {
                scoreboardManager.showScoreboard(tp, scoreboard);
            } catch (Exception ignored) {}
        });
    }

    public void resetScoreboard() {
        Bukkit.getScheduler().runTaskLater(AntiRelog.getPlugin(AntiRelog.class),
                () -> scoreboardManager.resetScoreboard(tabPlayer), 10L);
    }

    public void removeEnemy(@NonNull String name) {
        enemies.remove(name);
    }

    public void addEnemy(@NonNull String name) {
        Optional.of(name)
                .filter(n -> !n.trim().isEmpty())
                .ifPresent(enemies::add);
    }

    public @NonNull List<String> buildEnemies(final int time) {
        List<String> lines = scoreboardConfig.lines().stream()
                .map(line -> line.replace("{time}", String.valueOf(time))
                        .replace("{player}", player.getName())
                        .replace("{ping}", String.valueOf(player.getPing())))
                .collect(Collectors.toCollection(ArrayList::new));

        int enemiesIndex = lines.indexOf("{opponents}");
        if (enemiesIndex == -1) {
            return lines;
        }

        if (enemies.isEmpty()) {
            List<Integer> indexes = scoreboardConfig.removingLinesIfNoOpponents();
            if (indexes.isEmpty()) {
                lines.set(enemiesIndex, opponentsConfig.empty());
                return lines;
            }

            indexes.stream()
                    .filter(i -> i >= 0 && i < lines.size())
                    .sorted(Comparator.reverseOrder())
                    .forEach(lines::remove);
            return lines;
        }

        List<String> enemiesList = getSortedEnemyList();
        lines.remove(enemiesIndex);
        lines.addAll(enemiesIndex, enemiesList);
        return lines;
    }

    private @NonNull List<String> getSortedEnemyList() {
        List<Player> activeEnemies = enemies.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(pvpManager::getTimeRemainingInPvP).reversed())
                .toList();

        return IntStream.range(0, activeEnemies.size())
                .mapToObj(i -> {
                    Player p = activeEnemies.get(i);
                    String format = (activeEnemies.size() == 1 || i == activeEnemies.size() - 1)
                            ? opponentsConfig.oneLine()
                            : opponentsConfig.nextLine();

                    return format.replace("{player}", p.getName())
                            .replace("{ping}", String.valueOf(p.getPing()))
                            .replace("{health}", String.valueOf((int) p.getHealth()))
                            .replace("{time}", String.valueOf(pvpManager.getTimeRemainingInPvP(p)));
                })
                .toList();
    }
}