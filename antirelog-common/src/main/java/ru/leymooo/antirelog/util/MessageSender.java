package ru.leymooo.antirelog.util;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

@UtilityClass
public class MessageSender {
    public void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }

        player.sendMessage(message);
    }

    public void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }

        player.sendActionBar(message);
    }

    public void sendTitle(Player player, String title, String subtitle) {
        if (player == null) {
            return;
        }

        player.sendTitle(title, subtitle, 10, 20, 10);
    }
}
