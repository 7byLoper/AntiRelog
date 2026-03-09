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
import java.util.logging.Level;
import java.util.stream.Collectors;

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
        scoreboardConfig = antirelog.getConfigManager().getScoreboardConfig();
        opponentsConfig = antirelog.getConfigManager().getOpponentsConfig();
        pvpManager = antirelog.getPvpManager();
    }

    public void showScoreboard(final int time, @NonNull String startEnemy) {
        if (scoreboardManager.hasCustomScoreboard(tabPlayer)) {
            return;
        }

        enemies.add(startEnemy);
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
        try {
            scoreboardManager.showScoreboard(tabPlayer, scoreboard);
        } catch (Exception ignored) {
        }
    }

    public void resetScoreboard() {
        Bukkit.getScheduler().runTaskLater(AntiRelog.getPlugin(AntiRelog.class),
                () -> scoreboardManager.resetScoreboard(tabPlayer), 10L);
    }

    public void removeEnemy(@NonNull String name) {
        enemies.remove(name);
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
                    .filter(index -> index >= 0 && index < lines.size())
                    .sorted(Collections.reverseOrder())
                    .forEach(lines::remove);
            return lines;
        }

        final List<String> enemiesList = getSortedEnemyList();
        lines.remove(enemiesIndex);
        lines.addAll(enemiesIndex, enemiesList);
        return lines;
    }

    public void addEnemy(@NonNull String name) {
        if (name != null && !name.trim().isEmpty()) {
            enemies.add(name);
        }
    }

    private @NonNull List<String> getSortedEnemyList() {
        try {
            final List<String> enemiesList = new ArrayList<>(this.enemies);
            final List<String> enemiesLines = new ArrayList<>();
            final String oneFormat = opponentsConfig.oneLine();
            final String nextFormat = opponentsConfig.nextLine();

            enemiesList.removeIf(Objects::isNull);

            enemiesList.sort((name1, name2) -> {
                Player p1 = Bukkit.getPlayer(name1);
                Player p2 = Bukkit.getPlayer(name2);

                if (p1 == null && p2 == null) {
                    return 0;
                }
                if (p1 == null) {
                    return 1;
                }
                if (p2 == null) {
                    return -1;
                }

                int time1 = pvpManager.getTimeRemainingInPvP(p1);
                int time2 = pvpManager.getTimeRemainingInPvP(p2);

                return Integer.compare(time2, time1);
            });

            for (int i = 0; i < enemiesList.size(); i++) {
                final String enemyName = enemiesList.get(i);
                if (enemyName == null) {
                    continue;
                }

                Player p = Bukkit.getPlayer(enemyName);
                if (p == null) {
                    continue;
                }

                enemiesLines.add(((enemiesList.size() == 1 || i == enemiesList.size() - 1) ? oneFormat : nextFormat)
                        .replace("{player}", p.getName())
                        .replace("{ping}", String.valueOf(p.getPing()))
                        .replace("{health}", String.valueOf((int) p.getHealth()))
                        .replace("{time}", String.valueOf(pvpManager.getTimeRemainingInPvP(p))));
            }

            return enemiesLines;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error sort opponents list", e);
            return List.of();
        }
    }
}
