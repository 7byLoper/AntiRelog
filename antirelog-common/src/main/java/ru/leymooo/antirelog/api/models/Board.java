package ru.leymooo.antirelog.api.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.NonNull;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.leymooo.antirelog.AntiRelog;
import ru.leymooo.antirelog.api.config.OpponentsConfig;
import ru.leymooo.antirelog.api.config.ScoreboardConfig;
import ru.leymooo.antirelog.manager.PvPManager;

public class Board {

    private static final String OPPONENTS_PLACEHOLDER = "{opponents}";

    @Getter
    private final @NonNull Player player;

    private final @NonNull TabPlayer tabPlayer;

    private final @NonNull ScoreboardManager scoreboardManager;
    private final @NonNull ScoreboardConfig scoreboardConfig;
    private final @NonNull OpponentsConfig opponentsConfig;

    private final @NonNull PvPManager pvpManager;
    private final @NonNull Set<String> enemies = ConcurrentHashMap.newKeySet();

    @Nullable
    private volatile Scoreboard scoreboard;

    public Board(@NonNull Player player) {
        final AntiRelog antiRelog = AntiRelog.getPlugin(AntiRelog.class);
        final TabAPI tabAPI = TabAPI.getInstance();

        this.player = player;
        this.tabPlayer = Objects.requireNonNull(tabAPI.getPlayer(player.getUniqueId()));
        this.scoreboardManager = Objects.requireNonNull(tabAPI.getScoreboardManager());
        this.scoreboardConfig = antiRelog.getConfigManager().getScoreboardConfig();
        this.opponentsConfig = antiRelog.getConfigManager().getOpponentsConfig();
        this.pvpManager = antiRelog.getPvpManager();
    }

    public boolean showScoreboard(final int time, @NonNull String startEnemy) {
        if (scoreboard != null || scoreboardManager.hasCustomScoreboard(tabPlayer)) {
            return false;
        }

        addEnemy(startEnemy);

        final Scoreboard createdScoreboard =
                scoreboardManager.createScoreboard(getScoreboardName(), scoreboardConfig.title(), buildEnemies(time));

        scoreboardManager.showScoreboard(tabPlayer, createdScoreboard);
        scoreboard = createdScoreboard;
        return true;
    }

    public void updateScoreboard(final int time) {
        final Scoreboard activeScoreboard = scoreboard;
        if (activeScoreboard == null) {
            return;
        }

        final String title = scoreboardConfig.title();
        if (!activeScoreboard.getTitle().equals(title)) {
            activeScoreboard.setTitle(title);
        }

        activeScoreboard.setLines(buildEnemies(time));
    }

    public void resetScoreboard() {
        final Scoreboard activeScoreboard = scoreboard;
        if (activeScoreboard == null) {
            return;
        }

        scoreboard = null;

        if (scoreboardManager.hasCustomScoreboard(tabPlayer)) {
            scoreboardManager.resetScoreboard(tabPlayer);
        }

        activeScoreboard.unregister();
    }

    public void removeEnemy(@NonNull String name) {
        enemies.remove(name);
    }

    public void addEnemy(@NonNull String name) {
        if (name.isBlank()) {
            return;
        }

        enemies.add(name);
    }

    public @NonNull List<String> buildEnemies(final int time) {
        final List<String> lines = scoreboardConfig.lines().stream()
                .map(line -> replacePlayerPlaceholders(line, time))
                .collect(Collectors.toCollection(ArrayList::new));

        final int opponentsIndex = lines.indexOf(OPPONENTS_PLACEHOLDER);
        if (opponentsIndex < 0) {
            return lines;
        }

        final List<Player> activeEnemies = getActiveEnemies();
        if (activeEnemies.isEmpty()) {
            applyEmptyOpponents(lines, opponentsIndex);
            return lines;
        }

        lines.remove(opponentsIndex);
        lines.addAll(opponentsIndex, buildEnemyLines(activeEnemies));
        return lines;
    }

    private void applyEmptyOpponents(@NonNull List<String> lines, final int opponentsIndex) {
        final List<Integer> removingIndexes = scoreboardConfig.removingLinesIfNoOpponents();
        if (removingIndexes.isEmpty()) {
            lines.set(opponentsIndex, opponentsConfig.empty());
            return;
        }

        removingIndexes.stream()
                .filter(index -> index >= 0 && index < lines.size())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .forEach(index -> lines.remove(index.intValue()));

        final int remainingPlaceholderIndex = lines.indexOf(OPPONENTS_PLACEHOLDER);
        if (remainingPlaceholderIndex >= 0) {
            lines.set(remainingPlaceholderIndex, opponentsConfig.empty());
        }
    }

    private @NonNull List<Player> getActiveEnemies() {
        return enemies.stream()
                .map(Bukkit::getPlayerExact)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .sorted(Comparator.comparingInt(pvpManager::getTimeRemainingInPvP)
                        .reversed())
                .toList();
    }

    private @NonNull List<String> buildEnemyLines(@NonNull List<Player> activeEnemies) {
        return IntStream.range(0, activeEnemies.size())
                .mapToObj(index ->
                        replaceEnemyPlaceholders(getEnemyFormat(index, activeEnemies.size()), activeEnemies.get(index)))
                .toList();
    }

    private @NonNull String getEnemyFormat(final int index, final int size) {
        return size == 1 || index == size - 1 ? opponentsConfig.oneLine() : opponentsConfig.nextLine();
    }

    private @NonNull String replacePlayerPlaceholders(@NonNull String line, final int time) {
        return line.replace("{time}", String.valueOf(time))
                .replace("{player}", player.getName())
                .replace("{ping}", String.valueOf(player.getPing()));
    }

    private @NonNull String replaceEnemyPlaceholders(@NonNull String line, @NonNull Player enemy) {
        return line.replace("{player}", enemy.getName())
                .replace("{ping}", String.valueOf(enemy.getPing()))
                .replace("{health}", String.valueOf((int) enemy.getHealth()))
                .replace("{time}", String.valueOf(pvpManager.getTimeRemainingInPvP(enemy)));
    }

    private @NonNull String getScoreboardName() {
        return "antirelog-" + player.getUniqueId();
    }
}
