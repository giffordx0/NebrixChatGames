package com.chunksmith.nebrixChatGames.core;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing chat games
 * Thread-safe implementation following Bukkit best practices
 */
public class GameRegistry {

    private final NebrixChatGames plugin;
    private final Map<String, ChatGame> games = new ConcurrentHashMap<>();

    public GameRegistry(NebrixChatGames plugin) {
        this.plugin = plugin;
    }

    /**
     * Register a chat game
     *
     * @param game The game to register
     * @return true if registration was successful
     */
    public boolean registerGame(ChatGame game) {
        if (game == null) {
            plugin.getLogger().warning("Attempted to register null game");
            return false;
        }

        final String gameId = game.getId();
        if (gameId == null || gameId.isEmpty()) {
            plugin.getLogger().warning("Attempted to register game with null/empty ID");
            return false;
        }

        // Validate game ID format
        if (!isValidGameId(gameId)) {
            plugin.getLogger().warning("Invalid game ID format: " + gameId +
                    " (must be lowercase alphanumeric with hyphens only)");
            return false;
        }

        // Check for duplicates
        if (games.containsKey(gameId)) {
            plugin.getLogger().warning("Game with ID '" + gameId + "' is already registered");
            return false;
        }

        games.put(gameId, game);
        plugin.getLogger().info("Registered game: " + gameId + " (" + game.getDisplayName() + ")");
        return true;
    }

    /**
     * Unregister a chat game
     *
     * @param gameId The ID of the game to unregister
     * @return true if a game was removed
     */
    public boolean unregisterGame(String gameId) {
        final ChatGame removed = games.remove(gameId);
        if (removed != null) {
            plugin.getLogger().info("Unregistered game: " + gameId);
            return true;
        }
        return false;
    }

    /**
     * Get a game by ID
     *
     * @param gameId The game ID
     * @return Optional containing the game, or empty if not found
     */
    public Optional<ChatGame> getGame(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    /**
     * Get all registered games
     *
     * @return Unmodifiable list of all games
     */
    public List<ChatGame> getAllGames() {
        return Collections.unmodifiableList(new ArrayList<>(games.values()));
    }

    /**
     * Get all enabled games
     *
     * @return List of enabled games only
     */
    public List<ChatGame> getEnabledGames() {
        return games.values().stream()
                .filter(ChatGame::isEnabled)
                .toList();
    }

    /**
     * Get the number of registered games
     *
     * @return Game count
     */
    public int getRegisteredGameCount() {
        return games.size();
    }

    /**
     * Get the number of enabled games
     *
     * @return Enabled game count
     */
    public int getEnabledGameCount() {
        return (int) games.values().stream()
                .filter(ChatGame::isEnabled)
                .count();
    }

    /**
     * Clear all registered games
     */
    public void clearGames() {
        games.clear();
        plugin.getLogger().info("Cleared all registered games");
    }

    /**
     * Check if a game ID exists
     *
     * @param gameId The game ID to check
     * @return true if the game is registered
     */
    public boolean hasGame(String gameId) {
        return games.containsKey(gameId);
    }

    /**
     * Validate game ID format
     * Must be lowercase alphanumeric with hyphens only
     */
    private boolean isValidGameId(String gameId) {
        return gameId.matches("^[a-z0-9-]+$");
    }
}