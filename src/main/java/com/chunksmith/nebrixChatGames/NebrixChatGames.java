package com.chunksmith.nebrixChatGames;

import com.chunksmith.nebrixChatGames.command.ChatGamesCommand;
import com.chunksmith.nebrixChatGames.config.ConfigManager;
import com.chunksmith.nebrixChatGames.core.GameEngine;
import com.chunksmith.nebrixChatGames.core.GameRegistry;
import com.chunksmith.nebrixChatGames.core.GameScheduler;
import com.chunksmith.nebrixChatGames.data.PlayerDataManager;
import com.chunksmith.nebrixChatGames.games.MathGame;
import com.chunksmith.nebrixChatGames.games.ReactionGame;
import com.chunksmith.nebrixChatGames.games.UnscrambleGame;
import com.chunksmith.nebrixChatGames.integration.EconomyIntegration;
import com.chunksmith.nebrixChatGames.listeners.ChatListener;
import com.chunksmith.nebrixChatGames.listeners.PlayerListener;
import com.chunksmith.nebrixChatGames.rewards.RewardManager;
import com.chunksmith.nebrixChatGames.ui.MessageManager;
import com.chunksmith.nebrixChatGames.util.WordProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Main plugin class for Nebrix ChatGames
 * A modern chat-based minigame system for Minecraft servers
 *
 * @author Chunksmith
 * @version 1.0.0
 * @since 1.21.8
 */
public final class NebrixChatGames extends JavaPlugin {

    // Core managers - proper dependency injection pattern
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PlayerDataManager playerDataManager;
    private WordProvider wordProvider;
    private EconomyIntegration economyIntegration;
    private RewardManager rewardManager;

    // Game system components
    private GameRegistry gameRegistry;
    private GameEngine gameEngine;
    private GameScheduler gameScheduler;

    // Listeners
    private ChatListener chatListener;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        final long startTime = System.currentTimeMillis();

        try {
            getLogger().info("Starting Nebrix ChatGames v" + getDescription().getVersion());

            // Initialize core systems in proper order
            if (!initializeCoreManagers()) {
                disablePlugin("Failed to initialize core managers");
                return;
            }

            // Initialize game system
            if (!initializeGameSystem()) {
                disablePlugin("Failed to initialize game system");
                return;
            }

            // Register games
            registerBuiltInGames();

            // Register commands and events
            if (!registerCommandsAndEvents()) {
                disablePlugin("Failed to register commands and events");
                return;
            }

            // Start scheduler if enabled
            if (configManager.isSchedulerEnabled()) {
                gameScheduler.start();
            }

            final long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("Successfully enabled Nebrix ChatGames in " + loadTime + "ms");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Unexpected error during plugin initialization", e);
            disablePlugin("Unexpected initialization error");
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("Disabling Nebrix ChatGames...");

            // Stop scheduler gracefully
            if (gameScheduler != null) {
                gameScheduler.stop();
            }

            // End any active games
            if (gameEngine != null) {
                gameEngine.endCurrentGame();
            }

            // Save all player data asynchronously with timeout
            if (playerDataManager != null) {
                final CompletableFuture<Void> saveTask = playerDataManager.saveAllAsync();
                try {
                    // Wait up to 5 seconds for save to complete
                    saveTask.get(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Failed to save all player data before shutdown", e);
                }
            }

            // Cleanup economy integration
            if (economyIntegration != null) {
                economyIntegration.cleanup();
            }

            getLogger().info("Nebrix ChatGames disabled successfully");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }

    /**
     * Initialize core manager components
     * @return true if successful, false otherwise
     */
    private boolean initializeCoreManagers() {
        try {
            getLogger().info("Initializing core managers...");

            // Configuration must be loaded first
            this.configManager = new ConfigManager(this);
            if (!configManager.loadConfig()) {
                getLogger().severe("Failed to load configuration");
                return false;
            }

            // Message system depends on config
            this.messageManager = new MessageManager(this, configManager);

            // Data management
            this.playerDataManager = new PlayerDataManager(this, configManager);
            if (!playerDataManager.initialize()) {
                getLogger().severe("Failed to initialize player data manager");
                return false;
            }

            // Word provider
            this.wordProvider = new WordProvider(this);
            wordProvider.loadWords();

            // Economy integration (optional)
            this.economyIntegration = new EconomyIntegration(this);
            economyIntegration.initialize();

            // Reward system depends on economy
            this.rewardManager = new RewardManager(this, configManager, economyIntegration);

            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core managers", e);
            return false;
        }
    }

    /**
     * Initialize game system components
     * @return true if successful, false otherwise
     */
    private boolean initializeGameSystem() {
        try {
            getLogger().info("Initializing game system...");

            // Game registry
            this.gameRegistry = new GameRegistry(this);

            // Game engine - the heart of the system
            this.gameEngine = new GameEngine(this, gameRegistry, messageManager, rewardManager, playerDataManager);

            // Game scheduler
            this.gameScheduler = new GameScheduler(this, gameEngine, configManager);

            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize game system", e);
            return false;
        }
    }

    /**
     * Register built-in games with the registry
     */
    private void registerBuiltInGames() {
        getLogger().info("Registering built-in games...");

        try {
            // Register games based on config
            if (configManager.isGameEnabled("unscramble")) {
                gameRegistry.registerGame(new UnscrambleGame(this, wordProvider));
            }

            if (configManager.isGameEnabled("reaction")) {
                gameRegistry.registerGame(new ReactionGame(this, wordProvider));
            }

            if (configManager.isGameEnabled("math")) {
                gameRegistry.registerGame(new MathGame(this));
            }

            getLogger().info("Registered " + gameRegistry.getRegisteredGameCount() + " games");

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error registering some games", e);
        }
    }

    /**
     * Register commands and event listeners
     * @return true if successful, false otherwise
     */
    private boolean registerCommandsAndEvents() {
        try {
            getLogger().info("Registering commands and events...");

            // Register main command
            final PluginCommand mainCommand = getCommand("cg");
            if (mainCommand != null) {
                final ChatGamesCommand commandExecutor = new ChatGamesCommand(
                        this, gameEngine, gameScheduler, configManager, gameRegistry);
                mainCommand.setExecutor(commandExecutor);
                mainCommand.setTabCompleter(commandExecutor);
            } else {
                getLogger().warning("Main command 'chatgames' not found in plugin.yml");
                return false;
            }

            // Register event listeners
            final PluginManager pluginManager = getServer().getPluginManager();

            this.chatListener = new ChatListener(this, gameEngine, configManager);
            pluginManager.registerEvents(chatListener, this);

            this.playerListener = new PlayerListener(this, playerDataManager);
            pluginManager.registerEvents(playerListener, this);

            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register commands and events", e);
            return false;
        }
    }

    /**
     * Safely disable the plugin with a specific reason
     * @param reason The reason for disabling
     */
    private void disablePlugin(String reason) {
        getLogger().severe("Disabling plugin: " + reason);
        getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Reload the plugin configuration and dependent systems
     * @return true if reload was successful
     */
    public boolean reloadPlugin() {
        try {
            getLogger().info("Reloading Nebrix ChatGames...");

            // Stop scheduler during reload
            if (gameScheduler != null) {
                gameScheduler.stop();
            }

            // End current game
            if (gameEngine != null) {
                gameEngine.endCurrentGame();
            }

            // Reload configuration
            if (!configManager.loadConfig()) {
                getLogger().warning("Failed to reload configuration");
                return false;
            }

            // Reload word provider
            wordProvider.loadWords();

            // Re-register games based on new config
            gameRegistry.clearGames();
            registerBuiltInGames();

            // Restart scheduler if enabled
            if (configManager.isSchedulerEnabled()) {
                gameScheduler.start();
            }

            getLogger().info("Plugin reloaded successfully");
            return true;

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during plugin reload", e);
            return false;
        }
    }

    // Getters for dependency injection (package-private for better encapsulation)
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public GameRegistry getGameRegistry() {
        return gameRegistry;
    }

    public GameEngine getGameEngine() {
        return gameEngine;
    }

    public GameScheduler getGameScheduler() {
        return gameScheduler;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public EconomyIntegration getEconomyIntegration() {
        return economyIntegration;
    }

    public WordProvider getWordProvider() {
        return wordProvider;
    }
}