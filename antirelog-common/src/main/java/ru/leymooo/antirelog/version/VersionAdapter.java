package ru.leymooo.antirelog.version;

import org.bukkit.Material;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public interface VersionAdapter {
    List<PotionEffectType> getPotionEffects(PotionMeta potionMeta);

    Material getProjectileMaterial(Projectile projectile);
}
