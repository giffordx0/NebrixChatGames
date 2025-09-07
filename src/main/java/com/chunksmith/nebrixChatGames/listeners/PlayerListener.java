
package com.chunksmith.nebrixChatGames.listeners;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.data.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

/**
 * Handles player connection events for data management
 */
public class PlayerListener implements Listener {

    private final NebrixChatGames plugin;
    private final PlayerDataManager dataManager;

    public PlayerListener(NebrixChatGames plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    /**
     * Handle player join - load their data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        try {
            // Load player data asynchronously
            dataManager.loadPlayerDataAsync(player.getUniqueId())
                    .exceptionally(throwable -> {
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to load data for player " + player.getName(), throwable);
                        return null;
                    });

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error handling player join for " + player.getName(), e);
        }
    }

    /**
     * Handle player quit - save their data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        try {
            // Save player data asynchronously
            dataManager.savePlayerDataAsync(player.getUniqueId())
                    .exceptionally(throwable -> {
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to save data for player " + player.getName(), throwable);
                        return null;
                    });

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error handling player quit for " + player.getName(), e);
        }
    }
}