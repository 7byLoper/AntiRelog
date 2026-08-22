package ru.leymooo.antirelog.command.subcommands;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.loper.suncore.api.colorize.TextFormatter;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;

@RequiredArgsConstructor
@SubCommandRegister(permission = "antirelog.command.reload", aliases = "reload")
public class ReloadSubCommand implements BuildableCommand {
    private final PvpConfigManager configManager;

    @Override
    public void handle(@NotNull CommandSender commandSender, @NotNull String[] strings) {
        long start = System.currentTimeMillis();

        configManager.reloadAll();
        long totalMs = System.currentTimeMillis() - start;
        TextFormatter.send(
                commandSender,
                "%theme_color-status%▶ %theme_color-3%Плагин перезагружен за %theme_color-2%" + totalMs
                        + " мс%theme_gradient-close%");
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] strings) {
        return Collections.emptyList();
    }
}
