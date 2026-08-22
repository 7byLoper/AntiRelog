package ru.leymooo.antirelog.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.config.Messages;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.DamageSourceCompat;
import ru.leymooo.antirelog.util.MessageSender;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;
import ru.loper.suncore.api.colorize.StringColorize;

public class PvPListener implements Listener {
    private final PvPManager pvpManager;
    private final Messages messages;
    private final Settings settings;
    private final Map<Player, AtomicInteger> allowedTeleports = new HashMap<>();

    public PvPListener(Plugin plugin, PvPManager pvpManager, PvpConfigManager configManager) {
        this.pvpManager = pvpManager;
        settings = configManager.getSettings();
        messages = configManager.getMessages();
        plugin.getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        () -> {
                            allowedTeleports.values().forEach(AtomicInteger::incrementAndGet);
                            allowedTeleports.values().removeIf(value -> value.get() >= 5);
                        },
                        1L,
                        1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer().getType() != EntityType.PLAYER
                || event.getInventory().getType() != InventoryType.ENDER_CHEST) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (!settings.isDisableEnderChestInPvp() || !pvpManager.isInPvP(player)) {
            return;
        }

        event.setCancelled(true);
        MessageSender.sendMessage(player, messages.getEnderChestBlocked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderChestClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) {
            return;
        }

        Player player = event.getPlayer();
        if (!settings.isDisableEnderChestInPvp() || !pvpManager.isInPvP(player)) {
            return;
        }

        event.setCancelled(true);
        MessageSender.sendMessage(player, messages.getEnderChestBlocked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player target) {
            pvpManager.playerDamagedByPlayer(DamageSourceCompat.getDamager(event), target);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent) && event.getEntity() instanceof Player target) {
            pvpManager.playerDamagedByPlayer(DamageSourceCompat.getDamager(event), target);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractWithEntity(PlayerInteractEntityEvent event) {
        if (settings.isCancelInteractWithEntities()
                && pvpManager.isPvPModeEnabled()
                && pvpManager.isInPvP(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent event) {
        if (event.getEntity() instanceof Player target) {
            pvpManager.playerDamagedByPlayer(DamageSourceCompat.getDamager(event.getCombuster()), target);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player shooter)) {
            return;
        }

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player target) || shooter.equals(target)) {
                continue;
            }

            for (PotionEffect effect : event.getPotion().getEffects()) {
                if (effect.getType().equals(PotionEffectType.POISON)) {
                    pvpManager.playerDamagedByPlayer(shooter, target);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!settings.isDisableTeleportsInPvp() || !pvpManager.isInPvP(event.getPlayer())) {
            return;
        }

        if (allowedTeleports.containsKey(event.getPlayer())) {
            return;
        }

        if ((VersionUtils.isVersion(9) && event.getCause() == TeleportCause.CHORUS_FRUIT)
                || event.getCause() == TeleportCause.ENDER_PEARL) {
            allowedTeleports.put(event.getPlayer(), new AtomicInteger());
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            event.setCancelled(true);
            return;
        }

        if (event.getFrom().distanceSquared(event.getTo()) > 100) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!settings.isDisableCommandsInPvp() || !pvpManager.isInPvP(event.getPlayer())) {
            return;
        }

        String command = event.getMessage().split(" ")[0].replaceFirst("/", "");
        if (pvpManager.isCommandWhiteListed(command)) {
            return;
        }

        event.setCancelled(true);
        MessageSender.sendMessage(
                event.getPlayer(),
                Utils.replaceTime(messages.getCommandsDisabled(), pvpManager.getTimeRemainingInPvP(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();

        if (pvpManager.isInSilentPvP(player)) {
            pvpManager.stopPvPSilent(player);
            return;
        }

        if (!pvpManager.isInPvP(player)) {
            return;
        }

        pvpManager.stopPvPSilent(player);

        if (settings.getKickMessages().isEmpty()) {
            kickedInPvp(player);
            return;
        }

        String reason = ChatColor.stripColor(event.getReason().toLowerCase());
        settings.getKickMessages().stream()
                .filter(killReason -> reason.contains(killReason.toLowerCase()))
                .findFirst()
                .ifPresent(ignored -> kickedInPvp(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        allowedTeleports.remove(player);

        if (pvpManager.isInPvP(player)) {
            pvpManager.stopPvPSilent(player);
            if (settings.isKillOnLeave()) {
                sendLeavedInPvpMessage(player);
                player.setHealth(0);
            }

            runCommands(player);
        }

        if (pvpManager.isInSilentPvP(player)) {
            pvpManager.stopPvPSilent(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (pvpManager.isInSilentPvP(player) || pvpManager.isInPvP(player)) {
            pvpManager.stopPvPSilent(player);
        }
    }

    private void kickedInPvp(Player player) {
        if (settings.isKillOnKick()) {
            player.setHealth(0);
            sendLeavedInPvpMessage(player);
        }

        if (settings.isRunCommandsOnKick()) {
            runCommands(player);
        }
    }

    private void sendLeavedInPvpMessage(Player player) {
        String message = messages.getPvpLeaved().replace("%player%", player.getName());
        if (!message.isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.sendMessage(message));
        }
    }

    private void runCommands(Player player) {
        settings.getCommandsOnLeave()
                .forEach(command -> Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        StringColorize.parse(command).replace("%player%", player.getName())));
    }
}
