package ru.leymooo.antirelog.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

@UtilityClass
public class DamageSourceCompat {
    private static final Method GET_DAMAGE_SOURCE = findMethod(EntityDamageEvent.class, "getDamageSource");

    public Player getDamager(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            Player player = getDamager(byEntityEvent.getDamager());
            if (player != null) {
                return player;
            }
        }

        Object source = invoke(GET_DAMAGE_SOURCE, event);
        Player causingPlayer = getDamager(asEntity(invoke(source, "getCausingEntity")));
        return causingPlayer != null ? causingPlayer : getDamager(asEntity(invoke(source, "getDirectEntity")));
    }

    public Player getDamager(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }

        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }

        if (entity instanceof TNTPrimed tntPrimed) {
            return getDamager(tntPrimed.getSource());
        }

        if (entity instanceof AreaEffectCloud areaEffectCloud && areaEffectCloud.getSource() instanceof Player player) {
            return player;
        }

        return null;
    }

    private Entity asEntity(Object value) {
        return value instanceof Entity entity ? entity : null;
    }

    private Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private Object invoke(Object target, String name) {
        if (target == null) {
            return null;
        }

        return invoke(findMethod(target.getClass(), name), target);
    }

    private Object invoke(Method method, Object target, Object... arguments) {
        if (method == null || target == null) {
            return null;
        }

        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return null;
        }
    }
}
