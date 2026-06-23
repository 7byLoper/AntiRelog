package ru.leymooo.antirelog.command.subcommands;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@SubCommandRegister(permission = "antirelog.command.reload", aliases = "reload")
public class ReloadSubCommand implements BuildableCommand {
    private final PvpConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender commandSender, @NotNull String[] strings) {
        long start = System.currentTimeMillis();

        configManager.reloadAll();
        Bukkit.getConsoleSender().sendMessage(configManager.getSettings().toString());
        Bukkit.getConsoleSender().sendMessage(configManager.getMessages().toString());

        long totalMs = System.currentTimeMillis() - start;
        commandSender.sendMessage(StringColorize.parse(String.format("&#00FF00▶ &fПлагин &7перезагружен&f за &7%d&f мс", totalMs)));

    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
