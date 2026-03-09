package ru.leymooo.antirelog.listeners;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.event.PvpStartedEvent;
import ru.leymooo.antirelog.event.PvpStoppedEvent;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.PotionUtils;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;

import java.util.List;

public class CooldownListener implements Listener {

    private final CooldownManager cooldownManager;
    private final PvPManager pvpManager;
    private final PvpConfigManager configManager;

    public CooldownListener(Plugin plugin, CooldownManager cooldownManager, PvPManager pvpManager, PvpConfigManager configManager) {
        this.cooldownManager = cooldownManager;
        this.pvpManager = pvpManager;
        this.configManager = configManager;

        registerEntityResurrectEvent(plugin);
    }

    private void registerEntityResurrectEvent(Plugin plugin) {
        if (!VersionUtils.isVersion(11)) {
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
            public void onResurrect(EntityResurrectEvent event) {
                if (event.getEntityType() != EntityType.PLAYER) {
                    return;
                }

                Player player = (Player) event.getEntity();
                if (!pvpManager.isInPvP(player)) {
                    return;
                }

                handleCooldown(event, player, Material.TOTEM_OF_UNDYING, configManager.getMessages().getTotemCooldown(), configManager.getMessages().getTotemDisabledInPvp());
            }

        }, plugin);
    }

    private void handleCooldown(Cancellable event, Player player, Material material, String cooldownMessage, String disableMessage) {
        int cooldownTime = configManager.getSettings().getItemCooldown(material);
        if (cooldownTime == 0 || pvpManager.isBypassed(player)) {
            return;
        }
        boolean pvpStatus = pvpManager.isInPvP(player);

        if (cooldownTime <= -1) {
            if (pvpStatus) {
                event.setCancelled(true);
                player.sendMessage(disableMessage);
            }

            return;
        }

        if (cooldownManager.hasItemCooldown(player, material) && pvpStatus) {
            event.setCancelled(true);
            player.sendMessage(Utils.replaceTime(cooldownMessage, cooldownManager.getItemCooldownRemaining(player, material) / 1000));
            return;
        }

        if (pvpStatus) {
            cooldownManager.addItemCooldown(player, material);
        } else {
            cooldownManager.addItemMapCooldown(player, material);
        }
    }

    private boolean handlePotionCooldown(Cancellable event, Player player, PotionEffectType potionType, String cooldownMessage, String disableMessage) {
        int cooldownTime = configManager.getSettings().getPotionCooldown(potionType);
        if (cooldownTime == 0 || pvpManager.isBypassed(player)) {
            return false;
        }

        if (cooldownTime <= -1) {
            if (pvpManager.isInPvP(player)) {
                event.setCancelled(true);
                player.sendMessage(disableMessage);
            }
            return true;
        }

        if (cooldownManager.hasPotionCooldown(player, potionType) && pvpManager.isInPvP(player)) {
            event.setCancelled(true);
            player.sendMessage(Utils.replaceTime(cooldownMessage, cooldownManager.getPotionCooldownRemaining(player, potionType) / 1000));
            return true;
        }

        cooldownManager.addPotionCooldown(player, potionType);
        return false;
    }

    private boolean handlePotionItemCooldown(Cancellable event, Player player, ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        Material material = itemStack.getType();
        if (material != Material.POTION && material != Material.SPLASH_POTION && material != Material.LINGERING_POTION) {
            return false;
        }

        if (!itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof PotionMeta)) {
            return false;
        }

        PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

        boolean hasEffects = false;

        List<PotionEffectType> potionEffects = PotionUtils.getPotionEffects(potionMeta);
        if (!potionEffects.isEmpty()) {
            for (PotionEffectType potionType : potionEffects) {

                if (handlePotionCooldown(
                        event,
                        player,
                        potionType,
                        configManager.getMessages().getPotionCooldown(),
                        configManager.getMessages().getPotionDisabledInPvp()
                )) {
                    return true;
                }
                hasEffects = true;
            }
        }

        return hasEffects;
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack itemStack = event.getItem();
        Material material = itemStack.getType();

        if (material == Material.POTION) {
            if (handlePotionItemCooldown(event, event.getPlayer(), itemStack)) {
                return;
            }
        }

        handleCooldown(
                event,
                event.getPlayer(),
                material,
                configManager.getMessages().getItemCooldown(),
                configManager.getMessages().getItemDisabledInPvp()
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPerlLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        if (!(event.getEntity() instanceof ThrowableProjectile)) {
            return;
        }

        Player player = (Player) event.getEntity().getShooter();
        if (!pvpManager.isInPvP(player)) {
            return;
        }

        ThrowableProjectile entity = (ThrowableProjectile) event.getEntity();

        handleCooldown(
                event,
                player,
                entity.getItem().getType(),
                configManager.getMessages().getItemCooldown(),
                configManager.getMessages().getItemDisabledInPvp()
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        ItemStack itemStack = player.getInventory().getItem(event.getNewSlot());
        if (itemStack == null || !itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof PotionMeta)) {
            return;
        }

        PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();

        List<PotionEffectType> potionEffects = PotionUtils.getPotionEffects(potionMeta);
        if (potionEffects.isEmpty()) {
            return;
        }

        for (PotionEffectType potionType : potionEffects) {
            if (cooldownManager.hasPotionCooldown(player, potionType)) {
                player.sendMessage(Utils.replaceTime(configManager.getMessages().getPotionCooldown(), cooldownManager.getPotionCooldownRemaining(player, potionType) / 1000));
                break;
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK") || !pvpManager.isInPvP(event.getPlayer())) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Material material = itemStack.getType();

        if (material == Material.SPLASH_POTION || material == Material.LINGERING_POTION) {
            if (handlePotionItemCooldown(event, event.getPlayer(), itemStack)) {
                return;
            }
        }

        if (material.isEdible() || isThrowableItem(material)) {
            return;
        }

        handleCooldown(
                event,
                event.getPlayer(),
                material,
                configManager.getMessages().getItemCooldown(),
                configManager.getMessages().getItemDisabledInPvp()
        );
    }

    private boolean isThrowableItem(Material material) {
        return material == Material.SNOWBALL ||
                material == Material.EGG ||
                material == Material.ENDER_PEARL ||
                material == Material.EXPERIENCE_BOTTLE ||
                material == Material.TRIDENT ||
                material == Material.FISHING_ROD;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldownManager.remove(event.getPlayer());
    }

    @EventHandler
    public void onPvpStart(PvpStartedEvent event) {
        switch (event.getPvpStatus()) {
            case ALL_NOT_IN_PVP:
                cooldownManager.enteredToPvp(event.getDefender());
                cooldownManager.enteredToPvp(event.getAttacker());
                break;
            case ATTACKER_IN_PVP:
                cooldownManager.enteredToPvp(event.getDefender());
                break;
            case DEFENDER_IN_PVP:
                cooldownManager.enteredToPvp(event.getAttacker());
                break;
        }
    }

    @EventHandler
    public void onPvpStop(PvpStoppedEvent event) {
        cooldownManager.removedFromPvp(event.getPlayer());
    }
}