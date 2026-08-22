package ru.leymooo.antirelog.listeners;

import io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.leymooo.antirelog.api.models.cooldown.CooldownAction;
import ru.leymooo.antirelog.api.models.cooldown.ItemCooldownGroup;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.MessageSender;
import ru.leymooo.antirelog.util.Utils;

public final class ModernCooldownListener implements Listener {
    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final PvPManager pvpManager;
    private final PvpConfigManager configManager;

    public ModernCooldownListener(
            Plugin plugin, CooldownManager cooldownManager, PvPManager pvpManager, PvpConfigManager configManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.pvpManager = pvpManager;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpearUse(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isSpear(item.getType())) {
            return;
        }

        if (blockIfUnavailable(event.getPlayer(), item.getType(), CooldownAction.USE)) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    /**
     * Vanilla applies the spear dash cooldown only after the dash was
     * actually accepted. This event is therefore the commit point for USE.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpearCooldown(PlayerItemCooldownEvent event) {
        Material material = event.getType();
        if (event.getCooldown() <= 0
                || !isSpear(material)
                || cooldownManager.isVisualSyncInProgress(event.getPlayer())) {
            return;
        }

        scheduleCooldown(event.getPlayer(), material, CooldownAction.USE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPreAttack(PrePlayerAttackEntityEvent event) {
        if (!event.willAttack() || !(event.getAttacked() instanceof Player)) {
            return;
        }

        Material material = event.getPlayer().getInventory().getItemInMainHand().getType();
        if (!isModernAttackMaterial(material)) {
            return;
        }

        if (blockIfUnavailable(event.getPlayer(), material, CooldownAction.ATTACK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmashCheck(EntityAttemptSmashAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getTarget() instanceof Player)
                || event.getWeapon().getType() != Material.MACE) {
            return;
        }

        if (blockIfUnavailable(player, Material.MACE, CooldownAction.ATTACK)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * Starts ATTACK cooldown only after uncancelled positive damage. This
     * covers a normal mace hit, a mace smash and spear damage without
     * starting cooldown on a miss.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModernWeaponDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)
                || event.getFinalDamage() <= 0
                || !(event.getDamageSource().getCausingEntity() instanceof Player player)) {
            return;
        }

        Material material = resolveAttackMaterial(event, player);
        if (material != null) {
            scheduleCooldown(player, material, CooldownAction.ATTACK);
        }
    }

    private boolean blockIfUnavailable(Player player, Material material, CooldownAction action) {
        List<ItemCooldownGroup> groups = configManager.getSettings().getItemCooldownGroups(material, action);

        if (groups.isEmpty() || pvpManager.isBypassed(player) || !pvpManager.isInPvP(player)) {
            return false;
        }

        if (groups.stream().anyMatch(ItemCooldownGroup::isDisabled)) {
            MessageSender.sendMessage(player, configManager.getMessages().getItemDisabledInPvp());
            return true;
        }

        long remaining = groups.stream()
                .mapToLong(group -> cooldownManager.getItemCooldownRemaining(player, group))
                .max()
                .orElse(0L);

        if (remaining <= 0) {
            return false;
        }

        long seconds = Math.max(1L, (remaining + 999L) / 1000L);
        MessageSender.sendMessage(
                player, Utils.replaceTime(configManager.getMessages().getItemCooldown(), seconds));
        MessageSender.sendTitle(
                player,
                Utils.replaceTime(configManager.getMessages().getCooldownTitle(), seconds),
                Utils.replaceTime(configManager.getMessages().getCooldownSubTitle(), seconds));
        return true;
    }

    private void scheduleCooldown(Player player, Material material, CooldownAction action) {
        plugin.getServer().getScheduler().runTask(plugin, () -> addCooldown(player, material, action));
    }

    private void addCooldown(Player player, Material material, CooldownAction action) {
        if (!player.isOnline() || pvpManager.isBypassed(player)) {
            return;
        }

        configManager.getSettings().getItemCooldownGroups(material, action).stream()
                .filter(ItemCooldownGroup::hasCooldown)
                .filter(group -> !cooldownManager.hasItemCooldown(player, group))
                .forEach(group -> {
                    if (pvpManager.isInPvP(player)) {
                        cooldownManager.addItemCooldown(player, group);
                    } else {
                        cooldownManager.addItemMapCooldown(player, group);
                    }
                });
    }

    private Material resolveAttackMaterial(EntityDamageEvent event, Player player) {
        DamageType damageType = event.getDamageSource().getDamageType();
        if (DamageType.MACE_SMASH.equals(damageType)) {
            return Material.MACE;
        }

        if (DamageType.SPEAR.equals(damageType)) {
            return findHeldSpear(player);
        }

        Material mainHand = player.getInventory().getItemInMainHand().getType();
        return isModernAttackMaterial(mainHand) ? mainHand : null;
    }

    private Material findHeldSpear(Player player) {
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        if (isSpear(mainHand)) {
            return mainHand;
        }

        Material offHand = player.getInventory().getItemInOffHand().getType();
        return isSpear(offHand) ? offHand : null;
    }

    private boolean isModernAttackMaterial(Material material) {
        return material == Material.MACE || isSpear(material);
    }

    private boolean isSpear(Material material) {
        String name = material.name();
        return name.equals("SPEAR") || name.endsWith("_SPEAR");
    }
}
