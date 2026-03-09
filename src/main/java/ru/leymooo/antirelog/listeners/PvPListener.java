package ru.leymooo.antirelog.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.config.Messages;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PvPListener implements Listener {
    private final PvPManager pvpManager;
    private final Messages messages;
    private final Settings settings;
    private final Map<Player, AtomicInteger> allowedTeleports = new HashMap<>();

    public PvPListener(Plugin plugin, PvPManager pvpManager, PvpConfigManager configManager) {
        this.pvpManager = pvpManager;
        this.settings = configManager.getSettings();
        this.messages = configManager.getMessages();
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            allowedTeleports.values().forEach(ai -> ai.set(ai.get() + 1));
            allowedTeleports.values().removeIf(ai -> ai.get() >= 5);
        }, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer().getType() != EntityType.PLAYER || event.getInventory().getType() != InventoryType.ENDER_CHEST) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (!settings.isDisableEnderChestInPvp() || !pvpManager.isInPvP(player)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(messages.getEnderChestBlocked());
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
        player.sendMessage(messages.getEnderChestBlocked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER) {
            return;
        }
        Player target = (Player) event.getEntity();
        Player damager = getDamager(event.getDamager());
        pvpManager.playerDamagedByPlayer(damager, target);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractWithEntity(PlayerInteractEntityEvent event) {
        if (settings.isCancelInteractWithEntities() && pvpManager.isPvPModeEnabled() && pvpManager.isInPvP(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        Player damager = getDamager(event.getCombuster());
        pvpManager.playerDamagedByPlayer(damager, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player shooter)) {
            return;
        }

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (event.getEntityType() != EntityType.PLAYER || shooter.equals(entity)) {
                continue;
            }

            for (PotionEffect ef : event.getPotion().getEffects()) {
                if (ef.getType().equals(PotionEffectType.POISON)) {
                    pvpManager.playerDamagedByPlayer(shooter, (Player) entity);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        if (settings.isDisableTeleportsInPvp() && pvpManager.isInPvP(event.getPlayer())) {
            if (allowedTeleports.containsKey(event.getPlayer())) {
                return;
            }

            if ((VersionUtils.isVersion(9) && event.getCause() == TeleportCause.CHORUS_FRUIT) || event.getCause() == TeleportCause.ENDER_PEARL) {
                allowedTeleports.put(event.getPlayer(), new AtomicInteger(0));
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
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (settings.isDisableCommandsInPvp() && pvpManager.isInPvP(event.getPlayer())) {
            String command = event.getMessage().split(" ")[0].replaceFirst("/", "");
            if (pvpManager.isCommandWhiteListed(command)) {
                return;
            }
            event.setCancelled(true);
            String message = Utils.color(messages.getCommandsDisabled());
            if (!message.isEmpty()) {
                event.getPlayer().sendMessage(Utils.replaceTime(message, pvpManager.getTimeRemainingInPvP(event.getPlayer())));
            }
        }
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
        for (String killReason : settings.getKickMessages()) {
            if (reason.contains(killReason.toLowerCase())) {
                kickedInPvp(player);
                return;
            }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        allowedTeleports.remove(event.getPlayer());

        if (pvpManager.isInPvP(event.getPlayer())) {
            pvpManager.stopPvPSilent(event.getPlayer());
            if (settings.isKillOnLeave()) {
                sendLeavedInPvpMessage(event.getPlayer());
                event.getPlayer().setHealth(0);
            }
            runCommands(event.getPlayer());
        }

        if (pvpManager.isInSilentPvP(event.getPlayer())) {
            pvpManager.stopPvPSilent(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        if (pvpManager.isInSilentPvP(event.getEntity()) || pvpManager.isInPvP(event.getEntity())) {
            pvpManager.stopPvPSilent(event.getEntity());
        }
    }

    private void sendLeavedInPvpMessage(Player p) {
        String message = Utils.color(messages.getPvpLeaved()).replace("%player%", p.getName());
        if (!message.isEmpty()) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                pl.sendMessage(message);
            }
        }
    }

    private void runCommands(Player leaved) {
        if (!settings.getCommandsOnLeave().isEmpty()) {
            settings.getCommandsOnLeave().forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    Utils.color(command).replace("%player%", leaved.getName())));
        }
    }

    private Player getDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        } else if (damager instanceof Projectile proj) {
            if (proj.getShooter() instanceof Player) {
                return (Player) proj.getShooter();
            }
        } else if (damager instanceof TNTPrimed tntPrimed) {
            return getDamager(tntPrimed.getSource());
        } else if (VersionUtils.isVersion(9) && damager instanceof AreaEffectCloud aec) {
            if (aec.getSource() instanceof Player) {
                return (Player) aec.getSource();
            }
        }

        return null;
    }
}
