package com.chunksmith.nebrixChatGames.util;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter to prevent chat spam and bot abuse
 * Thread-safe implementation with automatic cleanup
 */
public class RateLimiter {

    private final NebrixChatGames plugin;
    private final ConcurrentHashMap<UUID, PlayerRateData> playerData;
    private volatile long lastCleanup;

    // Cleanup interval in milliseconds
    private static final long CLEANUP_INTERVAL = 60000L; // 1 minute

    public RateLimiter(NebrixChatGames plugin) {
        this.plugin = plugin;
        this.playerData = new ConcurrentHashMap<>();
        this.lastCleanup = System.currentTimeMillis();
    }

    /**
     * Check if a player is within rate limits
     * @param player The player to check
     * @return true if the player can send a message
     */
    public boolean checkPlayer(Player player) {
        final long now = System.currentTimeMillis();
        final UUID playerId = player.getUniqueId();

        // Perform periodic cleanup
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            cleanup(now);
        }

        // Get or create player rate data
        final PlayerRateData data = playerData.computeIfAbsent(playerId,
                k -> new PlayerRateData(now));

        return data.checkAndUpdate(now, plugin.getConfigManager().getRateLimit());
    }

    /**
     * Clean up old rate limit data
     * @param now Current timestamp
     */
    private void cleanup(long now) {
        lastCleanup = now;

        // Remove entries older than 5 seconds
        playerData.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > 5000L);
    }

    /**
     * Get current message count for a player (for debugging)
     * @param player The player
     * @return Current message count in the rate limit window
     */
    public int getMessageCount(Player player) {
        final PlayerRateData data = playerData.get(player.getUniqueId());
        return data != null ? data.messageCount : 0;
    }

    /**
     * Reset rate limit for a player (admin command use)
     * @param player The player to reset
     */
    public void resetPlayer(Player player) {
        playerData.remove(player.getUniqueId());
    }

    /**
     * Thread-safe rate limiting data for individual players
     */
    private static class PlayerRateData {
        private volatile long windowStart;
        private volatile int messageCount;

        PlayerRateData(long windowStart) {
            this.windowStart = windowStart;
            this.messageCount = 0;
        }

        synchronized boolean checkAndUpdate(long now, int maxMessages) {
            // Reset window if it's been more than 1 second
            if (now - windowStart > 1000L) {
                windowStart = now;
                messageCount = 0;
            }

            // Check if within limits
            if (messageCount >= maxMessages) {
                return false;
            }

            // Increment counter
            messageCount++;
            return true;
        }
    }
}