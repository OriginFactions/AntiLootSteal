package org.originmc.antilootsteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AntiLootSteal extends JavaPlugin implements Listener {

    private static final String PERMISSION_BYPASS = "antilootsteal.bypass";

    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    private Settings settings;

    @Override
    public void onEnable() {
        // Load settings
        saveDefaultConfig();
        settings = new Settings(this);
        if (settings.isOutdated()) {
            getLogger().warning("**WARNING**");
            getLogger().warning("Your configuration file is outdated.");
            getLogger().warning("Backup your old file and then delete it to generate a new copy.");
        }

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info(getName() + " has been enabled!");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void denyPickup(PlayerPickupItemEvent event) {
        // Do nothing if player has permission
        Player player = event.getPlayer();
        if (player.hasPermission(PERMISSION_BYPASS)) return;

        // Do nothing if loot is not protected
        Item item = event.getItem();
        if (!item.hasMetadata("AntiLootSteal")) return;

        // Remove metadata from loot if it is no longer being protected or the attacker is picking it up
        String[] itemData = item.getMetadata("AntiLootSteal").get(0).asString().split("-");
        long remaining = System.currentTimeMillis() - Long.valueOf(itemData[1]);
        if (player.getName().equals(itemData[0]) || remaining >= settings.getProtectionDuration() * 1000) {
            event.getItem().removeMetadata("AntiLootSteal", this);
            return;
        }

        // Prevent the player from picking up the loot
        event.setCancelled(true);

        // Do nothing if deny message is null
        if (settings.getDenyMessage() == null) return;

        // Do nothing if player is on a message cooldown
        UUID uuid = player.getUniqueId();
        if (messageCooldowns.containsKey(uuid) &&
                System.currentTimeMillis() - messageCooldowns.get(uuid) < 1000) return;

        // Tell the player this loot is protected
        int cooldown = settings.getProtectionDuration() - (int) (remaining / 1000);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', settings.getDenyMessage()
                .replace("{TIME}", "" + DurationUtils.format(cooldown))));

        // Give the player a message cooldown
        messageCooldowns.put(uuid, System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void protectLoot(PlayerDeathEvent event) {
        // Do nothing if the player did not die from PvP
        Player victim = event.getEntity();
        if (victim.getKiller() == null) return;

        // Send killer a confirmation message
        Player killer = victim.getKiller();
        if (settings.getDroppedMessage()  != null) {
            killer.sendMessage(ChatColor.translateAlternateColorCodes('&', settings.getDroppedMessage()
                    .replace("{TIME}", "" + DurationUtils.format(settings.getProtectionDuration()))
                    .replace("{PLAYER}", "" + victim.getName())));
        }

        // Iterate through each item dropped
        for (ItemStack item : event.getDrops()) {
            // Do nothing if item is not valid
            if (item == null || item.getType() == Material.AIR) continue;

            // Drop this item with the FBasics metadata
            Entity newItem = victim.getWorld().dropItemNaturally(victim.getLocation(), item);
            newItem.setMetadata("AntiLootSteal",
                    new FixedMetadataValue(this, killer.getName() + "-" + System.currentTimeMillis()));
        }

        // Remove all other drops as copies have already been dropped to prevent duplications
        event.getDrops().clear();
    }

}
