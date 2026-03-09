package ru.leymooo.antirelog.command.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import ru.leymooo.antirelog.Antirelog;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.suncore.utils.Colorize;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ReloadSubCommand implements SubCommand {
    private final Antirelog plugin;
    private final PvpConfigManager configManager;

    @Override
    public void onCommand(CommandSender commandSender, String[] strings) {
        long start = System.currentTimeMillis();

        configManager.reloadAll();
        plugin.getLogger().info(configManager.getSettings().toString());
        plugin.getLogger().info(configManager.getMessages().toString());

        long totalMs = System.currentTimeMillis() - start;
        commandSender.sendMessage(Colorize.parse(String.format("&#00FF00▶ &fПлагин &7перезагружен&f за &7%d&f мс", totalMs)));
    }

    @Override
    public List<String> onTabCompleter(CommandSender commandSender, String[] strings) {
        return Collections.emptyList();
    }
}
