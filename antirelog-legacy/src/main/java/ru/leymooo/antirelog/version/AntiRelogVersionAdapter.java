package ru.leymooo.antirelog.version;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public final class AntiRelogVersionAdapter implements VersionAdapter {
    @Override
    public List<PotionEffectType> getPotionEffects(PotionMeta potionMeta) {
        Set<PotionEffectType> effects = new LinkedHashSet<>();

        potionMeta.getCustomEffects().stream().map(PotionEffect::getType).forEach(effects::add);

        PotionType baseType = resolveBasePotionType(potionMeta);
        if (baseType != null) {
            addBasePotionEffects(baseType, effects);
        }

        return new ArrayList<>(effects);
    }

    private void addBasePotionEffects(PotionType baseType, Set<PotionEffectType> effects) {
        try {
            Object value = baseType.getClass().getMethod("getPotionEffects").invoke(baseType);
            if (value instanceof Iterable<?> iterable) {
                for (Object element : iterable) {
                    if (element instanceof PotionEffect potionEffect) {
                        effects.add(potionEffect.getType());
                    }
                }
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Legacy APIs expose one effect through getEffectType().
        }

        PotionEffectType baseEffect = baseType.getEffectType();
        if (baseEffect != null) {
            effects.add(baseEffect);
        }
    }

    private PotionType resolveBasePotionType(PotionMeta potionMeta) {
        try {
            PotionData baseData = potionMeta.getBasePotionData();
            if (baseData != null) {
                return baseData.getType();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Newer servers may no longer provide legacy PotionData reliably.
        }

        try {
            Object value = potionMeta.getClass().getMethod("getBasePotionType").invoke(potionMeta);
            return value instanceof PotionType potionType ? potionType : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    @Override
    public Material getProjectileMaterial(Projectile projectile) {
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
            default -> Material.matchMaterial(entityType);
        };
    }
}
