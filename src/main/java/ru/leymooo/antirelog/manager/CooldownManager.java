package ru.leymooo.antirelog.manager;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.api.models.PlayerCooldowns;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.config.Settings;

import java.util.HashMap;
import java.util.Map;

public class CooldownManager {
    @Getter
    private final Settings settings;
    private final Map<Player, PlayerCooldowns> playerCooldownsMap = new HashMap<>();

    public CooldownManager(PvpConfigManager configManager) {
        this.settings = configManager.getSettings();
    }

    public void addItemCooldown(Player player, Material material) {
        int cooldownSeconds = settings.getItemCooldown(material);
        if (cooldownSeconds > 0) {
            addItemCooldown(player, material, cooldownSeconds * 1000L);
        }
    }

    public void addItemMapCooldown(Player player, Material material) {
        int cooldownSeconds = settings.getItemCooldown(material);
        if (cooldownSeconds > 0) {
            addItemMapCooldown(player, material, cooldownSeconds * 1000L);
        }
    }

    public void addItemCooldown(Player player, Material material, long duration) {
        PlayerCooldowns cooldowns = playerCooldownsMap.computeIfAbsent(player, k -> new PlayerCooldowns());
        cooldowns.addItemCooldown(material, duration);

        int cooldownTicks = (int) (duration / 50L);
        player.setCooldown(material, cooldownTicks);
    }

    public void addItemMapCooldown(Player player, Material material, long duration) {
        PlayerCooldowns cooldowns = playerCooldownsMap.computeIfAbsent(player, k -> new PlayerCooldowns());
        cooldowns.addItemCooldown(material, duration);
    }

    public void addPotionCooldown(Player player, PotionEffectType potionType) {
        int cooldownSeconds = settings.getPotionCooldown(potionType);
        if (cooldownSeconds > 0) {
            addPotionCooldown(player, potionType, cooldownSeconds * 1000L);
        }
    }

    public void addPotionCooldown(Player player, PotionEffectType potionType, long duration) {
        PlayerCooldowns cooldowns = playerCooldownsMap.computeIfAbsent(player, k -> new PlayerCooldowns());
        cooldowns.addPotionCooldown(potionType, duration);
    }

    public void removeItemCooldown(Player player, Material material) {
        PlayerCooldowns cooldowns = playerCooldownsMap.get(player);
        if (cooldowns != null) {
            cooldowns.removeItemCooldown(material);
        }

        player.setCooldown(material, 0);
    }

    public void removePotionCooldown(Player player, PotionEffectType potionType) {
        PlayerCooldowns cooldowns = playerCooldownsMap.get(player);
        if (cooldowns != null) {
            cooldowns.removePotionCooldown(potionType);
        }
    }

    public void enteredToPvp(Player player) {
        PlayerCooldowns cooldowns = getPlayerCooldowns(player);
        cooldowns.getActiveItemsCooldown().forEach(entry -> {
            int cooldownTicks = (int) (entry.getValue() - System.currentTimeMillis()) / 50;
            player.setCooldown(entry.getKey(), Math.max(0, cooldownTicks));
        });
    }

    public void removedFromPvp(Player player) {
        for (Material material : settings.getItemCooldowns().keySet()) {
            player.setCooldown(material, 0);
        }
    }

    public boolean hasItemCooldown(Player player, Material material) {
        PlayerCooldowns cooldowns = playerCooldownsMap.get(player);
        if (cooldowns == null) {
            return false;
        }

        long remaining = cooldowns.getItemCooldownRemaining(material);
        if (remaining <= 0) {
            cooldowns.removeItemCooldown(material);
            return false;
        }

        return true;
    }

    public boolean hasPotionCooldown(Player player, PotionEffectType potionType) {
        PlayerCooldowns cooldowns = playerCooldownsMap.get(player);
        if (cooldowns == null) {
            return false;
        }

        long remaining = cooldowns.getPotionCooldownRemaining(potionType);
        if (remaining <= 0) {
            cooldowns.removePotionCooldown(potionType);
            return false;
        }

        return true;
    }

    public long getItemCooldownRemaining(Player player, Material material) {
        PlayerCooldowns cooldowns = playerCooldownsMap.get(player);
        if (cooldowns == null) {
            return 0;
        }

        long remaining = cooldowns.getItemCooldownRemaining(material);
        if (remaining <= 0) {
            cooldowns.removeItemCooldown(material);
            return 0;
        }

        return remaining;
    }

    public long getPotionCooldownRemaining(Player player, PotionEffectType potionType) {
        PlayerCooldowns cooldowns = playerCooldownsMap.get(player);
        if (cooldowns == null) {
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
        PlayerCooldowns cooldowns = playerCooldownsMap.remove(player);
        if (cooldowns != null) {
            cooldowns.clearAll();
        }
    }

    public void clearAll() {
        for (Player player : playerCooldownsMap.keySet()) {
            for (Material material : settings.getItemCooldowns().keySet()) {
                player.setCooldown(material, 0);
            }
        }

        playerCooldownsMap.forEach((player, cooldowns) -> cooldowns.clearAll());
        playerCooldownsMap.clear();
    }

    public PlayerCooldowns getPlayerCooldowns(Player player) {
        return playerCooldownsMap.computeIfAbsent(player, k -> new PlayerCooldowns());
    }

    public void shutdown() {
        clearAll();
    }
}