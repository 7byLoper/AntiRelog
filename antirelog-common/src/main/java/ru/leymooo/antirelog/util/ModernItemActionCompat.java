package ru.leymooo.antirelog.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

@UtilityClass
public class ModernItemActionCompat {
    private static final String ITEM_COOLDOWN_EVENT = "io.papermc.paper.event.player.PlayerItemCooldownEvent";
    private static final String STOP_USING_EVENT = "io.papermc.paper.event.player.PlayerStopUsingItemEvent";
    private static final String PRE_ATTACK_EVENT = "io.papermc.paper.event.player.PrePlayerAttackEntityEvent";
    private static final String SMASH_ATTACK_EVENT = "io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent";

    public void register(
            Plugin plugin,
            ItemCooldownHandler itemCooldownHandler,
            StopUsingHandler stopUsingHandler,
            PreAttackHandler preAttackHandler,
            SmashAttackHandler smashAttackHandler) {
        registerItemCooldownEvent(plugin, itemCooldownHandler);
        registerStopUsingEvent(plugin, stopUsingHandler);
        registerPreAttackEvent(plugin, preAttackHandler);
        registerSmashAttackEvent(plugin, smashAttackHandler);
    }

    private void registerItemCooldownEvent(Plugin plugin, ItemCooldownHandler handler) {
        registerEvent(plugin, ITEM_COOLDOWN_EVENT, EventPriority.MONITOR, false, event -> {
            Player player = invoke(event, "getPlayer");
            Material material = invoke(event, "getType");
            Integer ticks = invoke(event, "getCooldown");

            if (player != null && material != null && ticks != null) {
                handler.handle(player, material, ticks);
            }
        });
    }

    private void registerStopUsingEvent(Plugin plugin, StopUsingHandler handler) {
        registerEvent(plugin, STOP_USING_EVENT, EventPriority.MONITOR, false, event -> {
            Player player = invoke(event, "getPlayer");
            ItemStack item = invoke(event, "getItem");
            Integer ticksHeldFor = invoke(event, "getTicksHeldFor");

            if (player != null && item != null && ticksHeldFor != null) {
                handler.handle(player, item, ticksHeldFor);
            }
        });
    }

    private void registerPreAttackEvent(Plugin plugin, PreAttackHandler handler) {
        registerEvent(plugin, PRE_ATTACK_EVENT, EventPriority.HIGHEST, false, event -> {
            Player player = invoke(event, "getPlayer");
            Entity attacked = invoke(event, "getAttacked");
            Boolean willAttack = invoke(event, "willAttack");

            if (player != null && attacked != null && willAttack != null && event instanceof Cancellable cancellable) {
                handler.handle(player, attacked, willAttack, cancellable);
            }
        });
    }

    private void registerSmashAttackEvent(Plugin plugin, SmashAttackHandler handler) {
        registerEvent(plugin, SMASH_ATTACK_EVENT, EventPriority.HIGHEST, false, event -> {
            Entity entity = invoke(event, "getEntity");
            Entity target = invoke(event, "getTarget");
            ItemStack weapon = invoke(event, "getWeapon");
            Boolean originalResult = invoke(event, "getOriginalResult");

            if (entity instanceof Player player && target != null && weapon != null && originalResult != null) {
                handler.handle(player, target, weapon, originalResult, () -> setResult(event, Event.Result.DENY));
            }
        });
    }

    private void registerEvent(
            Plugin plugin, String className, EventPriority priority, boolean ignoreCancelled, EventHandler handler) {
        Class<? extends Event> eventClass = findEventClass(className);
        if (eventClass == null) {
            return;
        }

        Listener listener = new Listener() {};

        plugin.getServer()
                .getPluginManager()
                .registerEvent(
                        eventClass,
                        listener,
                        priority,
                        (registeredListener, event) -> handler.handle(event),
                        plugin,
                        ignoreCancelled);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Event> findEventClass(String className) {
        try {
            Class<?> type = Class.forName(className);
            return Event.class.isAssignableFrom(type) ? (Class<? extends Event>) type : null;
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private void setResult(Object event, Event.Result result) {
        Method method = findMethod(event.getClass(), "setResult", Event.Result.class);
        invoke(method, event, result);
    }

    private Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        return (T) invoke(findMethod(target.getClass(), methodName), target);
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

    @FunctionalInterface
    public interface ItemCooldownHandler {
        void handle(Player player, Material material, int ticks);
    }

    @FunctionalInterface
    public interface StopUsingHandler {
        void handle(Player player, ItemStack item, int ticksHeldFor);
    }

    @FunctionalInterface
    public interface PreAttackHandler {
        void handle(Player player, Entity attacked, boolean willAttack, Cancellable event);
    }

    @FunctionalInterface
    public interface SmashAttackHandler {
        void handle(Player player, Entity target, ItemStack weapon, boolean originalResult, Runnable deny);
    }

    @FunctionalInterface
    private interface EventHandler {
        void handle(Event event);
    }
}
