package com.chunksmith.nebrixChatGames.data;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player data with async loading/saving
 * Uses flat file storage for simplicity and compatibility
 */
public class PlayerDataManager {

    private final NebrixChatGames plugin;
    private final ConfigManager config;
    private final File dataFolder;

    // In-memory cache of player data
    private final ConcurrentHashMap<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();

    public PlayerDataManager(NebrixChatGames plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    /**
     * Initialize the data manager
     * @return true if successful
     */
    public boolean initialize() {
        try {
            // Create data folder if it doesn't exist
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().severe("Failed to create playerdata directory");
                return false;
            }

            plugin.getLogger().info("Player data manager initialized");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize player data manager", e);
            return false;
        }
    }

    /**
     * Load player data asynchronously
     * @param playerId The player's UUID
     * @return CompletableFuture with the loaded data
     */
    public CompletableFuture<PlayerData> loadPlayerDataAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadPlayerDataSync(playerId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to load data for player " + playerId, e);
                return new PlayerData(playerId);
            }
        });
    }

    /**
     * Save player data asynchronously
     * @param playerId The player's UUID
     * @return CompletableFuture for completion tracking
     */
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId) {
        final PlayerData data = playerCache.get(playerId);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                savePlayerDataSync(data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to save data for player " + playerId, e);
            }
        });
    }

    /**
     * Save all cached player data asynchronously
     * @return CompletableFuture for completion tracking
     */
    public CompletableFuture<Void> saveAllAsync() {
        return CompletableFuture.runAsync(() -> {
            plugin.getLogger().info("Saving all player data...");

            int saved = 0;
            for (PlayerData data : playerCache.values()) {
                try {
                    savePlayerDataSync(data);
                    saved++;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to save data for player " + data.getPlayerId(), e);
                }
            }

            plugin.getLogger().info("Saved data for " + saved + " players");
        });
    }

    /**
     * Get player data from cache or load it
     * @param playerId The player's UUID
     * @return Player data (never null)
     */
    public PlayerData getPlayerData(UUID playerId) {
        return playerCache.computeIfAbsent(playerId, this::loadPlayerDataSync);
    }

    /**
     * Increment wins for a player
     * @param playerId The player's UUID
     * @param gameId The game ID
     */
    public void incrementWins(UUID playerId, String gameId) {
        final PlayerData data = getPlayerData(playerId);
        data.incrementWins(gameId);
    }

    /**
     * Load player data synchronously
     */
    private PlayerData loadPlayerDataSync(UUID playerId) {
        final File playerFile = new File(dataFolder, playerId + ".yml");

        if (!playerFile.exists()) {
            return new PlayerData(playerId);
        }

        try {
            final FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            return PlayerData.fromConfig(playerId, config);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to load player data from " + playerFile.getName() +
                            ", creating new data", e);
            return new PlayerData(playerId);
        }
    }

    /**
     * Save player data synchronously
     */
    private void savePlayerDataSync(PlayerData data) throws IOException {
        final File playerFile = new File(dataFolder, data.getPlayerId() + ".yml");
        final FileConfiguration config = new YamlConfiguration();

        data.saveToConfig(config);
        config.save(playerFile);
    }
}

