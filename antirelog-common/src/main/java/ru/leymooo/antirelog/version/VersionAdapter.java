package ru.leymooo.antirelog.version;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PvPManager;

public interface VersionAdapter {
    List<PotionEffectType> getPotionEffects(PotionMeta potionMeta);

    Material getProjectileMaterial(Projectile projectile);

    default void registerVersionListeners(
            Plugin plugin, CooldownManager cooldownManager, PvPManager pvpManager, PvpConfigManager configManager) {}
}
