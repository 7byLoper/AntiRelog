package ru.leymooo.antirelog.command.subcommands;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;

@RequiredArgsConstructor
@SubCommandRegister(permission = "antirelog.command.start", aliases = "start")
public class StartSubCommand implements BuildableCommand {
    private final PvPManager pvpManager;

    @Override
    public void handle(@NotNull CommandSender commandSender, @NotNull String[] args) {
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            return;
        }

        pvpManager.startPvp(player, false, true);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(HumanEntity::getName)
                    .filter(line -> line.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
