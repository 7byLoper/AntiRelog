package ru.leymooo.antirelog.util;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class PotionUtils {
    public static List<PotionEffectType> getPotionEffects(PotionMeta potionMeta) {
        List<PotionEffectType> potionEffects = potionMeta.getCustomEffects().stream()
                .map(PotionEffect::getType)
                .collect(Collectors.toList());

        PotionEffectType baseEffect = potionMeta.getBasePotionType().getEffectType();
        if (baseEffect != null) {
            potionEffects.add(baseEffect);
        }

        return potionEffects;
    }
}
