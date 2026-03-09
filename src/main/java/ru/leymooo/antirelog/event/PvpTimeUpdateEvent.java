package ru.leymooo.antirelog.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;


@Getter
@Setter
public class PvpTimeUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final int oldTime, newTime;

    private Player damagedPlayer;
    private Player damagedBy;

    public PvpTimeUpdateEvent(Player player, int oldTime, int newTime) {
        super(true);
        this.player = player;
        this.oldTime = oldTime;
        this.newTime = newTime;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
