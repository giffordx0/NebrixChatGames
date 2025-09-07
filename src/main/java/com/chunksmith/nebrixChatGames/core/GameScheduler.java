package com.chunksmith.nebrixChatGames.core;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages automatic game scheduling based on configuration
 * Thread-safe implementation with proper cleanup
 */
public class GameScheduler {

    private final NebrixChatGames plugin;
    private final GameEngine gameEngine;
    private final ConfigManager config;

    private volatile BukkitTask schedulerTask;
    private volatile boolean running = false;

    public GameScheduler(NebrixChatGames plugin, GameEngine gameEngine, ConfigManager config) {
        this.plugin = plugin;
        this.gameEngine = gameEngine;
        this.config = config;
    }

    /**
     * Start the automatic scheduler
     * @return true if started successfully
     */
    public boolean start() {
        if (!config.isSchedulerEnabled()) {
            plugin.getLogger().info("Game scheduler is disabled in configuration");
            return false;
        }

        if (running) {
            plugin.getLogger().warning("Game scheduler is already running");
            return false;
        }

        final long intervalTicks = config.getSchedulerInterval() * 20L; // Convert seconds to ticks

        try {
            schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::schedulerTick,
                    intervalTicks, intervalTicks);
            running = true;

            plugin.getLogger().info("Started game scheduler with interval: " + config.getSchedulerInterval() + "s");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start game scheduler: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stop the automatic scheduler
     */
    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }

        if (running) {
            running = false;
            plugin.getLogger().info("Stopped game scheduler");
        }
    }

    /**
     * Check if the scheduler is currently running
     * @return true if running
     */
    public boolean isRunning() {
        return running && schedulerTask != null && !schedulerTask.isCancelled();
    }

    /**
     * Restart the scheduler (stop and start)
     * @return true if restart was successful
     */
    public boolean restart() {
        stop();
        return start();
    }

    /**
     * Execute scheduler tick - try to start a new game
     */
    private void schedulerTick() {
        try {
            // Don't start if conditions aren't met
            if (!canStartGame()) {
                return;
            }

            // Select a game to start
            final String selectedGameId = selectRandomGame();
            if (selectedGameId == null) {
                plugin.getLogger().fine("No suitable game found for scheduling");
                return;
            }

            // Start the game
            if (gameEngine.startGame(selectedGameId)) {
                plugin.getLogger().info("Scheduler started game: " + selectedGameId);
            } else {
                plugin.getLogger().warning("Scheduler failed to start game: " + selectedGameId);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error in scheduler tick: " + e.getMessage());
        }
    }

    /**
     * Check if conditions are met to start a new game
     */
    private boolean canStartGame() {
        // Don't start if a game is already active
        if (gameEngine.isGameActive()) {
            return false;
        }

        // Check minimum players
        final int minPlayers = config.getMinPlayersOnline();
        if (Bukkit.getOnlinePlayers().size() < minPlayers) {
            return false;
        }

        return true;
    }

    /**
     * Select a random game based on configured weights
     * @return Game ID, or null if no games available
     */
    private String selectRandomGame() {
        final Map<String, Integer> gameWeights = buildGameWeights();

        if (gameWeights.isEmpty()) {
            return null;
        }

        return selectWeightedRandom(gameWeights);
    }

    /**
     * Build map of game IDs to their weights
     */
    private Map<String, Integer> buildGameWeights() {
        final Map<String, Integer> weights = new HashMap<>();
        final Set<String> configuredWeights = config.getConfiguredGameWeights();

        // Get all enabled games from registry
        final List<ChatGame> enabledGames = plugin.getGameRegistry().getEnabledGames();

        for (ChatGame game : enabledGames) {
            final String gameId = game.getId();

            // Use configured weight or default to 1
            int weight = config.getGameWeight(gameId);

            // Only include games with positive weights
            if (weight > 0) {
                weights.put(gameId, weight);
            }
        }

        return weights;
    }

    /**
     * Select a random item based on weights
     * @param weights Map of items to their weights
     * @return Selected item, or null if map is empty
     */
    private String selectWeightedRandom(Map<String, Integer> weights) {
        if (weights.isEmpty()) {
            return null;
        }

        // Calculate total weight
        final int totalWeight = weights.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalWeight <= 0) {
            return null;
        }

        // Select random value
        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);

        // Find the selected item
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            randomValue -= entry.getValue();
            if (randomValue < 0) {
                return entry.getKey();
            }
        }

        // Fallback (shouldn't happen)
        return weights.keySet().iterator().next();
    }
}