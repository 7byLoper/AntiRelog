package ru.leymooo.antirelog.api.models.cooldown;

import java.util.Set;
import lombok.Value;
import org.bukkit.Material;

@Value
public class ItemCooldownGroup {
    String name;
    Set<Material> materials;
    int cooldown;
    Set<CooldownAction> actions;

    public boolean contains(Material material) {
        return material != null && materials.contains(material);
    }

    public boolean supports(CooldownAction action) {
        return action != null && (actions.contains(CooldownAction.ALL) || actions.contains(action));
    }

    public boolean isDisabled() {
        return cooldown < 0;
    }

    public boolean hasCooldown() {
        return cooldown > 0;
    }
}
