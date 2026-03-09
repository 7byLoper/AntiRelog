package ru.leymooo.antirelog;

import com.sk89q.worldguard.WorldGuard;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import ru.leymooo.antirelog.boards.BoardManager;
import ru.leymooo.antirelog.command.AntirelogCommand;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.leymooo.antirelog.config.Settings;
import ru.leymooo.antirelog.listeners.*;
import ru.leymooo.antirelog.manager.BossbarManager;
import ru.leymooo.antirelog.manager.CooldownManager;
import ru.leymooo.antirelog.manager.PowerUpsManager;
import ru.leymooo.antirelog.manager.PvPManager;
import ru.leymooo.antirelog.placeholder.AntirelogPlaceholder;
import ru.leymooo.antirelog.util.VersionUtils;
import ru.leymooo.antirelog.wg.AntiExitFlag;

import java.util.Optional;

import static ru.leymooo.antirelog.wg.AntiExitFlag.FACTORY;

@Getter
public class Antirelog extends JavaPlugin {
    private PvPManager pvpManager;
    private CooldownManager cooldownManager;

    private boolean protocolLibEnabled;
    private boolean worldguardEnabled;
    private boolean tabEnabled;

    private PvpConfigManager configManager;
    private BoardManager scoreboardManager;

    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            AntiExitFlag.initializeFlag();
        }
    }

    @Override
    public void onEnable() {
        configManager = new PvpConfigManager(this);
        cooldownManager = new CooldownManager(configManager);
        pvpManager = new PvPManager(configManager, this);

        detectPlugins();

        getServer().getPluginManager().registerEvents(new PvPListener(this, pvpManager, configManager), this);
        getServer().getPluginManager().registerEvents(new CooldownListener(this, cooldownManager, pvpManager, configManager), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AntirelogPlaceholder(pvpManager).register();
        }

        if (worldguardEnabled) {
            AntiExitFlag.setSettingsAndManager(configManager, pvpManager);
            WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(FACTORY, null);
        }

        if (tabEnabled) {
            scoreboardManager = new BoardManager();
            Bukkit.getPluginManager().registerEvents(new ScoreboardListener(this, scoreboardManager), this);
        }

        Optional.ofNullable(getCommand("antirelog"))
                .ifPresent(command -> command.setExecutor(new AntirelogCommand(this)));
    }

    @Override
    public void onDisable() {
        if (scoreboardManager != null) {
            scoreboardManager.resetAll();
        }
        if (pvpManager != null) {
            pvpManager.onPluginDisable();
        }
    }

    private void detectPlugins() {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            WorldGuardWrapper.getInstance().registerEvents(this);
            Bukkit.getPluginManager().registerEvents(new WorldGuardListener(configManager.getSettings(), pvpManager), this);
            worldguardEnabled = true;
        }

        try {
            Class.forName("net.ess3.api.events.teleport.PreTeleportEvent");
            Bukkit.getPluginManager().registerEvents(new EssentialsTeleportListener(pvpManager, configManager.getSettings()), this);
        } catch (ClassNotFoundException ignore) {
        }

        protocolLibEnabled = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib") && VersionUtils.isVersion(9);
        tabEnabled = Bukkit.getPluginManager().isPluginEnabled("TAB");
    }

    public Settings getSettings() {
        return configManager.getSettings();
    }

    public PowerUpsManager getPowerUpsManager() {
        return pvpManager.getPowerUpsManager();
    }

    public BossbarManager getBossbarManager() {
        return pvpManager.getBossbarManager();
    }
}
