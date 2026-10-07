package ru.leymooo.antirelog.listeners;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import ru.leymooo.antirelog.boards.BoardManager;
import ru.leymooo.antirelog.event.PvpStartedEvent;
import ru.leymooo.antirelog.event.PvpStoppedEvent;
import ru.leymooo.antirelog.event.PvpTimeUpdateEvent;
import ru.leymooo.antirelog.manager.PvPManager;

@RequiredArgsConstructor
public class ScoreboardListener implements Listener {
    private final Plugin plugin;
    private final BoardManager boardManager;
    private final PvPManager pvpManager;

    @EventHandler
    private void onStartPVP(PvpStartedEvent event) {
        Bukkit.getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {
                            String attackerName = event.getAttacker().getName();
                            String defenderName = event.getDefender().getName();
                            int pvpTime = event.getPvpTime();
                            switch (event.getPvpStatus()) {
                                case ALL_NOT_IN_PVP -> {
                                    showIfInPvp(event.getAttacker(), defenderName, pvpTime);
                                    showIfInPvp(event.getDefender(), attackerName, pvpTime);
                                }
                                case ATTACKER_IN_PVP -> {
                                    if (pvpManager.isInPvP(event.getAttacker())) {
                                        Optional.ofNullable(boardManager.getFrom(event.getAttacker()))
                                                .ifPresent(board -> board.addEnemy(defenderName));
                                    }
                                    showIfInPvp(event.getDefender(), attackerName, pvpTime);
                                }
                                case DEFENDER_IN_PVP -> {
                                    if (pvpManager.isInPvP(event.getDefender())) {
                                        Optional.ofNullable(boardManager.getFrom(event.getDefender()))
                                                .ifPresent(board -> board.addEnemy(attackerName));
                                    }
                                    showIfInPvp(event.getAttacker(), defenderName, pvpTime);
                                }
                            }
                        },
                        2L);
    }

    @EventHandler
    private void onPVP(PvpTimeUpdateEvent event) {
        // PvpTimeUpdateEvent is asynchronous, but both Bukkit's Player API and
        // TAB's scoreboard implementation must only be accessed from the server thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Optional.ofNullable(boardManager.getFrom(event.getPlayer())).ifPresent(board -> {
                if (event.getDamagedPlayer() != null && !event.getPlayer().equals(event.getDamagedPlayer())) {
                    board.addEnemy(event.getDamagedPlayer().getName());
                } else if (event.getDamagedBy() != null && !event.getPlayer().equals(event.getDamagedBy())) {
                    board.addEnemy(event.getDamagedBy().getName());
                }
                board.updateScoreboard(event.getNewTime());
            });
        });
    }

    @EventHandler
    private void onStopPVP(PvpStoppedEvent event) {
        boardManager.removeAll(event.getPlayer().getName());
        boardManager.reset(event.getPlayer());
    }

    private void showIfInPvp(Player player, String enemyName, int time) {
        if (player.isOnline() && !player.isDead() && pvpManager.isInPvP(player)) {
            boardManager.show(player, enemyName, time);
        }
    }
}
