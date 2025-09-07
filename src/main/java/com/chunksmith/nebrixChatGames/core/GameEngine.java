package com.chunksmith.nebrixChatGames.core;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import com.chunksmith.nebrixChatGames.api.events.GameEndEvent;
import com.chunksmith.nebrixChatGames.api.events.GameStartEvent;
import com.chunksmith.nebrixChatGames.api.events.PlayerAnswerEvent;
import com.chunksmith.nebrixChatGames.data.PlayerDataManager;
import com.chunksmith.nebrixChatGames.rewards.RewardManager;
import com.chunksmith.nebrixChatGames.ui.MessageManager;
import com.chunksmith.nebrixChatGames.util.RateLimiter;
import com.chunksmith.nebrixChatGames.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Core game engine that manages active game rounds
 * Thread-safe implementation following Bukkit best practices
 */
public class GameEngine {

    private final NebrixChatGames plugin;
    private final GameRegistry gameRegistry;
    private final MessageManager messageManager;
    private final RewardManager rewardManager;
    private final PlayerDataManager playerDataManager;

    // Thread-safe game state
    private final AtomicReference<ActiveGame> activeGame = new AtomicReference<>();

    // Rate limiting for players
    private final RateLimiter rateLimiter;

    // Player cooldowns to prevent spam
    private final ConcurrentHashMap<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();

    public GameEngine(NebrixChatGames plugin,
                      GameRegistry gameRegistry,
                      MessageManager messageManager,
                      RewardManager rewardManager,
                      PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.gameRegistry = gameRegistry;
        this.messageManager = messageManager;
        this.rewardManager = rewardManager;
        this.playerDataManager = playerDataManager;
        this.rateLimiter = new RateLimiter(plugin);
    }

    /**
     * Start a new game round
     * @param gameId The ID of the game to start
     * @return true if game started successfully
     */
    public boolean startGame(String gameId) {
        // Validate preconditions
        if (isGameActive()) {
            plugin.getLogger().warning("Cannot start game " + gameId + " - another game is already active");
            return false;
        }

        final Optional<ChatGame> gameOpt = gameRegistry.getGame(gameId);
        if (gameOpt.isEmpty()) {
            plugin.getLogger().warning("Cannot start game " + gameId + " - game not found");
            return false;
        }

        if (!canStartGame()) {
            plugin.getLogger().info("Cannot start game " + gameId + " - conditions not met");
            return false;
        }

        final ChatGame game = gameOpt.get();

        try {
            // Create new round
            final GameRound round = game.createRound();
            if (round == null) {
                plugin.getLogger().warning("Game " + gameId + " returned null round");
                return false;
            }

            // Create active game state
            final ActiveGame newActiveGame = new ActiveGame(game, round);

            // Set active game atomically
            if (!activeGame.compareAndSet(null, newActiveGame)) {
                plugin.getLogger().warning("Race condition detected when starting game " + gameId);
                return false;
            }

            // Fire game start event
            final GameStartEvent startEvent = new GameStartEvent(game, round);
            Bukkit.getPluginManager().callEvent(startEvent);

            if (startEvent.isCancelled()) {
                activeGame.set(null);
                plugin.getLogger().info("Game start cancelled by event listener");
                return false;
            }

            // Schedule timeout task
            scheduleTimeout(newActiveGame);

            // Broadcast game start
            messageManager.broadcastGameStart(game, round);

            plugin.getLogger().info("Started game: " + gameId + " with prompt: " + round.getPrompt());
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting game " + gameId, e);
            activeGame.set(null); // Ensure clean state
            return false;
        }
    }

    /**
     * Process a player's chat message as potential answer
     * @param player The player who sent the message
     * @param message The message content
     * @return true if the message was processed as a game answer
     */
    public boolean processAnswer(Player player, String message) {
        final ActiveGame current = activeGame.get();
        if (current == null) {
            return false; // No active game
        }

        // Check if player is eligible
        if (!isPlayerEligible(player)) {
            return false;
        }

        // Rate limiting check
        if (!rateLimiter.checkPlayer(player)) {
            return false;
        }

        final GameRound round = current.round;

        // Check warmup period
        final long timeSinceStart = System.currentTimeMillis() - round.getStartTime();
        if (timeSinceStart < round.getWarmupDuration()) {
            return false;
        }

        // Normalize the message
        final String normalizedMessage = TextUtils.normalizeAnswer(message, plugin, round.isCaseSensitive());
        if (normalizedMessage.isEmpty()) {
            return false;
        }

        // Fire player answer event
        final PlayerAnswerEvent answerEvent = new PlayerAnswerEvent(player, current.game, round, message);
        Bukkit.getPluginManager().callEvent(answerEvent);

        if (answerEvent.isCancelled()) {
            return false;
        }

        // Check if answer is correct
        final boolean correct = current.game.isCorrectAnswer(normalizedMessage, player, round);

        if (correct) {
            // End game with winner
            endGame(player);
            return true;
        }

        return false;
    }

    /**
     * End the current game
     * @param winner The winning player, or null if no winner (timeout)
     */
    public void endGame(Player winner) {
        final ActiveGame current = activeGame.getAndSet(null);
        if (current == null) {
            return; // No active game
        }

        try {
            // Cancel timeout task
            if (current.timeoutTask != null) {
                current.timeoutTask.cancel();
            }

            // Fire game end event
            final GameEndEvent endEvent = new GameEndEvent(current.game, current.round, winner);
            Bukkit.getPluginManager().callEvent(endEvent);

            // Handle rewards if there's a winner
            if (winner != null) {
                try {
                    rewardManager.grantRewards(winner, current.game, current.round);
                    playerDataManager.incrementWins(winner.getUniqueId(), current.game.getId());
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Error granting rewards to winner " + winner.getName(), e);
                }
            }

            // Broadcast game end
            messageManager.broadcastGameEnd(current.game, current.round, winner);

            // Clean up player cooldowns
            playerCooldowns.clear();

            plugin.getLogger().info("Ended game: " + current.game.getId() +
                    (winner != null ? " (winner: " + winner.getName() + ")" : " (timeout)"));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error ending game", e);
        }
    }

    /**
     * Force end the current game (used during shutdown)
     */
    public void endCurrentGame() {
        endGame(null);
    }

    /**
     * Check if a game is currently active
     * @return true if a game is active
     */
    public boolean isGameActive() {
        return activeGame.get() != null;
    }

    /**
     * Get the current active game round
     * @return Optional containing the active round, or empty if no game is active
     */
    public Optional<GameRound> getCurrentRound() {
        final ActiveGame current = activeGame.get();
        return current != null ? Optional.of(current.round) : Optional.empty();
    }

    /**
     * Get the current active game
     * @return Optional containing the active game, or empty if no game is active
     */
    public Optional<ChatGame> getCurrentGame() {
        final ActiveGame current = activeGame.get();
        return current != null ? Optional.of(current.game) : Optional.empty();
    }

    /**
     * Check if conditions are met to start a new game
     */
    private boolean canStartGame() {
        // Check minimum players
        final int minPlayers = plugin.getConfigManager().getMinPlayersOnline();
        if (Bukkit.getOnlinePlayers().size() < minPlayers) {
            return false;
        }

        // Add other conditions as needed
        return true;
    }

    /**
     * Check if a player is eligible to participate in games
     */
    private boolean isPlayerEligible(Player player) {
        // Check permission if required
        if (plugin.getConfigManager().requiresPermissionToPlay()) {
            if (!player.hasPermission("nebrixchatgames.play")) {
                return false;
            }
        }

        // Check disabled worlds
        final String worldName = player.getWorld().getName();
        if (plugin.getConfigManager().getDisabledWorlds().contains(worldName)) {
            return false;
        }

        return true;
    }

    /**
     * Schedule timeout task for the current game
     */
    private void scheduleTimeout(ActiveGame activeGame) {
        final long timeoutTicks = activeGame.round.getTimeoutDuration() / 50L; // Convert ms to ticks

        activeGame.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Check if this game is still active (avoid race conditions)
            if (this.activeGame.get() == activeGame) {
                endGame(null); // Timeout with no winner
            }
        }, Math.max(1L, timeoutTicks));
    }

    /**
     * Inner class to represent an active game state
     */
    private static class ActiveGame {
        final ChatGame game;
        final GameRound round;
        volatile BukkitTask timeoutTask;

        ActiveGame(ChatGame game, GameRound round) {
            this.game = game;
            this.round = round;
        }
    }
}