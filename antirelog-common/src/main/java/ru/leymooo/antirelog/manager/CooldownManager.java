package ru.leymooo.antirelog.manager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.api.models.PlayerCooldowns;
import ru.leymooo.antirelog.api.models.cooldown.ItemCooldownGroup;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.version.VersionAdapter;
import ru.leymooo.antirelog.version.VisualCooldownAdapter;

public class CooldownManager {
    @Getter
    private final Settings settings;

    private final VersionAdapter versionAdapter;

    private final Map<UUID, PlayerCooldowns> playerCooldownsMap = new HashMap<>();
    private final Map<UUID, Map<String, Long>> itemCooldownsMap = new HashMap<>();

    private final Set<UUID> visualSyncPlayers = new HashSet<>();

    public CooldownManager(PvpConfigManager configManager, VersionAdapter versionAdapter) {
        settings = configManager.getSettings();
        this.versionAdapter = versionAdapter;
    }

    public void addItemCooldown(Player player, ItemCooldownGroup group) {
        if (group == null || !group.hasCooldown()) {
            return;
        }

        addItemCooldown(player, group, group.getCooldown() * 1000L, true);
    }

    public void addItemMapCooldown(Player player, ItemCooldownGroup group) {
        if (group == null || !group.hasCooldown()) {
            return;
        }

        addItemCooldown(player, group, group.getCooldown() * 1000L, false);
    }

    public void addItemCooldown(Player player, Material material) {
        settings.getItemCooldownGroups(material).forEach(group -> addItemCooldown(player, group));
    }

    public void addItemMapCooldown(Player player, Material material) {
        settings.getItemCooldownGroups(material).forEach(group -> addItemMapCooldown(player, group));
    }

    public void addItemCooldown(Player player, Material material, long duration) {
        settings.getItemCooldownGroups(material).forEach(group -> addItemCooldown(player, group, duration, true));
    }

    public void addItemMapCooldown(Player player, Material material, long duration) {
        settings.getItemCooldownGroups(material).forEach(group -> addItemCooldown(player, group, duration, false));
    }

    public void addPotionCooldown(Player player, PotionEffectType potionType) {
        int seconds = settings.getPotionCooldown(potionType);
        if (seconds > 0) {
            addPotionCooldown(player, potionType, seconds * 1000L);
        }
    }

    public void addPotionCooldown(Player player, PotionEffectType potionType, long duration) {
        if (player == null || potionType == null || duration <= 0) {
            return;
        }

        getPlayerCooldowns(player).addPotionCooldown(potionType, duration);
    }

    public void removeItemCooldown(Player player, ItemCooldownGroup group) {
        if (player == null || group == null) {
            return;
        }

        Map<String, Long> cooldowns = itemCooldownsMap.get(player.getUniqueId());
        if (cooldowns != null) {
            cooldowns.remove(group.getName());
            if (cooldowns.isEmpty()) {
                itemCooldownsMap.remove(player.getUniqueId());
            }
        }

        group.getMaterials().forEach(material -> refreshMaterialCooldown(player, material));
    }

    public void removeItemCooldown(Player player, Material material) {
        settings.getItemCooldownGroups(material).forEach(group -> removeItemCooldown(player, group));
    }

    public void removePotionCooldown(Player player, PotionEffectType potionType) {
        PlayerCooldowns cooldowns = getExisting(player);
        if (cooldowns != null) {
            cooldowns.removePotionCooldown(potionType);
        }
    }

    public void enteredToPvp(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        settings.getItemCooldownGroups().values().stream()
                .flatMap(group -> group.getMaterials().stream())
                .distinct()
                .forEach(material -> refreshMaterialCooldown(player, material));
    }

    public void removedFromPvp(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        settings.getItemCooldownGroups().values().stream()
                .flatMap(group -> group.getMaterials().stream())
                .distinct()
                .forEach(material -> setVisualCooldown(player, material, 0));
    }

    /**
     * Prevents PlayerItemCooldownEvent, fired by our own visual sync, from
     * being interpreted as a real spear dash.
     */
    public boolean isVisualSyncInProgress(Player player) {
        return player != null && visualSyncPlayers.contains(player.getUniqueId());
    }

    public boolean hasItemCooldown(Player player, ItemCooldownGroup group) {
        return getItemCooldownRemaining(player, group) > 0;
    }

    public boolean hasItemCooldown(Player player, Material material) {
        return getItemCooldownRemaining(player, material) > 0;
    }

    public boolean hasPotionCooldown(Player player, PotionEffectType potionType) {
        return getPotionCooldownRemaining(player, potionType) > 0;
    }

    public long getItemCooldownRemaining(Player player, ItemCooldownGroup group) {
        if (player == null || group == null) {
            return 0;
        }

        Map<String, Long> cooldowns = itemCooldownsMap.get(player.getUniqueId());
        if (cooldowns == null) {
            return 0;
        }

        Long expiresAt = cooldowns.get(group.getName());
        if (expiresAt == null) {
            return 0;
        }

        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(group.getName());
            if (cooldowns.isEmpty()) {
                itemCooldownsMap.remove(player.getUniqueId());
            }
            return 0;
        }

        return remaining;
    }

    public long getItemCooldownRemaining(Player player, Material material) {
        return settings.getItemCooldownGroups(material).stream()
                .mapToLong(group -> getItemCooldownRemaining(player, group))
                .max()
                .orElse(0L);
    }

    public long getPotionCooldownRemaining(Player player, PotionEffectType potionType) {
        PlayerCooldowns cooldowns = getExisting(player);
        if (cooldowns == null || potionType == null) {
            return 0;
        }

        long remaining = cooldowns.getPotionCooldownRemaining(potionType);
        if (remaining <= 0) {
            cooldowns.removePotionCooldown(potionType);
            return 0;
        }

        return remaining;
    }

    public void remove(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        visualSyncPlayers.remove(playerId);
        itemCooldownsMap.remove(playerId);

        PlayerCooldowns cooldowns = playerCooldownsMap.remove(playerId);
        if (cooldowns != null) {
            cooldowns.clearAll();
        }
    }

    public void clearAll() {
        itemCooldownsMap.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null)
                .forEach(this::removedFromPvp);

        visualSyncPlayers.clear();
        playerCooldownsMap.values().forEach(PlayerCooldowns::clearAll);
        playerCooldownsMap.clear();
        itemCooldownsMap.clear();
    }

    public PlayerCooldowns getPlayerCooldowns(Player player) {
        return playerCooldownsMap.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerCooldowns());
    }

    public void shutdown() {
        clearAll();
    }

    private void addItemCooldown(Player player, ItemCooldownGroup group, long duration, boolean applyBukkitCooldown) {
        if (player == null || group == null || duration <= 0) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + duration;
        itemCooldownsMap
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .merge(group.getName(), expiresAt, Math::max);

        if (applyBukkitCooldown) {
            group.getMaterials().forEach(material -> refreshMaterialCooldown(player, material));
        }
    }

    private void refreshMaterialCooldown(Player player, Material material) {
        long remaining = getItemCooldownRemaining(player, material);
        setVisualCooldown(player, material, toTicks(remaining));
    }

    private void setVisualCooldown(Player player, Material material, int ticks) {
        UUID playerId = player.getUniqueId();
        boolean outerSync = visualSyncPlayers.add(playerId);

        try {
            if (versionAdapter instanceof VisualCooldownAdapter visualCooldownAdapter) {
                visualCooldownAdapter.setVisualCooldown(player, material, ticks);
            } else {
                player.setCooldown(material, ticks);
            }
        } finally {
            if (outerSync) {
                visualSyncPlayers.remove(playerId);
            }
        }
    }

    private PlayerCooldowns getExisting(Player player) {
        return player == null ? null : playerCooldownsMap.get(player.getUniqueId());
    }

    private int toTicks(long duration) {
        if (duration <= 0) {
            return 0;
        }

        long ticks = (duration + 49L) / 50L;
        return (int) Math.min(Integer.MAX_VALUE, ticks);
    }
}
