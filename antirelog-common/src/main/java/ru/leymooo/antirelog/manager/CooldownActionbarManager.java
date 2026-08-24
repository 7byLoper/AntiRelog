package ru.leymooo.antirelog.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.util.MessageSender;
import ru.leymooo.antirelog.util.Utils;

public class CooldownActionbarManager {

    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final PvpConfigManager configManager;

    private BukkitTask task;

    public CooldownActionbarManager(Plugin plugin, CooldownManager cooldownManager, PvpConfigManager configManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.configManager = configManager;
    }

    public void start() {
        stop();

        if (!configManager.getSettings().isItemCooldownActionbarEnabled()) {
            return;
        }

        long period = configManager.getSettings().getItemCooldownActionbarUpdateTicks();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        String message = configManager.getMessages().getItemCooldownActionbar();
        if (message.isEmpty()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Material material = player.getInventory().getItemInMainHand().getType();
            if (material.isAir()) {
                continue;
            }

            long remaining = cooldownManager.getItemCooldownRemaining(player, material);
            if (remaining <= 0) {
                continue;
            }

            long seconds = Math.max(1L, (remaining + 999L) / 1000L);
            MessageSender.sendActionBar(player, Utils.replaceTime(message, seconds));
        }
    }
}
