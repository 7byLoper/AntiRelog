package ru.leymooo.antirelog.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.leymooo.antirelog.event.PvpPreStartEvent.PvPStatus;

@Getter
public class PvpStartedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player defender;
    private final Player attacker;
    private final int pvpTime;
    private final PvPStatus pvpStatus;

    public PvpStartedEvent(Player defender, Player attacker, int pvpTime, PvPStatus pvpStatus) {
        this.defender = defender;
        this.attacker = attacker;
        this.pvpTime = pvpTime;
        this.pvpStatus = pvpStatus;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
