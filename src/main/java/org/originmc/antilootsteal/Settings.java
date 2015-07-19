package org.originmc.antilootsteal;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;

import java.util.ArrayList;

public final class Settings {

    private final AntiLootSteal plugin;

    Settings(AntiLootSteal plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        Configuration defaults = plugin.getConfig().getDefaults();
        defaults.set("denied-blocks", new ArrayList<>());
    }

    public int getConfigVersion() {
        return plugin.getConfig().getInt("config-version", 0);
    }

    public int getLatestConfigVersion() {
        return plugin.getConfig().getDefaults().getInt("config-version", 0);
    }

    public boolean isOutdated() {
        return getConfigVersion() < getLatestConfigVersion();
    }

    public int getProtectionDuration() {
        return plugin.getConfig().getInt("protection-duration", 15);
    }

    public String getDroppedMessage() {
        String message = plugin.getConfig().getString("dropped-message", null);
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : null;
    }

    public String getDenyMessage() {
        String message = plugin.getConfig().getString("deny-message", null);
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : null;
    }

}
