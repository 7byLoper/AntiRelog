package ru.leymooo.antirelog.config;

import lombok.Data;
import ru.loper.suncore.api.config.CustomConfig;

@Data
public class Messages {
    private String pvpStarted;
    private String pvpStartedTitle;
    private String pvpStartedSubtitle;
    private String pvpStopped;
    private String pvpStoppedTitle;
    private String pvpStoppedSubtitle;
    private String pvpStoppedActionbar;
    private String inPvpBossbar;
    private String inPvpActionbar;
    private String pvpLeaved;
    private String commandsDisabled;
    private String itemCooldown;
    private String itemDisabledInPvp;
    private String potionCooldown;
    private String potionDisabledInPvp;
    private String totemCooldown;
    private String totemDisabledInPvp;
    private String pvpStartedWithPowerups;
    private String pvpCantExit;
    private String noPermissions;
    private String enderChestBlocked;

    public void loadValues(CustomConfig config) {
        this.pvpStarted = config.configMessage("pvp-started", "&bВы начали &e&lPVP&b!");
        this.pvpStartedTitle = config.configMessage("pvp-started-title", "&bAntiRelog");
        this.pvpStartedSubtitle = config.configMessage("pvp-started-subtitle", "Вы вошли в режим &ePVP&a!");
        this.pvpStopped = config.configMessage("pvp-stopped", "&e&lPVP &bокончено");
        this.pvpStoppedTitle = config.configMessage("pvp-stopped-title", "&bAntiRelog");
        this.pvpStoppedSubtitle = config.configMessage("pvp-stopped-subtitle", "Вы вышли из режима &ePVP&a!");
        this.pvpStoppedActionbar = config.configMessage("pvp-stopped-actionbar", "&e&lPVP &aокончено, Вы снова можете использовать команды и выходить из игры!");
        this.inPvpBossbar = config.configMessage("in-pvp-bossbar", "&r&lРежим &c&lPVP &r&l- &a&l%time% &r&l%formated-sec%.");
        this.inPvpActionbar = config.configMessage("in-pvp-actionbar", "&r&lРежим &c&lPVP&r&l, не выходите из игры &a&l%time% &r&l%formated-sec%.");
        this.pvpLeaved = config.configMessage("pvp-leaved", "&aИгрок &c&l%player% &aпокинул игру во время &b&lПВП&a и был наказан.");
        this.commandsDisabled = config.configMessage("commands-disabled", "&b&lВы не можете использовать команды в &e&lPvP&b&l. &b&lПодождите &a&l%time% &b&l%formated-sec%.");
        this.itemCooldown = config.configMessage("item-cooldown", "&b&lВы сможете использовать этот предмет через &a&l%time% &b&l%formated-sec%.");
        this.itemDisabledInPvp = config.configMessage("item-disabled-in-pvp", "&b&lВы не можете использовать этот предмет в &e&lPVP &b&lрежиме");
        this.potionCooldown = config.configMessage("potion-cooldown", "&b&lВы сможете использовать это зелье через &a&l%time% &b&l%formated-sec%.");
        this.potionDisabledInPvp = config.configMessage("potion-disabled-in-pvp", "&b&lВы не можете использовать это зелье в &e&lPVP &b&lрежиме");
        this.totemCooldown = config.configMessage("totem-cooldown", "&b&lТотем небыл использован, т.к был недавно использован. Тотем будет доступен через &a&l%time% &b&l%formated-sec%.");
        this.totemDisabledInPvp = config.configMessage("totem-disabled-in-pvp", "&b&lТотем небыл использован, т.к он отключен в &e&lPVP &b&lрежиме");
        this.pvpStartedWithPowerups = config.configMessage("pvp-started-with-powerups", "&c&lВы начали пвп с включеным GM/FLY/и тд и за это получили негативный эффект");
        this.pvpCantExit = config.configMessage("pvp-cant-exit", "Вы не можете выйти из этого региона в режиме пвп!");
        this.noPermissions = config.configMessage("no-permissions", "У вас недостаточно прав");
        this.enderChestBlocked = config.configMessage("ender-chest-blocked", "Эндер сундук запрещен в пвп");
    }
}