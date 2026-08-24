package ru.leymooo.antirelog.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import ru.leymooo.antirelog.api.models.cooldown.CooldownAction;
import ru.leymooo.antirelog.api.models.cooldown.ItemCooldownGroup;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.event.PvpStartedEvent;
import ru.leymooo.antirelog.event.PvpStoppedEvent;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.util.MessageSender;
import ru.leymooo.antirelog.util.Utils;
import ru.leymooo.antirelog.util.VersionUtils;
import ru.leymooo.antirelog.version.VersionAdapter;

public class CooldownListener implements Listener {
    private static final long ATTACK_GRACE_MILLIS = 250L;
    private static final long USE_DEBOUNCE_MILLIS = 100L;

    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final PvPManager pvpManager;
    private final PvpConfigManager configManager;
    private final VersionAdapter versionAdapter;
    private final Set<GroupKey> pendingAttackCooldowns = new HashSet<>();
    private final Map<GroupKey, Long> attackGrace = new HashMap<>();
    private final Map<ActionKey, Long> recentActions = new HashMap<>();

    public CooldownListener(
            Plugin plugin,
            CooldownManager cooldownManager,
            PvPManager pvpManager,
            PvpConfigManager configManager,
            VersionAdapter versionAdapter) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.pvpManager = pvpManager;
        this.configManager = configManager;
        this.versionAdapter = versionAdapter;
        registerEntityResurrectEvent();
    }

    private void registerEntityResurrectEvent() {
        if (!VersionUtils.isVersion(11)) {
            return;
        }

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        new Listener() {
                            @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
                            public void onResurrect(EntityResurrectEvent event) {
                                if (event.getEntityType() != EntityType.PLAYER) {
                                    return;
                                }

                                handleAction(
                                        event,
                                        (Player) event.getEntity(),
                                        Material.TOTEM_OF_UNDYING,
                                        CooldownAction.USE,
                                        configManager.getMessages().getTotemCooldown(),
                                        configManager.getMessages().getTotemDisabledInPvp());
                            }
                        },
                        plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Material material = item.getType();

        if (material == Material.POTION && handlePotionItemCooldown(event, event.getPlayer(), item)) {
            return;
        }

        handleAction(event, event.getPlayer(), material, CooldownAction.EAT);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        Material material = getProjectileMaterial(player, event.getEntity());
        if (material == null || isSpear(material)) {
            return;
        }

        handleAction(event, player, material, CooldownAction.USE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        Material material = item.getType();
        if ((material == Material.SPLASH_POTION || material == Material.LINGERING_POTION)
                && handlePotionItemCooldown(event, event.getPlayer(), item)) {
            return;
        }

        if (isSpear(material)) {
            return;
        }

        if (material.isEdible()
                || isLaunchHandledMaterial(material)
                || !registerAction(event.getPlayer(), material, CooldownAction.USE)) {
            return;
        }

        handleAction(event, event.getPlayer(), material, CooldownAction.USE);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttackCheck(EntityDamageByEntityEvent event) {
        AttackContext context = getDirectAttackContext(event);
        if (context == null) {
            return;
        }

        List<ItemCooldownGroup> groups =
                configManager.getSettings().getItemCooldownGroups(context.material(), CooldownAction.ATTACK).stream()
                        .filter(group -> !isAttackGraceActive(context.player(), group))
                        .toList();

        blockIfUnavailable(
                event,
                context.player(),
                groups,
                configManager.getMessages().getItemCooldown(),
                configManager.getMessages().getItemDisabledInPvp());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttackCommit(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0) {
            return;
        }

        AttackContext context = getDirectAttackContext(event);
        if (context == null || pvpManager.isBypassed(context.player())) {
            return;
        }

        configManager.getSettings().getItemCooldownGroups(context.material(), CooldownAction.ATTACK).stream()
                .filter(ItemCooldownGroup::hasCooldown)
                .forEach(group -> scheduleAttackCooldown(context.player(), group));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item == null || !item.hasItemMeta() || !(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return;
        }

        versionAdapter.getPotionEffects(potionMeta).stream()
                .filter(type -> cooldownManager.hasPotionCooldown(event.getPlayer(), type))
                .findFirst()
                .ifPresent(type -> MessageSender.sendMessage(
                        event.getPlayer(),
                        Utils.replaceTime(
                                configManager.getMessages().getPotionCooldown(),
                                secondsRemaining(
                                        cooldownManager.getPotionCooldownRemaining(event.getPlayer(), type)))));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingAttackCooldowns.removeIf(key -> key.playerId().equals(playerId));
        attackGrace.keySet().removeIf(key -> key.playerId().equals(playerId));
        recentActions.keySet().removeIf(key -> key.playerId().equals(playerId));
        cooldownManager.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPvpStart(PvpStartedEvent event) {
        enterToPvp(event.getAttacker());
        enterToPvp(event.getDefender());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPvpStop(PvpStoppedEvent event) {
        cooldownManager.removedFromPvp(event.getPlayer());
    }

    private void enterToPvp(Player player) {
        if (player == null) {
            return;
        }

        cooldownManager.enteredToPvp(player);
        schedulePvpCooldownRefresh(player, 1L);
        schedulePvpCooldownRefresh(player, 2L);
    }

    private void schedulePvpCooldownRefresh(Player player, long delay) {
        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {
                            if (player.isOnline() && pvpManager.isInPvP(player)) {
                                cooldownManager.enteredToPvp(player);
                            }
                        },
                        delay);
    }

    private void handleAction(Cancellable event, Player player, Material material, CooldownAction action) {
        handleAction(
                event,
                player,
                material,
                action,
                configManager.getMessages().getItemCooldown(),
                configManager.getMessages().getItemDisabledInPvp());
    }

    private void handleAction(
            Cancellable event,
            Player player,
            Material material,
            CooldownAction action,
            String cooldownMessage,
            String disabledMessage) {
        List<ItemCooldownGroup> groups = configManager.getSettings().getItemCooldownGroups(material, action);

        if (groups.isEmpty() || blockIfUnavailable(event, player, groups, cooldownMessage, disabledMessage)) {
            return;
        }

        groups.stream().filter(ItemCooldownGroup::hasCooldown).forEach(group -> addCooldown(player, group));
    }

    private boolean blockIfUnavailable(
            Cancellable event,
            Player player,
            List<ItemCooldownGroup> groups,
            String cooldownMessage,
            String disabledMessage) {
        return blockIfUnavailable(player, groups, () -> event.setCancelled(true), cooldownMessage, disabledMessage);
    }

    private boolean blockIfUnavailable(
            Player player,
            List<ItemCooldownGroup> groups,
            Runnable deny,
            String cooldownMessage,
            String disabledMessage) {
        if (groups.isEmpty() || pvpManager.isBypassed(player) || !pvpManager.isInPvP(player)) {
            return false;
        }

        if (groups.stream().anyMatch(ItemCooldownGroup::isDisabled)) {
            deny.run();
            MessageSender.sendMessage(player, disabledMessage);
            return true;
        }

        long remaining = groups.stream()
                .mapToLong(group -> cooldownManager.getItemCooldownRemaining(player, group))
                .max()
                .orElse(0L);

        if (remaining <= 0) {
            return false;
        }

        deny.run();
        long seconds = secondsRemaining(remaining);
        MessageSender.sendMessage(player, Utils.replaceTime(cooldownMessage, seconds));
        MessageSender.sendTitle(
                player,
                Utils.replaceTime(configManager.getMessages().getCooldownTitle(), seconds),
                Utils.replaceTime(configManager.getMessages().getCooldownSubTitle(), seconds));
        return true;
    }

    private void addCooldown(Player player, ItemCooldownGroup group) {
        if (!group.hasCooldown() || pvpManager.isBypassed(player)) {
            return;
        }

        if (pvpManager.isInPvP(player)) {
            cooldownManager.addItemCooldown(player, group);
        } else {
            cooldownManager.addItemMapCooldown(player, group);
        }
    }

    private boolean handlePotionItemCooldown(Cancellable event, Player player, ItemStack item) {
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return false;
        }

        for (PotionEffectType type : versionAdapter.getPotionEffects(potionMeta)) {
            if (handlePotionCooldown(event, player, type)) {
                return true;
            }
        }

        return false;
    }

    private boolean handlePotionCooldown(Cancellable event, Player player, PotionEffectType type) {
        int cooldown = configManager.getSettings().getPotionCooldown(type);
        if (cooldown == 0 || pvpManager.isBypassed(player)) {
            return false;
        }

        if (cooldown < 0) {
            if (pvpManager.isInPvP(player)) {
                event.setCancelled(true);
                MessageSender.sendMessage(player, configManager.getMessages().getPotionDisabledInPvp());
                return true;
            }

            return false;
        }

        if (pvpManager.isInPvP(player) && cooldownManager.hasPotionCooldown(player, type)) {
            event.setCancelled(true);
            MessageSender.sendMessage(
                    player,
                    Utils.replaceTime(
                            configManager.getMessages().getPotionCooldown(),
                            secondsRemaining(cooldownManager.getPotionCooldownRemaining(player, type))));
            return true;
        }

        cooldownManager.addPotionCooldown(player, type);
        return false;
    }

    private AttackContext getDirectAttackContext(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)
                || !(event.getDamager() instanceof Player player)
                || player.equals(target)) {
            return null;
        }

        Material material = player.getInventory().getItemInMainHand().getType();
        return material.isAir() || isModernAttackMaterial(material) ? null : new AttackContext(player, material);
    }

    private void scheduleAttackCooldown(Player player, ItemCooldownGroup group) {
        GroupKey key = new GroupKey(player.getUniqueId(), group.getName());
        if (!pendingAttackCooldowns.add(key)) {
            return;
        }

        attackGrace.put(key, System.currentTimeMillis() + ATTACK_GRACE_MILLIS);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pendingAttackCooldowns.remove(key);
            if (player.isOnline()) {
                addCooldown(player, group);
            }
        });
    }

    private boolean isAttackGraceActive(Player player, ItemCooldownGroup group) {
        GroupKey key = new GroupKey(player.getUniqueId(), group.getName());
        Long expiresAt = attackGrace.get(key);
        if (expiresAt == null) {
            return false;
        }

        if (expiresAt <= System.currentTimeMillis()) {
            attackGrace.remove(key);
            return false;
        }

        return true;
    }

    private boolean isLaunchHandledMaterial(Material material) {
        return switch (material.name()) {
            case "BOW", "CROSSBOW", "SNOWBALL", "EGG", "ENDER_PEARL", "EXPERIENCE_BOTTLE", "TRIDENT", "WIND_CHARGE" -> true;
            default -> false;
        };
    }

    private Material getProjectileMaterial(Player player, Projectile projectile) {
        Material projectileMaterial = versionAdapter.getProjectileMaterial(projectile);
        if (hasUseCooldown(projectileMaterial)) {
            return projectileMaterial;
        }

        Material mainHand = player.getInventory().getItemInMainHand().getType();
        if (isRangedWeapon(mainHand)) {
            return mainHand;
        }

        Material offHand = player.getInventory().getItemInOffHand().getType();
        return isRangedWeapon(offHand) ? offHand : projectileMaterial;
    }

    private boolean hasUseCooldown(Material material) {
        return material != null && !configManager.getSettings().getItemCooldownGroups(material, CooldownAction.USE).isEmpty();
    }

    private boolean isRangedWeapon(Material material) {
        return (material == Material.BOW || material == Material.CROSSBOW) && hasUseCooldown(material);
    }

    private boolean isSpear(Material material) {
        String name = material.name();
        return name.equals("SPEAR") || name.endsWith("_SPEAR");
    }

    private boolean isModernAttackMaterial(Material material) {
        return material.name().equals("MACE") || isSpear(material);
    }

    private boolean registerAction(Player player, Material material, CooldownAction action) {
        long now = System.currentTimeMillis();
        ActionKey key = new ActionKey(player.getUniqueId(), material, action);
        Long previous = recentActions.put(key, now);
        recentActions.entrySet().removeIf(entry -> now - entry.getValue() > 1000L);
        return previous == null || now - previous >= USE_DEBOUNCE_MILLIS;
    }

    private long secondsRemaining(long millis) {
        return Math.max(1L, (millis + 999L) / 1000L);
    }

    private record AttackContext(Player player, Material material) {}

    private record GroupKey(UUID playerId, String groupName) {}

    private record ActionKey(UUID playerId, Material material, CooldownAction action) {}
}
