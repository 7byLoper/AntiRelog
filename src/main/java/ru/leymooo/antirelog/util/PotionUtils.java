package ru.leymooo.antirelog.util;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class PotionUtils {
    public static List<PotionEffectType> getPotionEffects(PotionMeta potionMeta) {
        List<PotionEffectType> potionEffects = potionMeta.getCustomEffects().stream()
                .map(PotionEffect::getType)
                .collect(Collectors.toList());

        PotionEffectType baseEffect = getBasePotionEffect(potionMeta.getBasePotionData().getType());
        if (baseEffect != null) {
            potionEffects.add(baseEffect);
        }

        return potionEffects;
    }

    public static PotionEffectType getBasePotionEffect(PotionType potionType) {
        return switch (potionType) {
            case INSTANT_HEAL -> PotionEffectType.HEAL;
            case INSTANT_DAMAGE -> PotionEffectType.HARM;
            case STRENGTH -> PotionEffectType.INCREASE_DAMAGE;
            case WEAKNESS -> PotionEffectType.WEAKNESS;
            case SPEED -> PotionEffectType.SPEED;
            case SLOWNESS -> PotionEffectType.SLOW;
            case JUMP -> PotionEffectType.JUMP;
            case REGEN -> PotionEffectType.REGENERATION;
            case POISON -> PotionEffectType.POISON;
            case WATER_BREATHING -> PotionEffectType.WATER_BREATHING;
            case INVISIBILITY -> PotionEffectType.INVISIBILITY;
            case NIGHT_VISION -> PotionEffectType.NIGHT_VISION;
            case FIRE_RESISTANCE -> PotionEffectType.FIRE_RESISTANCE;
            default -> null;
        };
    }
}
