package com.chunksmith.nebrixChatGames.listeners;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import com.chunksmith.nebrixChatGames.core.GameEngine;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Handles chat events for game participation
 * Supports both legacy and modern Paper chat events
 */
public class ChatListener implements Listener {

    private final NebrixChatGames plugin;
    private final GameEngine gameEngine;
    private final ConfigManager config;

    public ChatListener(NebrixChatGames plugin, GameEngine gameEngine, ConfigManager config) {
        this.plugin = plugin;
        this.gameEngine = gameEngine;
        this.config = config;
    }

    /**
     * Handle legacy AsyncPlayerChatEvent (for compatibility)
     * This runs at HIGH priority to process before other chat plugins
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        processPlayerMessage(event.getPlayer(), event.getMessage(), event);
    }

    /**
     * Handle modern Paper AsyncChatEvent
     * This runs at HIGH priority to process before other chat plugins
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        // Convert Component to plain text
        final String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        processPlayerMessage(event.getPlayer(), message, event);
    }

    /**
     * Process a player's chat message for game participation
     * @param player The player who sent the message
     * @param message The message content
     * @param event The cancellable event (either legacy or modern)
     */
    private void processPlayerMessage(Player player, String message, Object event) {
        try {
            // Only process if there's an active game
            if (!gameEngine.isGameActive()) {
                return;
            }

            // Check if player is eligible to participate
            if (!isPlayerEligible(player)) {
                return;
            }

            // Process the answer
            final boolean processed = gameEngine.processAnswer(player, message);

            // Cancel the chat event if it was processed as a correct answer
            if (processed) {
                if (event instanceof AsyncPlayerChatEvent legacyEvent) {
                    legacyEvent.setCancelled(true);
                } else if (event instanceof AsyncChatEvent modernEvent) {
                    modernEvent.setCancelled(true);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error processing chat message from " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Check if a player is eligible to participate in games
     */
    private boolean isPlayerEligible(Player player) {
        // Permission check
        if (config.requiresPermissionToPlay() && !player.hasPermission("nebrixchatgames.play")) {
            return false;
        }

        // World check
        final String worldName = player.getWorld().getName();
        if (config.getDisabledWorlds().contains(worldName)) {
            return false;
        }

        return true;
    }
}