package com.chunksmith.nebrixChatGames.core;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import com.chunksmith.nebrixChatGames.api.Round;
import com.chunksmith.nebrixChatGames.service.RewardService;
import com.chunksmith.nebrixChatGames.util.Text;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class GameManager implements Listener {
    private final NebrixChatGames plugin;
    private final RewardService rewards;
    private final Map<String, ChatGame> games = new HashMap<>();

    private volatile GameRound currentRound;
    private volatile UUID winner;

    private final Map<UUID, Integer> rateCounter = new ConcurrentHashMap<>();
    private volatile long lastRateWindow = System.currentTimeMillis();

    public GameManager(NebrixChatGames plugin, RewardService rewards) {
        this.plugin = plugin;
        this.rewards = rewards;
    }

    public void register(ChatGame game) {
        games.put(game.getId(), game);
    }

    public Collection<ChatGame> allGames() {
        return Collections.unmodifiableCollection(games.values());
    }

    public boolean start(String gameId) {
        final ChatGame game = games.get(gameId);
        if (game == null) return false;
        if (currentRound != null) return false;
        if (!EnvChecks.canStart(plugin)) return false;

        final GameRound round = game.createRound();
        this.currentRound = round;
        this.winner = null;

        broadcastStart(round);

        // Timeout task
        final long timeoutTicks = Math.max(20L, (round.getTimeoutDuration() / 50L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (currentRound == round && winner == null) {
                endRound(null);
            }
        }, timeoutTicks);

        return true;
    }

    public void stop() {
        endRound(null);
    }

    private void endRound(Player winner) {
        final GameRound round = this.currentRound;
        this.currentRound = null;
        this.winner = winner == null ? null : winner.getUniqueId();

        if (round == null) return;

        final String prefix = Text.prefix(plugin);

        if (winner != null) {
            try {
                // Create legacy Round object for RewardService compatibility
                final Round legacyRound = new Round(
                        round.getGameId(),
                        round.getPrompt(),
                        round.getCorrectAnswer(),
                        round.getTimeoutDuration(),
                        round.getWarmupDuration(),
                        round.isCaseSensitive()
                );
                rewards.grantWinner(winner, legacyRound);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to grant rewards to winner: " + winner.getName(), e);
            }

            final String winMessage = prefix + "§a" + winner.getName() +
                    " §7won §b" + round.getGameId() + "§7! Answer: §e" + round.getCorrectAnswer();
            Bukkit.broadcastMessage(winMessage);
        } else {
            final String timeoutMessage = prefix + "§7No one solved §b" +
                    round.getGameId() + "§7. Answer: §e" + round.getCorrectAnswer();
            Bukkit.broadcastMessage(timeoutMessage);
        }
    }

    private void broadcastStart(GameRound round) {
        final String message = Text.prefix(plugin) + "§7New game: §b" + round.getGameId() +
                "§7 • §f" + round.getPrompt() + " §8(§e" + (round.getTimeoutDuration() / 1000) + "s§8)";

        Bukkit.broadcastMessage(message);

        if (plugin.getConfig().getBoolean("settings.round.play-sound", true)) {
            playStartSound();
        }
    }

    private void playStartSound() {
        final String soundKey = plugin.getConfig()
                .getString("settings.round.sound", "entity.player.levelup");

        try {
            // Convert config string to proper key format
            final net.kyori.adventure.key.Key soundKeyObj = net.kyori.adventure.key.Key.key(
                    "minecraft", soundKey.toLowerCase().replace("_", ".")
            );

            // Use modern Adventure Sound API
            final Sound sound = Sound.sound()
                    .type(soundKeyObj)
                    .volume(1.0f)
                    .pitch(1.0f)
                    .build();

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(sound);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound key in config: " + soundKey +
                    ". Using default sound.");

            // Fallback to default sound using key
            try {
                final Sound fallbackSound = Sound.sound()
                        .type(net.kyori.adventure.key.Key.key("minecraft", "entity.player.levelup"))
                        .volume(1.0f)
                        .pitch(1.0f)
                        .build();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(fallbackSound);
                }
            } catch (Exception fallbackException) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to play fallback sound", fallbackException);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        final GameRound currentGameRound = this.currentRound;
        if (currentGameRound == null) return;

        final Player player = event.getPlayer();
        if (!EnvChecks.playerEligible(plugin, player)) return;

        // Anti-bot warmup period
        final long timeSinceStart = System.currentTimeMillis() - currentGameRound.getStartTime();
        if (timeSinceStart < currentGameRound.getWarmupDuration()) return;

        // Rate limiting
        if (!passesRateLimit(player)) return;

        final String normalizedMessage = Text.normalize(event.getMessage(), plugin, currentGameRound.isCaseSensitive());
        if (normalizedMessage.isEmpty()) return;

        final ChatGame game = games.get(currentGameRound.getGameId());
        if (game != null && game.isCorrectAnswer(normalizedMessage, player, currentGameRound)) {
            event.setCancelled(true);

            // Schedule the win processing on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> endRound(player));
        }
    }

    private boolean passesRateLimit(Player player) {
        final long now = System.currentTimeMillis();

        // Reset rate limit window if needed
        if (now - lastRateWindow > 1000) {
            rateCounter.clear();
            lastRateWindow = now;
        }

        final int currentCount = rateCounter.merge(player.getUniqueId(), 1, Integer::sum);
        final int maxMessagesPerSecond = plugin.getConfig()
                .getInt("settings.anti-cheat.rate-limit-per-sec", 3);

        return currentCount <= maxMessagesPerSecond;
    }

    /**
     * Gets the current active round, if any
     */
    public Optional<GameRound> getCurrentRound() {
        return Optional.ofNullable(currentRound);
    }

    /**
     * Checks if a game is currently active
     */
    public boolean isGameActive() {
        return currentRound != null;
    }

    /**
     * Gets the winner of the last completed round, if any
     */
    public Optional<UUID> getLastWinner() {
        return Optional.ofNullable(winner);
    }
}