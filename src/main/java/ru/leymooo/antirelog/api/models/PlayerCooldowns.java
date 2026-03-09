package ru.leymooo.antirelog.api.models;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class PlayerCooldowns {
    private final Map<Material, Long> itemsCooldowns;
    private final Map<PotionEffectType, Long> potionsCooldowns;

    public PlayerCooldowns() {
        itemsCooldowns = new HashMap<>();
        potionsCooldowns = new HashMap<>();
    }

    public void addItemCooldown(Material material, long duration) {
        itemsCooldowns.put(material, System.currentTimeMillis() + duration);
    }

    public void addPotionCooldown(PotionEffectType PotionEffectType, long duration) {
        potionsCooldowns.put(PotionEffectType, System.currentTimeMillis() + duration);
    }

    public Set<Map.Entry<Material, Long>> getActiveItemsCooldown() {
        long ms = System.currentTimeMillis();
        return itemsCooldowns.entrySet().stream()
                .filter(entry -> entry.getValue() > ms)
                .collect(Collectors.toSet());
    }

    public boolean hasItemCooldown(Material material) {
        Long endTime = itemsCooldowns.get(material);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public boolean hasPotionCooldown(PotionEffectType PotionEffectType) {
        Long endTime = potionsCooldowns.get(PotionEffectType);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public long getItemCooldownRemaining(Material material) {
        Long endTime = itemsCooldowns.get(material);
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public long getPotionCooldownRemaining(PotionEffectType PotionEffectType) {
        Long endTime = potionsCooldowns.get(PotionEffectType);
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public void removeItemCooldown(Material material) {
        itemsCooldowns.remove(material);
    }

    public void removePotionCooldown(PotionEffectType PotionEffectType) {
        potionsCooldowns.remove(PotionEffectType);
    }

    public void clearAll() {
        itemsCooldowns.clear();
        potionsCooldowns.clear();
    }
}