package com.Minecraft_server.Narkomanka.listeners;

import com.Minecraft_server.Narkomanka.Narkomanka;
import com.Minecraft_server.Narkomanka.database.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final Narkomanka plugin;

    public PlayerJoinListener(Narkomanka plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Make sure player data exists in the database
        PlayerData playerData = plugin.getPlayerService().getOrCreatePlayerData(player);

        // Update last login time
        playerData.setLastLogin(System.currentTimeMillis());
        plugin.getPlayerService().savePlayerData(playerData);

        // Setup UI for the player
        plugin.getUiManager().setupPlayerHUD(player);

        // Check if welcome message is enabled in the config
        if (plugin.getConfig().getBoolean("features.welcome-message")) {
            // Get welcome message from config and replace player placeholder
            String welcomeMessage = plugin.getConfig().getString("messages.welcome", "")
                    .replace("%player%", player.getName());

            // Convert to Adventure API component
            Component component = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(welcomeMessage);

            // Set join message
            event.joinMessage(component);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove UI for the player
        plugin.getUiManager().removePlayerHUD(player);

        // Check if goodbye message is enabled in the config
        if (plugin.getConfig().getBoolean("features.goodbye-message")) {
            // Get goodbye message from config and replace player placeholder
            String goodbyeMessage = plugin.getConfig().getString("messages.goodbye", "")
                    .replace("%player%", player.getName());

            // Convert to Adventure API component
            Component component = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(goodbyeMessage);

            // Set quit message
            event.quitMessage(component);
        }
    }
}