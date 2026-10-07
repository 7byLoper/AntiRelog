package ru.leymooo.antirelog.util;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.colorize.TextFormatter;

@UtilityClass
public class MessageSender {

    public void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }

        player.sendMessage(format(player, message));
    }

    public void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }

        player.sendActionBar(format(player, message));
    }

    public void sendTitle(Player player, String title, String subtitle) {
        if (player == null) {
            return;
        }

        player.sendTitle(
                title == null || title.isEmpty() ? null : format(player, title),
                subtitle == null || subtitle.isEmpty() ? null : format(player, subtitle),
                10,
                20,
                10);
    }

    public String format(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String rendered = TextFormatter.placeholders(player, text);
        return StringColorize.parse(rendered);
    }
}

