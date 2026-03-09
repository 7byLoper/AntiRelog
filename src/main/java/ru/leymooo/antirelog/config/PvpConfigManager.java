package ru.leymooo.antirelog.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import ru.leymooo.antirelog.api.config.OpponentsConfig;
import ru.leymooo.antirelog.api.config.ScoreboardConfig;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.utils.Colorize;

@Getter
public class PvpConfigManager extends ConfigManager {
    private Messages messages;
    private Settings settings;

    private ScoreboardConfig scoreboardConfig;
    private OpponentsConfig opponentsConfig;

    public PvpConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        plugin.saveDefaultConfig();
        addCustomConfig(new CustomConfig("messages.yml", plugin));
        addCustomConfig(new CustomConfig("scoreboard.yml", plugin));

        messages = new Messages();
        settings = new Settings();
    }

    @Override
    public void loadValues() {
        plugin.reloadConfig();
        messages.loadValues(getCustomConfig("messages.yml"));
        settings.loadValues(plugin.getConfig());

        FileConfiguration sc = getCustomConfig("scoreboard.yml").getConfig();
        this.scoreboardConfig = new ScoreboardConfig(
                Colorize.parse(sc.getString("scoreboard.title", "")),
                Colorize.parse(sc.getStringList("scoreboard.lines")),
                sc.getIntegerList("scoreboard.removingLinesIfNoEnemies")
        );

        this.opponentsConfig = new OpponentsConfig(
                sc.getInt("opponents.max", 10),
                Colorize.parse(sc.getString("opponents.one", "")),
                Colorize.parse(sc.getString("opponents.next", "")),
                Colorize.parse(sc.getString("opponents.empty", ""))
        );
    }
}
