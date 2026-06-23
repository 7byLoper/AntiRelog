package ru.leymooo.antirelog.config;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class Settings {
    private Map<Material, Integer> itemCooldowns;
    private Map<PotionEffectType, Integer> potionsCooldowns;

    private int pvpTime;

    private boolean disableCommandsInPvp;
    private List<String> whiteListedCommands;

    private boolean cancelInteractWithEntities;
    private boolean killOnLeave;
    private boolean killOnKick;
    private boolean runCommandsOnKick;

    private List<String> kickMessages;
    private List<String> commandsOnLeave;

    private boolean disablePowerups;
    private List<String> commandsOnPowerupsDisable;

    private boolean disableTeleportsInPvp;
    private boolean disableEnderChestInPvp;

    private boolean ignoreWorldGuard;
    private boolean joinPvPInWorldGuard;

    private List<String> ignoredWgRegions;
    private Set<String> ignoredWgRegionsSet;

    private boolean disablePvpInIgnoredRegion;

    private List<String> disabledWorlds;
    private Set<String> disabledWorldsSet;

    public void loadValues(FileConfiguration config) {
        this.itemCooldowns = new HashMap<>();

        ConfigurationSection itemsCooldownsSection = config.getConfigurationSection("items-cooldowns");
        if (itemsCooldownsSection != null) {
            for (String key : itemsCooldownsSection.getKeys(false)) {
                Material material = Material.getMaterial(key.toUpperCase());
                if (material == null) continue;

                itemCooldowns.put(material, itemsCooldownsSection.getInt(key, 0));
            }
        }

        this.potionsCooldowns = new HashMap<>();

        ConfigurationSection potionsCooldownsSection = config.getConfigurationSection("potions-cooldowns");
        if (potionsCooldownsSection != null) {
            for (String key : potionsCooldownsSection.getKeys(false)) {
                PotionEffectType potionType = PotionEffectType.getByName(key.toUpperCase());
                if (potionType == null) continue;

                potionsCooldowns.put(potionType, potionsCooldownsSection.getInt(key, 0));
            }
        }

        this.pvpTime = config.getInt("pvp-time", 12);
        this.disableCommandsInPvp = config.getBoolean("disable-commands-in-pvp", true);
        this.whiteListedCommands = config.getStringList("commands-whitelist");
        this.cancelInteractWithEntities = config.getBoolean("cancel-interact-with-entities", false);
        this.killOnLeave = config.getBoolean("kill-on-leave", true);
        this.killOnKick = config.getBoolean("kill-on-kick", true);
        this.runCommandsOnKick = config.getBoolean("run-commands-on-kick", true);
        this.kickMessages = config.getStringList("kick-messages");
        this.commandsOnLeave = config.getStringList("commands-on-leave");
        this.disablePowerups = config.getBoolean("disable-powerups", true);
        this.commandsOnPowerupsDisable = config.getStringList("commands-on-powerups-disable");
        this.disableTeleportsInPvp = config.getBoolean("disable-teleports-in-pvp", true);
        this.disableEnderChestInPvp = config.getBoolean("disable-ender-chest-in-pvp", true);
        this.ignoreWorldGuard = config.getBoolean("ignore-worldguard", true);
        this.joinPvPInWorldGuard = config.getBoolean("join-pvp-in-worldguard", false);
        this.disablePvpInIgnoredRegion = config.getBoolean("disable-pvp-in-ignored-region", false);

        this.ignoredWgRegions = config.getStringList("ignored-worldguard-regions");
        this.ignoredWgRegionsSet = ignoredWgRegions.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        this.disabledWorlds = config.getStringList("disabled-worlds");
        this.disabledWorldsSet = disabledWorlds.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public int getItemCooldown(Material material) {
        return itemCooldowns.getOrDefault(material, 0);
    }

    public boolean isItemDisabled(Material material) {
        Integer cooldown = itemCooldowns.get(material);
        return cooldown != null && cooldown == -1;
    }

    public boolean hasItemCooldown(Material material) {
        Integer cooldown = itemCooldowns.get(material);
        return cooldown != null && cooldown > 0;
    }

    public int getPotionCooldown(PotionEffectType potionType) {
        return potionsCooldowns.getOrDefault(potionType, 0);
    }

    public boolean isPotionDisabled(PotionEffectType potionType) {
        Integer cooldown = potionsCooldowns.get(potionType);
        return cooldown != null && cooldown == -1;
    }

    public boolean hasPotionCooldown(PotionEffectType potionType) {
        Integer cooldown = potionsCooldowns.get(potionType);
        return cooldown != null && cooldown > 0;
    }
}