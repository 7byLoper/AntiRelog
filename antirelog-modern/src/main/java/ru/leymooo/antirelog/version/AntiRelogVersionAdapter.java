package ru.leymooo.antirelog.version;

import org.bukkit.Material;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
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

        PotionType baseType = potionMeta.getBasePotionType();
        if (baseType != null) {
            for (PotionEffect baseEffect : baseType.getPotionEffects()) {
                effects.add(baseEffect.getType());
            }
        }

        return new ArrayList<>(effects);
    }

    @Override
    public Material getProjectileMaterial(Projectile projectile) {
        if (projectile instanceof ThrowableProjectile throwableProjectile) {
            ItemStack item = throwableProjectile.getItem();
            return item.getType();
        }

        return matchProjectileType(projectile.getType().name());
    }

    private Material matchProjectileType(String entityType) {
        return switch (entityType) {
            case "SNOWBALL" -> Material.SNOWBALL;
            case "EGG" -> Material.EGG;
            case "ENDER_PEARL" -> Material.ENDER_PEARL;
            case "THROWN_EXP_BOTTLE" -> Material.EXPERIENCE_BOTTLE;
            case "SPLASH_POTION" -> Material.SPLASH_POTION;
            case "LINGERING_POTION" -> Material.LINGERING_POTION;
            case "TRIDENT", "WIND_CHARGE" -> Material.matchMaterial(entityType);
            default -> null;
        };
    }
}
