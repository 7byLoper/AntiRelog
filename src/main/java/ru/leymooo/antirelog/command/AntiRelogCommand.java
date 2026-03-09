package ru.leymooo.antirelog.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.leymooo.antirelog.AntiRelog;
import ru.leymooo.antirelog.command.subcommands.ReloadSubCommand;
import ru.loper.suncore.api.command.executor.BaseCommandExecutor;
import ru.loper.suncore.api.command.register.CommandRegister;

@CommandRegister(name = "antirelog", permission = "antirelog.command.use")
public class AntiRelogCommand extends BaseCommandExecutor {
    private final AntiRelog plugin;

    public AntiRelogCommand(AntiRelog plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public String getNoPermissionMessage() {
        return plugin.getConfigManager().getMessages().getNoPermissions();
    }

    @Override
    public void registerWrappers() {
        addSubCommand(new ReloadSubCommand(plugin.getConfigManager()));
    }

    @Override
    public void handleNoArguments(@NotNull CommandSender commandSender) {

    }
}
