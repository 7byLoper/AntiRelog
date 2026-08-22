package ru.leymooo.antirelog.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import ru.leymooo.antirelog.api.models.cooldown.CooldownAction;
import ru.leymooo.antirelog.api.models.cooldown.ItemCooldownGroup;

@Data
public class Settings {
    private static final String SPEAR_GROUP = "SPEAR";

    private Map<String, ItemCooldownGroup> itemCooldownGroups;
    private Map<Material, List<ItemCooldownGroup>> itemCooldownGroupsByMaterial;
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
        loadItemCooldownGroups(config);
        loadPotionCooldowns(config);

        pvpTime = config.getInt("pvp-time", 12);
        disableCommandsInPvp = config.getBoolean("disable-commands-in-pvp", true);
        whiteListedCommands = config.getStringList("commands-whitelist");
        cancelInteractWithEntities = config.getBoolean("cancel-interact-with-entities", false);
        killOnLeave = config.getBoolean("kill-on-leave", true);
        killOnKick = config.getBoolean("kill-on-kick", true);
        runCommandsOnKick = config.getBoolean("run-commands-on-kick", true);
        kickMessages = config.getStringList("kick-messages");
        commandsOnLeave = config.getStringList("commands-on-leave");
        disablePowerups = config.getBoolean("disable-powerups", true);
        commandsOnPowerupsDisable = config.getStringList("commands-on-powerups-disable");
        disableTeleportsInPvp = config.getBoolean("disable-teleports-in-pvp", true);
        disableEnderChestInPvp = config.getBoolean("disable-ender-chest-in-pvp", true);
        ignoreWorldGuard = config.getBoolean("ignore-worldguard", true);
        joinPvPInWorldGuard = config.getBoolean("join-pvp-in-worldguard", false);
        disablePvpInIgnoredRegion = config.getBoolean("disable-pvp-in-ignored-region", false);

        ignoredWgRegions = config.getStringList("ignored-worldguard-regions");
        ignoredWgRegionsSet = ignoredWgRegions.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        disabledWorlds = config.getStringList("disabled-worlds");
        disabledWorldsSet = disabledWorlds.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public List<ItemCooldownGroup> getItemCooldownGroups(Material material) {
        if (material == null) {
            return List.of();
        }

        return itemCooldownGroupsByMaterial.getOrDefault(material, List.of());
    }

    public List<ItemCooldownGroup> getItemCooldownGroups(Material material, CooldownAction action) {
        return getItemCooldownGroups(material).stream()
                .filter(group -> group.supports(action))
                .toList();
    }

    public ItemCooldownGroup getItemCooldownGroup(String name) {
        return name == null ? null : itemCooldownGroups.get(name);
    }

    public int getItemCooldown(Material material) {
        return material == null ? 0 : itemCooldowns.getOrDefault(material, 0);
    }

    public boolean isItemDisabled(Material material) {
        return getItemCooldownGroups(material).stream().anyMatch(ItemCooldownGroup::isDisabled);
    }

    public boolean hasItemCooldown(Material material) {
        return getItemCooldownGroups(material).stream().anyMatch(ItemCooldownGroup::hasCooldown);
    }

    public int getPotionCooldown(PotionEffectType potionType) {
        return potionType == null ? 0 : potionsCooldowns.getOrDefault(potionType, 0);
    }

    public boolean isPotionDisabled(PotionEffectType potionType) {
        return getPotionCooldown(potionType) < 0;
    }

    public boolean hasPotionCooldown(PotionEffectType potionType) {
        return getPotionCooldown(potionType) > 0;
    }

    private void loadItemCooldownGroups(FileConfiguration config) {
        itemCooldownGroups = new LinkedHashMap<>();
        itemCooldownGroupsByMaterial = new EnumMap<>(Material.class);
        itemCooldowns = new EnumMap<>(Material.class);

        ConfigurationSection root = config.getConfigurationSection("item-cooldowns");
        if (root == null) {
            return;
        }

        root.getKeys(false).stream()
                .map(name -> loadItemCooldownGroup(root, name))
                .flatMap(Optional::stream)
                .forEach(group -> {
                    itemCooldownGroups.put(group.getName(), group);
                    group.getMaterials().forEach(material -> {
                        itemCooldownGroupsByMaterial
                                .computeIfAbsent(material, ignored -> new ArrayList<>())
                                .add(group);
                        itemCooldowns.merge(material, group.getCooldown(), this::mergeCooldown);
                    });
                });

        itemCooldownGroupsByMaterial.replaceAll((material, groups) -> List.copyOf(groups));
        itemCooldownGroups = Collections.unmodifiableMap(itemCooldownGroups);
        itemCooldownGroupsByMaterial = Collections.unmodifiableMap(itemCooldownGroupsByMaterial);
        itemCooldowns = Collections.unmodifiableMap(itemCooldowns);
    }

    private Optional<ItemCooldownGroup> loadItemCooldownGroup(ConfigurationSection root, String name) {
        ConfigurationSection section = root.getConfigurationSection(name);
        if (section == null) {
            return Optional.empty();
        }

        int cooldown = section.getInt("cooldown", 0);
        Set<Material> materials = readValues(section, "material").stream()
                .map(this::normalizeKey)
                .flatMap(this::resolveMaterials)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<CooldownAction> actions = readValues(section, "action_cooldown").stream()
                .map(CooldownAction::parse)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(CooldownAction.class)));

        if (cooldown == 0 || materials.isEmpty() || actions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ItemCooldownGroup(
                name, Collections.unmodifiableSet(materials), cooldown, Collections.unmodifiableSet(actions)));
    }

    private void loadPotionCooldowns(FileConfiguration config) {
        potionsCooldowns = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("potions-cooldowns");
        if (section == null) {
            return;
        }

        section.getKeys(false).stream()
                .flatMap(key -> createPotionCooldownEntry(section, key))
                .forEach(entry -> potionsCooldowns.put(entry.getKey(), entry.getValue()));
    }

    private List<String> readValues(ConfigurationSection section, String path) {
        Object value = section.get(path);
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }

        return value instanceof String string ? List.of(string) : List.of();
    }

    private Stream<Material> resolveMaterials(String value) {
        if (value.equals(SPEAR_GROUP) || value.equals("SHEAR")) {
            return Stream.of(Material.values()).filter(this::isSpear);
        }

        Material material = Material.matchMaterial(value);
        return material == null ? Stream.empty() : Stream.of(material);
    }

    private Stream<Map.Entry<PotionEffectType, Integer>> createPotionCooldownEntry(
            ConfigurationSection section, String key) {
        PotionEffectType type = PotionEffectType.getByName(normalizeKey(key));
        return type == null ? Stream.empty() : Stream.of(Map.entry(type, section.getInt(key, 0)));
    }

    private int mergeCooldown(int current, int next) {
        if (current < 0 || next < 0) {
            return -1;
        }

        return Math.max(current, next);
    }

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private boolean isSpear(Material material) {
        String name = material.name();
        return name.equals(SPEAR_GROUP) || name.endsWith("_SPEAR");
    }
}
