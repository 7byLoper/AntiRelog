package ru.leymooo.antirelog.manager;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import ru.leymooo.antirelog.config.PvpConfigManager;
import ru.loper.suncore.api.colorize.StringColorize;

public class PowerUpsManager {
    private final PvpConfigManager configManager;

    private boolean cmi;
    private Essentials essentials;

    public PowerUpsManager(PvpConfigManager configManager) {
        this.configManager = configManager;
        detectPlugins();
    }


    public boolean disablePowerUps(Player player) {
        if (player.hasPermission("antirelog.bypass.checks")) {
            return false;
        }

        boolean disabled = false;
        if (player.getGameMode() == GameMode.CREATIVE) {
            if (Bukkit.getDefaultGameMode() == GameMode.ADVENTURE) {
                player.setGameMode(GameMode.ADVENTURE);
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }
            disabled = true;
        }

        if (player.isFlying() || player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            disabled = true;
        }

        if (checkEssentials(player)) {
            disabled = true;
        }

        if (checkCMI(player)) {
            disabled = true;
        }

        return disabled;
    }


    public void disablePowerUpsWithRunCommands(Player player) {
        if (disablePowerUps(player) && !configManager.getSettings().getCommandsOnPowerupsDisable().isEmpty()) {
            configManager.getSettings().getCommandsOnPowerupsDisable().forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    StringColorize.parse(command.replace("%player%", player.getName()))));
            String message = configManager.getMessages().getPvpStartedWithPowerups();
            if (!message.isEmpty()) {
                player.sendMessage(message);
            }
        }
    }

    public void detectPlugins() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        this.essentials = pluginManager.isPluginEnabled("Essentials") ? (Essentials) pluginManager.getPlugin("Essentials") : null;
        this.cmi = pluginManager.isPluginEnabled("CMI");
    }


    private boolean checkEssentials(Player player) {
        boolean disabled = false;
        if (essentials != null) {
            User user = essentials.getUser(player);
            if (user.isVanished()) {
                user.setVanished(false);
                disabled = true;
            }

            if (user.isGodModeEnabled()) {
                user.setGodModeEnabled(false);
                disabled = true;
            }
        }

        return disabled;
    }

    private boolean checkCMI(Player player) {
        boolean disabled = false;
        if (cmi) {
            CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
            if (user != null) {
                if (user.isGod()) {
                    CMI.getInstance().getNMS().changeGodMode(player, false);
                    user.setTgod(0);
                    disabled = true;
                }
                if (user.isVanished()) {
                    user.setVanished(false);
                    disabled = true;
                }
            }
        }

        return disabled;
    }
}
