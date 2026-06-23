package ru.leymooo.antirelog.version;

import org.bukkit.Material;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AntiRelogVersionAdapter implements VersionAdapter {
    @Override
    public List<PotionEffectType> getPotionEffects(PotionMeta potionMeta) {
        Set<PotionEffectType> effects = new LinkedHashSet<>();

        for (PotionEffect customEffect : potionMeta.getCustomEffects()) {
            effects.add(customEffect.getType());
        }

        PotionData baseData = potionMeta.getBasePotionData();
        PotionType baseType = baseData.getType();
        PotionEffectType baseEffect = baseType.getEffectType();
        if (baseEffect != null) {
            effects.add(baseEffect);
        }

        return new ArrayList<>(effects);
    }

    @Override
    public Material getProjectileMaterial(Projectile projectile) {
        return matchProjectileType(projectile.getType().name());
    }

    private Material matchProjectileType(String entityType) {
        switch (entityType) {
            case "SNOWBALL":
                return Material.SNOWBALL;
            case "EGG":
                return Material.EGG;
            case "ENDER_PEARL":
                return Material.ENDER_PEARL;
            case "THROWN_EXP_BOTTLE":
                return Material.EXPERIENCE_BOTTLE;
            case "SPLASH_POTION":
                return Material.SPLASH_POTION;
            case "LINGERING_POTION":
                return Material.LINGERING_POTION;
            case "TRIDENT":
                return Material.matchMaterial("TRIDENT");
            default:
                return Material.matchMaterial(entityType);
        }
    }
}
