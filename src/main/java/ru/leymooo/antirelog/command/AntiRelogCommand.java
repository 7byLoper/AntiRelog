package ru.leymooo.antirelog.command;

import org.bukkit.permissions.Permission;
import ru.leymooo.antirelog.Antirelog;
import ru.leymooo.antirelog.command.impl.ReloadSubCommand;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.loper.suncore.api.command.AdvancedSmartCommandExecutor;

public class AntirelogCommand extends AdvancedSmartCommandExecutor {
    private final PvpConfigManager configManager;

    public AntirelogCommand(Antirelog plugin) {
        configManager = plugin.getConfigManager();
        addSubCommand(new ReloadSubCommand(plugin, plugin.getConfigManager()), new Permission("antirelog.command.reload"), "reload");
    }

    @Override
    public String getDontPermissionMessage() {
        return configManager.getMessages().getNoPermissions();
    }
}
