package ru.leymooo.antirelog.version;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.listeners.ModernCooldownListener;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PvPManager;

public final class AntiRelogVersionAdapter implements VersionAdapter, VisualCooldownAdapter {
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

    @Override
    public void registerVersionListeners(
            Plugin plugin, CooldownManager cooldownManager, PvPManager pvpManager, PvpConfigManager configManager) {
        plugin.getServer()
                .getPluginManager()
                .registerEvents(new ModernCooldownListener(plugin, cooldownManager, pvpManager, configManager), plugin);
    }

    /**
     * Paper 1.21.11 resolves an ItemStack cooldown through its USE_COOLDOWN
     * component and cooldown group. Keep this overload in the modern module;
     * the common module is intentionally compiled without it.
     */
    @Override
    public void setVisualCooldown(Player player, Material material, int ticks) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                player.setCooldown(item, ticks);
            }
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() == material) {
            player.setCooldown(offHand, ticks);
        }
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
