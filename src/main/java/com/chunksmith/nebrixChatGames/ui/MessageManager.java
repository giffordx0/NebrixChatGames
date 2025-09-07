package com.chunksmith.nebrixChatGames.ui;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.api.GameRound;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import com.chunksmith.nebrixChatGames.util.TextUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.logging.Level;

/**
 * Manages all UI messaging and notifications
 * Uses Adventure API for modern Minecraft 1.21.8 compatibility
 */
public class MessageManager {

    private final NebrixChatGames plugin;
    private final ConfigManager config;

    public MessageManager(NebrixChatGames plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Broadcast game start to all players
     */
    public void broadcastGameStart(ChatGame game, GameRound round) {
        final Component prefix = TextUtils.getPrefix(plugin);
        final Component message = prefix
                .append(Component.text(" New ", NamedTextColor.GRAY))
                .append(Component.text(game.getDisplayName(), NamedTextColor.AQUA))
                .append(Component.text(" game: ", NamedTextColor.GRAY))
                .append(Component.text(round.getPrompt(), NamedTextColor.WHITE))
                .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                .append(Component.text(round.getTimeoutDuration() / 1000 + "s", NamedTextColor.YELLOW))
                .append(Component.text(")", NamedTextColor.DARK_GRAY));

        // Broadcast message
        Bukkit.getServer().broadcast(message);

        // Play sound if enabled
        if (config.shouldPlaySounds()) {
            playGameStartSound();
        }

        // Show titles if enabled
        if (config.shouldShowTitles()) {
            showGameStartTitle(game, round);
        }
    }

    /**
     * Broadcast game end to all players
     */
    public void broadcastGameEnd(ChatGame game, GameRound round, Player winner) {
        final Component prefix = TextUtils.getPrefix(plugin);

        final Component message;
        if (winner != null) {
            message = prefix
                    .append(Component.text(winner.getName(), NamedTextColor.GREEN))
                    .append(Component.text(" won the ", NamedTextColor.GRAY))
                    .append(Component.text(game.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(" game! Answer: ", NamedTextColor.GRAY))
                    .append(Component.text(round.getCorrectAnswer(), NamedTextColor.YELLOW));
        } else {
            message = prefix
                    .append(Component.text("Time's up! The ", NamedTextColor.GRAY))
                    .append(Component.text(game.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(" answer was: ", NamedTextColor.GRAY))
                    .append(Component.text(round.getCorrectAnswer(), NamedTextColor.YELLOW));
        }

        Bukkit.getServer().broadcast(message);
    }

    /**
     * Play game start sound to all players
     */
    private void playGameStartSound() {
        try {
            final String soundKey = config.getStartSound();
            final Sound sound = Sound.sound()
                    .type(org.bukkit.Sound.valueOf(soundKey.toUpperCase().replace(".", "_")))
                    .volume(1.0f)
                    .pitch(1.0f)
                    .build();

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(sound);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to play game start sound", e);
        }
    }

    /**
     * Show game start title to all players
     */
    private void showGameStartTitle(ChatGame game, GameRound round) {
        try {
            final Component titleMain = TextUtils.parseMessage(config.getTitleMain());
            final Component titleSub = TextUtils.parseMessage(
                    config.getTitleSubtitle(game.getId()).replace("%prompt%", round.getPrompt())
            );

            final Title title = Title.title(
                    titleMain,
                    titleSub,
                    Title.Times.times(
                            Duration.ofMillis(config.getTitleFadeIn()),
                            Duration.ofMillis(config.getTitleStay()),
                            Duration.ofMillis(config.getTitleFadeOut())
                    )
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(title);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to show game start title", e);
        }
    }

    /**
     * Send a message to a specific player
     */
    public void sendMessage(Player player, Component message) {
        player.sendMessage(message);
    }

    /**
     * Send a prefixed message to a player
     */
    public void sendPrefixedMessage(Player player, String message) {
        final Component prefix = TextUtils.getPrefix(plugin);
        final Component fullMessage = prefix.append(Component.space()).append(TextUtils.parseMessage(message));
        player.sendMessage(fullMessage);
    }
}
