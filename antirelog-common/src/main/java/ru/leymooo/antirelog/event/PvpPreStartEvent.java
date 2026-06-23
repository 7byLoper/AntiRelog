package ru.leymooo.antirelog.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PvpPreStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player defender;
    private final Player attacker;
    private final int pvpTime;
    private final PvPStatus pvpStatus;
    private boolean cancelled;

    public PvpPreStartEvent(Player defender, Player attacker, int pvpTime, PvPStatus pvpStatus) {
        this.defender = defender;
        this.attacker = attacker;
        this.pvpTime = pvpTime;
        this.pvpStatus = pvpStatus;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public enum PvPStatus {
        ATTACKER_IN_PVP,
        DEFENDER_IN_PVP,
        ALL_NOT_IN_PVP
    }
}
