package com.chunksmith.nebrixChatGames.commands;

import com.chunksmith.nebrixChatGames.NebrixChatGames;
import com.chunksmith.nebrixChatGames.api.ChatGame;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import com.chunksmith.nebrixChatGames.core.GameEngine;
import com.chunksmith.nebrixChatGames.core.GameRegistry;
import com.chunksmith.nebrixChatGames.core.GameScheduler;
import com.chunksmith.nebrixChatGames.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main command handler for /chatgames
 * Follows Bukkit 1.21.8 best practices with Adventure API
 */
public class ChatGamesCommand implements CommandExecutor, TabCompleter {

    private final NebrixChatGames plugin;
    private final GameEngine gameEngine;
    private final GameScheduler gameScheduler;
    private final ConfigManager config;
    private final GameRegistry gameRegistry;

    private final List<String> subcommands = Arrays.asList(
            "start", "stop", "info", "list", "reload", "toggle", "stats"
    );

    public ChatGamesCommand(NebrixChatGames plugin, GameEngine gameEngine, GameScheduler gameScheduler,
                            ConfigManager config, GameRegistry gameRegistry) {
        this.plugin = plugin;
        this.gameEngine = gameEngine;
        this.gameScheduler = gameScheduler;
        this.config = config;
        this.gameRegistry = gameRegistry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Show help if no arguments
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        final String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender);
            case "info" -> handleInfo(sender);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "toggle" -> handleToggle(sender);
            case "stats" -> handleStats(sender, args);
            default -> {
                sendMessage(sender, Component.text("Unknown subcommand: " + subcommand, NamedTextColor.RED));
                showHelp(sender);
            }
        }

        return true;
    }

    /**
     * Handle /chatgames start <game>
     */
    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nebrixchatgames.admin.start")) {
            sendMessage(sender, Component.text("You don't have permission to start games.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, Component.text("Usage: /chatgames start <game>", NamedTextColor.YELLOW));
            return;
        }

        final String gameId = args[1].toLowerCase();

        if (gameEngine.isGameActive()) {
            sendMessage(sender, Component.text("A game is already active! Use /chatgames stop first.", NamedTextColor.RED));
            return;
        }

        if (gameEngine.startGame(gameId)) {
            sendMessage(sender, Component.text("Started game: " + gameId, NamedTextColor.GREEN));
        } else {
            sendMessage(sender, Component.text("Failed to start game: " + gameId +
                    " (game not found or conditions not met)", NamedTextColor.RED));
        }
    }

    /**
     * Handle /chatgames stop
     */
    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission("nebrixchatgames.admin.stop")) {
            sendMessage(sender, Component.text("You don't have permission to stop games.", NamedTextColor.RED));
            return;
        }

        if (!gameEngine.isGameActive()) {
            sendMessage(sender, Component.text("No game is currently active.", NamedTextColor.YELLOW));
            return;
        }

        gameEngine.endCurrentGame();
        sendMessage(sender, Component.text("Current game has been stopped.", NamedTextColor.GREEN));
    }

    /**
     * Handle /chatgames info
     */
    private void handleInfo(CommandSender sender) {
        final Component prefix = TextUtils.getPrefix(plugin);

        sendMessage(sender, prefix.append(Component.text(" Plugin Information", NamedTextColor.AQUA)));
        sendMessage(sender, Component.text("Version: ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getDescription().getVersion(), NamedTextColor.WHITE)));
        sendMessage(sender, Component.text("Registered Games: ", NamedTextColor.GRAY)
                .append(Component.text(gameRegistry.getRegisteredGameCount(), NamedTextColor.WHITE)));
        sendMessage(sender, Component.text("Active Game: ", NamedTextColor.GRAY)
                .append(Component.text(gameEngine.isGameActive() ? "Yes" : "No",
                        gameEngine.isGameActive() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sendMessage(sender, Component.text("Scheduler: ", NamedTextColor.GRAY)
                .append(Component.text(config.isSchedulerEnabled() ? "Enabled" : "Disabled",
                        config.isSchedulerEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }

    /**
     * Handle /chatgames list
     */
    private void handleList(CommandSender sender) {
        final List<ChatGame> games = gameRegistry.getAllGames();

        if (games.isEmpty()) {
            sendMessage(sender, Component.text("No games are registered.", NamedTextColor.YELLOW));
            return;
        }

        sendMessage(sender, Component.text("Available Games:", NamedTextColor.AQUA));

        for (ChatGame game : games) {
            final boolean enabled = game.isEnabled();
            final Component status = Component.text(enabled ? "✓" : "✗",
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED);

            sendMessage(sender, Component.text("  ")
                    .append(status)
                    .append(Component.text(" " + game.getId(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + game.getDisplayName(), NamedTextColor.GRAY)));
        }
    }

    /**
     * Handle /chatgames reload
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nebrixchatgames.admin.reload")) {
            sendMessage(sender, Component.text("You don't have permission to reload the plugin.", NamedTextColor.RED));
            return;
        }

        sendMessage(sender, Component.text("Reloading plugin...", NamedTextColor.YELLOW));

        if (plugin.reloadPlugin()) {
            sendMessage(sender, Component.text("Plugin reloaded successfully.", NamedTextColor.GREEN));
        } else {
            sendMessage(sender, Component.text("Plugin reload failed. Check console for errors.", NamedTextColor.RED));
        }
    }

    /**
     * Handle /chatgames toggle
     */
    private void handleToggle(CommandSender sender) {
        if (!sender.hasPermission("nebrixchatgames.admin.toggle")) {
            sendMessage(sender, Component.text("You don't have permission to toggle the scheduler.", NamedTextColor.RED));
            return;
        }

        if (config.isSchedulerEnabled()) {
            gameScheduler.stop();
            sendMessage(sender, Component.text("Scheduler stopped.", NamedTextColor.YELLOW));
        } else {
            gameScheduler.start();
            sendMessage(sender, Component.text("Scheduler started.", NamedTextColor.GREEN));
        }
    }

    /**
     * Handle /chatgames stats [player]
     */
    private void handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nebrixchatgames.stats")) {
            sendMessage(sender, Component.text("You don't have permission to view stats.", NamedTextColor.RED));
            return;
        }

        // For now, just show a placeholder message
        // This would integrate with the PlayerDataManager in a full implementation
        sendMessage(sender, Component.text("Stats feature coming soon!", NamedTextColor.YELLOW));
    }

    /**
     * Show command help
     */
    private void showHelp(CommandSender sender) {
        final Component prefix = TextUtils.getPrefix(plugin);

        sendMessage(sender, prefix.append(Component.text(" Commands", NamedTextColor.AQUA)));
        sendMessage(sender, Component.text("/chatgames start <game>", NamedTextColor.AQUA)
                .append(Component.text(" - Start a specific game", NamedTextColor.GRAY)));
        sendMessage(sender, Component.text("/chatgames stop", NamedTextColor.AQUA)
                .append(Component.text(" - Stop the current game", NamedTextColor.GRAY)));
        sendMessage(sender, Component.text("/chatgames info", NamedTextColor.AQUA)
                .append(Component.text(" - Show plugin information", NamedTextColor.GRAY)));
        sendMessage(sender, Component.text("/chatgames list", NamedTextColor.AQUA)
                .append(Component.text(" - List available games", NamedTextColor.GRAY)));
        sendMessage(sender, Component.text("/chatgames reload", NamedTextColor.AQUA)
                .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        sendMessage(sender, Component.text("/chatgames toggle", NamedTextColor.AQUA)
                .append(Component.text(" - Toggle auto-scheduler", NamedTextColor.GRAY)));
        sendMessage(sender, Component.text("/chatgames stats [player]", NamedTextColor.AQUA)
                .append(Component.text(" - View player statistics", NamedTextColor.GRAY)));
    }

    /**
     * Send a message to the command sender
     */
    private void sendMessage(CommandSender sender, Component message) {
        if (sender instanceof Player player) {
            player.sendMessage(message);
        } else {
            // Convert to legacy for console
            sender.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().serialize(message));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {

        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - return matching subcommands
            final String input = args[0].toLowerCase();
            completions.addAll(subcommands.stream()
                    .filter(sub -> sub.startsWith(input))
                    .collect(Collectors.toList()));

        } else if (args.length == 2) {
            // Second argument handling
            final String subcommand = args[0].toLowerCase();
            final String input = args[1].toLowerCase();

            switch (subcommand) {
                case "start" -> {
                    // Add available game IDs
                    completions.addAll(gameRegistry.getAllGames().stream()
                            .map(ChatGame::getId)
                            .filter(gameId -> gameId.startsWith(input))
                            .collect(Collectors.toList()));
                }
                case "stats" -> {
                    // Add online player names
                    completions.addAll(plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(input))
                            .collect(Collectors.toList()));
                }
            }
        }

        return completions;
    }
}