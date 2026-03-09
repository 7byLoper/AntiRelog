package ru.leymooo.antirelog.manager;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class BossbarManager {

    private final Map<Integer, BossBar> bossBars = new HashMap<>();
    private final PvpConfigManager configManager;

    public void createBossBars() {
        bossBars.clear();
        if (!VersionUtils.isVersion(9) || configManager.getSettings().getPvpTime() <= 0) {
            return;
        }

        String title = configManager.getMessages().getInPvpBossbar();
        if (title.isEmpty()) {
            return;
        }

        double add = 1d / (double) configManager.getSettings().getPvpTime();
        double progress = add;

        for (int i = 1; i <= configManager.getSettings().getPvpTime(); i++) {
            String actualTitle = Utils.replaceTime(title, i);

            BossBar bar = Bukkit.createBossBar(actualTitle, BarColor.RED, BarStyle.SOLID);
            bar.setProgress(progress);
            bossBars.put(i, bar);

            progress += add;
            if (progress > 1.000d) {
                progress = 1.000d;
            }
        }
    }

    public void setBossBar(Player player, int time) {
        if (!bossBars.isEmpty()) {
            for (BossBar bar : bossBars.values()) {
                bar.removePlayer(player);
            }

            bossBars.get(time).addPlayer(player);
        }
    }

    public void clearBossbar(Player player) {
        for (BossBar bar : bossBars.values()) {
            bar.removePlayer(player);
        }
    }

    public void clearBossbars() {
        if (!bossBars.isEmpty()) {
            for (BossBar bar : bossBars.values()) {
                bar.removeAll();
            }
        }

        bossBars.clear();
    }
}
