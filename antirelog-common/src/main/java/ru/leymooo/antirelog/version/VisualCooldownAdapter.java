package ru.leymooo.antirelog.version;

import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Version-specific bridge for the client-side item cooldown animation.
 *
 * <p>The common module must not reference modern Paper overloads such as
 * {@code Player#setCooldown(ItemStack, int)}. Modern implementations keep
 * those calls inside the antirelog-modern module.</p>
 */
public interface VisualCooldownAdapter {
    void setVisualCooldown(Player player, Material material, int ticks);
}
